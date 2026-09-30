package net.shadowmaster435.parsinator.util

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmSynthetic

@JvmInline
value class ParsinatorConstructor<T>(@JvmSynthetic internal val func: (obj: Any?) -> T)