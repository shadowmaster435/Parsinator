package net.shadowmaster435.parsinator.parser

import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorErrorType
import net.shadowmaster435.parsinator.util.ParsinatorParserOutput

class ChainParsinatorParser(
    optional: Boolean,
    @JvmField vararg val chain: AbstractParsinatorParser,
) : AbstractParsinatorParser(optional) {
    init {
        if (chain.isEmpty()) throw IllegalArgumentException("A chain must contain at least one value.")
    }
    val minRequiredLength = chain.count {
        !it.optional && !(it is WrapperParsinatorParser && it.isTrailing) && !(
            it is WhitespaceParsinatorParser || it is WrapperParsinatorParser && it.isWhitespace
        )
    }

    override fun shouldParse(parsinator: Parsinator) = chain.first().shouldParse(parsinator)

    override fun parse(parsinator: Parsinator): ParsinatorErrorType? {
        val chainStorage = mutableListOf<Any?>()
        var length = 0
        var index = 0
        val start = parsinator.index
        var first = true
        while (true) {
            if (index >= chain.size) {
                if (!optional && length < minRequiredLength) return ParsinatorErrorType.CHAIN_INCORRECT_LENGTH
                storage = chainStorage
                finish(null, start..parsinator.index)
                storage = construct(parsinator)
                return null
            }
            val entry = chain[index]
            val trailing = (entry is WrapperParsinatorParser) && (entry.isTrailing)
            if (trailing && parsinator.char == null) {
                if (!optional && length < minRequiredLength) return ParsinatorErrorType.CHAIN_INCORRECT_LENGTH
                storage = chainStorage
                finish(null, start..parsinator.index)
                storage = construct(parsinator)
                return null
            }
            if (first || entry.shouldParse(parsinator)) {

                val parsed = entry.parse(parsinator)

                if (trailing && parsed != null) {
                    storage = chainStorage
                    finish(null, start..parsinator.index)
                    storage = construct(parsinator)
                    return null
                }
                if (parsed == null || parsed == ParsinatorErrorType.EOF || entry.optional) {
                    if (!entry.optional) length++
                    val isWhitespace =
                        entry is WhitespaceParsinatorParser || entry is WrapperParsinatorParser && entry.isWhitespace
                    if (parsed != ParsinatorErrorType.EOF) {
                        if (!isWhitespace) chainStorage.add(entry.storage)
                    } else if (parsinator.char == null) return ParsinatorErrorType.CHAIN_EOF
                    else if (!optional) return ParsinatorErrorType.CHAIN_FAIL
                    index++
                }
                else return parsed
                first = false
            } else return if (entry.optional) null else ParsinatorErrorType.CHAIN_FAIL
        }
    }
}