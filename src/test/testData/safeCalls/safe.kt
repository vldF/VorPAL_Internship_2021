fun foo() {
    val a: Int? = null
    a?.and(1) ?: 0
}
// a = 1, b = 1, c = 1