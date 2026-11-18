package com.example.projetop.front.telas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projetop.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.toColor(): Color {
    return Color(android.graphics.Color.parseColor(this))
}

data class VisitaPontos(
    val id: Int,
    val local: String,
    val observacao: String,
    val nota: Float,
    val endereco: String,
    val imagem: Int,
    val data: String = "29/10/2026"
)

val listaTeste = List(20) { index ->
    VisitaPontos(
        id = index,
        local = "$index",
        observacao = "$index",
        nota = index.toFloat(),
        endereco = "$index",
        imagem = R.drawable.buscador_icon
    )
}

@Composable
fun CardVisita(
    local: String,
    observacao: String,
    nota: Float,
    endereco: String,
    imagem: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = "#FFFFC5".toColor()
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "FOTO",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = "#000000".toColor()
            )

            Image(
                painter = painterResource(id = imagem),
                contentDescription = "Foto do local",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ENDEREÇO",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = "#000000".toColor()
            )
            Text(
                text = endereco,
                fontSize = 12.sp,
                color = "#555555".toColor()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "LOCAL",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = "#000000".toColor()
            )
            Text(
                text = local,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = "#000000".toColor()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Nota: $nota",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = "#000000".toColor()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "OBSERVAÇÃO",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = "#000000".toColor()
            )
            Text(
                text = observacao,
                fontSize = 12.sp,
                color = "#555555".toColor()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun home(onAbrirCamera: () -> Unit) {
    var buscador by remember { mutableStateOf("") }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var dataSelecionada by remember { mutableStateOf("Filtrar Data") }
    val datePickerState = rememberDatePickerState()

    val listaVisitas = listOf(
        VisitaPontos(
            id = 1,
            local = "Supermercado Central",
            observacao = "Tudo organizado no corredor.",
            nota = 9.5f,
            endereco = "Av. Brasil, 1500",
            imagem = R.drawable.teste1
        ),
        VisitaPontos(
            id = 2,
            local = "Bar do Seu Zé",
            observacao = "Zap fora do lugar.",
            nota = 6.0f,
            endereco = "Rua Minas Gerais, 42",
            imagem = R.drawable.teste2
        ),
        VisitaPontos(
            id = 3,
            local = "Mercearia Santos",
            observacao = "Falta refrigerante na prateleira.",
            nota = 5.0f,
            endereco = "Rua Floriano Peixoto, 300",
            imagem = R.drawable.teste3
        )
    )

    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    mostrarCalendario = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        dataSelecionada = formato.format(Date(millis))
                    }
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background("#87CEFF".toColor())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                TextField(
                    value = buscador,
                    onValueChange = { buscador = it },
                    placeholder = { Text("Pesquisar") },
                    trailingIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                painter = painterResource(id = R.drawable.buscador_icon),
                                contentDescription = "Buscar"
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = "#EEEED1".toColor(),
                        unfocusedContainerColor = "#FFFFC5".toColor(),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                FilterChip(
                    selected = dataSelecionada != "Filtrar Data",
                    onClick = { mostrarCalendario = true },
                    border = null,
                    label = {
                        Text(
                            text = dataSelecionada,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = "#000000".toColor()
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.calendar_icon),
                            contentDescription = null,
                            tint = "#000000".toColor(),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = "#FFFFC5".toColor(),
                        selectedContainerColor = "#FFFFC5".toColor(),
                        labelColor = "#000000".toColor(),
                        selectedLabelColor = "#000000".toColor()
                    ),
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background("#87CEFF".toColor())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(id = R.drawable.gallery_icon),
                        contentDescription = "Galeria",
                        tint = "#000000".toColor()
                    )
                }
                IconButton(onClick = onAbrirCamera) {
                    Icon(
                        painter = painterResource(id = R.drawable.camera_icon),
                        contentDescription = "Câmera",
                        tint = "#000000".toColor()
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(id = R.drawable.config_icon),
                        contentDescription = "Configurações",
                        tint = "#000000".toColor()
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background("#F8F8FF".toColor()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listaVisitas) { item ->
                CardVisita(
                    local = item.local,
                    observacao = item.observacao,
                    nota = item.nota,
                    endereco = item.endereco,
                    imagem = item.imagem
                )
            }
        }
    }
}