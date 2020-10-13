package org.nypl.simplified.books.borrowing.internal

import com.io7m.junreachable.UnreachableCodeException
import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCancelled
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCompletedSuccessfully
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedExceptionally
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedServer
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedUnacceptableMIME
import org.librarysimplified.http.downloads.LSHTTPDownloads
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskCancelled
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericEPUBFiles
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericPDFFiles
import java.io.File
import java.net.URI

/**
 * A task that downloads a file directly and saves it to the book database. It _does not_
 * do any special logic such as audio book manifest fulfillment, Adobe ACS operations, or
 * anything else.
 */

class BorrowDirectDownload private constructor() : BorrowSubtaskType {

  companion object : BorrowSubtaskFactoryType {
    override val name: String
      get() = "Direct HTTP Download"

    override fun createSubtask(): BorrowSubtaskType {
      return BorrowDirectDownload()
    }

    override fun isApplicableFor(
      type: MIMEType,
      target: URI?
    ): Boolean {
      if (MIMECompatibility.isCompatibleStrictWithoutAttributes(type, genericEPUBFiles)) {
        return true
      }
      if (MIMECompatibility.isCompatibleStrictWithoutAttributes(type, genericPDFFiles)) {
        return true
      }
      return false
    }
  }

  override fun execute(context: BorrowContextType) {
    context.taskRecorder.beginNewStep("Downloading directly...")
    context.bookDownloadIsRunning(null, 0L, 0L, "Requesting download...")

    return try {
      val currentURI = context.currentURICheck()
      context.logDebug("downloading {}", currentURI)
      context.taskRecorder.beginNewStep("Downloading $currentURI...")
      context.taskRecorder.addAttribute("URI", currentURI.toString())

      val temporaryFile = context.temporaryFile()

      try {
        val downloadRequest =
          BorrowHTTP.createDownloadRequest(
            context = context,
            target = currentURI,
            outputFile = temporaryFile
          )

        when (val result = LSHTTPDownloads.download(downloadRequest)) {
          DownloadCancelled ->
            throw BorrowSubtaskCancelled()
          is DownloadFailedServer ->
            throw BorrowHTTP.onDownloadFailedServer(context, result)
          is DownloadFailedUnacceptableMIME ->
            throw BorrowSubtaskFailed()
          is DownloadFailedExceptionally ->
            throw BorrowHTTP.onDownloadFailedExceptionally(context, result)
          is DownloadCompletedSuccessfully ->
            this.saveDownloadedContent(context, temporaryFile)
        }
      } finally {
        temporaryFile.delete()
      }
    } catch (e: BorrowSubtaskFailed) {
      context.bookDownloadFailed()
      throw e
    }
  }

  private fun saveDownloadedContent(
    context: BorrowContextType,
    temporaryFile: File
  ) {
    context.taskRecorder.beginNewStep("Saving book...")

    return when (val formatHandle = context.bookDatabaseEntry.findFormatHandleForContentType(context.currentAcquisitionPathElement.mimeType)) {
      is BookDatabaseEntryFormatHandleEPUB -> {
        formatHandle.copyInBook(temporaryFile)
        context.bookDownloadSucceeded()
      }
      is BookDatabaseEntryFormatHandlePDF -> {
        formatHandle.copyInBook(temporaryFile)
        context.bookDownloadSucceeded()
      }
      is BookDatabaseEntryFormatHandleAudioBook,
      null ->
        throw UnreachableCodeException()
    }
  }
}
