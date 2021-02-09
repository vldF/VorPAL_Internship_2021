interface I

abstract class A

class B : A(), I

fun foo() {
    val t = object : I {

    }
}