package com.example.calma.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calma.R
import com.example.calma.assistant.ChatTurn
import com.example.calma.ui.components.NeuIconButton
import com.example.calma.ui.components.neumorphic
import com.example.calma.ui.theme.ImminentPastel
import com.example.calma.ui.theme.InkPrimary
import com.example.calma.ui.theme.InkSecondary
import com.example.calma.ui.theme.NeuLight
import com.example.calma.ui.theme.Pink

/** Avisa al asistente de cada alerta nueva del detector. Lo usan el teléfono y el reloj. */
@Composable
fun AssistantAlertEffect(state: CalmaUiState, assistant: AssistantViewModel) {
    val latest = state.alertLog.firstOrNull()
    LaunchedEffect(latest) {
        // En Android los datos siempre vienen del simulador.
        if (latest != null) assistant.onAlert(latest, state.latestSample, state.baselineMeanBpm, simulated = true)
    }
}

/** Pestaña del asistente: la charla ocupa la pantalla y el campo de texto queda abajo. */
@Composable
fun AssistantTab(
    state: AssistantUiState,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val canSend = state.enabled && !state.isThinking && draft.isNotBlank()
    val submit = {
        if (canSend) {
            onSend(draft)
            draft = ""
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.isThinking) {
        val last = state.messages.size + (if (state.isThinking) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp, horizontal = 4.dp)
        ) {
            if (state.messages.isEmpty() && !state.isThinking) {
                item {
                    Text(
                        text = if (state.enabled) "Cuando salte una alerta te va a escribir. También puedes hablarle cuando quieras."
                        else state.error.orEmpty(),
                        fontSize = 14.sp,
                        color = InkSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            items(state.messages) { ChatBubble(it) }
            if (state.isThinking) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Pink)
                        Text("Escribiendo…", fontSize = 13.sp, color = InkSecondary)
                    }
                }
            }
        }

        state.error?.takeIf { state.enabled }?.let { error ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = error, fontSize = 12.sp, color = ImminentPastel, modifier = Modifier.weight(1f))
                if (!state.isThinking) TextButton(onClick = onRetry) { Text("Reintentar", fontSize = 12.sp, color = Pink) }
            }
        }

        if (state.enabled) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .neumorphic(18.dp, elevation = 4.dp, pressed = true)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    textStyle = TextStyle(fontSize = 15.sp, color = InkPrimary),
                    cursorBrush = SolidColor(Pink),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("Escribe cómo te sientes…", fontSize = 15.sp, color = InkSecondary)
                            inner()
                        }
                    }
                )
                NeuIconButton(
                    iconRes = R.drawable.ic_send,
                    contentDescription = "Enviar",
                    onClick = submit,
                    size = 50.dp,
                    filled = canSend,
                    enabled = canSend
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = "No reemplaza ayuda profesional. Si estás en crisis, llama a emergencias.",
                fontSize = 11.sp,
                color = InkSecondary,
                modifier = Modifier.weight(1f)
            )
            if (state.messages.isNotEmpty() && !state.isThinking) {
                TextButton(onClick = onClear) { Text("Nueva charla", fontSize = 12.sp, color = Pink) }
            }
        }
    }
}

@Composable
private fun ChatBubble(turn: ChatTurn) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (turn.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            text = turn.text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = if (turn.fromUser) NeuLight else InkPrimary,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .then(
                    if (turn.fromUser) Modifier.background(Pink, shape)
                    else Modifier.neumorphic(18.dp, elevation = 4.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
