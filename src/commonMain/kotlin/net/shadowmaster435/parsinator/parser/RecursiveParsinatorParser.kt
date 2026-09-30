package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType
import net.shadowmaster435.parsinator.util.Stack
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmField
import kotlin.jvm.JvmSynthetic
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
            if (parsinator.eof) {
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
                    queueStarted = true // set started to be true next iteration
                }
                while (shouldDecreaseDepth(parsinator)) {
                    depth--
                    storageStack.pop()
                    if (depth > 0) // prevents an exception caused by the stack being empty
                        currentStorage = storageStack.peek()
                }
            }
            if (depth == 0 && started /* allows condition not to be prematurely met*/) {
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