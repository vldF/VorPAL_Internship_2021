import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import simpleTree.Scope
import statistics.ABCMetric

abstract class SimpleTreeNode {
    abstract val name: String
    abstract val scope: Scope

    abstract fun json(): JsonElement

    open val children: MutableList<SimpleTreeNode> = mutableListOf()

    fun resolveAll() {
        for (child in children) {
            if (child is ClassDeclarationNode) {
                child.resolveAllSuperclasses()
            } else {
                child.resolveAll()
            }
        }
    }
}

class RootNode(
    override val name: String,
    override val scope: Scope
) : SimpleTreeNode() {
    var globalABC = ABCMetric.empty
    val packageName
        get() = children.firstOrNull { it is PackageNameNode }?.name ?: "DEFAULT"

    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("Root"))
            add("name", JsonPrimitive(name))
            add("package", JsonPrimitive(packageName))
            add("scope", scope.json)
            add("children", children.toJson())
        }
    }

    fun merge(other: RootNode): RootNode {
        val newScope = scope.merge(other.scope)
        val result = RootNode("root", newScope)

        this.children.toMutableList().forEach { it.scope.previousScope = newScope }
        other.children.toMutableList().forEach { it.scope.previousScope = newScope }

        result.children.addAll(this.children)
        result.children.addAll(other.children)

        return result
    }

    fun doImports(fileMap: Map<String, RootNode>) {
        val importListNode = children.firstOrNull { it is ImportListNode }?.children ?: this.children

        for (import in importListNode) {
            if (import is ImportNode) {
                val importPackageName = import.packageName
                val importName = import.name

                val declaration = fileMap[importPackageName]?.children?.firstOrNull { it.name == importName } as? ClassDeclarationNode
                if (declaration != null) {
                    scope.declarations.add(declaration)
                } else {
                    System.err.println("unresolved import: $importPackageName.$importName")
                }
            } else if (import is ImportPackageNode) {
                val importPackageNode = import.name
                val foreignTree = fileMap[importPackageNode]
                if (foreignTree == null) {
                    System.err.println("unresolved import: $importPackageNode.*")
                    continue
                }
                scope.declarations.addAll(foreignTree.children.filterIsInstance<ClassDeclarationNode>())
            }
        }
    }
}

class SimpleBlockNode(
    override val scope: Scope
) : SimpleTreeNode() {
    override val name = ""

    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("simple block"))
            add("scope", scope.json)
            add("children", children.toJson())
        }
    }
}

class ClassDeclarationNode(
    override val name: String,
    override val scope: Scope,
    val superclasses: MutableList<SimpleTreeNode>
) : SimpleTreeNode() {
    var abcMetric = ABCMetric.empty

    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("Class Declaration"))
            add("name", JsonPrimitive(name))
            add("scope", scope.json)

            val superclassesArray = JsonArray().apply {
                superclasses
                    .map { if (it is ClassDeclarationNode) JsonPrimitive(it.name) else it.json() }
                    .forEach { add(it) }
            }

            add("superclasses", superclassesArray)
            add("children", children.toJson())
        }
    }

    internal fun resolveAllSuperclasses() {
        for ((i, superclass) in superclasses.filterIsInstance<UnresolvedClass>().withIndex()) {
            val resolved = superclass.resolved ?: continue
            superclasses[i] = resolved
        }
    }

    val resolvedSuperclasses
        get() = superclasses.filterIsInstance<ClassDeclarationNode>()



}

class UnresolvedClass(
    override val name: String,
    override val scope: Scope
) : SimpleTreeNode() {
    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("unresolved class"))
            add("name", JsonPrimitive(name))
            add("scope", scope.json)
        }
    }

    internal val resolved: ClassDeclarationNode?
        get() = scope.getResolvedDeclaration(name)
}

class ImportListNode(
    override val scope: Scope
) : SimpleTreeNode() {
    override val name = "import list"

    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("import list"))
            add("name", JsonPrimitive(name))
            add("children", children.toJson())
        }
    }
}

class ImportNode(
    override val name: String,
    override val scope: Scope,
    val packageName: String
) : SimpleTreeNode() {
    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("import"))
            add("packageName", JsonPrimitive(packageName))
            add("name", JsonPrimitive(name))
        }
    }
}

class ImportPackageNode(
    override val name: String,
    override val scope: Scope
) : SimpleTreeNode() {
    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("import package"))
            add("packageName", JsonPrimitive(name))
        }
    }
}

class PackageNameNode(
    override val name: String,
    override val scope: Scope

) : SimpleTreeNode() {
    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("package name"))
            add("name", JsonPrimitive(name))
        }
    }
}

class OverrideFunctionNode(
    override val name: String,
    override val scope: Scope

) : SimpleTreeNode() {
    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("override function"))
            add("name", JsonPrimitive(name))
            add("scope", scope.json)
            add("children", children.toJson())
        }
    }
}

class PropertyNode(
    override val name: String,
    override val scope: Scope
) : SimpleTreeNode() {
    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("property"))
            add("name", JsonPrimitive(name))
            add("scope", scope.json)
            add("children", children.toJson())
        }
    }
}

class NodeGroup(
    override val scope: Scope,
    override val children: MutableList<SimpleTreeNode>
) : SimpleTreeNode() {
    override val name: String = "nodes group"

    override fun json(): JsonElement {
        return JsonObject().apply {
            add("type", JsonPrimitive("node group"))
            add("children", children.toJson())
        }
    }
}

private fun Collection<SimpleTreeNode>.toJson(): JsonArray {
    val result = JsonArray()
    for (element in this) {
        result.add(element.json())
    }

    return result
}