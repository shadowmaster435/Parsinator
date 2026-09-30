package net.shadowmaster435.parsinator

import net.shadowmaster435.parsinator.dsl.ParsinatorBuilder
import net.shadowmaster435.parsinator.exception.ParsinatorIllegalIndexException
import net.shadowmaster435.parsinator.parser.AbstractParsinatorParser
import net.shadowmaster435.parsinator.util.ParsinatorDebugFlags
import net.shadowmaster435.parsinator.exception.ParsinatorLockedException
import net.shadowmaster435.parsinator.exception.ParsinatorParseException
import net.shadowmaster435.parsinator.util.ParsinatorError
import net.shadowmaster435.parsinator.util.ParsinatorConstructor
import kotlin.math.min

class Parsinator(
    val debugFlags: ParsinatorDebugFlags,
    private val handlers: Map<AbstractParsinatorParser, ParsinatorConstructor<*>?>,
    private val parser: AbstractParsinatorParser
) {
    private var actualString: String? = null

    val string: String get() {
        if (actualString == null) throw IllegalStateException("Cannot get string while not parsing")
        return actualString!!
    }

    var index = 0; private set
    var locked = false; @JvmSynthetic internal set
    val char get() = string.getOrNull(index)
    val eof get() = char == null

    fun inc(by: Int = 1): Int {
        if (locked) throw ParsinatorLockedException()
        val prev = index
        if (boundsDebug(index + by)) return prev
        index += by
        return prev
    }

    fun dec(by: Int = 1): Int {
        if (locked) throw ParsinatorLockedException()
        val prev = index
        if (boundsDebug(index - by)) return prev
        index -= by
        return prev
    }

    fun goto(i: Int): Int {
        if (locked) throw ParsinatorLockedException()
        val prev = this.index
        if (boundsDebug(i)) return prev
        this.index = i
        return prev
    }

    fun substring(offset: Int): String {
        val range = index..<(min(string.length, index + offset))
        return string.substring(range)
    }

    fun parse(string: String) {
        locked = true
        this.actualString = string
        index = 0
        parser.parse(this)?.let {
            // this block is triggered when a parser returns an error
            if (debugFlags.parserHardThrow) throw ParsinatorParseException(string, ParsinatorError(index, it))
            if (debugFlags.reportParserErrors) {
                System.err.println(ParsinatorParseException.makeMessage(string, ParsinatorError(min(index, string.length - 1), it)))
                if (debugFlags.parserStackTrace) Thread.dumpStack()
            }
            index = string.length
        }
        this.actualString = null
        locked = false

    }

    // call a parser's "makes" block (if it exists, else just return v) and return what the block returns for storage
    internal fun construct(parser: AbstractParsinatorParser, v: Any?): Any? {
        return handlers[parser]?.func(v) ?: v
    }

    private fun boundsDebug(newIndex: Int): Boolean {
        if (debugFlags.indexHardThrow) throw ParsinatorIllegalIndexException(string.length, newIndex)
        if (debugFlags.reportIndexBoundsErrors && newIndex !in 0..string.length) {
            System.err.println("Index just went too far out of bounds. Max safe index [${string.length}], Invalid index [$newIndex]")
            if (debugFlags.indexBoundsStackTrace) Thread.dumpStack()
            return true
        }
        return false
    }
    fun inbounds(srcIndex: Int, offset: Int) = srcIndex + offset < string.length
    fun inbounds(offset: Int) = index + offset < string.length

    companion object {
        operator fun invoke(flags: ParsinatorDebugFlags = ParsinatorDebugFlags.NONE, init: ParsinatorBuilder.() -> Unit) =
            ParsinatorBuilder(flags, init)
    }
}