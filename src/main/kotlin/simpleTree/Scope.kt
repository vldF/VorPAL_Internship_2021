package simpleTree

import ClassDeclarationNode
import SimpleTreeNode
import com.google.gson.JsonArray

class Scope(
    private val name: String,
    var previousScope: Scope? = null
) {
    val declarations = hashSetOf<ClassDeclarationNode>()
    var scopeOwner: SimpleTreeNode? = null

    internal fun getResolvedDeclaration(name: String): ClassDeclarationNode? {
        return declarations.firstOrNull { it.name == name } ?: previousScope?.getResolvedDeclaration(name)
    }

    fun merge(other: Scope): Scope {
        val newScope = Scope(name)
        newScope.declarations.addAll(this.declarations)
        newScope.declarations.addAll(other.declarations)

        return newScope
    }

    private val visibleDeclaration : Set<ClassDeclarationNode>
        get() = declarations + (previousScope?.visibleDeclaration ?: setOf())

    val json: JsonArray
        get() {
            val declarations = visibleDeclaration.sortedBy { it.name }
            val array = JsonArray(declarations.size)
            declarations.forEach { array.add(it.name) }

            return array
        }

    override fun toString(): String {
        return "scope[$fullName]"
    }

    val fullName: String
        get() = (previousScope?.fullName ?: "") + "." + name

    companion object {
        val empty = Scope("empty scope")
    }
}