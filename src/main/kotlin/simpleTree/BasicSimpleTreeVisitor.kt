package simpleTree

import ClassDeclarationNode
import ImportListNode
import ImportNode
import ImportPackageNode
import PackageNameNode
import RootNode
import SimpleBlockNode
import SimpleTreeNode
import UnresolvedClass

abstract class BasicSimpleTreeVisitor<T> : AbstractSimpleTreeVisitor<T>() {
    override fun visitSimpleTreeNode(node: SimpleTreeNode): T {
        return when (node) {
            is RootNode -> visitRootNode(node)
            is SimpleBlockNode -> visitSimpleBlockNode(node)
            is ClassDeclarationNode -> visitClassDeclarationNode(node)
            is UnresolvedClass -> visitUnresolvedClass(node)
            is ImportListNode -> visitImportListNode(node)
            is ImportNode -> visitImportNode(node)
            is ImportPackageNode -> visitImportPackageNode(node)
            is PackageNameNode -> visitPackageNameNode(node)

            else -> throw IllegalArgumentException("unknown node type: ${node::class}")
        }
    }

    override fun visitRootNode(node: RootNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.reduce(::mergeResults)
    }

    override fun visitSimpleBlockNode(node: SimpleBlockNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.reduce(::mergeResults)
    }

    override fun visitClassDeclarationNode(node: ClassDeclarationNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.reduce(::mergeResults)
    }

    override fun visitUnresolvedClass(node: UnresolvedClass): T {
        return node.children.map { visitSimpleTreeNode(it) }.reduce(::mergeResults)
    }

    override fun visitImportListNode(node: ImportListNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.reduce(::mergeResults)
    }

    override fun visitImportNode(node: ImportNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.reduce(::mergeResults)
    }

    override fun visitImportPackageNode(node: ImportPackageNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.reduce(::mergeResults)
    }

    override fun visitPackageNameNode(node: PackageNameNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.reduce(::mergeResults)
    }

    abstract fun mergeResults(previous: T, next: T): T
}