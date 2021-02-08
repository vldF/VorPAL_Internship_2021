import simpleTree.SimpleTreeBuilder
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.kotlin.spec.grammar.parser.KotlinLexer
import org.jetbrains.kotlin.spec.grammar.parser.KotlinParser
import java.io.File
import java.io.FileNotFoundException

fun main() {
    val tree = processAllFilesInDirectory("./testData/")
    tree.doAllImports()
    tree.resolveAllTrees()
}

private fun processAllFilesInDirectory(path: String): MutableMap<String, RootNode> {
    val directory = File(path)
    if (!directory.isDirectory) throw FileNotFoundException("this directory doesn't exist")

    var result = mutableMapOf<String, RootNode>()

    val currentDirectoryTrees = processDirectory(path)

    result = result.merge(currentDirectoryTrees)

    val dirs = File(path).listFiles(File::isDirectory)

    if (dirs != null) {
        for (dir in dirs) {
            val dirTree = processAllFilesInDirectory(dir.path)
            result = result.merge(dirTree)
        }
    }

    return result
}

private fun processDirectory(path: String): Map<String, RootNode> {
    val files = File(path).listFiles()?.filter { it.isFile && it.name.endsWith(".kt") } ?: return mapOf()
    val result = mutableMapOf<String, RootNode>()

    for (file in files) {
        val tree = processFile(file) ?: continue
        val packageName = tree.packageName
        if (result.containsKey(packageName)) {
            result[packageName] = result[packageName]!!.merge(tree)
        } else {
            result[packageName] = tree
        }
    }

    return result
}

private fun processFile(file: File): RootNode? {
    val content = file.readText()

    val iStream = CharStreams.fromString(content)
    val lexer = KotlinLexer(iStream)
    val tokens = CommonTokenStream(lexer)
    val parser = KotlinParser(tokens)
    val context = parser.kotlinFile()
    val simpleTree = context.accept(SimpleTreeBuilder()) ?: return null

    return simpleTree as? RootNode
}

private fun Map<String, RootNode>.merge(other: Map<String, RootNode>): MutableMap<String, RootNode> {
    val result = mutableMapOf<String, RootNode>()
    // todo: refactor it
    for ((k, v) in this) {
        if (other.containsKey(k)) {
            val otherValue = other[k]!!
            result[k] = v.merge(otherValue)
        } else {
            result[k] = v
        }
    }

    for (k in other.keys - this.keys) {
        result[k] = other[k]!!
    }

    return result
}

private fun Map<String, RootNode>.doAllImports() {
    for ((_, v) in this) {
        v.doImports(this)
    }
}

private fun Map<String, RootNode>.resolveAllTrees() {
    for ((_, v) in this) {
        v.resolveAll()
    }
}