package com.example.projetop.front.telas

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.projetop.R
import com.example.projetop.back.IA.Deteccao
import com.example.projetop.back.IA.motorDeDeteccao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Size
import android.view.MotionEvent
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun camera(onVoltar: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val detector = remember { motorDeDeteccao(context) }

    val resolutionSelector = remember{
        ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(ResolutionStrategy(
                Size(1280,960),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )).build()
    }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolutionSelector).build()
    }

    var fotoMarcada by remember { mutableStateOf<Bitmap?>(null) }
    var processando by remember { mutableStateOf(false) }
    var deteccoesAtuais by remember { mutableStateOf<List<Deteccao>>(emptyList()) }

    DisposableEffect(Unit) {
        onDispose { detector.fechar() }
    }

    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { concedida -> temPermissao = concedida }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            processando = true
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { uriParaBitmap(context, it) }
                if (bmp != null) {
                    fotoMarcada = bmp
                    val (resultado, deteccoes) = withContext(Dispatchers.Default) {
                        val lista = detector.detectar(bmp)
                        Pair(desenharCaixas(bmp, lista), lista)
                    }
                    deteccoesAtuais = deteccoes
                    fotoMarcada = resultado
                }
                processando = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!temPermissao) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        bottomBar = {
            if (!processando) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFF87CEFF))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fotoMarcada == null) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }, enabled = !processando) {
                            Icon(
                                painter = painterResource(id = R.drawable.gallery_icon),
                                contentDescription = "Galeria",
                                tint = Color.Black
                            )
                        }

                        IconButton(
                            enabled = temPermissao && !processando,
                            onClick = {
                                processando = true
                                imageCapture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmapRotacionado = image.toBitmapRotacionado()
                                            image.close()
                                            fotoMarcada = bitmapRotacionado

                                            scope.launch {
                                                val (resultado, deteccoes) = withContext(Dispatchers.Default) {
                                                    val lista = detector.detectar(bitmapRotacionado)
                                                    Pair(desenharCaixas(bitmapRotacionado, lista), lista)
                                                }
                                                deteccoesAtuais = deteccoes
                                                fotoMarcada = resultado
                                                processando = false
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                            processando = false
                                        }
                                    }
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.camera_icon),
                                contentDescription = "Tirar foto",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(onClick = onVoltar, enabled = !processando) {
                            Icon(
                                painter = painterResource(id = R.drawable.back_icon),
                                contentDescription = "Voltar",
                                tint = Color.Black
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                fotoMarcada = null
                                deteccoesAtuais = emptyList()
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.camera_icon),
                                    contentDescription = "Tirar outra foto",
                                    tint = Color.Black
                                )
                                Text(
                                    text = "Outra foto",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        TextButton(onClick = onVoltar) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.back_icon),
                                    contentDescription = "Tela inicial",
                                    tint = Color.Black
                                )
                                Text(
                                    text = "Tela inicial",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (fotoMarcada != null) {
                Image(
                    bitmap = fotoMarcada!!.asImageBitmap(),
                    contentDescription = "Foto processada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                if (!processando) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.9f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                            Text(
                                text = "Identificados:",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            if (deteccoesAtuais.isEmpty()) {
                                Text(text = "Nenhum produto encontrado.", color = Color.DarkGray)
                            } else {
                                val contagem = deteccoesAtuais.groupingBy { it.label }.eachCount()
                                contagem.forEach { (produto, qtd) ->
                                    Text(text = "• ${qtd}x $produto", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            } else if (temPermissao) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FIT_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder()
                                .setResolutionSelector(resolutionSelector)
                                .build()
                                .also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview, imageCapture
                                )
                                previewView.setOnTouchListener { view, event ->
                                    if(event.action == MotionEvent.ACTION_UP){
                                        val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                            .setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                        camera.cameraControl.startFocusAndMetering(action)
                                        view.performClick()
                                    }
                                    true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (processando) {
                CircularProgressIndicator(color = Color(0xFF87CEFF))
            }
        }
    }
}

private fun uriParaBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun ImageProxy.toBitmapRotacionado(): Bitmap {
    val bitmap = toBitmap()
    val graus = imageInfo.rotationDegrees.toFloat()
    if (graus == 0f) return bitmap

    val matrix = Matrix().apply { postRotate(graus) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun desenharCaixas(original: Bitmap, deteccoes: List<Deteccao>): Bitmap {
    val copia = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(copia)

    val paintCaixa = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = (copia.width * 0.006f).coerceAtLeast(4f)
    }

    for (det in deteccoes) {
        paintCaixa.color = corDaClasse(det.label)

        val left = det.box.left * copia.width
        val top = det.box.top * copia.height
        val right = det.box.right * copia.width
        val bottom = det.box.bottom * copia.height

        canvas.drawRect(left, top, right, bottom, paintCaixa)
    }

    return copia
}

private fun corDaClasse(classe: String): Int {
    val hue = ((classe.hashCode() * 137) and 0x7FFFFFFF) % 360
    return android.graphics.Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.9f, 0.95f))
}