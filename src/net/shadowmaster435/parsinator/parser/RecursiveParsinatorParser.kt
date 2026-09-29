package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType
import net.shadowmaster435.parsinator.util.Stack

open class RecursiveParsinatorParser(
    @JvmField val shouldIncreaseDepth: (parsinator: Parsinator) -> Boolean,
    @JvmField val shouldDecreaseDepth: (parsinator: Parsinator) -> Boolean,
    @JvmField val subParser: AbstractParsinatorParser,
    optional: Boolean,
) : AbstractParsinatorParser(optional) {

    override fun shouldParse(parsinator: Parsinator) = shouldIncreaseDepth(parsinator)

    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        val recursionStorage = mutableListOf<Any?>()
        val storageStack = Stack<MutableList<Any?>>()

        var depth = 1
        val start = parsinator.index
        while (true) {
            if (!parsinator.inbounds(parsinator.index - start)) {
                parsinator.goto(start)
                return if (optional) {
                    parsinator.goto(start)
                    null
                } else ParsinatorErrorType.EOF
            }
            relock(parsinator) {
                if (shouldIncreaseDepth(parsinator)) {
                    depth++
                    storageStack.push(mutableListOf())
                }
                if (shouldDecreaseDepth(parsinator)) {
                    depth--
                    recursionStorage.add(storageStack.pop())
                }
            }
            if (depth == 0) {
                storage = recursionStorage
                finish(null, start..parsinator.index)
                construct(parsinator)
                return null
            }
            val parsed = subParser.parse(parsinator)
            if (parsed != null) {
                return if (optional) {
                    parsinator.goto(start)
                    null
                } else parsed
            } else storageStack.peek().add(subParser.storage)
        }
    }
}