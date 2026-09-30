package net.shadowmaster435.parsinator

import kotlinx.cinterop.ExperimentalForeignApi
import net.shadowmaster435.parsinator.exception.ParsinatorIllegalIndexException
import net.shadowmaster435.parsinator.exception.ParsinatorParseException
import net.shadowmaster435.parsinator.util.ParsinatorError
import platform.posix.fprintf
import platform.posix.stderr
import kotlin.math.min


@OptIn(ExperimentalForeignApi::class)
internal actual fun Parsinator.multiplatformBoundsDebug(newIndex: Int): Boolean {
    if (debugFlags.indexHardThrow) throw ParsinatorIllegalIndexException(string.length, newIndex)
    if (debugFlags.reportIndexBoundsErrors && newIndex !in 0..string.length) {
        fprintf(stderr, "%s\n", "Index just went too far out of bounds. Max safe index [${string.length}], Invalid index [$newIndex]")
        if (debugFlags.indexBoundsStackTrace) RuntimeException().printStackTrace()
        return true
    }
    return false
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun Parsinator.multiplatformParse(string: String) {

    locked = true
    this.actualString = string
    index = 0
    parser.parse(this)?.let {
        // this block is triggered when a parser returns an error
        if (debugFlags.parserHardThrow) throw ParsinatorParseException(string, ParsinatorError(index, it))
        if (debugFlags.reportParserErrors) {
            fprintf(
                stderr, "%s\n", ParsinatorParseException.makeMessage(
                    string, ParsinatorError(
                        min(
                            index,
                            string.length - 1
                        ), it
                    )
                )
            )
            index = string.length
        }
        this.actualString = null
        locked = false
    }
}