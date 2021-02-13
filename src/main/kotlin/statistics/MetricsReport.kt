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
                        addProperty("properties", it.propertiesCount)
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