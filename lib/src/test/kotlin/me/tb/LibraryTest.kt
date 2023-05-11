package me.tb

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryTest {
    @Test
    fun `helloCashu returns expected string`() {
        assertEquals(helloCashu(), "Hello Cashu!")
    }
}
