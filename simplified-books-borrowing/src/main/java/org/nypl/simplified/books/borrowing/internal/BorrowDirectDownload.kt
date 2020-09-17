package org.nypl.simplified.books.borrowing.internal

import com.io7m.junreachable.UnreachableCodeException
import one.irradia.mime.api.MIMECompatibility
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpConnectionFailed
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpContentTypeIncompatible
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpEmptyBody
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import java.io.File
import java.io.InputStream

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
  }

  override fun execute(context: BorrowContextType) {
    context.taskRecorder.beginNewStep("Downloading directly...")

    val currentURI = context.currentURICheck()
    context.logDebug("downloading {}", currentURI)
    context.taskRecorder.beginNewStep("Downloading $currentURI...")
    context.taskRecorder.addAttribute("URI", currentURI.toString())

    context.checkCancelled()

    val request =
      context.httpClient.newRequest(currentURI)
        .build()

    return request.execute().use { response ->
      when (val status = response.status) {
        is LSHTTPResponseStatus.Responded.OK ->
          this.handleOKRequest(context, status)
        is LSHTTPResponseStatus.Responded.Error ->
          this.handleHTTPError(context, status)
        is LSHTTPResponseStatus.Failed ->
          this.handleHTTPFailure(context, status)
      }
    }
  }

  private fun handleOKRequest(
    context: BorrowContextType,
    status: LSHTTPResponseStatus.Responded.OK
  ) {
    val expectedType = context.currentAcquisitionPathElement.mimeType
    val receivedType = status.contentType
    if (!MIMECompatibility.isCompatibleLax(receivedType, expectedType)) {
      context.taskRecorder.currentStepFailed(
        message = "The server returned an incompatible context type: We wanted something compatible with ${expectedType.fullType} but received ${receivedType.fullType}.",
        errorCode = httpContentTypeIncompatible
      )
      throw BorrowSubtaskFailed()
    }

    val inputStream = status.bodyStream
    if (inputStream == null) {
      context.taskRecorder.currentStepFailed(
        message = "The server returned an empty HTTP body.",
        errorCode = httpEmptyBody
      )
      throw BorrowSubtaskFailed()
    }

    val file = context.temporaryFile()
    try {
      this.transfer(
        context = context,
        expectedSize = status.contentLength,
        file = file,
        inputStream = inputStream
      )

      when (val formatHandle = context.bookDatabaseEntry.findFormatHandleForContentType(expectedType)) {
        is BookDatabaseEntryFormatHandleEPUB ->
          formatHandle.copyInBook(file)
        is BookDatabaseEntryFormatHandlePDF ->
          formatHandle.copyInBook(file)
        is BookDatabaseEntryFormatHandleAudioBook,
        null ->
          throw UnreachableCodeException()
      }
    } finally {
      file.delete()
    }
  }

  private fun transfer(
    context: BorrowContextType,
    file: File,
    expectedSize: Long?,
    inputStream: InputStream
  ) {
    file.outputStream().use { outputStream ->
      val unitsPerSecond = BorrowUnitsPerSecond(context.clock)
      val buffer = ByteArray(65536)
      var total = 0L

      context.bookIsDownloading(
        expectedSize = expectedSize,
        receivedSize = total,
        bytesPerSecond = 0L
      )

      while (true) {
        context.checkCancelled()

        val read = inputStream.read(buffer)
        if (read == -1) {
          break
        }
        outputStream.write(buffer, 0, read)
        total += read
        if (unitsPerSecond.update(read.toLong())) {
          context.bookIsDownloading(
            expectedSize = expectedSize,
            receivedSize = total,
            bytesPerSecond = unitsPerSecond.now
          )
        }
      }
    }
  }

  private fun handleHTTPFailure(
    context: BorrowContextType,
    status: LSHTTPResponseStatus.Failed
  ) {
    context.taskRecorder.currentStepFailed(
      message = status.exception.message ?: "Exception raised during connection attempt.",
      errorCode = httpConnectionFailed,
      exception = status.exception
    )
    throw BorrowSubtaskFailed()
  }

  private fun handleHTTPError(
    context: BorrowContextType,
    status: LSHTTPResponseStatus.Responded.Error
  ) {
    context.taskRecorder.addAttributes(BorrowHTTP.problemReportAsAttributes(status.problemReport))
    context.taskRecorder.currentStepFailed(
      message = "HTTP request failed: ${status.originalStatus} ${status.message}",
      errorCode = BorrowErrorCodes.httpRequestFailed,
      exception = null
    )
    throw BorrowSubtaskFailed()
  }
}
