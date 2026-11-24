package com.example.calma.assistant

import com.example.calma.detection.AlertLevel
import com.example.calma.detection.Sample
import java.util.Locale

/** Un turno de la charla con el asistente. */
data class ChatTurn(val fromUser: Boolean, val text: String)

object AssistantPrompts {

    const val SYSTEM_PROMPT = """Eres el asistente de CalmaDetector, una app que detecta señales de estrés o ansiedad a partir del pulso, la variabilidad cardíaca (HRV) y el movimiento.

Cuando la app detecta una alerta te pasa los números y tú le escribes a la persona. Después puede seguir la charla contigo.

Cómo responder:
- En español, con calidez y sin dramatizar. Mensajes cortos: entre 2 y 4 frases, porque se leen en el teléfono o en el reloj.
- Ofrece una técnica concreta para ese momento: respiración 4-7-8 (inhalar 4 segundos, sostener 7, exhalar 8), anclaje 5-4-3-2-1 o soltar los hombros y la mandíbula. Una sola por mensaje.
- Si los datos son simulados, puedes mencionarlo con naturalidad.
- No diagnosticas ni das indicaciones médicas o de medicación. Si preguntan por eso, sugiere hablarlo con un profesional de salud.
- Si la persona habla de hacerse daño, de no querer vivir o de una emergencia, prioriza su seguridad: pídele que llame ya a los servicios de emergencia de su país o a una línea de prevención del suicidio, y que no se quede sola.
- No inventes datos que no tengas."""

    /** Primer mensaje de la charla: solo números del detector, ningún dato de identidad. */
    fun alertContext(level: AlertLevel, sample: Sample, baselineBpm: Double, simulated: Boolean): String {
        val levelText = when (level) {
            AlertLevel.LEVEL_2_IMMINENT -> "ansiedad inminente (nivel 2)"
            AlertLevel.LEVEL_1_MILD -> "estrés leve (nivel 1)"
            AlertLevel.NONE -> "sin alerta"
        }
        val hrv = sample.hrvMs?.let { "%.0f ms".format(Locale.US, it) } ?: "sin dato"
        return buildString {
            appendLine("Se disparó una alerta de $levelText.")
            appendLine("Pulso actual: %.0f BPM (en reposo suele estar en %.0f BPM).".format(Locale.US, sample.bpm, baselineBpm))
            appendLine("Variabilidad cardíaca (HRV): $hrv.")
            appendLine("Movimiento: %.2f (la persona está quieta).".format(Locale.US, sample.motion))
            appendLine(if (simulated) "Los datos vienen del simulador de la app." else "Los datos vienen de los sensores del reloj.")
            append("Escríbele un primer mensaje breve para acompañarla.")
        }
    }
}
