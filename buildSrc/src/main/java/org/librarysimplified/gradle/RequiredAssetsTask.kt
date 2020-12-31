package org.librarysimplified.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

/**
 * Returns the SHA-256 message digest of the given file.
 */
fun sha256(file: File): String {
  return MessageDigest.getInstance("SHA-256")
    .digest(file.readBytes())
    .fold("", { str, it -> str + "%02x".format(it) })
}

/**
 * Ensure the required files are present in the assets/ directory of
 * the assembled APK.
 */
open class RequiredAssetsTask : DefaultTask() {

  /** A map of filenames to a sha256 hash. */
  @get:Input
  var required: Map<String, String> = emptyMap()

  /** A list of input APKs to check for the required files. */
  @get:InputFiles
  var apkFiles: List<File> = emptyList()

  companion object {
    const val STRICT_MODE = "org.librarysimplified.strictmode"
  }

  private val strictMode by lazy {
    project.findProperty(STRICT_MODE)?.toString()?.toBoolean() ?: false
  }

  @TaskAction
  fun execute() {
    if (!strictMode) return // Nothing to do

    apkFiles.forEach { apk ->
      // Filter the file tree by our required assets
      //
      val zipTree =
        project.zipTree(apk)
          .matching {
            include("assets/*")
            include(required.keys)
          }

      val missing = required.toMutableMap()

      // Iterate over each match and check the file's signature
      //
      zipTree.files.forEach { apkFile ->
        missing[apkFile.name]?.let { digest ->
          val apkFileDigest = sha256(apkFile)

          if (digest == apkFileDigest) {
            missing.remove(apkFile.name)
          } else {
            project.logger.error(
              """
              ERROR: '${apkFile.name}' has an invalid digest
                Expected: sha256($digest)
                Found:    sha256($apkFileDigest)
              """.trimIndent()
            )
          }
        }
      }

      // Throw an error if we're missing any files
      //
      if (missing.isNotEmpty()) {
        var message = "ERROR: ${missing.count()} assets missing from '${apk.name}'\n"
        missing.forEach { (file, digest) ->
          message += "$file\n"
          message += "  sha256($digest)\n"
        }
        message += "\nThis check can be disabled by removing the '$STRICT_MODE' property."
        throw GradleException(message.trimEnd())
      }
    }
  }
}
