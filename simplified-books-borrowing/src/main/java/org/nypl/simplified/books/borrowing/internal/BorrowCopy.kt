package org.nypl.simplified.books.borrowing.internal

import com.io7m.junreachable.UnreachableCodeException
import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.contentFileNotFound
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import org.nypl.simplified.books.bundled.api.BundledURIs
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI

/**
 * A task that copies content from `content://` URIs and bundled resources.
 */

class BorrowCopy private constructor() : BorrowSubtaskType {

  companion object : BorrowSubtaskFactoryType {
    override val name: String
      get() = "Content Copy"

    override fun createSubtask(): BorrowSubtaskType {
      return BorrowCopy()
    }

    override fun isApplicableFor(
      type: MIMEType,
      target: URI?
    ): Boolean {
      return if (target != null) {
        target.scheme == "content"
      } else {
        false
      }
    }
  }

  override fun execute(context: BorrowContextType) {
    context.taskRecorder.beginNewStep("Copying from content...")
    context.bookDownloadIsRunning(null, 0L, 0L, "Requesting download...")

    return try {
      val currentURI = context.currentURICheck()
      context.logDebug("downloading {}", currentURI)
      context.taskRecorder.beginNewStep("Downloading $currentURI...")
      context.taskRecorder.addAttribute("URI", currentURI.toString())

      val temporaryFile = context.temporaryFile()
      try {
        this.doCopy(
          context = context,
          sourceStream = this.streamOf(context, currentURI),
          file = temporaryFile
        )
        this.saveDownloadedContent(context, temporaryFile)
      } finally {
        temporaryFile.delete()
      }
    } catch (e: BorrowSubtaskFailed) {
      context.bookDownloadFailed()
      throw e
    }
  }

  private fun streamOf(
    context: BorrowContextType,
    source: URI
  ): InputStream {
    try {
      return if (BundledURIs.isBundledURI(source)) {
        streamOfBundled(context, source)
      } else {
        streamOfContent(context, source)
      }
    } catch (e: FileNotFoundException) {
      context.taskRecorder.currentStepFailed(
        message = "File not found.",
        errorCode = contentFileNotFound,
        exception = e
      )
      throw BorrowSubtaskFailed()
    }
  }

  private fun streamOfBundled(
    context: BorrowContextType,
    source: URI
  ): InputStream {
    return context.bundledContent.resolve(source)
  }

  private fun streamOfContent(
    context: BorrowContextType,
    source: URI
  ): InputStream {
    return context.contentResolver.openInputStreamOrThrow(source)
  }

  private fun doCopy(
    context: BorrowContextType,
    sourceStream: InputStream,
    file: File
  ) {
    context.taskRecorder.beginNewStep("Copying book from content resolver...")

    return FileOutputStream(file).use { output ->
      val buffer = ByteArray(65536)

      sourceStream.use { input ->
        val size = input.available().toLong()
        var consumed = 0L

        context.bookDownloadIsRunning(
          expectedSize = size,
          receivedSize = consumed,
          bytesPerSecond = 0L,
          message = "Copying..."
        )

        val perSecond = BorrowUnitsPerSecond(context.clock)
        while (true) {
          val r = input.read(buffer)
          if (r == -1) {
            break
          }

          consumed += r.toLong()
          output.write(buffer, 0, r)

          if (perSecond.update(r.toLong())) {
            context.bookDownloadIsRunning(
              expectedSize = size,
              receivedSize = consumed,
              bytesPerSecond = 0L,
              message = "Copying..."
            )
          }
        }
        output.flush()
      }
      context.taskRecorder.currentStepSucceeded("Copied book.")
    }
  }

  private fun saveDownloadedContent(
    context: BorrowContextType,
    file: File
  ) {
    context.taskRecorder.beginNewStep("Saving book...")

    return when (val formatHandle = context.bookDatabaseEntry.findFormatHandleForContentType(context.currentAcquisitionPathElement.mimeType)) {
      is BookDatabaseEntryFormatHandleEPUB -> {
        formatHandle.copyInBook(file)
        context.bookDownloadSucceeded()
      }
      is BookDatabaseEntryFormatHandlePDF -> {
        formatHandle.copyInBook(file)
        context.bookDownloadSucceeded()
      }
      is BookDatabaseEntryFormatHandleAudioBook,
      null ->
        throw UnreachableCodeException()
    }
  }
}
