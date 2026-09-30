package net.shadowmaster435.parsinator.dsl

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.parser.AbstractParsinatorParser
import net.shadowmaster435.parsinator.parser.BranchParsinatorParser
import net.shadowmaster435.parsinator.parser.ChainParsinatorParser
import net.shadowmaster435.parsinator.parser.NameParsinatorParser
import net.shadowmaster435.parsinator.parser.SkipParsinatorParser
import net.shadowmaster435.parsinator.parser.RecursiveParsinatorParser
import net.shadowmaster435.parsinator.parser.RepeatParsinatorParser
import net.shadowmaster435.parsinator.parser.TokenParsinatorParser
import net.shadowmaster435.parsinator.parser.WhitespaceParsinatorParser
import net.shadowmaster435.parsinator.parser.WrapperParsinatorParser
import net.shadowmaster435.parsinator.util.ParsinatorConstructor
import net.shadowmaster435.parsinator.util.ParsinatorDebugFlags


@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
private annotation class Marker

@JvmInline
@Marker
value class ParsinatorTemplate internal constructor( // exists to prevent autocomplete from trying to add a code block
    @JvmSynthetic @JvmField internal val init: @Marker ParsinatorBuilder.() -> Unit
)

@Marker
open class ParsinatorBuilder protected constructor(protected val isRoot: Boolean = false, protected val isTemplate: Boolean = false) {
    @JvmField protected val entries = mutableListOf<ParsinatorHolder>()
    @JvmField protected var rootEntry: ParsinatorHolder? = null
    fun template(init: @Marker ParsinatorBuilder.() -> Unit) = ParsinatorTemplate(init)
    private fun ParsinatorHolder.checkRoot(): ParsinatorHolder {
        this@ParsinatorBuilder.entries.add(this)
        if (this@ParsinatorBuilder.rootEntry == null) this@ParsinatorBuilder.rootEntry = this else this@ParsinatorBuilder.checkThrow()
        return this
    }

    open fun branch(optional: Boolean, init: @Marker BranchParsinatorBuilder.() -> Unit): ParsinatorHolder {
        val builder = BranchParsinatorBuilder.create()
        val list = mutableListOf<AbstractParsinatorParser>()
        builder.init()
        for (holder in builder.entries) {
            entries.add(holder)

            list.add(holder.parser)
        }
        return ParsinatorHolder(BranchParsinatorParser(optional, *list.toTypedArray())).checkRoot()
    }

    protected open fun repeatingChain(optional: Boolean, init: @Marker RepeatingParsinatorBuilder.() -> Unit): ParsinatorHolder {
        val builder = RepeatingParsinatorBuilder.create()
        builder.init()
        return ParsinatorHolder(ChainParsinatorParser(optional, *buildList {
            for (holder in builder.entries) {
                this@ParsinatorBuilder.entries.add(holder)
                add(holder.parser)
            }
        }.toTypedArray()))
    }

    open fun use(template: ParsinatorTemplate): ParsinatorHolder {
        val builder = ParsinatorBuilder(true, true)
        template.init.invoke(builder)
        if (builder.entries.isEmpty())
            throw IllegalStateException("Template cannot be empty")
        checkThrow()
        return builder.rootEntry!!.checkRoot()
    }

    open fun chain(optional: Boolean, init: @Marker ParsinatorBuilder.() -> Unit): ParsinatorHolder {
        val builder = ParsinatorBuilder()
        builder.init()
        return ParsinatorHolder(ChainParsinatorParser(optional, *buildList {
            for (holder in builder.entries) {
                this@ParsinatorBuilder.entries.add(holder)
                add(holder.parser)
            }
        }.toTypedArray())).checkRoot()
    }

    open fun name(optional: Boolean, allowedCharacters: Regex, cannotStartWith: Regex? = null) =
        ParsinatorHolder(NameParsinatorParser(allowedCharacters, cannotStartWith, optional)).checkRoot()

    open fun recurse(
        optional: Boolean,
        shouldIncreaseDepth: (parsinator: Parsinator) -> Boolean,
        shouldDecreaseDepth: (parsinator: Parsinator) -> Boolean,
        subParser: @Marker RecursiveParsinatorBuilder.() -> Unit,
    ): ParsinatorHolder {
        val builder = RecursiveParsinatorBuilder.create()
        builder.subParser()
        entries.add(builder.rootEntry!!)
        return ParsinatorHolder(RecursiveParsinatorParser(
            shouldIncreaseDepth, shouldDecreaseDepth, builder.rootEntry!!.parser, optional)
        ).checkRoot()
    }

