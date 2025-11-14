import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  id("java")
  kotlin("jvm") version "2.2.21"
  kotlin("plugin.serialization") version "2.2.21"
  kotlin("kapt") version "2.2.21"
  application
}

group = "com.github.hoshinotented"
version = libs.versions.project.get()

application.mainClass.set("com.github.hoshinotented.osuutils.cli.MainKt")

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlinx.serialization.json)
  
  implementation(libs.kala.collection)
  implementation(libs.kala.gson)
  implementation(libs.gson)
  implementation(libs.jline)
  implementation(libs.picocli)
  
  kapt(libs.picocli.codegen)

//  implementation(libs.io.netty)
  
  testImplementation(kotlin("test"))
}


kapt {
  arguments {
    // required by picocli-codegen
    arg("project", "${project.group}/${project.name}")
  }
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
}

//tasks.named<KotlinCompilationTask<*>>("compileKotlin") {
//  compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
//}

//tasks.named<KotlinCompilationTask<*>>("compileTestKotlin") {
//  compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
//}

val genDir = layout.projectDirectory.dir("src/main/gen")

sourceSets.main {
  java.srcDirs(genDir)
}

val generateVersion = tasks.register("generateVersion") {
  doLast {
    val code = """
      public class GenerateVersion {
        public static final String VERSION = "${libs.versions.project.get()}";
      }
    """.trimIndent()
    
    genDir.asFile.mkdirs()
    genDir.file("GenerateVersion.java")
      .asFile
      .writeText(code)
  }
}

tasks.register<Jar>("fatJar") {
  archiveClassifier.set("jar")
  from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
  manifest {
    attributes["Main-Class"] = application.mainClass.get()
  }
  
  val jar = tasks.jar
  with(jar.get())
}

tasks.compileJava {
  dependsOn(generateVersion)
}