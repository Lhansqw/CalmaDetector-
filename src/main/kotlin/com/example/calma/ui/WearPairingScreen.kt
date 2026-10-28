package com.example.calma.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import com.example.calma.ui.theme.CalmaTheme
import com.example.calma.ui.theme.CardBorder
import com.example.calma.ui.theme.CyanPrimary
import com.example.calma.ui.theme.StatusImminent
import com.example.calma.ui.theme.StatusMild
import com.example.calma.ui.theme.TextPrimary
import com.example.calma.ui.theme.TextSecondary

/** Pantalla de inicio de sesión del reloj: muestra el código para confirmar desde el teléfono. */
@Composable
fun WearPairingScreen(
    code: String,
    secondsLeft: Long,
    totalSeconds: Long,
    notice: String?,
    onNewCode: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds else 0f
    val timeLabel = "%d:%02d".format(secondsLeft / 60, secondsLeft % 60)

    AppScaffold {
        ScreenScaffold(
            scrollState = columnState,
            timeText = { TimeText { time() } },
            content = {
                TransformingLazyColumn(
                    state = columnState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Text(
                            text = "⌚ Vincular reloj",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(
                            text = code,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Código de vinculación ${code.toList().joinToString(" ")}" }
                        )
                    }

                    item {
                        Text(
                            text = "Escríbelo en CalmaDetector de tu teléfono",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(4.dp),
                                color = if (secondsLeft <= 30) StatusMild else CyanPrimary,
                                trackColor = CardBorder
                            )
                            Text(text = "Caduca en $timeLabel", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    if (notice != null) {
                        item {
                            Text(
                                text = notice,
                                fontSize = 11.sp,
                                color = StatusMild,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        CompactButton(
                            onClick = onNewCode,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            label = { Text("Nuevo código", fontSize = 11.sp) }
                        )
                    }
                }
            }
        )
    }
}

@WearPreviewLargeRound
@Composable
fun WearPairingScreenPreview() {
    CalmaTheme {
        WearPairingScreen(
            code = "482 913",
            secondsLeft = 272,
            totalSeconds = 300,
            notice = "Código incorrecto (quedan 4)",
            onNewCode = {}
        )
    }
}

/** Fila al final del panel del reloj con el teléfono vinculado y el botón para cerrar sesión. */
@Composable
internal fun WearUnpairButton(onUnpair: () -> Unit) {
    CompactButton(
        onClick = onUnpair,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = StatusImminent.copy(alpha = 0.6f)),
        label = { Text("Desvincular teléfono", fontSize = 11.sp) }
    )
}
