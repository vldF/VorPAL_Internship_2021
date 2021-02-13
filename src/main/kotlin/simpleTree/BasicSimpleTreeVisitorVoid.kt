package simpleTree

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

    override fun visitPropertyNode(node: PropertyNode) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun visitOverrideFunctionNode(node: OverrideFunctionNode) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun visitNodeGroup(node: NodeGroup) {
        node.children.map { visitSimpleTreeNode(it) }
    }

    override fun List<Unit>.mergeResults() { }
}