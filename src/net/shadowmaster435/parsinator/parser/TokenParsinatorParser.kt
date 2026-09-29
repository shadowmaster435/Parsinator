package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType

class TokenParsinatorParser(@JvmField val string: String, optional: Boolean) : AbstractParsinatorParser(optional) {
    override fun shouldParse(parsinator: Parsinator) = true

    override fun parse(parsinator: Parsinator): ParsinatorErrorType?  {

        if (parsinator.substring(string.length) == string) {
            val start = parsinator.index
            parsinator.inc(string.length)
            storage = string
            finish(string, start..parsinator.index)

            storage = construct(parsinator)
            return null
        }

        return if (optional) null else ParsinatorErrorType.EOF
    }
}