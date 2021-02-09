import statistics.ClassInfoCollector
import statistics.InheritanceHierarchy
import java.io.File

fun main() {
    for (file in File("./src/test/kotlin/testData/").listFiles(File::isDirectory)!!) {
        println("file: ${file.name}")
        val tree = processAllFilesInDirectory(file)
        for ((packageName, treeRoot) in tree) {
            val hierarchyTree = InheritanceHierarchy()
            hierarchyTree.visitRootNode(treeRoot)
            println(hierarchyTree.inheritanceChains)

            println()
            println("classesInfo")
            val classInfo = ClassInfoCollector().visitRootNode(treeRoot)
            println(classInfo)

            println()
            println("ABC metric")
            println("A: " + treeRoot.assignmentsCount)
            println("B: " + treeRoot.branchesCount)
            println("C: " + treeRoot.conditionsCount)
        }

    }
}
