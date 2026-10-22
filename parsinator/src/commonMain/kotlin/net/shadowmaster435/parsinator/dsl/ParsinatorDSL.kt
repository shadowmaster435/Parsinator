package net.shadowmaster435.parsinator.dsl

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.parser.AbstractParsinatorParser
import net.shadowmaster435.parsinator.parser.BranchParsinatorParser
import net.shadowmaster435.parsinator.parser.ChainParsinatorParser
import net.shadowmaster435.parsinator.parser.ManualParsinatorParser
import net.shadowmaster435.parsinator.parser.NameParsinatorParser
import net.shadowmaster435.parsinator.parser.SkipParsinatorParser
import net.shadowmaster435.parsinator.parser.RecursiveParsinatorParser
import net.shadowmaster435.parsinator.parser.RepeatParsinatorParser
import net.shadowmaster435.parsinator.parser.TokenParsinatorParser
import net.shadowmaster435.parsinator.parser.WhitespaceParsinatorParser
import net.shadowmaster435.parsinator.parser.WrapperParsinatorParser
import net.shadowmaster435.parsinator.util.ParsinatorConstructor
import net.shadowmaster435.parsinator.util.ParsinatorDebugFlags
import net.shadowmaster435.parsinator.util.ParsinatorErrorType
import kotlin.jvm.JvmField
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmSynthetic


@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
private annotation class Marker

@JvmInline
@Marker
value class ParsinatorTemplate( // exists to prevent autocomplete from trying to add a code block
    @JvmSynthetic @JvmField internal val init: @Marker ParsinatorBuilder.() -> Unit
)

@Marker
open class ParsinatorBuilder protected constructor(protected val isRoot: Boolean = false, protected val isTemplate: Boolean = false) {
    @JvmField protected val entries = mutableListOf<ParsinatorHolder>()
    @JvmField protected var rootEntry: ParsinatorHolder? = null
    fun template(init: @Marker ParsinatorBuilder.() -> Unit) =
        ParsinatorTemplate(init)
    private fun ParsinatorHolder.checkRoot(): ParsinatorHolder {
        this@ParsinatorBuilder.entries.add(this)
        if (this@ParsinatorBuilder.rootEntry == null) this@ParsinatorBuilder.rootEntry = this else this@ParsinatorBuilder.checkThrow()
        return this
    }

    /**
     * Works exactly like a chain of if and else if statements would. Any sub parsers defined here will act as a branch.
     * Passes whatever the first successful branch's makes function returns to its own.
     *
     * Exclusive to this parser type is [BranchParsinatorBuilder.exit], these allow you to go a certain point in a string
     * without an error returned (For specifics see [BranchParsinatorBuilder.exit]).
     * These must be defined last or an exception will be thrown.
     *
     * If not optional, an [BranchParsinatorBuilder.exit] is not defined, and no branches succeeded it will return a
     * [ParsinatorErrorType.BRANCH_FAIL] error.
     */
    open fun branch(optional: Boolean, excludeFromChains: Boolean = false, init: @Marker BranchParsinatorBuilder.() -> Unit): ParsinatorHolder {
        val builder = BranchParsinatorBuilder.create()
        val list = mutableListOf<AbstractParsinatorParser>()
        builder.init()
        for (holder in builder.entries) {
            entries.add(holder)

            list.add(holder.parser)
        }
        return ParsinatorHolder(
            BranchParsinatorParser(
                optional, excludeFromChains,
                *list.toTypedArray()
            )
        ).checkRoot()
    }

    protected open fun repeatingChain(optional: Boolean, excludeFromChains: Boolean = false, init: @Marker RepeatingParsinatorBuilder.() -> Unit): ParsinatorHolder {
        val builder = RepeatingParsinatorBuilder.create()
        builder.init()
        return ParsinatorHolder(
            ChainParsinatorParser(
                optional, excludeFromChains,
                *buildList {
                    for (holder in builder.entries) {
                        this@ParsinatorBuilder.entries.add(holder)
                        add(holder.parser)
                    }
                }.toTypedArray()
            )
        )
    }

    /**
     * Simply uses a template defined elsewhere or by [template] best thought as a function call for parsers.
     */
    open fun use(template: ParsinatorTemplate): ParsinatorHolder {
        val builder = ParsinatorBuilder(true, true)
        template.init.invoke(builder)
        if (builder.entries.isEmpty())
            throw IllegalStateException("Template cannot be empty")
        checkThrow()
        return builder.rootEntry!!.checkRoot()
    }

