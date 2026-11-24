package com.example.calma.assistant

import com.example.calma.detection.AlertLevel
import com.example.calma.detection.Sample
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPromptsTest {

    private val sample = Sample(timestampMs = 1_700_000_000_000L, bpm = 101.6, hrvMs = 22.4, motion = 0.06)

    @Test
    fun `el contexto lleva los numeros de la alerta`() {
        val text = AssistantPrompts.alertContext(AlertLevel.LEVEL_2_IMMINENT, sample, baselineBpm = 65.2, simulated = true)
        assertTrue(text.contains("ansiedad inminente"))
        assertTrue(text.contains("102 BPM"))
        assertTrue(text.contains("65 BPM"))
        assertTrue(text.contains("22 ms"))
        assertTrue(text.contains("simulador"))
    }

    @Test
    fun `sin HRV lo dice y no manda la marca de tiempo`() {
        val text = AssistantPrompts.alertContext(AlertLevel.LEVEL_1_MILD, sample.copy(hrvMs = null), baselineBpm = 65.0, simulated = false)
        assertTrue(text.contains("estrés leve"))
        assertTrue(text.contains("sin dato"))
        assertTrue(text.contains("sensores del reloj"))
        assertFalse(text.contains(sample.timestampMs.toString()))
    }

    @Test
    fun `sin clave el asistente queda apagado`() {
        assertFalse(CalmaAssistant(apiKey = "").isConfigured)
    }
}
