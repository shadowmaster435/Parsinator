package net.shadowmaster435.parsinator.util

import net.shadowmaster435.parsinator.Parsinator
import kotlin.collections.set
import kotlin.jvm.JvmInline
import kotlin.reflect.KClass

@JvmInline
value class ParsinatorContext(
    private val v: Triple<String, IntRange, Parsinator>
) {
    val inputString: String get() = v.first
    val indexRange: IntRange get() = v.second
    val indexRangeExclusive: IntRange get() = v.second.first..<v.second.last

    inline fun <reified T : Any> getData() = getData(T::class)
    inline fun <reified T : Any> getDataOrElse(ifAbsent: T) = getDataOrElse(T::class, ifAbsent)
    inline fun <reified T : Any> putDataIfAbsent(value: T) = putDataIfAbsent(T::class, value)
    inline fun <reified T : Any> putData(value: T) = putData(T::class, value)
    inline fun <reified T : Any> hasData() = hasData(T::class)
    inline fun <reified T : Any> ifDataPresent(noinline func: T.() -> Unit) = ifDataPresent(T::class, func)
    @Suppress("UNCHECKED_CAST") fun <T : Any> getData(klass: KClass<T>): T? = v.third.data[klass] as? T
    fun <T : Any> getDataOrElse(klass: KClass<T>, ifAbsent: T): T = getData(klass) ?: run {
        v.third.data[klass] = ifAbsent
        ifAbsent
    }
    fun <T : Any> putData(klass: KClass<T>, value: T) = v.third.data.put(klass, value)
    fun <T : Any> putDataIfAbsent(klass: KClass<T>, value: T) = v.third.data.getOrPut(klass) {value}
    fun <T : Any> hasData(klass: KClass<T>) = v.third.data.containsKey(klass)
    fun <T : Any> ifDataPresent(klass: KClass<T>, func: T.() -> Unit) {
        getData(klass)?.let { it.func() }
    }

}