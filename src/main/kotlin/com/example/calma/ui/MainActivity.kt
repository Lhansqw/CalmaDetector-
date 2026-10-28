package com.example.calma.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calma.pairing.PhonePairingViewModel
import com.example.calma.ui.theme.CalmaTheme

/** Único punto de entrada: en un reloj abre la interfaz de Wear OS, en el teléfono el panel. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            startActivity(Intent(this, WearActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            CalmaTheme {
                val viewModel: CalmaViewModel = viewModel()
                val pairingViewModel: PhonePairingViewModel = viewModel()
                CalmaDashboardScreen(viewModel = viewModel, pairingViewModel = pairingViewModel)
            }
        }
    }
}
