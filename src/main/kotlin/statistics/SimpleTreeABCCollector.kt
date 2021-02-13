package statistics


import simpleTree.BasicSimpleTreeVisitor
import simpleTree.ClassDeclarationNode

object SimpleTreeABCCollector : BasicSimpleTreeVisitor<Map<ClassDeclarationNode, ABCMetric>>() {
    override fun visitClassDeclarationNode(node: ClassDeclarationNode): Map<ClassDeclarationNode, ABCMetric> {
        return mutableMapOf(node to (node.abcMetric))
    }

    override fun List<Map<ClassDeclarationNode, ABCMetric>>.mergeResults(): Map<ClassDeclarationNode, ABCMetric> {
        return this.fold(mutableMapOf()) { res, new ->
            res.putAll(new)
            res
        }
    }
}