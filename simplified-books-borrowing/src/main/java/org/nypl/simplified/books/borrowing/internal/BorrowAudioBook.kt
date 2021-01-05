package org.nypl.simplified.books.borrowing.internal

import com.io7m.junreachable.UnreachableCodeException
import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.audio.AudioBookCredentials
import org.nypl.simplified.books.audio.AudioBookManifestRequest
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.audioStrategyFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import org.nypl.simplified.books.formats.api.StandardFormatNames
import org.nypl.simplified.taskrecorder.api.TaskResult
import java.io.File
import java.net.URI

/**
 * A task that copies content from `content://` URIs and bundled resources.
 */

class BorrowAudioBook private constructor() : BorrowSubtaskType {

  companion object : BorrowSubtaskFactoryType {
    override val name: String
      get() = "Audio Book"

    override fun createSubtask(): BorrowSubtaskType {
      return BorrowAudioBook()
    }

    override fun isApplicableFor(
      type: MIMEType,
      target: URI?,
      account: AccountReadableType?
    ): Boolean {
      for (audioType in StandardFormatNames.allAudioBooks) {
        if (MIMECompatibility.isCompatibleStrictWithoutAttributes(type, audioType)) {
          return true
        }
      }
      return false
    }
  }

  override fun execute(context: BorrowContextType) {
    context.taskRecorder.beginNewStep("Downloading audio book...")
    context.bookDownloadIsRunning(null, 0L, 0L, "Requesting download...")

    return try {
      val currentURI = context.currentURICheck()
      context.taskRecorder.addAttribute("URI", currentURI.toString())

      val downloaded = this.runStrategy(context, currentURI)
      try {
        this.saveDownloadedContent(context, downloaded)
      } finally {
        downloaded.file.delete()
      }
    } catch (e: BorrowSubtaskFailed) {
      context.bookDownloadFailed()
      throw e
    }
  }

  private data class DownloadedManifest(
    val file: File,
    val sourceURI: URI
  )

  private fun runStrategy(
    context: BorrowContextType,
    currentURI: URI
  ): DownloadedManifest {
    context.taskRecorder.beginNewStep("Executing audio book manifest strategy...")

    val audioBookCredentials: AudioBookCredentials? =
      context.account.loginState.credentials?.let { credentials ->
        when (credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            if (credentials.password.value.isBlank()) {
              AudioBookCredentials.UsernameOnly(
                userName = credentials.userName.value
              )
            } else {
              AudioBookCredentials.UsernamePassword(
                userName = credentials.userName.value,
                password = credentials.password.value
              )
            }
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary ->
            AudioBookCredentials.BearerToken(accessToken = credentials.accessToken)
          is AccountAuthenticationCredentials.SAML2_0 ->
            AudioBookCredentials.BearerToken(accessToken = credentials.accessToken)
        }
      }

    val strategy =
      context.audioBookManifestStrategies.createStrategy(
        AudioBookManifestRequest(
          targetURI = currentURI,
          contentType = context.currentAcquisitionPathElement.mimeType,
          userAgent = PlayerUserAgent(context.httpClient.userAgent()),
          credentials = audioBookCredentials,
          services = context.services,
          cacheDirectory = context.cacheDirectory()
        )
      )

    val subscription =
      strategy.events.subscribe { message ->
        context.bookDownloadIsRunning(
          expectedSize = 100L,
          receivedSize = 50L,
          bytesPerSecond = 0L,
          message = message
        )
      }

    return try {
      when (val result = strategy.execute()) {
        is TaskResult.Success -> {
          context.taskRecorder.currentStepSucceeded("Strategy succeeded.")
          context.taskRecorder.addAll(result.steps)
          context.taskRecorder.addAttributes(result.attributes)
          context.taskRecorder.beginNewStep("Checking AudioBook strategy result…")

          val outputFile = File.createTempFile("manifest", "data", context.cacheDirectory())
          outputFile.writeBytes(result.result.fulfilled.data)
          DownloadedManifest(
            file = outputFile,
            sourceURI = currentURI
          )
        }
        is TaskResult.Failure -> {
          context.taskRecorder.currentStepFailed("Strategy failed.", audioStrategyFailed)
          context.taskRecorder.addAll(result.steps)
          context.taskRecorder.addAttributes(result.attributes)
          context.taskRecorder.beginNewStep("Checking AudioBook strategy result…")

          val exception = BorrowSubtaskFailed()
          context.taskRecorder.currentStepFailed("Failed", audioStrategyFailed, exception)
          throw exception
        }
      }
    } finally {
      subscription.unsubscribe()
    }
  }

  private fun saveDownloadedContent(
    context: BorrowContextType,
    data: DownloadedManifest
  ) {
    context.taskRecorder.beginNewStep("Saving book...")

    return when (val formatHandle = context.bookDatabaseEntry.findFormatHandleForContentType(context.currentAcquisitionPathElement.mimeType)) {
      is BookDatabaseEntryFormatHandleAudioBook -> {
        formatHandle.copyInManifestAndURI(
          data = data.file.readBytes(),
          manifestURI = data.sourceURI
        )
        context.bookDownloadSucceeded()
      }
      is BookDatabaseEntryFormatHandlePDF,
      is BookDatabaseEntryFormatHandleEPUB,
      null ->
        throw UnreachableCodeException()
    }
  }
}