    /**
     * Uses a sequence of sub parsers defined in [init]. Passes a list of whatever the makes functions
     * of contained sub parsers return in defined order (top to bottom) to its own makes function.
     * If a sub parser is optional it will be skipped if it fails but no list entry will be substituted,
     * thus, changing the length of the list. Make sure to account for this.
     */
    open fun chain(optional: Boolean, excludeFromChains: Boolean = false, init: @Marker ParsinatorBuilder.() -> Unit): ParsinatorHolder {
        val builder = ParsinatorBuilder()
        builder.init()
        return ParsinatorHolder(
            ChainParsinatorParser(
                optional, excludeFromChains,
                *buildList {
                    for (holder in builder.entries) {
                        this@ParsinatorBuilder.entries.add(holder)
                        add(holder.parser)
                    }
                }.toTypedArray()
            )
        ).checkRoot()
    }

    /**
     * Reads characters until a disallowed character is encountered at which point collected characters are
     * passed to a makes function.
     *
     * Something to note is that the regexes here only care about one character at a time so lookaheads and whatnot
     * will not work here. If you need more granular control use a [manual] parser
     */
    open fun name(optional: Boolean, allowedCharacters: Regex, cannotStartWith: Regex? = null, excludeFromChains: Boolean = false) =
        ParsinatorHolder(
            NameParsinatorParser(
                allowedCharacters,
                cannotStartWith,
                optional, excludeFromChains
            )
        ).checkRoot()

    /**
     * Return null for no error, passes [ManualParsinatorParser.held] to a makes function.
     * Incrementing [Parsinator.index] is not automatically incremented and must be handled.
     * If an error is return and [optional] is true, [Parsinator.index] is set back to where parsing started.
     *
     */
    open fun manual(
        optional: Boolean,
        canParse: (parsinator: Parsinator) -> Boolean = {true},
        excludeFromChains: Boolean = false,
        parseFunc: ManualParsinatorParser.(parsinator: Parsinator) -> ParsinatorErrorType?
    ) =
        ParsinatorHolder(
            ManualParsinatorParser(
                optional, excludeFromChains,
                parseFunc,
            )
        ).checkRoot()

    /**
     * Starts parsing when [shouldIncreaseDepth] returns true, at which point it will run [subParser] this will
     * continue until a matching pair is found at base depth. If no such pair is found a [ParsinatorErrorType.EOF]
     * will be returned. If not optional and [subParser] returns an error, this parser will return that error.
     * Otherwise, parsing will resume at the index at the start of the first pair.
     *
     * Passes a nested list of whatever the [subParser]'s makes function would return.
     */
    open fun recurse(
        optional: Boolean,
        shouldIncreaseDepth: (parsinator: Parsinator) -> Boolean,
        shouldDecreaseDepth: (parsinator: Parsinator) -> Boolean,
        excludeFromChains: Boolean = false,
        subParser: @Marker RecursiveParsinatorBuilder.() -> Unit,
    ): ParsinatorHolder {
        val builder = RecursiveParsinatorBuilder.create()
        builder.subParser()
        entries.add(builder.rootEntry!!)
        return ParsinatorHolder(
            RecursiveParsinatorParser(
                shouldIncreaseDepth, shouldDecreaseDepth, builder.rootEntry!!.parser, optional, excludeFromChains
            )
        ).checkRoot()
    }

    /**
     * Similar to a [chain], a repeating parser will attempt to parse a sequence of sub parsers. Where this differs is
     * once a sequence is complete, it will start again until it fails, at which point it will pass a list of whatever
     * the sub parser's makes functions return to its own. Repetitions in said list are not separated in their own lists
     * so make sure to account for this.
     *
     * Exclusive to this parser type is [RepeatingParsinatorBuilder.trailing] which can be used for things such as
     * trailing commas. These must be defined last or an exception will be thrown.
     *
     * If not [optional], [canBeEmpty] is false, and no single attempt succeeds.
     * A [ParsinatorErrorType.EMPTY_REPEAT] error will be returned.
     * Otherwise, if it can be empty it will simply return an empty list regardless of the state of [optional]
     */
    open fun repeating(optional: Boolean, canBeEmpty: Boolean, excludeFromChains: Boolean = false, init: @Marker RepeatingParsinatorBuilder.() -> Unit) =
        ParsinatorHolder(
            RepeatParsinatorParser(canBeEmpty, repeatingChain(optional, excludeFromChains, init).parser as ChainParsinatorParser, optional, excludeFromChains)
        ).checkRoot()

    /**
     * A simple string check.
     * Passes [string] to its makes function if defined.
     */
    open fun token(optional: Boolean, string: String, requiresSurroundingWhitespace: Boolean = true, excludeFromChains: Boolean = false) =
        ParsinatorHolder(TokenParsinatorParser(string, optional, excludeFromChains, requiresSurroundingWhitespace)).checkRoot()

