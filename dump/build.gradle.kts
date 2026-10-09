dependencies {
  implementation(kotlin("reflect"))
  api(project(":data"))
  api(libs.guava)

  testImplementation(project(":"))
}