package com.github.hoshinotented.gradle

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.configuration.TaskDetailPrinter
import org.gradle.jvm.tasks.Jar
import org.jetbrains.annotations.Nullable

final class CommonTasks {
    static TaskProvider<Jar> fatJar(Project project, @Nullable Property<String> mainClass) {
        project.tasks.register('fatJar', Jar) {
            it.archiveClassifier.set 'fat'
            it.from project.configurations.runtimeClasspath.collect {
                if (it.isDirectory()) it else project.zipTree(it)
            }
            it.duplicatesStrategy = DuplicatesStrategy.INCLUDE
            it.exclude '**/module-info.class'
            if (mainClass != null) it.manifest.attributes(
                'Main-Class': mainClass,
            )
            def jar = project.tasks.jar
            it.dependsOn(jar)
            //noinspection GroovyAssignabilityCheck
            with jar
        }
    }
}