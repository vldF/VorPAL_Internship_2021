package foo

open class A

open class B

open class C : A() {
    override fun toString(): String {
        return super.toString()
    }

    val prop = 1
}

val test = ""