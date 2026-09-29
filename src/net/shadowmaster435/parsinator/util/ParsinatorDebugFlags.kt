package net.shadowmaster435.parsinator.util

@JvmInline
value class ParsinatorDebugFlags private constructor(
    private val flags: Int
) {
    val reportIndexBoundsErrors get() = (flags and 0b1) != 0
    val indexBoundsStackTrace get() = (flags and 0b10) != 0
    val indexHardThrow get() = (flags and 0b100) != 0
    val reportParserErrors get() = (flags and 0b1000) != 0
    val parserStackTrace get() = (flags and 0b10000) != 0
    val parserHardThrow get() = (flags and 0b100000) != 0

    infix fun or(other: ParsinatorDebugFlags) = flags or other.flags
    constructor(
        indexBoundsErrors: Boolean = false,
        indexBoundsStackTrace: Boolean = false,
        indexHardThrow: Boolean = false,
        parserErrors: Boolean = false,
        parserStackTrace: Boolean = false,
        parserHardThrow: Boolean = false
    ) : this(
        merge(
            indexBoundsErrors,
            indexBoundsStackTrace,
            indexHardThrow,
            parserErrors,
            parserStackTrace,
            parserHardThrow
        )
    )



    companion object {
        @JvmStatic
        val NONE = ParsinatorDebugFlags(0)
        @JvmStatic
        val PARSER = ParsinatorDebugFlags(0b10000)

        @JvmStatic
        fun create(flags: Int) = ParsinatorDebugFlags(flags)

        private fun merge(vararg flags: Boolean): Int {
            var result = 0
            repeat(flags.size) {
                val flag = flags[it]
                result = result or ((if (flag) 1 else 0) shl it)
            }
            return result
        }
    }
}