package net.shadowmaster435.parsinator.util

import net.shadowmaster435.parsinator.Parsinator
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmSynthetic

@JvmInline
value class ParsinatorConstructor<T>(@JvmSynthetic internal val func: Parsinator.(obj: Any?) -> T)