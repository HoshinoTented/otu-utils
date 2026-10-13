import com.github.hoshinotented.gradle.CommonTasks

plugins {
  application
}

val mainClass = "com.github.hoshinotented.osuutils.dump.cli.Main"
application.mainClass = mainClass

tasks.named<JavaExec>("run") {
  standardInput = System.`in`
}

dependencies {
  implementation(project(":dump"))
  implementation(kotlin("reflect"))
  implementation(libs.picocli)
  implementation(libs.kala.gson)
  implementation(libs.gson)
}

CommonTasks.fatJar(project, application.mainClass)