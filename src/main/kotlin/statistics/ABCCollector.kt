package statistics

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.kotlin.spec.grammar.parser.KotlinLexer
import org.jetbrains.kotlin.spec.grammar.parser.KotlinParser
import org.jetbrains.kotlin.spec.grammar.parser.KotlinParserBaseVisitor

class ABCCollector : KotlinParserBaseVisitor<ABCMetric>() {
    companion object {
        val conditionalTokens = setOf(
            KotlinLexer.CONJ,
            KotlinLexer.DISJ,
            KotlinLexer.EXCL_WS,
            KotlinLexer.EXCL_NO_WS,
            KotlinLexer.EQEQ,
            KotlinLexer.EQEQEQ
        )
    }

    // skipping class declarations, it will be collected manually
    override fun visitClassDeclaration(ctx: KotlinParser.ClassDeclarationContext?): ABCMetric {
        return ABCMetric.empty
    }

    // skipping object declarations, it will be collected manually
    override fun visitObjectDeclaration(ctx: KotlinParser.ObjectDeclarationContext?): ABCMetric {
        return ABCMetric.empty
    }

    // skipping object literals, it will be collected manually
    override fun visitObjectLiteral(ctx: KotlinParser.ObjectLiteralContext?): ABCMetric {
        return ABCMetric.empty
    }

    override fun visitVariableDeclaration(ctx: KotlinParser.VariableDeclarationContext?): ABCMetric {
        val parent = ctx?.parent
        val result = ABCMetric()
        if (parent is KotlinParser.PropertyDeclarationContext && parent.ASSIGNMENT() != null) {
            result.assignments++
        }
        return result + super.visitVariableDeclaration(ctx)
    }

    override fun visitAssignment(ctx: KotlinParser.AssignmentContext?): ABCMetric {
        val result = ABCMetric()
        result.assignments++
        return result + super.visitAssignment(ctx)
    }

    override fun visitPrefixUnaryOperator(ctx: KotlinParser.PrefixUnaryOperatorContext?): ABCMetric {
        return processOperator(ctx) + super.visitPrefixUnaryOperator(ctx)
    }

    override fun visitPostfixUnaryOperator(ctx: KotlinParser.PostfixUnaryOperatorContext?): ABCMetric {
        return processOperator(ctx) + super.visitPostfixUnaryOperator(ctx)
    }

    private fun processOperator(ctx: ParserRuleContext?): ABCMetric {
        val result = ABCMetric()
        val firstChild = ctx?.children?.firstOrNull()
        if (firstChild is TerminalNode) {
            val type = firstChild.symbol.type
            if (type == KotlinLexer.INCR || type == KotlinLexer.DECR) {
                result.assignments++
            }
        }

        return result
    }

    override fun visitCallSuffix(ctx: KotlinParser.CallSuffixContext?): ABCMetric {
        val result = ABCMetric()
        result.branches++
        return result + super.visitCallSuffix(ctx)
    }

    override fun visitEqualityOperator(ctx: KotlinParser.EqualityOperatorContext?): ABCMetric {
        val result = ABCMetric()
        result.conditions++
        return result + super.visitEqualityOperator(ctx)
    }

    override fun visitExcl(ctx: KotlinParser.ExclContext?): ABCMetric {
        val result = ABCMetric()
        result.conditions++
        return result + super.visitExcl(ctx)
    }

    override fun visitComparisonOperator(ctx: KotlinParser.ComparisonOperatorContext?): ABCMetric {
        val result = ABCMetric()
        result.conditions++
        return result + super.visitComparisonOperator(ctx)
    }

    override fun visitConjunction(ctx: KotlinParser.ConjunctionContext?): ABCMetric {
        val result = ABCMetric()
        if (ctx != null) {
            result.conditions += ctx.children.count { it is TerminalNode && it.symbol.type in conditionalTokens }
        }
        return result + super.visitConjunction(ctx)
    }

    override fun visitDisjunction(ctx: KotlinParser.DisjunctionContext?): ABCMetric {
        val result = ABCMetric()
        if (ctx != null) {
            result.conditions += ctx.children.count { it is TerminalNode && it.symbol.type in conditionalTokens }
        }
        return result + super.visitDisjunction(ctx)
    }

    override fun visitIfExpression(ctx: KotlinParser.IfExpressionContext?): ABCMetric {
        val result = ABCMetric()
        if (ctx != null) {
            result.conditions += ctx.children.count { it is TerminalNode && it.symbol.type == KotlinLexer.ELSE }
        }
        return result + super.visitIfExpression(ctx)
    }

    override fun visitWhenExpression(ctx: KotlinParser.WhenExpressionContext?): ABCMetric {
        val result = ABCMetric()
        if (ctx != null && ctx.children.any { c ->
                c is KotlinParser.WhenEntryContext && c.children.any {
                    it is TerminalNode && it.symbol.type == KotlinLexer.ELSE
                }
            }) {
            result.conditions++
        }
        return result + super.visitWhenExpression(ctx)
    }

    override fun visitTryExpression(ctx: KotlinParser.TryExpressionContext?): ABCMetric {
        val result = ABCMetric()
        if (ctx != null) {
            result.conditions += ctx.children.count {
                it is TerminalNode && (it.symbol.type == KotlinLexer.TRY)
            }
        }
        return result + super.visitTryExpression(ctx)
    }

    override fun visitCatchBlock(ctx: KotlinParser.CatchBlockContext?): ABCMetric {
        val result = ABCMetric()
        if (ctx != null) {
            result.conditions += ctx.children.count {
                it is TerminalNode && (it.symbol.type == KotlinLexer.CATCH)
            }
        }
        return result + super.visitCatchBlock(ctx)
    }

    override fun visitElvis(ctx: KotlinParser.ElvisContext?): ABCMetric {
        val result = ABCMetric()
        result.conditions++
        return result + super.visitElvis(ctx)
    }

    override fun aggregateResult(aggregate: ABCMetric?, nextResult: ABCMetric?): ABCMetric {
        return ABCMetric.merge(aggregate, nextResult)
    }
}