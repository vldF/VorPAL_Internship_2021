fun foo() {
    var a = 1
    a = 2
    a += 3
    a -= 4
    a *= 5
    a /= 6

    foo() // b = 1
    foo() // b = 2
    foo() // b = 3

    if (a == foo() && !false) { // b = 4, c = 3

    } else { // c = 4

    }

    try { // c = 5
        foo()
    } catch (e: java.lang.Exception) { // c = 6

    }
}