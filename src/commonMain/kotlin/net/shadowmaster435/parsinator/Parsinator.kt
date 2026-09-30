package net.shadowmaster435.parsinator

import net.shadowmaster435.parsinator.dsl.ParsinatorBuilder
import net.shadowmaster435.parsinator.exception.ParsinatorLockedException
import net.shadowmaster435.parsinator.parser.AbstractParsinatorParser
import net.shadowmaster435.parsinator.util.ParsinatorConstructor
import net.shadowmaster435.parsinator.util.ParsinatorDebugFlags
import kotlin.jvm.JvmSynthetic
import kotlin.math.min


class Parsinator(
    val debugFlags: ParsinatorDebugFlags,
    private val handlers: Map<AbstractParsinatorParser, ParsinatorConstructor<*>?>,
    @JvmSynthetic internal val parser: AbstractParsinatorParser
) {
    @JvmSynthetic
    internal var actualString: String? = null

    val string: String get() {
        if (actualString == null) throw IllegalStateException("Cannot get string while not parsing")
        return actualString!!
    }

    var index = 0; @JvmSynthetic internal set
    var locked = false; @JvmSynthetic internal set
    val char get() = string.getOrNull(index)
    val eof get() = char == null

    fun inc(by: Int = 1): Int {
        if (locked) throw ParsinatorLockedException()
        val prev = index
        if (multiplatformBoundsDebug(index + by)) return prev
        index += by
        return prev
    }

    fun dec(by: Int = 1): Int {
        if (locked) throw ParsinatorLockedException()
        val prev = index
        if (multiplatformBoundsDebug(index - by)) return prev
        index -= by
        return prev
    }

    fun goto(i: Int): Int {
        if (locked) throw ParsinatorLockedException()
        val prev = this.index
        if (multiplatformBoundsDebug(i)) return prev
        this.index = i
        return prev
    }

    fun substring(offset: Int): String {
        val range = index..<(min(string.length, index + offset))
        return string.substring(range)
    }
    fun parse(string: String) = multiplatformParse(string)

    // call a parser's "makes" block (if it exists, else just return v) and return what the block returns for storage
    @JvmSynthetic
    internal fun construct(parser: AbstractParsinatorParser, v: Any?) = handlers[parser]?.func(v) ?: v


    fun inbounds(srcIndex: Int, offset: Int) = srcIndex + offset < string.length
    fun inbounds(offset: Int) = index + offset < string.length

    companion object {
        operator fun invoke(flags: ParsinatorDebugFlags = ParsinatorDebugFlags.NONE, init: ParsinatorBuilder.() -> Unit) =
            ParsinatorBuilder(flags, init)
    }
}

internal expect fun Parsinator.multiplatformParse(string: String)
internal expect fun Parsinator.multiplatformBoundsDebug(newIndex: Int): Boolean
