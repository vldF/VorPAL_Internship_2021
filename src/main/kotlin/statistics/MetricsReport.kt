package statistics

import ClassDeclarationNode
import RootNode
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class MetricsReport(
    treeRoot: RootNode
) {
    val abc: Triple<Int, Int, Int>
    val inheritanceChains: List<List<ClassDeclarationNode>>
    val classInfo: Set<ClassUsage>
    val packageName: String

    init {
        abc = Triple(treeRoot.assignmentsCount, treeRoot.branchesCount, treeRoot.conditionsCount)

        val hierarchyTree = InheritanceHierarchy()
        hierarchyTree.visitRootNode(treeRoot)

        inheritanceChains = hierarchyTree.inheritanceChains

        classInfo = ClassInfoCollector().visitRootNode(treeRoot)
        packageName = treeRoot.packageName
    }

    fun dumpJson(): JsonObject {
        return JsonObject().apply {
            addProperty("package", packageName)
            addProperty("ABC", abc.toString())
            add("chains", Gson().toJsonTree(inheritanceChains.map { chain ->
                chain.map {
                    it.name
                }
            }))
            add("classes usages", JsonObject().apply {
                classInfo.forEach {
                    addProperty("class node name", it.classNode.name)
                    addProperty("overridens", it.overridens)
                    addProperty("properties count", it.propertiesCount)
                }
            })
        }
    }

    companion object {
        fun getJsonForMetricsList(list: List<MetricsReport>) = JsonArray().apply {
            list.forEach { add(it.dumpJson()) }
        }
    }
}