    open fun repeating(optional: Boolean, canBeEmpty: Boolean, init: @Marker RepeatingParsinatorBuilder.() -> Unit) =
        ParsinatorHolder(
            RepeatParsinatorParser(canBeEmpty, repeatingChain(optional, init).parser as ChainParsinatorParser, optional)
        ).checkRoot()

    open fun token(optional: Boolean, string: String) =
        ParsinatorHolder(TokenParsinatorParser(string, optional)).checkRoot()
    open fun whitespace() =
        ParsinatorHolder(WhitespaceParsinatorParser()).checkRoot()
    open fun recurse(
        optional: Boolean,
        depthIncreaseChar: Char,
        depthDecreaseChar: Char,
        subParser: @Marker (RecursiveParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        return recurse(optional, {
            val v = it.char == '{'
            if (v) it.inc()
            v
        }, {
            val v = it.char == '}'
            if (v) it.inc()
            v
        }, subParser)
    }
    protected open fun checkThrow() {
        if (isRoot)
            if (isTemplate) throw IllegalArgumentException("Templates passed to a use block may only contain one parser")
            else throw IllegalArgumentException("Root block may only contain one parser")
    }
    companion object {
        @JvmSynthetic
        internal operator fun invoke(flags: ParsinatorDebugFlags, init: @Marker ParsinatorBuilder.() -> Unit): Parsinator {
            val builder = ParsinatorBuilder(true)
            builder.init()
            val rootEntry =
                builder.rootEntry ?: throw NullPointerException("Root block must contain at least one parser")
            val entries = buildMap {
                builder.entries.forEach {
                    put(it.parser, it.handler)
                }
            }
            return Parsinator(flags, entries, rootEntry.parser)
        }
    }
}
@Marker class RecursiveParsinatorBuilder private constructor(): ParsinatorBuilder(false) {
    override fun checkThrow() {
        if (isRoot) throw IllegalArgumentException("Recursion subParser block may only contain one parser")
    }
    companion object {
        @JvmSynthetic
        internal fun create() = RecursiveParsinatorBuilder()
    }
}


@Marker class BranchParsinatorBuilder private constructor(isRoot: Boolean = false): ParsinatorBuilder(isRoot) {
    private var exited = false
    private fun ParsinatorHolder.checkRoot(): ParsinatorHolder {
        this@BranchParsinatorBuilder.entries.add(this)
        return this
    }
    fun exit(incBy: Int = 1) {
        check()
        exited = true
        ParsinatorHolder(SkipParsinatorParser(incBy)).checkRoot()
    }
    fun exit(incUntil: (char: Char) -> Boolean) {
        check()
        exited = true
        ParsinatorHolder(SkipParsinatorParser(1, incUntil)).checkRoot()
    }
    fun exit(incUntilCharMatch: Regex) {
        check()
        exited = true
        ParsinatorHolder(SkipParsinatorParser(1) { incUntilCharMatch.matches(it.toString()) }).checkRoot()
    }

