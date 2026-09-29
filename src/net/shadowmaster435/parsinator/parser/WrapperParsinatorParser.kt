package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType

internal class WrapperParsinatorParser(
    @JvmField val parser: AbstractParsinatorParser,
    @JvmSynthetic internal val isTrailing: Boolean = false,
): AbstractParsinatorParser(parser.optional) {
    override fun shouldParse(parsinator: Parsinator) = parser.shouldParse(parsinator)
    @JvmField val isWhitespace = parser is WhitespaceParsinatorParser


    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        val parsed = parser.parse(parsinator)
        if (!optional && parsed != null) return parsed
        return null
    }
}