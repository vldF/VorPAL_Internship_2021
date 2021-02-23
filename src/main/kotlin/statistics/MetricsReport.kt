package statistics

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import simpletree.ClassDeclarationNode
import simpletree.RootNode

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
            add("ABC", JsonObject().apply {
                add("<global>", treeRoot.globalABC.toJson())
                abc.forEach { (k, v) ->
                    add(k.name, v.toJson()) }
                }
            )
            add("chains", Gson().toJsonTree(inheritanceChains.map { chain ->
                chain.map {
                    it.name
                }
            }))
            add("classesUsages", JsonArray().apply {
                classInfo.forEach {
                    add(JsonObject().apply {
                        addProperty("name", it.classNode.name)
                        addProperty("overrides", it.overrides)
                        addProperty("properties", it.properties)
                    })
                }
            })
            add("avgClassesUsages", JsonObject().apply {
                val denominator = if (classInfo.isNotEmpty()) classInfo.size else 1
                val overrides = classInfo.sumBy { it.overrides } * 1.0 / denominator
                val properties = classInfo.sumBy { it.properties } * 1.0 / denominator

                addProperty("overrides", overrides)
                addProperty("properties", properties)
            })
        }
    }

    companion object {
        fun getJsonForMetricsList(list: List<MetricsReport>) = JsonArray().apply {
            list.forEach { add(it.dumpJson()) }
        }
    }
}