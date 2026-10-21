package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType

class ManualParsinatorParser(optional: Boolean, excludeFromChains: Boolean, private val parseFunc: ManualParsinatorParser.(parsinator: Parsinator) -> ParsinatorErrorType?) : AbstractParsinatorParser(optional, excludeFromChains) {
    var held: Any? = null
    override fun shouldParse(parsinator: Parsinator) = true

    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        held = null
        storage = null
        val start = parsinator.index
        val error = parseFunc(parsinator)
        storage = held
        finish(parsinator.string, start..parsinator.index)
        storage = construct(parsinator)
        return error
    }

}