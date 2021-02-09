import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException

const val testData = "./src/test/kotlin/testData/"

class TestRunner {
    @Test
    fun runTests() {
        val testDirs = File(testData).listFiles(File::isDirectory) ?: throw FileNotFoundException("testData is empty!")
        for (dir in testDirs) {
            runTest(dir)
        }
    }

    private fun runTest(dir: File) {
        val dirTree = processAllFilesInDirectory(dir)
        val actualTreeObject = dirTree.toJson()
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

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

    private fun Map<String, RootNode>.toJson(): JsonArray {
        return JsonArray().apply {
            for ((_, v) in this@toJson) {
                add(v.json())
            }
        }
    }
}
