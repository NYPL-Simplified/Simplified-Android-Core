package org.nypl.simplified.reports

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import org.nypl.simplified.reports.Reports.Result.NoFiles
import org.nypl.simplified.reports.Reports.Result.RaisedException
import org.nypl.simplified.reports.Reports.Result.Sent
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

/**
 * Functions to send reports.
 */

object Reports {

  private val logger = LoggerFactory.getLogger(Reports::class.java)

  /**
   * The result of trying to send.
   */

  sealed class Result {

    /**
     * An attempt was made to send.
     */

    object Sent : Result()

    /**
     * There were no files to send.
     */

    object NoFiles : Result()

    /**
     * An exception was raised.
     */

    data class RaisedException(
      val exception: Exception
    ) : Result()
  }

  /**
   * Try to send a report using the default settings.
   */

  @JvmStatic
  fun sendReportsDefault(
    context: Context,
    address: String,
    subject: String,
    body: String
  ): Result {
    val directories: List<File> = context.cacheDir?.let { cacheDir ->
      arrayListOf(cacheDir, File(cacheDir, "migrations"))
    } ?: emptyList()

    return sendReport(
      context = context,
      baseDirectories = directories,
      address = address,
      subject = subject,
      body = body,
      includeFile = this::isLogFileOrMigrationReport
    )
  }

  @JvmStatic
  private fun isLogFileOrMigrationReport(name: String): Boolean {
    if (name.startsWith("report-") && name.endsWith(".xml")) {
      return true
    }
    return name.startsWith("log.txt")
  }

  /**
   * Try to send a report.
   */

  @JvmStatic
  fun sendReport(
    context: Context,
    baseDirectories: List<File>,
    address: String,
    subject: String,
    body: String,
    includeFile: (String) -> Boolean
  ): Result {
    this.logger.debug("preparing report")

    try {
      val files =
        this.collectFiles(baseDirectories, includeFile)
      val compressedFiles =
        files.mapNotNull(this::compressFile)
      val contentUris =
        compressedFiles.toSet().map { file -> this.mapFileToContentURI(context, file) }

      this.logger.debug("attaching {} files", compressedFiles.size)

      return if (compressedFiles.isNotEmpty()) {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
          this.type = "text/plain"
          this.putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
          this.putExtra(Intent.EXTRA_SUBJECT, extendSubject(context, subject))
          this.putExtra(Intent.EXTRA_TEXT, extendBody(body))
          this.putExtra(Intent.EXTRA_STREAM, ArrayList(contentUris))
          this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        Sent
      } else {
        NoFiles
      }
    } catch (e: Exception) {
      this.logger.error("failed to send report: ", e)
      return RaisedException(e)
    }
  }

  private fun extendBody(
    body: String
  ): String {
    return buildString {
      this.append(body)
      this.append("\n")
      this.append("--")
      this.append("\n")
      this.append("Commit: ")
      this.append(BuildConfig.SIMPLIFIED_GIT_COMMIT)
      this.append("\n")
    }
  }

  private fun extendSubject(
    context: Context,
    subject: String
  ): String {
    val pkgManager = context.packageManager
    val pkgInfo = try {
      pkgManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
      this.logger.error("unable to retrieve package information: ", e)
      return subject
    }

    return buildString {
      this.append(subject)
      this.append(' ')
      this.append(pkgInfo.versionName)
      this.append(" (")
      this.append(pkgInfo.versionCode)
      this.append(")")
    }
  }

  private fun mapFileToContentURI(context: Context, file: File) =
    FileProvider.getUriForFile(context, context.packageName + ".fileProvider", file)

  @JvmStatic
  private fun collectFiles(
    baseDirectories: List<File>,
    includeFile: (String) -> Boolean
  ): MutableList<File> {
    val files = mutableListOf<File>()
    for (baseDirectory in baseDirectories) {
      val list = baseDirectory.absoluteFile.list() ?: emptyArray()
      for (file in list) {
        val filePath = File(baseDirectory, file)
        if (includeFile.invoke(file) && filePath.isFile) {
          this.logger.debug("including {}", file)
          files.add(filePath)
        } else {
          this.logger.debug("excluding {}", file)
        }
      }
    }

    this.logger.debug("collected {} files", files.size)
    return files
  }

  @JvmStatic
  private fun compressFile(file: File): File? {
    if (file.name.endsWith(".gz")) return file

    return try {
      val parent = file.parentFile
      val fileGz = File(parent, file.name + ".gz")

      FileInputStream(file).use { inputStream ->
        FileOutputStream(fileGz, false).use { stream ->
          BufferedOutputStream(stream).use { bStream ->
            GZIPOutputStream(bStream).use { zStream ->
              inputStream.copyTo(zStream)
              zStream.finish()
              zStream.flush()
              this.logger.debug("compressed {}", file)
              fileGz
            }
          }
        }
      }
    } catch (e: Exception) {
      this.logger.error("could not compress: {}: ", file, e)
      null
    }
  }
}
