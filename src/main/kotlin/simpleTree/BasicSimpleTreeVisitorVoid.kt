package simpleTree

import ClassDeclarationNode
import ImportListNode
import ImportNode
import ImportPackageNode
import PackageNameNode
import RootNode
import SimpleBlockNode
import UnresolvedClass

open class BasicSimpleTreeVisitorVoid : BasicSimpleTreeVisitor<Unit>() {
    override fun visitRootNode(node: RootNode) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun visitSimpleBlockNode(node: SimpleBlockNode) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun visitClassDeclarationNode(node: ClassDeclarationNode) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun visitUnresolvedClass(node: UnresolvedClass) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun visitImportListNode(node: ImportListNode) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun visitImportNode(node: ImportNode) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun visitImportPackageNode(node: ImportPackageNode) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun visitPackageNameNode(node: PackageNameNode) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun mergeResults(previous: Unit, next: Unit) { }
}