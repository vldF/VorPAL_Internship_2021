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
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.kotlin.spec.grammar.parser.KotlinParser
import org.jetbrains.kotlin.spec.grammar.parser.KotlinParserBaseVisitor

class SimpleTreeBuilder : KotlinParserBaseVisitor<SimpleTreeNode?>() {
    private val callStack = mutableListOf<Scope>()

    override fun visitClassDeclaration(ctx: KotlinParser.ClassDeclarationContext?): SimpleTreeNode? {
        return visitClassLikeDeclaration(ctx)
    }

    override fun visitObjectDeclaration(ctx: KotlinParser.ObjectDeclarationContext?): SimpleTreeNode? {
        return visitClassLikeDeclaration(ctx)
    }

    override fun visitBlock(ctx: KotlinParser.BlockContext?): SimpleTreeNode? {
        if (ctx == null) return null

        val newScope = Scope("block[${ctx.hashCode()}]", callStack.last())
        callStack.add(newScope)

        val result = SimpleBlockNode(newScope)
        for (child in ctx.children) {
            val newNode = child.accept(this) ?: continue
            result.children.add(newNode)
        }

        callStack.removeLast()
        return result
    }

    override fun visitObjectLiteral(ctx: KotlinParser.ObjectLiteralContext?): SimpleTreeNode? {
        return visitClassLikeDeclaration(ctx)
    }

    private fun visitClassLikeDeclaration(ctx: ParserRuleContext?): SimpleTreeNode? {
        if (ctx == null) return null

        var name = ctx.name

        if (name == null) {
            if (ctx is KotlinParser.ObjectLiteralContext) {
                name = "[anonymous object]"
            } else {
                throw IllegalArgumentException("declaration name wasn't found")
            }
        }

        val currentScope = callStack.last()
        val newScope = Scope(name, currentScope)
        callStack.add(newScope)

        val supertypeNames = ctx.superclassesNames
        val unresolvedSupertypes = supertypeNames.map { UnresolvedClass(it, newScope) }

        val declarationNode = ClassDeclarationNode(name, newScope, unresolvedSupertypes.toMutableList())
        currentScope.declarations.add(declarationNode)

        when (ctx) {
            is KotlinParser.ClassDeclarationContext -> {
                super.visitClassDeclaration(ctx)
            }
            is KotlinParser.ObjectDeclarationContext -> {
                super.visitObjectDeclaration(ctx)
            }
            is KotlinParser.ObjectLiteralContext -> {
                super.visitObjectLiteral(ctx)
            }
        }

        callStack.removeLast()
        return declarationNode
    }

    override fun aggregateResult(aggregate: SimpleTreeNode?, nextResult: SimpleTreeNode?): SimpleTreeNode? {
        if (aggregate is ImportNode || aggregate is ImportPackageNode) {
            val importList = ImportListNode(callStack.last())
            importList.children.add(aggregate)
            if (nextResult != null) {
                importList.children.add(nextResult)
            }
            return importList
        } else if (aggregate is ImportListNode) {
            if (nextResult != null) {
                aggregate.children.add(nextResult)
            }
        }
        return aggregate ?: nextResult
    }

    override fun visitKotlinFile(ctx: KotlinParser.KotlinFileContext?): SimpleTreeNode? {
        val rootScope = Scope("global")
        callStack.add(rootScope)

        val rootNode = RootNode("root", rootScope)
        if (ctx == null) return null
        for (child in ctx.children) {
            val newNode = child.accept(this) ?: continue
            rootNode.children.add(newNode)
        }

        return rootNode
    }

    override fun visitImportHeader(ctx: KotlinParser.ImportHeaderContext?): SimpleTreeNode? {
        if (ctx == null) return null

        return if (ctx.MULT() != null) {
            // this is import *
            val packageName = ctx.identifier().text
            ImportPackageNode(packageName, callStack.last())
        } else {
            // this is concrete import
            val fullImportName = ctx.identifier().text
            val fullNameSplit = fullImportName.split(".")
            val importName = fullNameSplit.last()
            val packageName = fullImportName.removeSuffix(".$importName")

            ImportNode(importName, callStack.last(), packageName)
        }
    }

    override fun visitPackageHeader(ctx: KotlinParser.PackageHeaderContext?): SimpleTreeNode? {
        val name = ctx?.identifier()?.text ?: return null
        return PackageNameNode(name, callStack.last())
    }

    private val ParserRuleContext.superclassesNames: List<String>
        get() {
            return children
                .filterIsInstance<KotlinParser.DelegationSpecifiersContext>()
                .firstOrNull()
                ?.children
                ?.filterIsInstance<KotlinParser.AnnotatedDelegationSpecifierContext>()
                ?.mapNotNull { it.simpleIdentifierName ?: it.text }
                ?.toList() ?: listOf()
        }

    private val KotlinParser.AnnotatedDelegationSpecifierContext.simpleIdentifierName
        get() = delegationSpecifier()
            ?.constructorInvocation()
            ?.userType()
            ?.simpleUserType(0)
            ?.simpleIdentifier()
            ?.Identifier()
            ?.symbol
            ?.text

    private val ParserRuleContext.name
        get() = children
            .filterIsInstance<KotlinParser.SimpleIdentifierContext>()
            .firstOrNull()
            ?.children
            ?.firstOrNull { it is TerminalNode }
            ?.toString()

}