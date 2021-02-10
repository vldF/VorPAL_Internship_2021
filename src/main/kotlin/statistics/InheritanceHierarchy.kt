package statistics

import ClassDeclarationNode
import UnresolvedClass
import simpleTree.BasicSimpleTreeVisitorVoid

class InheritanceHierarchy : BasicSimpleTreeVisitorVoid() {
    private val hierarchy = mutableMapOf<ClassDeclarationNode, MutableList<ClassDeclarationNode>>()
    private val builtPaths = mutableMapOf<ClassDeclarationNode, MutableList<MutableList<ClassDeclarationNode>>>()

    val inheritanceChains: List<List<ClassDeclarationNode>>
        get() {
            val result = mutableListOf<List<ClassDeclarationNode>>()
            for (superClass in hierarchy.keys) {
                val chains = hierarchyWalker(superClass)
                val currentChain = mutableListOf<List<ClassDeclarationNode>>()
                for (chain in chains) {
                    chain.add(0, superClass)
                    currentChain.add(chain)
                }
                result.addAll(currentChain)
            }

            return result
        }

    private fun hierarchyWalker(node: ClassDeclarationNode): MutableList<MutableList<ClassDeclarationNode>> {
        if (node in builtPaths.keys) return builtPaths[node]!!.copy
        val superclasses = hierarchy[node] ?: return mutableListOf(mutableListOf())
        val result = mutableListOf<MutableList<ClassDeclarationNode>>()
        for (superClass in superclasses) {
            result.addAll(hierarchyWalker(superClass).map {
                it.add(0, superClass)
                it
            }.toMutableList())
        }

        builtPaths[node] = result.copy

        return result
    }

    override fun visitClassDeclarationNode(node: ClassDeclarationNode) {
        for (superClass in node.superclasses) {
            val virtualSuperClass = if (superClass is ClassDeclarationNode) {
                superClass
            } else {
                superClass as UnresolvedClass
                ClassDeclarationNode(superClass.name, superClass.scope, mutableListOf())
            }
            if (hierarchy[virtualSuperClass] != null) {
                hierarchy[virtualSuperClass]!!.add(node)
            } else {
                hierarchy[virtualSuperClass] = mutableListOf(node)
            }
        }

        super.visitClassDeclarationNode(node)
    }

    private val MutableList<MutableList<ClassDeclarationNode>>.copy
        get() = mutableListOf<MutableList<ClassDeclarationNode>>().also {
            for (list in this) {
                it.add(list.toMutableList())
            }
        }
}
