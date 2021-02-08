import simpleTree.Scope
import javax.swing.tree.TreeNode

abstract class SimpleTreeNode {
    abstract val name: String
    abstract val scope: Scope

    val children: MutableList<SimpleTreeNode> = mutableListOf()

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
    val packageName
        get() = children.firstOrNull { it is PackageNameNode }?.name ?: "DEFAULT"

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
                    throw IllegalStateException("unresolved import: $importPackageName.$importName")
                }
            } else if (import is ImportPackageNode) {
                val importPackageNode = import.name
                val foreignTree = fileMap[importPackageNode] ?: throw IllegalStateException("unresolved import: $importPackageNode.*")
                scope.declarations.addAll(foreignTree.children.filterIsInstance<ClassDeclarationNode>())
            }
        }
    }
}

class ClassDeclarationNode(
    override val name: String,
    override val scope: Scope,
    val superclasses: MutableList<SimpleTreeNode>
) : SimpleTreeNode() {
    val knewSubclasses = mutableListOf<ClassDeclarationNode>()

    internal fun resolveAllSuperclasses() {
        for ((i, superclass) in superclasses.filterIsInstance<UnresolvedClass>().withIndex()) {
            val resolved = superclass.resolved ?: continue
            superclasses[i] = resolved
        }
    }
}

class UnresolvedClass(
    override val name: String,
    override val scope: Scope
) : SimpleTreeNode() {
    internal val resolved: ClassDeclarationNode?
        get() = scope.getResolvedDeclaration(name)
}

class ImportListNode(
    override val scope: Scope
) : SimpleTreeNode() {
    override val name = "import list"
}

class ImportNode(
    override val name: String,
    override val scope: Scope,
    val packageName: String
) : SimpleTreeNode()

class ImportPackageNode(
    override val name: String,
    override val scope: Scope
) : SimpleTreeNode()

class PackageNameNode(
    override val name: String,
    override val scope: Scope

) : SimpleTreeNode()