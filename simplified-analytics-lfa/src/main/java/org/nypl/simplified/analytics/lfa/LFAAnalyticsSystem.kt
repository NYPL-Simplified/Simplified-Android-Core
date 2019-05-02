package org.nypl.simplified.analytics.lfa

import android.provider.Settings
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.analytics.api.AnalyticsConfiguration
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsSystem
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.http.core.HTTPAuthBasic
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPProblemReportLogging
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultMatcherType
import org.nypl.simplified.http.core.HTTPResultOKType
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.zip.GZIPOutputStream

/**
 * The LFA analytics system.
 */

class LFAAnalyticsSystem(
  private val baseConfiguration: AnalyticsConfiguration,
  private val lfaConfiguration: LFAAnalyticsConfiguration,
  private val baseDirectory: File,
  private val executor: ExecutorService) : AnalyticsSystem {

  private val logger =
    LoggerFactory.getLogger(LFAAnalyticsSystem::class.java)

  private val outbox =
    File(this.baseDirectory, "outbox")
  private val logFile =
    File(this.baseDirectory, "logFile.txt")

  private lateinit var output: FileWriter

  init {
    this.executor.execute {
      this.logger.debug("creating analytics directory {}", this.baseDirectory)
      DirectoryUtilities.directoryCreate(this.baseDirectory)
      DirectoryUtilities.directoryCreate(this.outbox)
      this.output = FileWriter(this.logFile, true)
      this.executor.execute { this.trySendAll() }
    }
  }

  override fun onAnalyticsEvent(event: AnalyticsEvent): Unit =
    this.executor.execute {
      this.consumeEvent(event)
    }

  private fun consumeEvent(event: AnalyticsEvent) {

    /*
     * Roll over the log file if necessary, and trigger a send of everything else.
     */

    if (this.logFile.length() >= this.lfaConfiguration.logFileSizeLimit) {
      val outboxFile = File(this.outbox, UUID.randomUUID().toString() + ".log")
      FileUtilities.fileRename(this.logFile, outboxFile)
      this.output = FileWriter(this.logFile, true)
      this.trySendAll()
    }

    /*
     * Write the event to the output file.
     */

    val eventText = this.eventToText(event)
    if (eventText != null) {
      this.output.append(eventText)
      this.output.write("\n")
      this.output.flush()
    }
  }

  private fun eventToText(event: AnalyticsEvent): String? {
    return when (event) {
      is AnalyticsEvent.ProfileLoggedIn ->
        "profile_selected,${event.profileUUID},${event.displayName},${event.gender},${event.birthDate}"
      is AnalyticsEvent.ProfileLoggedOut ->
        null
      is AnalyticsEvent.CatalogSearched ->
        "catalog_searched,${event.searchQuery}"
      is AnalyticsEvent.BookOpened ->
        "book_opened,${event.profileUUID},${event.profileDisplayName},${event.bookTitle}"
      is AnalyticsEvent.BookPageTurned ->
        "book_open_page,${event.bookPage}/${event.bookPagesTotal},${event.bookTitle}"
      is AnalyticsEvent.BookClosed ->
        null
      is AnalyticsEvent.ApplicationOpened ->
        "app_open,${event.packageName},${event.packageVersion},${event.packageVersionCode}"
    }
  }

  private fun trySendAll() {
    this.outbox.list().forEach { file ->
      this.executor.execute { this.trySend(File(this.outbox, file)) }
    }
  }

  private fun trySend(file: File) {
    this.logger.debug("attempting send of {}", file)

    val auth: OptionType<HTTPAuthType> =
      Option.some(HTTPAuthBasic.create(
        this.lfaConfiguration.deviceID,
        this.lfaConfiguration.token))

    val data = this.compressAndReadLogFile(file)
    this.logger.debug("compressed data size: {}", data.size)

    if (data.isEmpty()) {
      file.delete()
    }

    val result =
      this.baseConfiguration.http.post(
        auth,
        this.lfaConfiguration.targetURI,
        data,
        "application/json")

    return result.matchResult(
      object : HTTPResultMatcherType<InputStream, Unit, Exception> {
        override fun onHTTPError(error: HTTPResultError<InputStream>) {
          HTTPProblemReportLogging.logError(
            this@LFAAnalyticsSystem.logger,
            this@LFAAnalyticsSystem.lfaConfiguration.targetURI,
            error.message,
            error.status,
            error.problemReport)
          return
        }

        override fun onHTTPException(exception: HTTPResultException<InputStream>) {
          this@LFAAnalyticsSystem.logger.debug("failed to send analytics data: ", exception.error)
          return
        }

        @Throws(Exception::class)
        override fun onHTTPOK(result: HTTPResultOKType<InputStream>) {
          this@LFAAnalyticsSystem.logger.debug("server accepted {}, deleting it", file)
          file.delete()
          return
        }
      })
  }

  @Throws(IOException::class)
  private fun compressAndReadLogFile(file: File): ByteArray {
    val buffer = ByteArray(4096)
    ByteArrayOutputStream(this.lfaConfiguration.logFileSizeLimit / 10).use { output ->
      GZIPOutputStream(output).use { gzip ->
        FileInputStream(file).use { input ->
          while (true) {
            val r = input.read(buffer)
            if (r == -1) {
              break
            }
            gzip.write(buffer, 0, r)
          }
        }
        gzip.flush()
        gzip.finish()
      }
      return output.toByteArray()
    }
  }

}