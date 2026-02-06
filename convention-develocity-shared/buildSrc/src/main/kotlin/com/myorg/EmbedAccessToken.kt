package com.myorg

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class EmbedAccessToken : DefaultTask() {

  private companion object {
    // CHANGE ME: Apply your desired package name here.
    private const val PACKAGE_NAME = "com.myorg"
  }

  @get:Input
  abstract val accessToken: Property<String>

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun action() {
    val file = outputDirectory.get().asFile
      .resolve(PACKAGE_NAME.replace('.', File.separatorChar))
      .resolve("EmbeddedAccessToken.java")

    file.parentFile.mkdirs()
    file.writeText("""
      package $PACKAGE_NAME;
      
      final class EmbeddedAccessToken {
        public static final String ACCESS_TOKEN = "${accessToken.get()}";
      }
    """.trimIndent())
  }
}
