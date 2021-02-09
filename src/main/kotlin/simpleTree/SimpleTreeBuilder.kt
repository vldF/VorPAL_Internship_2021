package simpleTree

import ClassDeclarationNode
import ImportListNode
import ImportNode
import ImportPackageNode
import OverrideFunctionNode
import PackageNameNode
import PropertyNode
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
        val block = SimpleBlockNode(newScope)
        newScope.scopeOwner = block

        for (child in ctx.children) {
            val newNode = child.accept(this) ?: continue
            block.children.add(newNode)
        }

        callStack.removeLast()
        return block
    }

    override fun visitObjectLiteral(ctx: KotlinParser.ObjectLiteralContext?): SimpleTreeNode? {
        return visitClassLikeDeclaration(ctx)
    }

    override fun visitFunctionDeclaration(ctx: KotlinParser.FunctionDeclarationContext?): SimpleTreeNode? {
        if (ctx == null) return super.visitFunctionDeclaration(ctx)

        val modifiers = ctx.modifiers
        if ("override" !in modifiers) return super.visitFunctionDeclaration(ctx)

        val name = ctx.name ?: "no name"

        val lastClassLikeDeclarationNodeScope = lastClassLikeScope
        lastClassLikeDeclarationNodeScope?.scopeOwner?.children?.add(
            OverrideFunctionNode(name, lastClassLikeDeclarationNodeScope)
        )

        return super.visitFunctionDeclaration(ctx)
    }

    override fun visitClassMemberDeclarations(ctx: KotlinParser.ClassMemberDeclarationsContext?): SimpleTreeNode? {
        if (ctx == null) return super.visitClassMemberDeclarations(ctx)

        val propertiesNames = ctx.properties
        val lastClassScope = lastClassLikeScope ?: return super.visitClassMemberDeclarations(ctx)

        lastClassScope.scopeOwner?.children?.addAll(propertiesNames.map { PropertyNode(it, lastClassScope) })

        return super.visitClassMemberDeclarations(ctx)
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
        newScope.scopeOwner = declarationNode

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
        rootScope.scopeOwner = rootNode

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

    private val KotlinParser.FunctionDeclarationContext.modifiers: List<String>
        get() = children
            .filterIsInstance<KotlinParser.ModifiersContext>()
            .firstOrNull()
            ?.children
            ?.filterIsInstance<KotlinParser.ModifierContext>()
            ?.mapNotNull { it.text }
            ?: listOf()

    private val KotlinParser.ClassMemberDeclarationsContext.properties
        get() = children
            ?.filterIsInstance<KotlinParser.ClassMemberDeclarationContext>()
            //.filter { it.children.any { it is KotlinParser.DeclarationContext && it.children.any { it is KotlinParser.PropertyDeclarationContext } } }
            ?.flatMap { it.children.filterIsInstance<KotlinParser.DeclarationContext>() }
            ?.mapNotNull { it.propertyDeclaration()?.variableDeclaration()?.name }
            .orEmpty()

    private val lastClassLikeScope
        get() = callStack.findLast { it.scopeOwner is ClassDeclarationNode }
}