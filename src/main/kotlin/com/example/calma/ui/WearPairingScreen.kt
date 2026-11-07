package com.example.calma.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
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
    val d = rememberWearDimens()
    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds else 0f
    val timeLabel = "%d:%02d".format(secondsLeft / 60, secondsLeft % 60)

    AppScaffold {
        ScreenScaffold(
            scrollState = columnState,
            timeText = { TimeText { time() } },
            content = {
                TransformingLazyColumn(
                    state = columnState,
                    contentPadding = d.listPadding,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(d.itemSpacing)
                ) {
                    item {
                        Text(
                            text = "⌚ Vincular reloj",
                            fontSize = d.sp(13f),
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(
                            text = code,
                            // El código es lo más importante: grande pero sin salirse en relojes pequeños.
                            fontSize = d.sp(if (d.isSmall) 28f else 32f),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Código de vinculación ${code.toList().joinToString(" ")}" }
                        )
                    }

                    item {
                        Text(
                            text = "Escríbelo en CalmaDetector de tu teléfono",
                            fontSize = d.sp(11f),
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = d.textInset)
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
                            Text(text = "Caduca en $timeLabel", fontSize = d.sp(10f), color = TextSecondary, maxLines = 1)
                        }
                    }

                    if (notice != null) {
                        item {
                            Text(
                                text = notice,
                                fontSize = d.sp(11f),
                                color = StatusMild,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = d.textInset)
                            )
                        }
                    }

                    item {
                        CompactButton(
                            onClick = onNewCode,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            label = {
                                Text(
                                    "Nuevo código",
                                    fontSize = d.sp(12f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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
        WearPairingScreen(code = "482 913", secondsLeft = 272, totalSeconds = 300, notice = "Código incorrecto (quedan 4)", onNewCode = {})
    }
}

@WearPreviewSmallRound
@Composable
fun WearPairingScreenSmallPreview() {
    CalmaTheme {
        WearPairingScreen(code = "482 913", secondsLeft = 272, totalSeconds = 300, notice = null, onNewCode = {})
    }
}

/** Botón para cerrar sesión (desvincular el teléfono) al final del panel del reloj. */
@Composable
internal fun WearUnpairButton(d: WearDimens, onUnpair: () -> Unit) {
    CompactButton(
        onClick = onUnpair,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = StatusImminent.copy(alpha = 0.6f)),
        label = {
            Text(
                "Desvincular teléfono",
                fontSize = d.sp(11f),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
