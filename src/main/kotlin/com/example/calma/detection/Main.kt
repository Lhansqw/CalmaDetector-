package com.example.calma.detection

import kotlin.random.Random

/** Simulador: reposo -> estrés quieto -> ejercicio. Muestra cuándo se dispara cada alerta. */
fun main() {
    val rnd = Random(42)
    val baseline = Baseline(minSamples = 200)       // pequeño solo para la demo
    val detector = StressDetector(baseline)
    var t = 0L
    val step = 10_000L                              // una muestra cada 10 s

    fun feed(label: String, minutes: Int, bpm: Double, hrv: Double, motion: Double) {
        println("\n=== $label ===")
        repeat(minutes * 6) {
            t += step
            val s = Sample(
                timestampMs = t,
                bpm = bpm + rnd.nextDouble(-3.0, 3.0),
                hrvMs = hrv + rnd.nextDouble(-4.0, 4.0),
                motion = motion + rnd.nextDouble(0.0, 0.1)
            )
            val level = detector.onSample(s)
            if (level != AlertLevel.NONE) {
                println("  [min ${"%.1f".format(t / 60_000.0)}] ALERTA -> $level (bpm=${"%.0f".format(s.bpm)})")
            }
        }
    }

    feed("1) Reposo normal (aprendiendo linea base)", 40, bpm = 65.0, hrv = 50.0, motion = 0.05)
    println("Linea base lista: ${baseline.isReady}, BPM medio = ${"%.1f".format(baseline.bpm.mean)}")
    feed("2) Reposo (no debe alertar)", 5, bpm = 66.0, hrv = 49.0, motion = 0.05)
    feed("3) Ejercicio: BPM alto + movimiento (no debe alertar)", 6, bpm = 120.0, hrv = 20.0, motion = 3.0)
    feed("4) Estres en reposo: BPM alto + HRV baja + quieto (debe alertar)", 8, bpm = 100.0, hrv = 25.0, motion = 0.05)
    feed("5) Recuperacion", 5, bpm = 66.0, hrv = 48.0, motion = 0.05)
}
