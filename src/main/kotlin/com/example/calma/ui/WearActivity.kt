package com.example.calma.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calma.pairing.WatchPairingState
import com.example.calma.pairing.WatchPairingViewModel
import com.example.calma.ui.theme.CalmaTheme

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalmaTheme {
                val pairingViewModel: WatchPairingViewModel = viewModel()
                val pairing by pairingViewModel.state.collectAsStateWithLifecycle()

                when (val s = pairing) {
                    WatchPairingState.Loading -> Unit
                    is WatchPairingState.WaitingForPhone -> WearPairingScreen(
                        code = s.code,
                        secondsLeft = s.secondsLeft,
                        totalSeconds = s.totalSeconds,
                        notice = s.notice,
                        onNewCode = { pairingViewModel.startWaiting(null) }
                    )
                    is WatchPairingState.Paired -> {
                        // El panel solo se crea tras iniciar sesión.
                        val viewModel: CalmaViewModel = viewModel()
                        CalmaWearApp(
                            viewModel = viewModel,
                            pairedPhoneName = s.phone.name,
                            onUnpair = pairingViewModel::unpair
                        )
                    }
                }
            }
        }
    }
}
