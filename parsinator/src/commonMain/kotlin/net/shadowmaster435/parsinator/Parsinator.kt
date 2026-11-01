package net.shadowmaster435.parsinator

import net.shadowmaster435.parsinator.dsl.ParsinatorBuilder
import net.shadowmaster435.parsinator.exception.ParsinatorLockedException
import net.shadowmaster435.parsinator.parser.AbstractParsinatorParser
import net.shadowmaster435.parsinator.util.ParsinatorConstructor
import net.shadowmaster435.parsinator.util.ParsinatorContext
import net.shadowmaster435.parsinator.util.ParsinatorDebugFlags
import kotlin.jvm.JvmSynthetic
import kotlin.math.min
import kotlin.reflect.KClass


class Parsinator(
    val debugFlags: ParsinatorDebugFlags,
    private val handlers: Map<AbstractParsinatorParser, ParsinatorConstructor<*>?>,
    @JvmSynthetic internal val parser: AbstractParsinatorParser
) {
    @JvmSynthetic internal var actualString: String? = null
    @JvmSynthetic internal val data = hashMapOf<KClass<*>, Any>()


    val string: String get() {
        if (actualString == null) throw IllegalStateException("Cannot get string while not parsing")
        return actualString!!
    }
    var index = 0; @JvmSynthetic internal set
    var locked = false; @JvmSynthetic internal set
    val char get() = string.getOrNull(index)
    val eof get() = char == null
    val last get() = string.getOrNull(index - 1)

    fun inc(by: Int = 1): Int {
        if (locked) throw ParsinatorLockedException()
        val prev = index
        if (multiplatformBoundsDebug(index + by)) return prev
        index += by
        return prev
    }

    fun atOffset(by: Int) = string.getOrNull(by) ?.toString()

    fun dec(by: Int = 1): Int {
        if (locked) throw ParsinatorLockedException()
        val prev = index
        if (multiplatformBoundsDebug(index - by)) return prev
        index -= by
        return prev
    }

    fun goto(i: Int): Int {
        if (locked) throw ParsinatorLockedException()
        val prev = this.index
        if (multiplatformBoundsDebug(i)) return prev
        this.index = i
        return prev
    }

    fun substring(offset: Int): String {
        val range = index..<(min(string.length, index + offset))
        return string.substring(range)
    }
    fun parse(string: String): Any? {
        return multiplatformParse(string)
    }
    inline fun <reified T : Any> getData() = getData(T::class)
    inline fun <reified T : Any> getDataOrElse(ifAbsent: T) = getDataOrElse(T::class, ifAbsent)
    inline fun <reified T : Any> putDataIfAbsent(value: T) = putDataIfAbsent(T::class, value)
    inline fun <reified T : Any> putData(value: T) = putData(T::class, value)
    inline fun <reified T : Any> hasData() = hasData(T::class)
    inline fun <reified T : Any> ifDataPresent(noinline func: T.() -> Unit) = ifDataPresent(T::class, func)
    @Suppress("UNCHECKED_CAST") fun <T : Any> getData(klass: KClass<T>): T? = data[klass] as? T
    fun <T : Any> getDataOrElse(klass: KClass<T>, ifAbsent: T): T = getData(klass) ?: run {
        data[klass] = ifAbsent
        ifAbsent
    }
    fun <T : Any> putData(klass: KClass<T>, value: T) = data.put(klass, value)
    fun <T : Any> putDataIfAbsent(klass: KClass<T>, value: T) = data.getOrPut(klass) {value}
    fun <T : Any> hasData(klass: KClass<T>) = data.containsKey(klass)
    fun <T : Any> ifDataPresent(klass: KClass<T>, func: T.() -> Unit) {
        getData(klass)?.let { it.func() }
    }
    // call a parser's "makes" block (if it exists, else just return v) and return what the block returns for storage
    @JvmSynthetic
    internal fun construct(parser: AbstractParsinatorParser, v: Any?, start: Int) = handlers[parser]?.func(
        ParsinatorContext(Triple(string, start..index, this)), v
    ) ?: v


    fun inbounds(srcIndex: Int, offset: Int) = srcIndex + offset < string.length
    fun inbounds(offset: Int) = index + offset < string.length

    companion object {
        operator fun invoke(flags: ParsinatorDebugFlags = ParsinatorDebugFlags.NONE, init: ParsinatorBuilder.() -> Unit) =
            ParsinatorBuilder(flags, init)
    }
}

internal expect fun Parsinator.multiplatformParse(string: String): Any?
internal expect fun Parsinator.multiplatformBoundsDebug(newIndex: Int): Boolean
