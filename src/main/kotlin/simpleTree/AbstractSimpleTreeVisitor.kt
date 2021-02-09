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

abstract class AbstractSimpleTreeVisitor<T> {
    abstract fun visitSimpleTreeNode(node: SimpleTreeNode): T

    abstract fun visitRootNode(node: RootNode): T

    abstract fun visitSimpleBlockNode(node: SimpleBlockNode): T

    abstract fun visitClassDeclarationNode(node: ClassDeclarationNode): T

    abstract fun visitUnresolvedClass(node: UnresolvedClass): T

    abstract fun visitImportListNode(node: ImportListNode): T

    abstract fun visitImportNode(node: ImportNode): T

    abstract fun visitImportPackageNode(node: ImportPackageNode): T

    abstract fun visitPackageNameNode(node: PackageNameNode): T
}