package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType

class NameParsinatorParser(@JvmField val allowedChars: Regex, @JvmField val cannotStartWith: Regex? = null, optional: Boolean) : AbstractParsinatorParser(optional) {
    override fun shouldParse(parsinator: Parsinator) = parsinator.inbounds(0) && allowedChars.matches(parsinator.char.toString())

    override fun parse(parsinator: Parsinator): ParsinatorErrorType?  {
        var collected = ""
        val start = parsinator.index
        while (true) {
            if (collected.isEmpty()) {
                if (cannotStartWith != null) {
                    parsinator.string.getOrNull(parsinator.index)?.let { startChar ->
                        if (cannotStartWith.matches(startChar.toString()))
                            return ParsinatorErrorType.ILLEGAL_START_CHAR
                    }
                }
            }
            val char = parsinator.char
            if (parsinator.eof || !allowedChars.matches(char.toString())) {
                storage = collected
                finish(collected, start..parsinator.index)
                storage = construct(parsinator)
                return null
            }
            collected += parsinator.char
            relock(parsinator) {parsinator.inc()}
        }
    }
}