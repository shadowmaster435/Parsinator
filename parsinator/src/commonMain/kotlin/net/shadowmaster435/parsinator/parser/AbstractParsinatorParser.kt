package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType
import net.shadowmaster435.parsinator.util.ParsinatorParserOutput
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmField
import kotlin.jvm.JvmSynthetic
abstract class AbstractParsinatorParser(optional: Boolean) {
    var optional = optional; @JvmSynthetic internal set

    @JvmSynthetic internal open var storage: Any? = null
    abstract fun shouldParse(parsinator: Parsinator): Boolean
    abstract fun parse(parsinator: Parsinator): ParsinatorErrorType?
    protected fun relock(parsinator: Parsinator, block: () -> Unit) {
        parsinator.locked = false
        block()
        parsinator.locked = true
    }
    internal var inlinerCallback: ((ParsinatorParserOutput) -> Unit)? = null

    // convenience function
    protected fun construct(parsinator: Parsinator) =
        parsinator.construct(this, storage)

    protected fun finish(string: String?, range: IntRange) {
        val output = ParsinatorParserOutput(string to range)
        inlinerCallback?.invoke(output)
    }
}