interface I

abstract class A

class B : A(), I

object O : I

fun foo() {
    val t = object : I {

    }
}