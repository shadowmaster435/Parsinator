package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType

class BranchParsinatorParser(
    optional: Boolean,
    @JvmField vararg val branches: AbstractParsinatorParser,
) : AbstractParsinatorParser(optional) {
    init {
        if (branches.size < 2) throw IllegalArgumentException("A branch must contain at least two values.")
    }

    override fun shouldParse(parsinator: Parsinator) = true

    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        var index = 0
        val start = parsinator.index
        var error: ParsinatorErrorType? = null
        while (true) {
            if (index >= branches.size) return error
            val entry = branches[index]
            if (entry.shouldParse(parsinator)) {
                val parsed = entry.parse(parsinator)
                if (parsed != null) relock(parsinator) {parsinator.goto(start)} else {
                    // make sure not to pass null to storage to prevent cluttering chain, recursive, and repeat parsers
                    if (entry !is SkipParsinatorParser) {
                        storage = entry.storage
                        finish(null, start..parsinator.index)
                        storage = construct(parsinator)
                    }
                    return null
                }
            } else error = ParsinatorErrorType.BRANCH_FAIL
            index++
        }
    }
}