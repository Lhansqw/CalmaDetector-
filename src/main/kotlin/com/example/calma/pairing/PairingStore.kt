package com.example.calma.pairing

import android.content.Context

/** Dispositivo vinculado guardado en este equipo (en el reloj: el teléfono; en el teléfono: el reloj). */
data class PairedDevice(
    val nodeId: String,
    val name: String,
    val token: String,
    val pairedAtMs: Long
)

/** Persistencia mínima de la vinculación en SharedPreferences privadas de la app. */
class PairingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("calma_pairing", Context.MODE_PRIVATE)

    fun load(): PairedDevice? {
        val nodeId = prefs.getString(KEY_NODE, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return PairedDevice(
            nodeId = nodeId,
            name = prefs.getString(KEY_NAME, null) ?: "Dispositivo",
            token = token,
            pairedAtMs = prefs.getLong(KEY_AT, 0L)
        )
    }

    fun save(device: PairedDevice) {
        prefs.edit()
            .putString(KEY_NODE, device.nodeId)
            .putString(KEY_NAME, device.name)
            .putString(KEY_TOKEN, device.token)
            .putLong(KEY_AT, device.pairedAtMs)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_NODE = "node_id"
        const val KEY_NAME = "name"
        const val KEY_TOKEN = "token"
        const val KEY_AT = "paired_at"
    }
}
