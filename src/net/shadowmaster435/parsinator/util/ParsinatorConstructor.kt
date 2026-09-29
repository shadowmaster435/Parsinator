package net.shadowmaster435.parsinator.util

@JvmInline
value class ParsinatorConstructor<T>(@JvmSynthetic internal val func: (obj: Any?) -> T)