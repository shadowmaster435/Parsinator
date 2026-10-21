package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmField
import kotlin.jvm.JvmSynthetic
class TokenParsinatorParser(
    @JvmField val string: String,
    optional: Boolean,
    excludeFromChains: Boolean,

    val requiresSurroundingWhitespace: Boolean
) : AbstractParsinatorParser(optional, true) {
    override fun shouldParse(parsinator: Parsinator) = true

    override fun parse(parsinator: Parsinator): ParsinatorErrorType?  {
        val w = WhitespaceParsinatorParser.WHITESPACE
        val b = !requiresSurroundingWhitespace
        val first = b || w.matches(parsinator.atOffset(-1) ?: " ")
        val last =  b || w.matches(parsinator.atOffset(string.length) ?: " ")
        if (
            first && last &&
            parsinator.substring(string.length) == string
        ) {
            val start = parsinator.index
            relock(parsinator) {parsinator.inc(string.length)}
            storage = string
            finish(string, start..parsinator.index)

            storage = construct(parsinator)
            return null
        }

        return if (optional) null else ParsinatorErrorType.EOF
    }
}