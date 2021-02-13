package simpleTree

import ClassDeclarationNode
import ImportListNode
import ImportNode
import ImportPackageNode
import NodeGroup
import OverrideFunctionNode
import PackageNameNode
import PropertyNode
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
            is PropertyNode -> visitPropertyNode(node)
            is OverrideFunctionNode -> visitOverrideFunctionNode(node)
            is NodeGroup -> {
                System.err.println("Node group in simple tree. Visiting will be OK, but sometimes it's wired")
                System.err.println(node.json().toString())
                visitNodeGroup(node)
            }

            else -> throw IllegalArgumentException("unknown node type: ${node::class}")
        }
    }

    override fun visitRootNode(node: RootNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitSimpleBlockNode(node: SimpleBlockNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitClassDeclarationNode(node: ClassDeclarationNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitUnresolvedClass(node: UnresolvedClass): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitImportListNode(node: ImportListNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitImportNode(node: ImportNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitImportPackageNode(node: ImportPackageNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitPackageNameNode(node: PackageNameNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitOverrideFunctionNode(node: OverrideFunctionNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitPropertyNode(node: PropertyNode): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    override fun visitNodeGroup(node: NodeGroup): T {
        return node.children.map { visitSimpleTreeNode(it) }.mergeResults()
    }

    abstract fun List<T>.mergeResults(): T
}