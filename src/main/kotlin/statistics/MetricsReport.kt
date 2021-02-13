package statistics

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import simpleTree.ClassDeclarationNode
import simpleTree.RootNode

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
                add(JsonObject().apply { add("<global>", treeRoot.globalABC.toJson())  })
                abc.forEach { (k, v) ->
                    add(JsonObject().apply {
                        add(k.name, v.toJson())
                    }) }
                }
            )
            add("chains", Gson().toJsonTree(inheritanceChains.map { chain ->
                chain.map {
                    it.name
                }
            }))
            add("classes usages", JsonArray().apply {
                classInfo.forEach {
                    add(JsonObject().apply {
                        addProperty("class node name", it.classNode.name)
                        addProperty("overrides", it.overrides)
                        addProperty("properties count", it.propertiesCount)
                    })
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