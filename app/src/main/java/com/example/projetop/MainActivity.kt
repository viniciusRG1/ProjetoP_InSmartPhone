package com.example.projetop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.projetop.front.telas.camera
import com.example.projetop.front.telas.home

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}

@Composable
fun App() {
    var telaAtual by remember { mutableStateOf("home") }

    when (telaAtual) {
        "home" -> home(
            onAbrirCamera = { telaAtual = "camera" }
        )
        "camera" -> camera(
            onVoltar = { telaAtual = "home" }
        )
    }
}