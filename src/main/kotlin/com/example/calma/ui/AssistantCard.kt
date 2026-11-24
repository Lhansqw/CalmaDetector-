package com.example.calma.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calma.assistant.ChatTurn
import com.example.calma.ui.theme.CardBackground
import com.example.calma.ui.theme.CardBorder
import com.example.calma.ui.theme.CyanPrimary
import com.example.calma.ui.theme.DarkBackground
import com.example.calma.ui.theme.StatusImminent
import com.example.calma.ui.theme.TextPrimary
import com.example.calma.ui.theme.TextSecondary

/** Avisa al asistente de cada alerta nueva del detector. Lo usan el teléfono y el reloj. */
@Composable
fun AssistantAlertEffect(state: CalmaUiState, assistant: AssistantViewModel) {
    val latest = state.alertLog.firstOrNull()
    LaunchedEffect(latest) {
        // En Android los datos siempre vienen del simulador.
        if (latest != null) assistant.onAlert(latest, state.latestSample, state.baselineMeanBpm, simulated = true)
    }
}

/** Charla con el asistente en el panel del teléfono. */
@Composable
fun AssistantCard(
    state: AssistantUiState,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val canSend = state.enabled && !state.isThinking && draft.isNotBlank()
    val submit = {
        if (canSend) {
            onSend(draft)
            draft = ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🤖 Asistente",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (state.messages.isNotEmpty() && !state.isThinking) {
                    TextButton(onClick = onClear) { Text("Nueva charla", fontSize = 12.sp, color = TextSecondary) }
                }
            }

            if (state.messages.isEmpty() && !state.isThinking && state.enabled) {
                Text(
                    text = "Cuando salte una alerta te va a escribir. También podés hablarle cuando quieras.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            state.messages.forEach { ChatBubble(it) }

            if (state.isThinking) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CyanPrimary)
                    Text("Escribiendo…", fontSize = 12.sp, color = TextSecondary)
                }
            }

            state.error?.let { error ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = error, fontSize = 12.sp, color = StatusImminent, modifier = Modifier.weight(1f))
                    if (state.enabled && !state.isThinking) {
                        TextButton(onClick = onRetry) { Text("Reintentar", fontSize = 12.sp, color = CyanPrimary) }
                    }
                }
            }

            if (state.enabled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribí cómo te sentís…", fontSize = 13.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submit() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        )
                    )
                    Button(
                        onClick = submit,
                        enabled = canSend,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DarkBackground)
                    ) {
                        Text("Enviar")
                    }
                }
            }

            Text(
                text = "No reemplaza ayuda profesional. Si estás en crisis, llamá a emergencias.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ChatBubble(turn: ChatTurn) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (turn.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            text = turn.text,
            fontSize = 13.sp,
            color = if (turn.fromUser) DarkBackground else TextPrimary,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (turn.fromUser) CyanPrimary else DarkBackground,
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
