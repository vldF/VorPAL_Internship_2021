import java.io.File

fun main() {
    for (file in File("./testData/").listFiles(File::isDirectory)!!) {
        val tree = processAllFilesInDirectory(file)
    }
}
