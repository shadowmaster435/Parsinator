package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType

class ManualParsinatorParser(optional: Boolean, private val parseFunc: ManualParsinatorParser.(parsinator: Parsinator) -> ParsinatorErrorType?) : AbstractParsinatorParser(optional) {
    var held: String = ""
    override fun shouldParse(parsinator: Parsinator) = true

    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        held = ""
        storage = null
        val start = parsinator.index
        val error = parseFunc(parsinator)
        storage = held
        finish(held, start..parsinator.index)
        storage = construct(parsinator)
        return error
    }

}