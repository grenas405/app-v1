package io.github.grenas405.tasks.core.hello

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {
    @Test
    fun `greets by name`() = assertEquals("Hello, Android!", greeting("Android"))

    @Test
    fun `trims surrounding whitespace`() = assertEquals("Hello, Android!", greeting("  Android "))

    @Test
    fun `falls back to World when blank`() = assertEquals("Hello, World!", greeting("   "))
}
