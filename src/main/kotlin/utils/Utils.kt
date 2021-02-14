package utils

import simpleTree.ClassDeclarationNode
import simpleTree.RootNode
import statistics.ABCMetric
import statistics.ClassUsage
import statistics.MetricsReport
import java.lang.Integer.max

fun chainsToPrettyText(chains: List<List<ClassDeclarationNode>>): String {
    var maxLength = 0
    var sumLength = 0
    var longestChain = listOf<ClassDeclarationNode>()
    return buildString {
        if (chains.isEmpty()) {
            appendLine("Inheritance hierarchy is empty")
        } else {
            for (chain in chains) {
                val currentLen = chain.size
                sumLength += currentLen
                if (currentLen > maxLength) {
                    maxLength = currentLen
                    longestChain = chain
                }
                appendLine(chain.prettyString)
            }

            appendLine()
            appendLine("Max Inheritance hierarchy chain size: $maxLength")
            appendLine("The longest chain is: ${longestChain.prettyString}")
            appendLine("Avg chain size: ${sumLength * 1.0 / chains.size}")
        }
    }
}

fun classInfoToPrettyText(infos: Set<ClassUsage>): String {
    var sumOverridesMethods = 0
    var sumPropertiesCount = 0

    for (info in infos) {
        sumOverridesMethods += info.overrides
        sumPropertiesCount += info.properties
    }
    return buildString {
        if (infos.isNotEmpty()) {
            appendLine("Avg overrides functions: ${sumOverridesMethods * 1.0 / infos.size}")
            appendLine("Avg properties count: ${sumPropertiesCount * 1.0 / infos.size}")
        } else {
            appendLine("No classes were found")
        }
    }
}

fun abcToPrettyTest(abc: Map<ClassDeclarationNode, ABCMetric>, rootABC: ABCMetric): String {
    var sumABC = ABCMetric.empty
    return buildString {
        appendLine("global ABC: $rootABC")
        abc.forEach { (klass, value) ->
            appendLine("${klass.name}: $value")
            sumABC += value
        }
        appendLine()
        appendLine("ABC summary: $sumABC")
    }
}

fun StringBuilder.appendTextWithOffset(text: String, offset: Int) {
    for (line in text.lines()) {
        append(" ".repeat(offset))
        appendLine(line)
    }
}

fun projectStatisticsSummary(reports: List<MetricsReport>, dirTree: MutableMap<String, RootNode>): SummaryReport {
    var abc = dirTree.values.fold(ABCMetric()) { prev, cur -> prev + cur.globalABC }
    var maxInheritanceChain = listOf<ClassDeclarationNode>()
    var sumChainLens = 0.0
    var chainsCount = 0
    var overrides = 0.0
    var properties = 0.0
    var classesCount = 0

    for (report in reports) {
        abc += report.abc.values.fold(ABCMetric()) { prev, cur -> prev + cur }
        for (chain in report.inheritanceChains) {
            if (chain.size > maxInheritanceChain.size) {
                maxInheritanceChain = chain
            }
            sumChainLens += chain.size
            chainsCount++
        }

        for (classInfo in report.classInfo) {
            overrides += classInfo.overrides
            properties += classInfo.properties
            classesCount++
        }
    }
    val avgChainLens = sumChainLens / max(1, chainsCount)
    val avgOverrides = overrides / max(1, classesCount)
    val avgProperties = properties / max(1, classesCount)

    return SummaryReport(
        abc,
        maxInheritanceChain,
        maxInheritanceChain.size,
        avgChainLens,
        avgOverrides,
        avgProperties,
        classesCount
    )
}

class SummaryReport(
    private val abc: ABCMetric,
    private val maxInheritanceChain: List<ClassDeclarationNode>,
    private val chainLen: Int,
    private val avgChainLens: Double,
    private val avgOverrides: Double,
    private val avgProperties: Double,
    private val classesCount: Int
) {
    override fun toString(): String {
        return buildString {
            appendLine("ABC = $abc")
            appendLine("Max inheritance chain = ${maxInheritanceChain.prettyString}")
            appendLine("Max inheritance chain len = $chainLen")
            appendLine("Avg inheritance chain len = $avgChainLens")
            appendLine("Avg overrides count (all overrides count / classes count) = $avgOverrides")
            appendLine("Avg properties count (all properties count / classes count) = $avgProperties")
            appendLine("Classes count = $classesCount")
        }
    }
}

private val List<ClassDeclarationNode>.prettyString
    get() = if (isNotEmpty()) joinToString(separator = "->") { it.name } else "<empty>"

