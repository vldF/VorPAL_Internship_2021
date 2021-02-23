package simpletree

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.kotlin.spec.grammar.parser.KotlinParser
import org.jetbrains.kotlin.spec.grammar.parser.KotlinParserBaseVisitor
import statistics.ABCCollector
import statistics.ABCMetric

class SimpleTreeBuilder : KotlinParserBaseVisitor<SimpleTreeNode?>() {
    private val callStack = mutableListOf<Scope>()
    private val abcCollector = ABCCollector()

    override fun visitKotlinFile(ctx: KotlinParser.KotlinFileContext?): SimpleTreeNode? {
        val rootScope = Scope("global")
        callStack.add(rootScope)

        val rootNode = RootNode("root", rootScope)
        rootScope.scopeOwner = rootNode

        // getting ABC for non-class functions, declarations, etc.
        rootNode.globalABC = abcCollector.visitKotlinFile(ctx)

        if (ctx == null) return null
        for (child in ctx.children) {
            val newNode = child.accept(this) ?: continue
            if (newNode !is NodeGroup) {
                rootNode.children.add(newNode)
            } else {
                rootNode.children.addAll(newNode.children)
            }
        }

        return rootNode
    }

    override fun visitClassDeclaration(ctx: KotlinParser.ClassDeclarationContext?): SimpleTreeNode? {
        return visitClassLikeDeclaration(ctx)
    }

    override fun visitObjectDeclaration(ctx: KotlinParser.ObjectDeclarationContext?): SimpleTreeNode? {
        return visitClassLikeDeclaration(ctx)
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
                System.err.println("declaration $name wasn't found")
                visitClassLikeDefaults(ctx)
                return null
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

        // getting ABC top-name non-classes declarations' ABC
        // yeah, this is wrong place to get it
        declarationNode.abcMetric = collectClassABC(ctx)

        val resultVisitDeclaration = visitClassLikeDefaults(ctx)
        if (resultVisitDeclaration is NodeGroup) {
            declarationNode.children.addAll(resultVisitDeclaration.children)
        } else if (resultVisitDeclaration != null) {
            declarationNode.children.add(resultVisitDeclaration)
        }

        callStack.removeLast()
        return declarationNode
    }

    private fun collectClassABC(ctx: ParserRuleContext): ABCMetric {
        val classBody = when (ctx) {
            is KotlinParser.ClassDeclarationContext -> {
                ctx.classBody()
            }
            is KotlinParser.ObjectDeclarationContext -> {
                ctx.classBody()
            }
            is KotlinParser.ObjectLiteralContext -> {
                ctx.classBody()
            }
            else -> throw IllegalStateException()
        }

        return classBody?.let {
            abcCollector.visit(it)
        } ?: ABCMetric.empty
    }

    private fun visitClassLikeDefaults(ctx: ParserRuleContext): SimpleTreeNode? {
        return when (ctx) {
            is KotlinParser.ClassDeclarationContext -> {
                super.visitClassDeclaration(ctx)
            }
            is KotlinParser.ObjectDeclarationContext -> {
                super.visitObjectDeclaration(ctx)
            }
            is KotlinParser.ObjectLiteralContext -> {
                super.visitObjectLiteral(ctx)
            }
            else -> visit(ctx)
        }
    }

    override fun visitBlock(ctx: KotlinParser.BlockContext?): SimpleTreeNode? {
        if (ctx == null) return null

        val newScope = Scope("block[${ctx.hashCode()}]", callStack.last())
        callStack.add(newScope)
        val block = SimpleBlockNode(newScope, "")
        newScope.scopeOwner = block

        for (child in ctx.children) {
            val newNode = child.accept(this) ?: continue
            if (newNode is NodeGroup) {
                block.children.addAll(newNode.children)
            } else {
                block.children.add(newNode)
            }
        }

        callStack.removeLast()
        return block
    }

