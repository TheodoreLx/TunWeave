package io.github.theodorelx.tunweave.data

private const val HEADER = "# TunWeave app selection v1"
private val packageNamePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+$")

fun exportAppSelection(packageNames: Set<String>): String = buildString {
    appendLine(HEADER)
    packageNames.sorted().forEach(::appendLine)
}.trimEnd()

fun importAppSelection(text: String): Set<String> = text
    .lineSequence()
    .flatMap { line -> line.split(',', ' ', '\t').asSequence() }
    .map(String::trim)
    .filter { packageNamePattern.matches(it) }
    .toSet()
