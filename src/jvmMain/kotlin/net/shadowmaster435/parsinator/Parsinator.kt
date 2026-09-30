package net.shadowmaster435.parsinator

import net.shadowmaster435.parsinator.exception.ParsinatorIllegalIndexException
import net.shadowmaster435.parsinator.exception.ParsinatorParseException
import net.shadowmaster435.parsinator.util.ParsinatorError
import kotlin.math.min


@JvmSynthetic
internal actual fun Parsinator.multiplatformBoundsDebug(newIndex: Int): Boolean {
    if (debugFlags.indexHardThrow) throw ParsinatorIllegalIndexException(string.length, newIndex)
    if (debugFlags.reportIndexBoundsErrors && newIndex !in 0..string.length) {
        System.err.println("Index just went too far out of bounds. Max safe index [${string.length}], Invalid index [$newIndex]")
        if (debugFlags.indexBoundsStackTrace) Thread.dumpStack()
        return true
    }
    return false
}


@JvmSynthetic
internal actual fun Parsinator.multiplatformParse(string: String) {
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