package statistics

import ClassDeclarationNode
import OverrideFunctionNode
import PropertyNode
import simpleTree.BasicSimpleTreeVisitor

class ClassInfoCollector : BasicSimpleTreeVisitor<Set<ClassUsage>>() {
    override fun visitClassDeclarationNode(node: ClassDeclarationNode): Set<ClassUsage> {
        val overrides = node.children.count { it is OverrideFunctionNode }
        val propertiesCount = node.children.count { it is PropertyNode }
        return super.visitClassDeclarationNode(node) + ClassUsage(node, overrides, propertiesCount)
    }

    override fun List<Set<ClassUsage>>.mergeResults(): Set<ClassUsage> {
        return this.fold(setOf()) { a, b -> a + b }
    }
}

data class ClassUsage(
    val classNode: ClassDeclarationNode,
    val overridens: Int,
    val propertiesCount: Int
)