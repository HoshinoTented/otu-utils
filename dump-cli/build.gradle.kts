plugins {
  application
}

application.mainClass = "com.github.hoshinotented.osuutils.dump.cli.Main"

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

tasks.register<Jar>("fatJar") {
  archiveClassifier.set("fat")
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
  manifest {
    attributes["Main-Class"] = application.mainClass.get()
  }

  exclude("**/module-info.class")

  val jar = tasks.jar
  dependsOn(jar)
  with(jar.get())
}