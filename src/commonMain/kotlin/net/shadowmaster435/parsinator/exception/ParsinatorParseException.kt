package net.shadowmaster435.parsinator.exception

import net.shadowmaster435.parsinator.util.ParsinatorError
import kotlin.text.iterator

class ParsinatorParseException(string: String, error: ParsinatorError) : Exception(
    makeMessage(
        string,
        error
    )
) {
    companion object {
        fun makeMessage(string: String, error: ParsinatorError): String {
            val lineColumn = string.indexToLineColumn(error.index)
            val atString = "at line ${lineColumn.first}, column ${lineColumn.second}"
            return "$atString\nFailed to parse string with error '${error.type.message}'"
        }
        fun String.indexToLineColumn(index: Int): Pair<Int, Int> {
            if (index !in indices) throw IndexOutOfBoundsException("Index $index out of string bounds")
            var currentIndex = index
            var line = 0
            var column = 0
            for (it in this) {
                if (it == '\n') {
                    line++
                    currentIndex -= column
                    column = 0
                } else column++
                if (column >= currentIndex) break
            }
            return line to currentIndex
        }
    }
}