    /**
     * Adds a whitespace requirement.
     * If optional this always succeeds regardless of whitespace presence and vice versa.
     *
     * This also never uses a makes function nor provides any entries to [chain], [recurse] [group], or [repeating].
     * Thus, any makes function attached will never be used. If this is not desired one may simply use a
     * [name] parser with the regex `Regex("\\s")`.
     */
    open fun whitespace(optional: Boolean = true) =
        ParsinatorHolder(WhitespaceParsinatorParser(optional)).checkRoot()


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
@Marker
class RecursiveParsinatorBuilder private constructor(): ParsinatorBuilder(false) {
    override fun checkThrow() {
        if (isRoot) throw IllegalArgumentException("Recursion subParser block may only contain one parser")
    }
    companion object {
        @JvmSynthetic
        internal fun create() = RecursiveParsinatorBuilder()
    }
}


@Marker
class BranchParsinatorBuilder private constructor(isRoot: Boolean = false): ParsinatorBuilder(isRoot) {
    private var exited = false
    private fun ParsinatorHolder.checkRoot(): ParsinatorHolder {
        this@BranchParsinatorBuilder.entries.add(this)
        return this
    }
    /**
     * When encountered, increment [Parsinator.index] by [incBy].
     */
    fun exit(incBy: Int = 1): ParsinatorHolder {
        check()
        exited = true
        return ParsinatorHolder(SkipParsinatorParser(incBy)).checkRoot()
    }
    /**
     * When encountered, increment [Parsinator.index] until [incUntil] returns true or the end of a string is reached
     */
    fun exit(incUntil: (char: Char) -> Boolean) {
        check()
        exited = true
        ParsinatorHolder(SkipParsinatorParser(1, incUntil)).checkRoot()
    }
    /**
     * When encountered, increment [Parsinator.index] until [incUntilCharMatch] matches or the end of a string is reached
     */
    fun exit(incUntilCharMatch: Regex) {
        check()
        exited = true
        ParsinatorHolder(SkipParsinatorParser(1) {
            incUntilCharMatch.matches(
                it.toString()
            )
        }).checkRoot()
    }

