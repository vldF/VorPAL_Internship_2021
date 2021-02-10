import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import statistics.MetricsReport
import java.io.File
import java.io.FileNotFoundException

const val testData = "./src/test/testData/"

class TestRunner {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    @Test
    fun runTests() {
        val testDirs = File(testData).listFiles(File::isDirectory) ?: throw FileNotFoundException("testData is empty!")
        for (dir in testDirs) {
            runTest(dir)
        }
    }

    private fun runTest(dir: File) {
        val dirTree = processAllFilesInDirectory(dir)
        compareTree(dirTree, dir)
        compareMetrics(dirTree, dir)
    }

    private fun compareTree(dirTree: Map<String, RootNode>, dir: File) {
        val actualTreeObject = dirTree.toJson()
        val actualText = gson.toJson(actualTreeObject)

        val exceptedFile = dir.listFiles(File::isFile)!!.firstOrNull { it.name == "tree.json" }
        if (exceptedFile == null) {
            val newFile = File("${dir.path}/tree.json")
            newFile.createNewFile()
            newFile.writeText(actualText)

            throw FileNotFoundException("missing file tree.json was created")
        }

        val exceptedText = exceptedFile.readText()

        Assertions.assertEquals(exceptedText, actualText)
    }

    private fun compareMetrics(dirTrees: Map<String, RootNode>, dir: File) {
        val exceptedMetrics = dir.listFiles(File::isFile)!!.firstOrNull { it.name == "metrics.json" }
        val actualMetrics = JsonObject().apply {
            dirTrees.map { (packageName, root) ->
                val metrics = MetricsReport(root)
                add(packageName, metrics.dumpJson())
            }
        }
        val actualMetricsJson = gson.toJson(actualMetrics)

        if (exceptedMetrics == null) {
            val newFile = File("${dir.path}/metrics.json")
            newFile.createNewFile()
            newFile.writeText(actualMetricsJson)

            throw FileNotFoundException("missing file metrics.json was created")
        }

        Assertions.assertEquals(exceptedMetrics.readText(), actualMetricsJson)
    }

    private fun Map<String, RootNode>.toJson(): JsonArray {
        return JsonArray().apply {
            for ((_, v) in this@toJson) {
                add(v.json())
            }
        }
    }
}
