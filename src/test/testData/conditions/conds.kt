fun foo() {
    if (cond) {
        foo()
    } else if (cond == 1) {
        foo()
    } else {
        foo()
    }
    // c = 4

    when {
        a == 3 -> {}
        else -> {}
    }
    // c = 4 + 1 = 5

    try {
        foo()
    } catch (e: Exception) {
        foo()
    } catch (e: Exception) {
        foo()
    }

    // a = 0
    // b = 6
    // c = 5 + 3 = 8
}