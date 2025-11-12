import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  id("java")
  kotlin("jvm") version "2.2.21"
  kotlin("plugin.serialization") version "2.2.21"
}

group = "com.github.hoshinotented"
version = "1.0-SNAPSHOT"

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

//  implementation(libs.io.netty)
  
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}

tasks.named<KotlinCompilationTask<*>>("compileKotlin") {
  compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
}

tasks.withType()

tasks.named<KotlinCompilationTask<*>>("compileTestKotlin") {
  compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
}