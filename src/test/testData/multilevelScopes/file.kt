fun foo() {
    fun boo() {
        open class A
        open class B : A()
        open class C : B()
    }

    fun bar() {
        class D : C()
    }
}