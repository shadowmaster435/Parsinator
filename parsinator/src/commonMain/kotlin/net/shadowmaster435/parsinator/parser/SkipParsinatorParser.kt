package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmField
import kotlin.jvm.JvmSynthetic

class SkipParsinatorParser(@JvmField val incBy: Int, @JvmField val incUntil: ((char: Char) -> Boolean)? = null) : AbstractParsinatorParser(false, true) {
    @JvmSynthetic
    var hasMakes: Boolean = false

    override fun shouldParse(parsinator: Parsinator) = true
    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        val start = parsinator.index
        relock(parsinator) {
            if (incUntil != null)
                while (parsinator.char != null && !incUntil(parsinator.char!!)) relock(parsinator) {parsinator.inc()}
            else relock(parsinator) {
                parsinator.inc(incBy)
            }
        }
        if (hasMakes) {
            finish(parsinator.string, start..parsinator.index)
            storage = construct(parsinator)
        }
        return null
    }
}