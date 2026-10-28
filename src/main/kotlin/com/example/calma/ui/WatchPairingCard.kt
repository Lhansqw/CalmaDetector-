package com.example.calma.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calma.pairing.PhonePairingState
import com.example.calma.ui.theme.CardBackground
import com.example.calma.ui.theme.CardBorder
import com.example.calma.ui.theme.CyanPrimary
import com.example.calma.ui.theme.DarkBackground
import com.example.calma.ui.theme.StatusCalm
import com.example.calma.ui.theme.StatusImminent
import com.example.calma.ui.theme.TextPrimary
import com.example.calma.ui.theme.TextSecondary
import java.text.DateFormat
import java.util.Date

/** Tarjeta del teléfono para confirmar el código que muestra el reloj. */
@Composable
fun WatchPairingCard(
    state: PhonePairingState,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUnpair: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (state.paired != null) StatusCalm.copy(alpha = 0.5f) else CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⌚ Reloj vinculado",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            val paired = state.paired
            if (paired != null) {
                Text(
                    text = "${paired.name} · desde ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(paired.pairedAtMs))}",
                    fontSize = 12.sp,
                    color = StatusCalm
                )
                OutlinedButton(
                    onClick = onUnpair,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusImminent)
                ) {
                    Text("Desvincular reloj", fontSize = 12.sp)
                }
            } else {
                Text(
                    text = "Abre CalmaDetector en el reloj y escribe aquí el código de 6 dígitos que aparece.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.codeInput,
                        onValueChange = onCodeChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !state.isSending,
                        placeholder = { Text("000000", fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center,
                            color = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        )
                    )
                    Button(
                        onClick = onSubmit,
                        enabled = !state.isSending && state.codeInput.length == 6,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DarkBackground)
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = DarkBackground)
                        } else {
                            Text("Vincular")
                        }
                    }
                }
            }

            state.message?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = if (state.isError) StatusImminent else StatusCalm
                )
            }
        }
    }
}
