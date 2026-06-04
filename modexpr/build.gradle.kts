plugins {
  antlr
}

dependencies {
  implementation(project(":data"))
  implementation(libs.kotlinx.serialization.json)
  antlr(rootProject.libs.antlr)
}