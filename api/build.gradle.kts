plugins {
  kotlin("plugin.serialization")
}

dependencies {
  implementation(project(":data"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlinx.serialization.json)
}

kotlin {
  compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
}