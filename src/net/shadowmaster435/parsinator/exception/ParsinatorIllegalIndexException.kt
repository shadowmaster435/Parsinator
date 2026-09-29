package net.shadowmaster435.parsinator.exception

class ParsinatorIllegalIndexException(len: Int, index: Int) : Exception(MESSAGE.format(len, index)) {

    companion object {
        const val MESSAGE = "Index just went too far out of bounds. Max length [%s], Index [%s]"
    }
}