package simpleTree

import ClassDeclarationNode

class Scope(
    var previousScope: Scope? = null
) {
    val declarations = hashSetOf<ClassDeclarationNode>()

    internal fun getResolvedDeclaration(name: String): ClassDeclarationNode? {
        return declarations.firstOrNull { it.name == name } ?: previousScope?.getResolvedDeclaration(name)
    }

    fun merge(other: Scope): Scope {
        val newScope = Scope()
        newScope.declarations.addAll(this.declarations)
        newScope.declarations.addAll(other.declarations)

        return newScope
    }
}