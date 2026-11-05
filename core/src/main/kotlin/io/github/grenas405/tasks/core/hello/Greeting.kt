package io.github.grenas405.tasks.core.hello

fun greeting(name: String): String {
    val trimmed = name.trim()
    return "Hello, ${trimmed.ifEmpty { "World" }}!"
}
