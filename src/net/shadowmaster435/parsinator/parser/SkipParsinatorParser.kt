package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType

class SkipParsinatorParser(val incBy: Int, val incUntil: ((char: Char) -> Boolean)? = null) : AbstractParsinatorParser(false) {
    override fun shouldParse(parsinator: Parsinator) = true
    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        if (incUntil != null)
            while (parsinator.char != null && !incUntil(parsinator.char!!)) parsinator.inc()
        else parsinator.inc(incBy)
        return null
    }
}