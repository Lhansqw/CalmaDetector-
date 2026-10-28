package com.example.calma.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingCodeManagerTest {

    private var now = 1_000_000L
    private val manager = PairingCodeManager(clock = { now }, ttlMs = 60_000L, maxAttempts = 3)

    @Test
    fun `genera codigos de 6 digitos`() {
        repeat(200) {
            val code = manager.newCode().code
            assertEquals(6, code.length)
            assertTrue(code.all { it.isDigit() })
        }
    }

    @Test
    fun `codigo correcto vincula una sola vez`() {
        val code = manager.newCode()
        assertEquals(VerifyResult.Success, manager.verify(code.formatted)) // acepta "123 456"
        assertEquals(VerifyResult.NoActiveCode, manager.verify(code.code))
    }

    @Test
    fun `codigo caducado se rechaza`() {
        val code = manager.newCode()
        now += 60_000L
        assertEquals(VerifyResult.Expired, manager.verify(code.code))
        assertNull(manager.active)
    }

    @Test
    fun `demasiados intentos invalidan el codigo`() {
        val code = manager.newCode()
        val wrong = if (code.code == "000000") "111111" else "000000"
        assertEquals(VerifyResult.WrongCode(2), manager.verify(wrong))
        assertEquals(VerifyResult.WrongCode(1), manager.verify(wrong))
        assertEquals(VerifyResult.TooManyAttempts, manager.verify(wrong))
        // Ni siquiera el código correcto sirve ya
        assertEquals(VerifyResult.NoActiveCode, manager.verify(code.code))
    }

    @Test
    fun `codigo nuevo reinicia los intentos`() {
        manager.newCode()
        manager.verify("x")
        manager.verify("x")
        val code = manager.newCode()
        assertEquals(VerifyResult.Success, manager.verify(code.code))
    }

    @Test
    fun `normalize quita espacios y guiones`() {
        assertEquals("482913", PairingCodeManager.normalize(" 482-913 "))
    }
}