    private fun check() {
        if (exited) throw IllegalStateException("Cannot add more branches after exiting.")
    }
    override fun branch(
        optional: Boolean,
        init: @Marker (BranchParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.branch(optional, init)
        v.parser.optional = false
        return v
    }

    override fun use(template: ParsinatorTemplate): ParsinatorHolder {
        check()
        val v = super.use(template)
        v.parser.optional = false
        return v
    }
    override fun repeatingChain(
        optional: Boolean,
        init: @Marker (RepeatingParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.repeatingChain(optional, init)
        v.parser.optional = false
        return v
    }
    override fun chain(
        optional: Boolean,
        init: @Marker (ParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.chain(optional, init)
        v.parser.optional = false
        return v
    }

    override fun name(
        optional: Boolean,
        allowedCharacters: Regex,
        cannotStartWith: Regex?,
    ): ParsinatorHolder {
        check()
        val v = super.name(optional, allowedCharacters, cannotStartWith)
        v.parser.optional = false
        return v
    }

    override fun recurse(
        optional: Boolean,
        shouldIncreaseDepth: (parsinator: Parsinator) -> Boolean,
        shouldDecreaseDepth: (parsinator: Parsinator) -> Boolean,
        subParser: @Marker (RecursiveParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.recurse(optional, shouldIncreaseDepth, shouldDecreaseDepth, subParser)
        v.parser.optional = false
        return v
    }
    override fun recurse(
        optional: Boolean,
        depthIncreaseChar: Char,
        depthDecreaseChar: Char,
        subParser: @Marker (RecursiveParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.recurse(optional, depthIncreaseChar, depthDecreaseChar, subParser)
        v.parser.optional = false
        return v
    }
    override fun repeating(
        optional: Boolean,
        canBeEmpty: Boolean,
        init: @Marker (RepeatingParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.repeating(optional, canBeEmpty, init)
        v.parser.optional = false
        return v
    }

    override fun token(
        optional: Boolean,
        string: String,
    ): ParsinatorHolder {
        check()
        val v = super.token(optional, string)
        v.parser.optional = false
        return v
    }

    override fun whitespace(): ParsinatorHolder {
        check()
        val v = super.whitespace()
        v.parser.optional = false
        return v
    }

    companion object {
        @JvmSynthetic
        internal fun create(isRoot: Boolean = false) = BranchParsinatorBuilder(isRoot)
    }
}
@Marker class RepeatingParsinatorBuilder private constructor(isRoot: Boolean = false): ParsinatorBuilder(isRoot) {
    private var exited = false
    private fun ParsinatorHolder.checkRoot(): ParsinatorHolder {
        this@RepeatingParsinatorBuilder.entries.add(this)
        return this
    }
    fun trailing(init: @Marker (RepeatingParsinatorBuilder.() -> Unit)): ParsinatorHolder {
        val builder = RepeatingParsinatorBuilder(true)
        exited = true
        builder.init()
        checkThrow()
        return ParsinatorHolder(WrapperParsinatorParser(builder.rootEntry!!.parser, true)).checkRoot()
    }
    fun trailing(holder: ParsinatorHolder): ParsinatorHolder {
        entries.removeLast()
        return ParsinatorHolder(WrapperParsinatorParser(holder.parser, true)).checkRoot()
    }

    private fun check() {
        if (exited) throw IllegalStateException("Cannot add more entries after trailing.")
    }
    override fun branch(
        optional: Boolean,
        init: @Marker (BranchParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.branch(optional, init)
    }
    override fun repeatingChain(
        optional: Boolean,
        init: @Marker (RepeatingParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.repeatingChain(optional, init)
    }
    override fun chain(
        optional: Boolean,
        init: @Marker (ParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.chain(optional, init)
    }

    override fun use(template: ParsinatorTemplate): ParsinatorHolder {
        check()
        return super.use(template)
    }
    override fun name(
        optional: Boolean,
        allowedCharacters: Regex,
        cannotStartWith: Regex?,
    ): ParsinatorHolder {
        check()
        return super.name(optional, allowedCharacters, cannotStartWith)
    }


    override fun recurse(
        optional: Boolean,
        depthIncreaseChar: Char,
        depthDecreaseChar: Char,
        subParser: @Marker (RecursiveParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.recurse(optional, depthIncreaseChar, depthDecreaseChar, subParser)
    }


    override fun recurse(
        optional: Boolean,
        shouldIncreaseDepth: (parsinator: Parsinator) -> Boolean,
        shouldDecreaseDepth: (parsinator: Parsinator) -> Boolean,
        subParser: @Marker (RecursiveParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.recurse(optional, shouldIncreaseDepth, shouldDecreaseDepth, subParser)
    }

    override fun repeating(
        optional: Boolean,
        canBeEmpty: Boolean,
        init: @Marker (RepeatingParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.repeating(optional, canBeEmpty, init)
    }

    override fun token(
        optional: Boolean,
        string: String,
    ): ParsinatorHolder {
        check()
        return super.token(optional, string)
    }

    override fun whitespace(): ParsinatorHolder {
        check()
        return super.whitespace()
    }
    override fun checkThrow() {
        if (isRoot) throw IllegalArgumentException("Trailing branch block may only contain one parser")
    }
    companion object {
        @JvmSynthetic
        internal fun create(isRoot: Boolean = false) = RepeatingParsinatorBuilder(isRoot)
    }
}

@Marker class ParsinatorHolder(@JvmSynthetic @JvmField internal val parser: AbstractParsinatorParser) {
    @JvmSynthetic internal var handler: ParsinatorConstructor<*>? = null

    infix fun <T> makes(handler: @Marker (Any?) -> T) = parser.apply {
        this@ParsinatorHolder.handler = ParsinatorConstructor(handler)
    }
}


