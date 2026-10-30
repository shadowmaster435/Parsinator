package net.shadowmaster435.parsinator.util

import kotlin.jvm.JvmInline

@JvmInline
value class ParsinatorContext(
    private val v: Pair<String, IntRange>
) {
    val inputString: String get() = v.first
    val indexRange: IntRange get() = v.second
    val indexRangeExclusive: IntRange get() = v.second.first..<v.second.last
}