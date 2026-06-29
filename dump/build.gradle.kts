dependencies {
  implementation(kotlin("reflect"))
  implementation(project(":data"))
  implementation(libs.guava)

  testImplementation(project(":"))
}