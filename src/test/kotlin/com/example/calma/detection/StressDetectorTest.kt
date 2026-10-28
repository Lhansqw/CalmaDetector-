package com.example.calma.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StressDetectorTest {

    private val rnd = Random(7)
    private var t = 0L

    private fun feed(d: StressDetector, n: Int, bpm: Double, hrv: Double, motion: Double): List<AlertLevel> =
        List(n) {
            t += 10_000L
            d.onSample(Sample(t, bpm + rnd.nextDouble(-2.0, 2.0), hrv + rnd.nextDouble(-3.0, 3.0), motion))
        }

    private fun trainedDetector(): StressDetector {
        val d = StressDetector(Baseline(minSamples = 100))
        feed(d, 120, bpm = 65.0, hrv = 50.0, motion = 0.05)
        return d
    }

    @Test
    fun `ejercicio no dispara alertas`() {
        val d = trainedDetector()
        val alerts = feed(d, 30, bpm = 120.0, hrv = 20.0, motion = 3.0)
        assertTrue(alerts.all { it == AlertLevel.NONE })
    }

    @Test
    fun `estres en reposo dispara alerta y el nivel actual se mantiene durante el cooldown`() {
        val d = trainedDetector()
        val alerts = feed(d, 30, bpm = 100.0, hrv = 22.0, motion = 0.05)
        assertEquals(1, alerts.count { it == AlertLevel.LEVEL_2_IMMINENT }) // una sola vez por el cooldown
        assertEquals(AlertLevel.LEVEL_2_IMMINENT, d.currentLevel)           // pero el estado sigue activo
    }

    @Test
    fun `la sensibilidad se acumula y tiene limites`() {
        val d = trainedDetector()
        repeat(20) { d.onUserFeedback(wasAnxiety = false) }
        assertEquals(1.5, d.currentSensitivity, 1e-9)
        repeat(20) { d.onUserFeedback(wasAnxiety = true) }
        assertEquals(0.8, d.currentSensitivity, 1e-9)
    }
}
