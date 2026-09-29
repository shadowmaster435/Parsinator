package net.shadowmaster435.parsinator.util

@JvmInline
value class ParsinatorParserOutput(private val output: Pair<String?, IntRange>) {
    operator fun component1() = output.first
    operator fun component2() = output.second
}