    private fun check() {
        if (exited) throw IllegalStateException("Cannot add more branches after exiting.")
    }
    override fun branch(
        optional: Boolean,
        excludeFromChains: Boolean,
        init: @Marker (BranchParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.branch(optional, excludeFromChains, init)
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
        excludeFromChains: Boolean,
        init: @Marker (RepeatingParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.repeatingChain(optional, excludeFromChains, init)
        v.parser.optional = false
        return v
    }
    override fun chain(
        optional: Boolean,
        excludeFromChains: Boolean,
        init: @Marker (ParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.chain(optional, excludeFromChains, init)
        v.parser.optional = false
        return v
    }

    override fun name(
        optional: Boolean,
        allowedCharacters: Regex,
        cannotStartWith: Regex?,
        excludeFromChains: Boolean,
    ): ParsinatorHolder {
        check()
        val v = super.name(optional, allowedCharacters, cannotStartWith, excludeFromChains)
        v.parser.optional = false
        return v
    }

    override fun recurse(
        optional: Boolean,
        shouldIncreaseDepth: (parsinator: Parsinator) -> Boolean,
        shouldDecreaseDepth: (parsinator: Parsinator) -> Boolean,
        excludeFromChains: Boolean,
        subParser: @Marker (RecursiveParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.recurse(optional, shouldIncreaseDepth, shouldDecreaseDepth, excludeFromChains, subParser)
        v.parser.optional = false
        return v
    }


    override fun repeating(
        optional: Boolean,
        canBeEmpty: Boolean,
        excludeFromChains: Boolean,
        init: @Marker (RepeatingParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        val v = super.repeating(optional, canBeEmpty, excludeFromChains, init)
        v.parser.optional = false
        return v
    }

    override fun token(
        optional: Boolean,
        string: String,
        requiresSurroundingWhitespace: Boolean,
        excludeFromChains: Boolean,
    ): ParsinatorHolder {
        check()
        val v = super.token(optional, string, requiresSurroundingWhitespace, excludeFromChains)
        v.parser.optional = false
        return v
    }


    override fun manual(
        optional: Boolean,
        canParse: (parsinator: Parsinator) -> Boolean,
        excludeFromChains: Boolean,
        parseFunc: ManualParsinatorParser.(parsinator: Parsinator) -> ParsinatorErrorType?
    ): ParsinatorHolder {
        check()
        val v = super.manual(optional, canParse, excludeFromChains, parseFunc)
        v.parser.optional = false
        return v
    }

    override fun whitespace(optional: Boolean): ParsinatorHolder {
        check()
        val v = super.whitespace(optional)
        v.parser.optional = false
        return v
    }

    companion object {
        @JvmSynthetic
        internal fun create(isRoot: Boolean = false) =
            BranchParsinatorBuilder(isRoot)
    }
}
@Marker
class RepeatingParsinatorBuilder private constructor(isRoot: Boolean = false): ParsinatorBuilder(isRoot) {
    private var exited = false
    private fun ParsinatorHolder.checkRoot(): ParsinatorHolder {
        this@RepeatingParsinatorBuilder.entries.add(this)
        return this
    }

    /**
     * @see repeating
     */
    fun trailing(init: @Marker (RepeatingParsinatorBuilder.() -> Unit)): ParsinatorHolder {
        val builder = RepeatingParsinatorBuilder(true)
        exited = true
        builder.init()
        checkThrow()
        return ParsinatorHolder(
            WrapperParsinatorParser(
                builder.rootEntry!!.parser,
                true
            )
        ).checkRoot()
    }
    /**
     * @see repeating
     */
    fun trailing(holder: ParsinatorHolder): ParsinatorHolder {
        entries.removeLast()
        return ParsinatorHolder(
            WrapperParsinatorParser(
                holder.parser,
                true
            )
        ).checkRoot()
    }

    private fun check() {
        if (exited) throw IllegalStateException("Cannot add more entries after trailing.")
    }
    override fun branch(
        optional: Boolean,
        excludeFromChains: Boolean,
        init: @Marker (BranchParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.branch(optional, excludeFromChains, init)
    }
    override fun repeatingChain(
        optional: Boolean,
        excludeFromChains: Boolean,
        init: @Marker (RepeatingParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.repeatingChain(optional, excludeFromChains, init)
    }
    override fun chain(
        optional: Boolean,
        excludeFromChains: Boolean,
        init: @Marker (ParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.chain(optional, excludeFromChains, init)
    }

    override fun use(template: ParsinatorTemplate): ParsinatorHolder {
        check()
        return super.use(template)
    }
    override fun name(
        optional: Boolean,
        allowedCharacters: Regex,
        cannotStartWith: Regex?,
        excludeFromChains: Boolean,
    ): ParsinatorHolder {
        check()
        return super.name(optional, allowedCharacters, cannotStartWith, excludeFromChains)
    }

    override fun manual(
        optional: Boolean,
        canParse: (parsinator: Parsinator) -> Boolean,
        excludeFromChains: Boolean,
        parseFunc: ManualParsinatorParser.(parsinator: Parsinator) -> ParsinatorErrorType?
    ): ParsinatorHolder {
        check()
        return super.manual(optional, canParse, excludeFromChains, parseFunc)
    }


    override fun recurse(
        optional: Boolean,
        shouldIncreaseDepth: (parsinator: Parsinator) -> Boolean,
        shouldDecreaseDepth: (parsinator: Parsinator) -> Boolean,
        excludeFromChains: Boolean,
        subParser: @Marker (RecursiveParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.recurse(optional, shouldIncreaseDepth, shouldDecreaseDepth, excludeFromChains, subParser)
    }

    override fun repeating(
        optional: Boolean,
        canBeEmpty: Boolean,
        excludeFromChains: Boolean,
        init: @Marker (RepeatingParsinatorBuilder.() -> Unit)
    ): ParsinatorHolder {
        check()
        return super.repeating(optional, canBeEmpty, excludeFromChains, init)
    }

    override fun token(
        optional: Boolean,
        string: String,
        requiresSurroundingWhitespace: Boolean,
        excludeFromChains: Boolean,
    ): ParsinatorHolder {
        check()
        return super.token(optional, string, requiresSurroundingWhitespace, excludeFromChains)
    }

    override fun whitespace(optional: Boolean): ParsinatorHolder {
        check()
        return super.whitespace(optional)
    }
    override fun checkThrow() {
        if (isRoot) throw IllegalArgumentException("Trailing branch block may only contain one parser")
    }
    companion object {
        @JvmSynthetic
        internal fun create(isRoot: Boolean = false) =
            RepeatingParsinatorBuilder(isRoot)
    }
}

@Marker
class ParsinatorHolder(@JvmSynthetic @JvmField internal val parser: AbstractParsinatorParser) {
    @JvmSynthetic internal var handler: ParsinatorConstructor<*>? = null

    infix fun <T> makes(handler: @Marker (Any?) -> T) = parser.apply {
        if (parser is SkipParsinatorParser) parser.hasMakes = true
        this@ParsinatorHolder.handler = ParsinatorConstructor(handler)
    }
}


