package com.example.calma.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calma.ui.components.NeuPill
import com.example.calma.ui.components.alertColor
import com.example.calma.ui.components.alertTitle
import com.example.calma.ui.theme.InkSecondary
import com.example.calma.ui.theme.Pink
import java.text.SimpleDateFormat
import java.util.Date

/** Historial de alertas de la sesión, con el formato de las filas de "World Clock" de la referencia. */
@Composable
fun HistoryTab(alertLog: List<AlertEvent>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionLabel("Alertas de esta sesión (${alertLog.size})")
        if (alertLog.isEmpty()) {
            Text(
                "Todavía no saltó ninguna alerta. Puedes probar con \"Estrés en reposo\" en Calibración.",
                fontSize = 14.sp,
                color = InkSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            return@Column
        }
        val locale = LocalConfiguration.current.locales[0]
        val timeFormat = remember(locale) { SimpleDateFormat("HH:mm", locale) }
        val secondsFormat = remember(locale) { SimpleDateFormat(":ss", locale) }
        alertLog.forEach { alert ->
            NeuPill {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${alert.scenarioName.substringAfter(". ")} · ${alert.bpm.toInt()} BPM",
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                    Text(alertTitle(alert.level), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = alertColor(alert.level))
                }
                Text(timeFormat.format(Date(alert.timestampMs)), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Pink)
                Text(secondsFormat.format(Date(alert.timestampMs)), fontSize = 12.sp, color = InkSecondary)
            }
        }
    }
}
