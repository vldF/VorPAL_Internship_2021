package utils

import simpleTree.ClassDeclarationNode
import statistics.ABCMetric
import statistics.ClassUsage

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
        sumPropertiesCount += info.propertiesCount
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

private val List<ClassDeclarationNode>.prettyString
    get() = joinToString(separator = "->") { it.name }

