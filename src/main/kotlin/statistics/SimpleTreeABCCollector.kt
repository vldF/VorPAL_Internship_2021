package statistics

import ClassDeclarationNode
import simpleTree.BasicSimpleTreeVisitor

object SimpleTreeABCCollector : BasicSimpleTreeVisitor<Map<ClassDeclarationNode, ABCMetric>>() {
    override fun visitClassDeclarationNode(node: ClassDeclarationNode): Map<ClassDeclarationNode, ABCMetric> {
        return mutableMapOf(node to (node.abcMetric ?: ABCMetric()))
    }

    override fun List<Map<ClassDeclarationNode, ABCMetric>>.mergeResults(): Map<ClassDeclarationNode, ABCMetric> {
        return this.fold(mutableMapOf()) { res, new ->
            res.putAll(new)
            res
        }
    }
}