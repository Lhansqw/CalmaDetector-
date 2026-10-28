package com.example.calma.pairing

import java.security.MessageDigest
import java.security.SecureRandom

/** Código de vinculación que muestra el reloj. */
data class ActiveCode(val code: String, val expiresAtMs: Long) {
    /** "482913" -> "482 913" para que sea fácil de leer en el reloj. */
    val formatted: String get() = code.chunked(3).joinToString(" ")
}

sealed interface VerifyResult {
    data object Success : VerifyResult
    data class WrongCode(val attemptsLeft: Int) : VerifyResult
    data object Expired : VerifyResult
    data object TooManyAttempts : VerifyResult
    data object NoActiveCode : VerifyResult
}

/**
 * Genera y valida códigos de un solo uso (estilo "device code").
 * El código caduca tras [ttlMs] y se invalida tras [maxAttempts] intentos fallidos,
 * así que adivinarlo por fuerza bruta no es práctico.
 */
class PairingCodeManager(
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
    val ttlMs: Long = 5 * 60_000L,
    private val maxAttempts: Int = 5,
    private val digits: Int = 6
) {
    var active: ActiveCode? = null
        private set
    private var failedAttempts = 0

    fun newCode(): ActiveCode {
        val bound = pow10(digits)
        val code = random.nextInt(bound).toString().padStart(digits, '0')
        failedAttempts = 0
        return ActiveCode(code, clock() + ttlMs).also { active = it }
    }

    fun isExpired(): Boolean = active?.let { clock() >= it.expiresAtMs } ?: true

    fun verify(input: String): VerifyResult {
        val current = active ?: return VerifyResult.NoActiveCode
        if (clock() >= current.expiresAtMs) {
            active = null
            return VerifyResult.Expired
        }
        val normalized = normalize(input)
        // Comparación en tiempo constante.
        val ok = MessageDigest.isEqual(normalized.toByteArray(), current.code.toByteArray())
        if (ok) {
            active = null // un solo uso
            return VerifyResult.Success
        }
        failedAttempts++
        if (failedAttempts >= maxAttempts) {
            active = null
            return VerifyResult.TooManyAttempts
        }
        return VerifyResult.WrongCode(maxAttempts - failedAttempts)
    }

    fun clear() {
        active = null
        failedAttempts = 0
    }

    companion object {
        /** Quita espacios, guiones y cualquier carácter que no sea dígito. */
        fun normalize(input: String): String = input.filter { it.isDigit() }

        private fun pow10(n: Int): Int {
            var r = 1
            repeat(n) { r *= 10 }
            return r
        }
    }
}
