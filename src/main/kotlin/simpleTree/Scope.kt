package simpleTree

import ClassDeclarationNode
import com.google.gson.JsonArray

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

    private val visibleDeclaration : Set<ClassDeclarationNode>
        get() = declarations + (previousScope?.visibleDeclaration ?: setOf())

    val json: JsonArray
        get() {
            val declarations = visibleDeclaration
            val array = JsonArray(declarations.size)
            declarations.forEach { array.add(it.name) }

            return array
        }
}