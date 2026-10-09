plugins {
  id("java")
  kotlin("jvm") version "2.3.21"
  kotlin("plugin.serialization") version "2.3.21"
  kotlin("kapt") version "2.3.21"
  application
}

val projectVersion: String = libs.versions.project.get()

application.mainClass.set("com.github.hoshinotented.osuutils.cli.MainKt")

tasks.withType<JavaExec> {
  standardInput = System.`in`
  standardOutput = System.out
}

allprojects {
  val libs = rootProject.libs

  apply {
    plugin("kotlin")
  }

  group = "com.github.hoshinotented"
  version = projectVersion

  repositories {
    mavenCentral()
  }

  dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kala.collection)

    testImplementation(kotlin("test"))
  }

  tasks.test {
    useJUnitPlatform()
    enableAssertions = true
  }
}

dependencies {
  implementation(project(":data"))
  implementation(project(":api"))
  implementation(project(":dump"))
  implementation(project(":modexpr"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlinx.serialization.json)

  implementation(libs.kala.gson)
  implementation(libs.gson)
  implementation(libs.jline)
  implementation(libs.picocli)
  implementation(libs.jfreechart)
  implementation(libs.guava)
  implementation(libs.sqlite)
  
  kapt(libs.picocli.codegen)
}


kapt {
  arguments {
    // required by picocli-codegen
    arg("project", "${project.group}/${project.name}")
  }
}

kotlin {
  compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
}

val genDir = layout.projectDirectory.dir("src/main/gen")

sourceSets.main {
  java.srcDirs(genDir)
}

val generateVersion = tasks.register("generateVersion") {
  doLast {
    val code = """
      public class GenerateVersion {
        public static final String VERSION = "$projectVersion";
      }
    """.trimIndent()
    
    genDir.asFile.mkdirs()
    genDir.file("GenerateVersion.java")
      .asFile
      .writeText(code)
  }
}

tasks.register<Jar>("fatJar") {
  archiveClassifier.set("fat")
  from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
  manifest {
    attributes["Main-Class"] = application.mainClass.get()
  }
  
  exclude("**/module-info.class")
  
  val jar = tasks.jar
  dependsOn(jar)
  with(jar.get())
}

tasks.compileJava {
  dependsOn(generateVersion)
}

tasks.withType<AntlrTask> {
  this.packageName.set("com.github.hoshinotented.osuutils.parser")
}