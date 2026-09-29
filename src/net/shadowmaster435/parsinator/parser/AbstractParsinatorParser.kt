package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType
import net.shadowmaster435.parsinator.util.ParsinatorParserOutput

abstract class AbstractParsinatorParser(optional: Boolean) {
    var optional = optional; @JvmSynthetic internal set

    @JvmSynthetic internal open var storage: Any? = null
    abstract fun shouldParse(parsinator: Parsinator): Boolean
    abstract fun parse(parsinator: Parsinator): ParsinatorErrorType?
    protected fun relock(parsinator: Parsinator, block: () -> Unit) {
        parsinator.locked = true
        block()
        parsinator.locked = false
    }
    internal var inlinerCallback: ((ParsinatorParserOutput) -> Unit)? = null

    protected fun construct(parsinator: Parsinator) =
        parsinator.construct(this, storage)

    protected fun finish(string: String?, range: IntRange) {
        val output = ParsinatorParserOutput(string to range)
        inlinerCallback?.invoke(output)
    }
}