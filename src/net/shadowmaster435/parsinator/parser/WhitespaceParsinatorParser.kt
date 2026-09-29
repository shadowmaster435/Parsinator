package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType

class WhitespaceParsinatorParser : AbstractParsinatorParser(false) {

    override fun shouldParse(parsinator: Parsinator) = parsinator.char?.let{WHITESPACE.matches(it.toString())} ?: false

    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        while (true) {
            if (parsinator.char == null || !WHITESPACE.matches(parsinator.char.toString())) {
                return null
            }
            parsinator.inc()
        }
    }
    companion object {
        val WHITESPACE = Regex("\\s")
    }

}