import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

fun main(args: Array<String>) {
    val parser = ArgParser("kotlin code analyzer")
    val inputProjectPath by parser.option(ArgType.String, shortName = "p", description = "path to code dir").required()
    val outputPath by parser.option(ArgType.String, shortName = "o", description = "path to results dir")
    parser.parse(args)

    processProjectDir(inputProjectPath, outputPath)
}
