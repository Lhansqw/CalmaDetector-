package com.example.calma.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calma.ui.theme.CalmaTheme

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalmaTheme {
                val viewModel: CalmaViewModel = viewModel()
                CalmaWearApp(viewModel = viewModel)
            }
        }
    }
}
