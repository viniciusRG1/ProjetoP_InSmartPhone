package com.example.projetop.back.IA

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Deteccao(
    val label: String,
    val conf: Float,
    val box: RectF
)

class motorDeDeteccao(
    context: Context,
    private val confThreshold: Float = 0.4f,
    private val iouThreshold: Float = 0.3f
) {
    private val env = OrtEnvironment.getEnvironment()
    private val session = env.createSession(context.assets.open("best.onnx").readBytes())
    private val inputName = session.inputNames.iterator().next()
    private val classes = context.assets.open("classes.txt")
        .bufferedReader()
        .readLines()
        .filter { it.isNotBlank() }

    fun detectar(bitmap: Bitmap): List<Deteccao> {
        val (padded, scale, padX, padY) = aplicarLetterbox(bitmap, 640)
        val area = 640 * 640
        val pixels = IntArray(area)
        padded.getPixels(pixels, 0, 640, 0, 0, 640, 640)

        val byteBuffer = ByteBuffer.allocateDirect(3 * area * 4).order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()

        for (i in 0 until area) {
            val px = pixels[i]
            floatBuffer.put(i, ((px shr 16) and 0xFF) / 255f)
            floatBuffer.put(area + i, ((px shr 8) and 0xFF) / 255f)
            floatBuffer.put(2 * area + i, (px and 0xFF) / 255f)
        }
        floatBuffer.rewind()

        val tensor = OnnxTensor.createTensor(env, floatBuffer, longArrayOf(1, 3, 640, 640))
        val results = session.run(mapOf(inputName to tensor))

        @Suppress("UNCHECKED_CAST")
        val data = (results[0].value as Array<Array<FloatArray>>)[0]
        val numClasses = data.size - 4
        val numAnchors = data[0].size

        val newW = bitmap.width * scale
        val newH = bitmap.height * scale

        var maiorNota = 0f
        val candidatos = mutableListOf<Deteccao>()

        for (i in 0 until numAnchors) {
            var maxScore = 0f
            var classId = -1
            for (c in 0 until numClasses) {
                val score = data[4 + c][i]
                if (score > maxScore) {
                    maxScore = score
                    classId = c
                }
            }

            if (maxScore > maiorNota) maiorNota = maxScore

            if (maxScore >= confThreshold && classId in classes.indices) {
                val cx = data[0][i]
                val cy = data[1][i]
                val w = data[2][i]
                val h = data[3][i]

                val left = ((cx - w / 2f - padX) / newW).coerceIn(0f, 1f)
                val top = ((cy - h / 2f - padY) / newH).coerceIn(0f, 1f)
                val right = ((cx + w / 2f - padX) / newW).coerceIn(0f, 1f)
                val bottom = ((cy + h / 2f - padY) / newH).coerceIn(0f, 1f)

                candidatos.add(
                    Deteccao(
                        label = classes[classId],
                        conf = maxScore,
                        box = RectF(left, top, right, bottom)
                    )
                )
            }
        }

        Log.d("YOLO_TESTE", "Maior nota: $maiorNota | Encontrados: ${candidatos.size}")

        tensor.close()
        results.close()
        return aplicarNMS(candidatos)
    }

    private fun aplicarLetterbox(src: Bitmap, target: Int): LetterboxResult {
        val scale = minOf(target.toFloat() / src.width, target.toFloat() / src.height)
        val newW = (src.width * scale).toInt()
        val newH = (src.height * scale).toInt()
        val padX = (target - newW) / 2f
        val padY = (target - newH) / 2f

        val resized = Bitmap.createScaledBitmap(src, newW, newH, true)
        val output = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(resized, padX, padY, null)

        return LetterboxResult(output, scale, padX, padY)
    }

    private data class LetterboxResult(
        val bitmap: Bitmap,
        val scale: Float,
        val padX: Float,
        val padY: Float
    )

    private fun aplicarNMS(lista: List<Deteccao>): List<Deteccao> {
        val ordenadas = lista.sortedByDescending { it.conf }.toMutableList()
        val final = mutableListOf<Deteccao>()

        while (ordenadas.isNotEmpty()) {
            val atual = ordenadas.removeAt(0)
            final.add(atual)
            ordenadas.removeAll { calcularIoU(atual.box, it.box) > iouThreshold }
        }
        return final
    }

    private fun calcularIoU(a: RectF, b: RectF): Float {
        val l = maxOf(a.left, b.left)
        val t = maxOf(a.top, b.top)
        val r = minOf(a.right, b.right)
        val bot = minOf(a.bottom, b.bottom)

        val inter = maxOf(0f, r - l) * maxOf(0f, bot - t)
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        val union = areaA + areaB - inter

        return if (union <= 0f) 0f else inter / union
    }

    fun fechar() {
        session.close()
    }
}