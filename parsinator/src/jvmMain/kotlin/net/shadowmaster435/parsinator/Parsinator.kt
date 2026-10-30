package net.shadowmaster435.parsinator

import net.shadowmaster435.parsinator.dsl.ParsinatorTemplate
import net.shadowmaster435.parsinator.exception.ParsinatorIllegalIndexException
import net.shadowmaster435.parsinator.exception.ParsinatorParseException
import net.shadowmaster435.parsinator.util.ParsinatorError
import kotlin.math.ceil
import kotlin.math.max
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
internal actual fun Parsinator.multiplatformParse(string: String): Any? {
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
    return parser.storage
}

fun main() {
    template.wrap().parse("1111")
}

val template = ParsinatorTemplate {
    chain(false) {
        branch(false) {
            token(false, "-", false) makes {true}
            exit(0) makes {
                println("????")
                false
            }
        }
        branch(false) {
            token(false, "0x") makes {2}
            token(false, "0b") makes {1}
            exit(0) makes {0}
        }
        name(false, Regex("[0-9.eE-]"), Regex("[eE-]"))
        branch(true) {
            token(false, "f", false, )
            token(false, "F",false, )
        }
    } makes {
        it as List<*>
        val negative = it[0] as Boolean
        val stringType = it[1] as Int
        val numberString = it[2] as String
        val containsExponent = numberString.contains(Regex("[eE]"))
        if (negative && stringType != 0) throw IllegalArgumentException("Negative signs are not supported for hex and binary strings")
        if (stringType != 0 && containsExponent) throw IllegalArgumentException("Exponents cannot be used here")
        val result = when(stringType) {
            2 -> 1f
            1 -> 1f
            0 -> ((if (negative) "-" else "") + numberString).toFloat()
            else -> throw RuntimeException("Failed to parse number")
        }
        result
    }
}