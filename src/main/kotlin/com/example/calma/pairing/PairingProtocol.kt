package com.example.calma.pairing

import org.json.JSONObject

/**
 * Mensajes que intercambian teléfono y reloj por la Wearable Data Layer (MessageClient).
 *
 * Flujo:
 *  1. El reloj muestra un código de 6 dígitos.
 *  2. El usuario lo escribe en el teléfono -> el teléfono envía [PATH_REQUEST] con el código.
 *  3. El reloj lo valida y responde con [PATH_RESULT] (ok + token de sesión, o error).
 *  4. Cualquiera de los dos puede desvincular enviando [PATH_REVOKE].
 */
object PairingProtocol {
    const val PATH_REQUEST = "/calma/pair/request"
    const val PATH_RESULT = "/calma/pair/result"
    const val PATH_REVOKE = "/calma/pair/revoke"

    data class Request(val code: String, val phoneName: String)

    data class Result(
        val ok: Boolean,
        val token: String? = null,
        val watchName: String? = null,
        val error: String? = null,
        val attemptsLeft: Int? = null
    )

    fun encode(r: Request): ByteArray =
        JSONObject().put("code", r.code).put("phoneName", r.phoneName).toString().toByteArray()

    fun decodeRequest(bytes: ByteArray): Request? = runCatching {
        val o = JSONObject(String(bytes))
        Request(o.getString("code"), o.optString("phoneName", "Teléfono"))
    }.getOrNull()

    fun encode(r: Result): ByteArray = JSONObject().apply {
        put("ok", r.ok)
        r.token?.let { put("token", it) }
        r.watchName?.let { put("watchName", it) }
        r.error?.let { put("error", it) }
        r.attemptsLeft?.let { put("attemptsLeft", it) }
    }.toString().toByteArray()

    fun decodeResult(bytes: ByteArray): Result? = runCatching {
        val o = JSONObject(String(bytes))
        Result(
            ok = o.getBoolean("ok"),
            token = o.optString("token").ifEmpty { null },
            watchName = o.optString("watchName").ifEmpty { null },
            error = o.optString("error").ifEmpty { null },
            attemptsLeft = if (o.has("attemptsLeft")) o.getInt("attemptsLeft") else null
        )
    }.getOrNull()

    /** Texto de error para mostrar al usuario según el resultado de la verificación. */
    fun errorFor(result: VerifyResult): Result = when (result) {
        VerifyResult.Success -> error("Success no es un error")
        is VerifyResult.WrongCode -> Result(false, error = "Código incorrecto", attemptsLeft = result.attemptsLeft)
        VerifyResult.Expired -> Result(false, error = "El código caducó. Usa el nuevo código del reloj")
        VerifyResult.TooManyAttempts -> Result(false, error = "Demasiados intentos. El reloj generó un código nuevo")
        VerifyResult.NoActiveCode -> Result(false, error = "El reloj no está esperando una vinculación")
    }
}
