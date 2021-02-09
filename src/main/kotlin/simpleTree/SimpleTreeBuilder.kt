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
import org.antlr.v4.runtime.tree.xpath.XPath
import org.jetbrains.kotlin.spec.grammar.parser.KotlinLexer
import org.jetbrains.kotlin.spec.grammar.parser.KotlinParser
import org.jetbrains.kotlin.spec.grammar.parser.KotlinParserBaseVisitor

class SimpleTreeBuilder : KotlinParserBaseVisitor<SimpleTreeNode?>() {
    private val callStack = mutableListOf<Scope>()

    private val conditionalTokens = setOf(
        KotlinLexer.CONJ,
        KotlinLexer.DISJ,
        KotlinLexer.EXCL_WS,
        KotlinLexer.EXCL_NO_WS,
        KotlinLexer.EQEQ,
        KotlinLexer.EQEQEQ
    )

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

    // ABC metric

    override fun visitVariableDeclaration(ctx: KotlinParser.VariableDeclarationContext?): SimpleTreeNode? {
        val parent = ctx?.parent
        if (parent is KotlinParser.PropertyDeclarationContext && parent.ASSIGNMENT() != null) {
            (callStack[0].scopeOwner as RootNode).assignmentsCount++
        }
        return super.visitVariableDeclaration(ctx)
    }

    override fun visitAssignment(ctx: KotlinParser.AssignmentContext?): SimpleTreeNode? {
        (callStack[0].scopeOwner as RootNode).assignmentsCount++
        return super.visitAssignment(ctx)
    }

    override fun visitPrefixUnaryOperator(ctx: KotlinParser.PrefixUnaryOperatorContext?): SimpleTreeNode? {
        processOperator(ctx)
        return super.visitPrefixUnaryOperator(ctx)
    }

    override fun visitPostfixUnaryOperator(ctx: KotlinParser.PostfixUnaryOperatorContext?): SimpleTreeNode? {
        processOperator(ctx)
        return super.visitPostfixUnaryOperator(ctx)
    }

    private fun processOperator(ctx: ParserRuleContext?) {
        val firstChild = ctx?.children?.firstOrNull()
        if (firstChild is TerminalNode) {
            val type = firstChild.symbol.type
            if (type == KotlinLexer.INCR || type == KotlinLexer.DECR) {
                (callStack[0].scopeOwner as RootNode).assignmentsCount++
            }
        }
    }

    override fun visitCallableReference(ctx: KotlinParser.CallableReferenceContext?): SimpleTreeNode? {
        // todo (callStack[0].scopeOwner as RootNode).branchesCount++
        return super.visitCallableReference(ctx)
    }

    override fun visitCallSuffix(ctx: KotlinParser.CallSuffixContext?): SimpleTreeNode? {
        (callStack[0].scopeOwner as RootNode).branchesCount++
        return super.visitCallSuffix(ctx)
    }

    override fun visitEqualityOperator(ctx: KotlinParser.EqualityOperatorContext?): SimpleTreeNode? {
        (callStack[0].scopeOwner as RootNode).conditionsCount++
        return super.visitEqualityOperator(ctx)
    }

    override fun visitExcl(ctx: KotlinParser.ExclContext?): SimpleTreeNode? {
        (callStack[0].scopeOwner as RootNode).conditionsCount++
        return super.visitExcl(ctx)
    }

    override fun visitComparisonOperator(ctx: KotlinParser.ComparisonOperatorContext?): SimpleTreeNode? {
        (callStack[0].scopeOwner as RootNode).conditionsCount++
        return super.visitComparisonOperator(ctx)
    }

    override fun visitConjunction(ctx: KotlinParser.ConjunctionContext?): SimpleTreeNode? {
        if (ctx != null) {
            (callStack[0].scopeOwner as RootNode).conditionsCount += ctx.children.count { it is TerminalNode && it.symbol.type in conditionalTokens }
        }
        return super.visitConjunction(ctx)
    }

    override fun visitDisjunction(ctx: KotlinParser.DisjunctionContext?): SimpleTreeNode? {
        if (ctx != null) {
            (callStack[0].scopeOwner as RootNode).conditionsCount += ctx.children.count { it is TerminalNode && it.symbol.type in conditionalTokens }
        }
        return super.visitDisjunction(ctx)
    }

    // todo: add supportion of else in when
    override fun visitIfExpression(ctx: KotlinParser.IfExpressionContext?): SimpleTreeNode? {
        if (ctx != null) {
            (callStack[0].scopeOwner as RootNode).conditionsCount += ctx.children.count { it is TerminalNode && it.symbol.type == KotlinLexer.ELSE }
        }
        return super.visitIfExpression(ctx)
    }

    // todo: add supportion of else in when
    override fun visitTryExpression(ctx: KotlinParser.TryExpressionContext?): SimpleTreeNode? {
        if (ctx != null) {
            (callStack[0].scopeOwner as RootNode).conditionsCount += ctx.children.count {
                it is TerminalNode && (it.symbol.type == KotlinLexer.TRY)
            }
        }
        return super.visitTryExpression(ctx)
    }

    override fun visitCatchBlock(ctx: KotlinParser.CatchBlockContext?): SimpleTreeNode? {
        if (ctx != null) {
            (callStack[0].scopeOwner as RootNode).conditionsCount += ctx.children.count {
                it is TerminalNode && (it.symbol.type == KotlinLexer.CATCH)
            }
        }
        return super.visitCatchBlock(ctx)
    }



    override fun visitWhenEntry(ctx: KotlinParser.WhenEntryContext?): SimpleTreeNode? {
        // todo: is it needed?
        return super.visitWhenEntry(ctx)
    }

    override fun visitElvis(ctx: KotlinParser.ElvisContext?): SimpleTreeNode? {
        (callStack[0].scopeOwner as RootNode).conditionsCount++
        return super.visitElvis(ctx)
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