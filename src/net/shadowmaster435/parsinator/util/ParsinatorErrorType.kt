package net.shadowmaster435.parsinator.util

enum class ParsinatorErrorType(val message: String) {
    EOF("Reached end of file"),
    CHAIN_FAIL("Invalid syntax for chain"),
    CHAIN_EOF("Chain reached end of file."),
    CHAIN_INCORRECT_LENGTH("Not enough entries for chain"),
    BRANCH_FAIL("Branch failed to parse"),
    EMPTY_REPEAT("Must contain at least one entry"),
    ILLEGAL_START_CHAR("Illegal start char"),
}