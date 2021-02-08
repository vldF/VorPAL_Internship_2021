import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    java
    antlr
}

group = "me.vldf"
version = "1.0-SNAPSHOT"

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/java")
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    antlr("org.antlr:antlr4:4.+")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<AntlrTask> {
    // this code copy-pasted from kotlin-spec repo
    outputDirectory =
        File("${project.rootDir}/src/main/java/org/jetbrains/kotlin/spec/grammar/parser").also { it.mkdirs() }

    arguments.add("-visitor")
    arguments.add("-package")
    arguments.add("org.jetbrains.kotlin.spec.grammar.parser")
}

configure<SourceSetContainer> {
    named("main") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            kotlin.srcDir("src/main/java/org/jetbrains/kotlin/spec/grammar/parser")
        }
    }
}