import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import statistics.ClassInfoCollector
import statistics.ClassUsage
import statistics.InheritanceHierarchy
import java.io.File

fun main(args: Array<String>) {
    val parser = ArgParser("kotlin code analyzer")
    val inputProjectPath by parser.option(ArgType.String, shortName = "p", description = "path to code dir")
    val inputOutputPath by parser.option(ArgType.String, shortName = "o", description = "path to results dir")
    parser.parse(args)

    val projectPath = inputProjectPath ?: "./src/test/testData"
    val outputPath = inputOutputPath ?: "./"

    processProjectDir(projectPath, outputPath)
}

private fun processProjectDir(projectPath: String, outputPath: String) {
    val directory = File(projectPath)
    val tree = processAllFilesInDirectory(directory)

    for ((packageName, treeRoot) in tree) {
        println("package: $packageName")
        val hierarchyTree = InheritanceHierarchy()
        hierarchyTree.visitRootNode(treeRoot)
        println(chainsToPrettyText(hierarchyTree.inheritanceChains))

        val classInfo = ClassInfoCollector().visitRootNode(treeRoot)
        println(classInfoToPrettyText(classInfo))

        println("ABC metric")
        println("A: " + treeRoot.assignmentsCount)
        println("B: " + treeRoot.branchesCount)
        println("C: " + treeRoot.conditionsCount)
    }
}

private fun chainsToPrettyText(chains: List<List<ClassDeclarationNode>>): String {
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

private fun classInfoToPrettyText(infos: Set<ClassUsage>): String {
    var sumOverridenMethods = 0
    var sumPropertiesCount = 0

    for (info in infos) {
        sumOverridenMethods += info.overridens
        sumPropertiesCount += info.propertiesCount
    }
    return buildString {
        if (infos.isNotEmpty()) {
            appendLine("Avg overriden functions: ${sumOverridenMethods * 1.0 / infos.size}")
            appendLine("Avg properties count: ${sumPropertiesCount * 1.0 / infos.size}")
        } else {
            appendLine("No classes were found")
        }
    }
}

private val List<ClassDeclarationNode>.prettyString
    get() = joinToString(separator = "->") { it.name }

