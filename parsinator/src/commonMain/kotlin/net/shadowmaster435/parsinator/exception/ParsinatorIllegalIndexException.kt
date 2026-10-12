package net.shadowmaster435.parsinator.exception

class ParsinatorIllegalIndexException(len: Int, index: Int) : Exception(MESSAGE.replace("!", len.toString()).replace("?", index.toString())) {

    companion object {
        const val MESSAGE = "Index just went too far out of bounds. Max length !, Index ?"
    }
}