    override fun visitFunctionDeclaration(ctx: KotlinParser.FunctionDeclarationContext?): SimpleTreeNode? {
        if (ctx == null) return super.visitFunctionDeclaration(ctx)

        val modifiers = ctx.modifiers
        if ("override" !in modifiers) return super.visitFunctionDeclaration(ctx)

        val name = ctx.name ?: "no name"

        val lastScopedDeclaration = lastScopedDeclaration
        if (lastScopedDeclaration != null) {
            val functionNode = OverrideFunctionNode(name, lastScopedDeclaration)

            val child =  super.visitFunctionDeclaration(ctx)
            if (child != null) {
                functionNode.children.add(child)
            }

            return functionNode
        }

        return super.visitFunctionDeclaration(ctx)
    }

    override fun visitClassMemberDeclarations(ctx: KotlinParser.ClassMemberDeclarationsContext?): SimpleTreeNode? {
        if (ctx == null) return super.visitClassMemberDeclarations(ctx)

        val propertiesNames = ctx.properties
        val lastClassScope = lastScopedDeclaration ?: return super.visitClassMemberDeclarations(ctx)

        lastClassScope.scopeOwner?.children?.addAll(propertiesNames.map { PropertyNode(it, lastClassScope) })

        return super.visitClassMemberDeclarations(ctx)
    }

    override fun visitImportHeader(ctx: KotlinParser.ImportHeaderContext?): SimpleTreeNode? {
        if (ctx == null) return null

        return if (ctx.MULT() != null) {
            // this is import *
            val packageName = ctx.identifier().text
            ImportPackageNode(packageName, callStack.last())
        } else {
            // this is package import
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

    override fun aggregateResult(aggregate: SimpleTreeNode?, nextResult: SimpleTreeNode?): SimpleTreeNode? {
        if (nextResult == null) return aggregate
        if (aggregate == null) return nextResult

        return if (aggregate is ImportNode || aggregate is ImportPackageNode) {
            val importList = ImportListNode(callStack.last())
            importList.children.add(aggregate)
            importList.children.add(nextResult)

            importList
        } else if (aggregate is ImportListNode) {
            aggregate.children.add(nextResult)

            aggregate
        } else if (aggregate is NodeGroup && nextResult is NodeGroup) {
            NodeGroup(aggregate.scope, (aggregate.children + nextResult.children).toMutableList())
        } else if (aggregate is NodeGroup || nextResult is NodeGroup){
            val nodeGroup = setOf(aggregate, nextResult).first { it is NodeGroup }
            val nonNodeGroup = (setOf(aggregate, nextResult) - nodeGroup).first()
            nodeGroup.children.add(nonNodeGroup)

            nodeGroup
        } else {
            NodeGroup(aggregate.scope, mutableListOf(aggregate, nextResult))
        }
    }

    private val ParserRuleContext.superclassesNames: List<String>
        get() {
            return children
                .filterIsInstance<KotlinParser.DelegationSpecifiersContext>()
                .firstOrNull()
                ?.children
                ?.filterIsInstance<KotlinParser.AnnotatedDelegationSpecifierContext>()
                ?.flatMap { it.children }
                ?.filterIsInstance<KotlinParser.DelegationSpecifierContext>()
                ?.flatMap { it.children }
                ?.mapNotNull {
                    val userType = when (it) {
                        is KotlinParser.ConstructorInvocationContext -> {
                            it.userType()
                        }
                        is KotlinParser.UserTypeContext -> {
                            it
                        }
                        else -> {
                            null
                        }
                    }
                    userType?.simpleUserType()?.firstOrNull()?.simpleIdentifier()?.Identifier()?.text
                }
                ?.toList() ?: listOf()
        }

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
            ?.filter { it.children != null }
            ?.flatMap { it.children.filterIsInstance<KotlinParser.DeclarationContext>() }
            ?.mapNotNull { it.propertyDeclaration()?.variableDeclaration()?.name }
            .orEmpty()

    private val lastScopedDeclaration
        get() = callStack.findLast { it.scopeOwner is ClassDeclarationNode || it.scopeOwner is SimpleBlockNode }
}