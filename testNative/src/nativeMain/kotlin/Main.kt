
import net.shadowmaster435.parsinator.Parsinator
import net.shadowmaster435.parsinator.util.ParsinatorDebugFlags


fun main() {
    runTests()
}

fun runTests() {

//    println("--repeating")
//    test.testRepeating()
//    println("--branch")
//    test.testBranch()
//    println("--chain")
//    test.testChain()
    println("--recursion")
    testRecursion()
}

fun testRepeating() {
    Parsinator(
        ParsinatorDebugFlags(parserErrors = true, parserStackTrace = true)
    ) {
        repeating(false, false) {
            token(false, "test")
            whitespace()
            name(false, Regex("[a-z]")) makes {println(it); 1}
            trailing(whitespace())
        } makes {println(it)}
    }.parse("test toost    test the  ")
}

fun testChain() {
    val parser = Parsinator(
        ParsinatorDebugFlags(parserErrors = true, parserStackTrace = true)
    ) {
        chain(false) {
            token(false, "test") makes {false}
            whitespace()
            token(false, "2test") makes {true}
        } makes {println(it)}
    }
    parser.parse("test 2test")
    parser.parse("test test") // will report an error
}


fun testBranch() {
    val parser = Parsinator(
        ParsinatorDebugFlags(parserErrors = true, parserStackTrace = true)
    ) {
        branch(false) {
            token(false, "test") makes {false}
            token(false, "toast") makes {true}
            exit(5)
        } makes {println(it)}
    }
    parser.parse("test")
    parser.parse("toast")
    parser.parse("toost") // will not return anything
}

fun testRecursion() {
    val parser = Parsinator(
        ParsinatorDebugFlags(parserErrors = true, parserStackTrace = true)
    ) {
        val t = template {
            branch(false) {
                token(false, "t")
                token(false, "ss")
            } makes {it}
        }
        recurse(false, '{', '}') {
            use(t)
        } makes {println(it)}
    }
    parser.parse("{t{ss{tss}}t}")

}