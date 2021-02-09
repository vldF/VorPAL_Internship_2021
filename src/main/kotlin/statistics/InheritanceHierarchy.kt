package statistics

import ClassDeclarationNode
import simpleTree.BasicSimpleTreeVisitorVoid

class InheritanceHierarchy : BasicSimpleTreeVisitorVoid() {
    private val hierarchy = mutableMapOf<ClassDeclarationNode, MutableList<ClassDeclarationNode>>()

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
        val n = hierarchy[node] ?: return mutableListOf(mutableListOf(node))
        val result = mutableListOf<MutableList<ClassDeclarationNode>>()
        for (superClass in n) {
            result.addAll(hierarchyWalker(superClass))
        }
        return result
    }

    override fun visitClassDeclarationNode(node: ClassDeclarationNode) {
        for (superClass in node.resolvedSuperclasses) {
            if (hierarchy[superClass] != null) {
                hierarchy[superClass]!!.add(node)
            } else {
                hierarchy[superClass] = mutableListOf(node)
            }
        }

        super.visitClassDeclarationNode(node)
    }

    override fun mergeResults(previous: Unit, next: Unit) { }
}
