package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType
import net.shadowmaster435.parsinator.util.ParsinatorParserOutput

class RepeatParsinatorParser(
    val canBeEmpty: Boolean,
    val chain: ChainParsinatorParser,
    optional: Boolean
) : AbstractParsinatorParser(optional) {

    override fun shouldParse(parsinator: Parsinator) = chain.shouldParse(parsinator)

    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        val repeatStorage = mutableListOf<Any?>()
        var first = true
        while (true) {
            val start = parsinator.index
            val parsed = chain.parse(parsinator)
            if (parsed != null) {
                if (parsed == ParsinatorErrorType.EOF) return ParsinatorErrorType.EMPTY_REPEAT
                if (!canBeEmpty && first && !optional) {
                    if (parsinator.char == null) {
                        storage = repeatStorage
                        finish(null, start..parsinator.index)
                        return null
                    }
                    return ParsinatorErrorType.EMPTY_REPEAT
                }
                if (parsinator.char != null) parsinator.goto(start)
                storage = repeatStorage
                finish(null, start..parsinator.index)
                return null
            } else {
                chain.storage?.let { repeatStorage.addAll(it as Collection<Any?>) }
            }

            if (parsinator.char == null) {
                storage = repeatStorage
                finish(null, start..parsinator.index)
                construct(parsinator)
                return null
            }
            first = false
        }
    }
}