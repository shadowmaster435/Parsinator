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
        var currentStorage = mutableListOf<Any?>()
        val storageStack = Stack<MutableList<Any?>>()

        var depth = 0
        var started: Boolean
        var queueStarted = false
        val start = parsinator.index
        while (true) {
            started = queueStarted
            if (parsinator.char == null) {
                relock(parsinator) {parsinator.goto(start)}
                return if (optional) {
                    relock(parsinator) {parsinator.goto(start)}
                    null
                } else ParsinatorErrorType.EOF
            }
            relock(parsinator) {
                while (shouldIncreaseDepth(parsinator)) {
                    depth++
                    val new = mutableListOf<Any?>()
                    currentStorage.add(new)
                    currentStorage = new
                    storageStack.push(currentStorage)
                    queueStarted = true
                }
                while (shouldDecreaseDepth(parsinator)) {
                    depth--
                    storageStack.pop()
                    if (depth > 0) currentStorage = storageStack.peek()
                }
            }
            if (depth == 0 && started) {
                storage = currentStorage
                finish(null, start..parsinator.index)
                storage = construct(parsinator)
                return null
            }
            val parsed = subParser.parse(parsinator)
            if (parsed != null) {
                return if (optional) {
                    relock(parsinator) {parsinator.goto(start)}
                    null
                } else parsed
            } else currentStorage.add(parsinator.construct(subParser, subParser.storage))
        }
    }
}