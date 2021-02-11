package statistics

import ClassDeclarationNode
import RootNode
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class MetricsReport(
    private val treeRoot: RootNode
) {
    val abc: Map<ClassDeclarationNode, ABCMetric>
    val inheritanceChains: List<List<ClassDeclarationNode>>
    val classInfo: Set<ClassUsage>
    private val packageName: String

    init {
        abc = SimpleTreeABCCollector.visitRootNode(treeRoot)

        val hierarchyTree = InheritanceHierarchy()
        hierarchyTree.visitRootNode(treeRoot)

        inheritanceChains = hierarchyTree.inheritanceChains

        classInfo = ClassInfoCollector().visitRootNode(treeRoot)
        packageName = treeRoot.packageName
    }

    fun dumpJson(): JsonObject {
        return JsonObject().apply {
            addProperty("package", packageName)
            add("ABC", JsonArray().apply{
                add(JsonObject().apply { addProperty("<global>", treeRoot.globalABC.toString())  })
                abc.forEach { (k, v) ->
                    add(JsonObject().apply {
                        addProperty(k.name, v.toString())
                    }) }
                }
            )
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