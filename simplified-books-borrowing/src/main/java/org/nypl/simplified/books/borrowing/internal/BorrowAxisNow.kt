package org.nypl.simplified.books.borrowing.internal

import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.downloads.LSHTTPDownloadState
import org.librarysimplified.http.downloads.LSHTTPDownloads
import org.nypl.drm.core.AxisNowFulfillment
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import org.nypl.simplified.books.formats.api.StandardFormatNames
import java.net.URI

/**
 * A task that downloads AxisNow data and then fulfills a book.
 */

class BorrowAxisNow private constructor() : BorrowSubtaskType {

  companion object : BorrowSubtaskFactoryType {
    override val name: String
      get() = "AxisNow Download"

    override fun createSubtask(): BorrowSubtaskType {
      return BorrowAxisNow()
    }

    override fun isApplicableFor(
      type: MIMEType,
      target: URI?,
      account: AccountReadableType?
    ): Boolean {
      return MIMECompatibility.isCompatibleStrictWithoutAttributes(type, StandardFormatNames.axisNow)
    }
  }

  override fun execute(context: BorrowContextType) {
    try {
      checkDRMSupport(context)

      context.taskRecorder.beginNewStep("Downloading AxisNow token...")

      val currentURI = context.currentURICheck()
      context.logDebug("downloading {}", currentURI)
      context.taskRecorder.beginNewStep("Downloading $currentURI...")
      context.taskRecorder.addAttribute("URI", currentURI.toString())
      context.checkCancelled()

      // Download AxisNow fulfillment token

      val temporaryFile = context.temporaryFile()

      try {
        val downloadRequest =
          BorrowHTTP.createDownloadRequest(
            context = context,
            target = currentURI,
            outputFile = temporaryFile
          )

        when (val result = LSHTTPDownloads.download(downloadRequest)) {
          LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCancelled ->
            throw BorrowSubtaskException.BorrowSubtaskCancelled()
          is LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedServer ->
            throw BorrowHTTP.onDownloadFailedServer(context, result)
          is LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedUnacceptableMIME ->
            throw BorrowSubtaskFailed()
          is LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedExceptionally ->
            throw BorrowHTTP.onDownloadFailedExceptionally(context, result)
          is LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCompletedSuccessfully -> {
            this.fulfill(context, temporaryFile.readBytes())
          }
        }
      } finally {
        temporaryFile.delete()
      }
    } catch (e: BorrowSubtaskFailed) {
      context.bookDownloadFailed()
      throw e
    }
  }

  /**
   * Check that we actually have the required DRM support.
   */

  private fun checkDRMSupport(
    context: BorrowContextType
  ) {
    context.taskRecorder.beginNewStep("Checking for AxisNow support...")
    if (context.axisNowService == null) {
      context.taskRecorder.currentStepFailed(
        message = "This build of the application does not support AxisNow DRM.",
        errorCode = BorrowErrorCodes.axisNowNotSupported
      )
      throw BorrowSubtaskFailed()
    }
  }

  private fun fulfill(context: BorrowContextType, token: ByteArray) {
    context.bookDownloadIsRunning("Downloading...")
    context.taskRecorder.beginNewStep("Fulfilling book...")
    context.checkCancelled()

    var fulfillment: AxisNowFulfillment? = null

    try {
      fulfillment = context.axisNowService!!.fulfill(token, context::temporaryFile)
      saveFulfilledBook(context, fulfillment)
    } catch (e: Exception) {
      context.taskRecorder.currentStepFailed(
        message = "AxisNow fulfillment error: ${e.message}",
        errorCode = BorrowErrorCodes.axisNowFulfillmentFailed,
        exception = e
      )
      throw BorrowSubtaskFailed()
    } finally {
      fulfillment?.book?.deleteRecursively()
      fulfillment?.license?.delete()
      fulfillment?.userKey?.delete()
    }
  }

  private fun saveFulfilledBook(context: BorrowContextType, fulfillment: AxisNowFulfillment) {
    context.taskRecorder.beginNewStep("Saving fulfilled book...")
    val formatHandle = context.bookDatabaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)
    checkNotNull(formatHandle) {
      "A format handle for EPUB must be available."
    }

    formatHandle.setDRMKind(BookDRMKind.AXIS)
    formatHandle.copyInBook(fulfillment.book)
    context.taskRecorder.currentStepSucceeded("Saved book.")

    val drmHandle = formatHandle.drmInformationHandle as BookDRMInformationHandle.AxisHandle
    drmHandle.copyInAxisLicense(fulfillment.license)
    context.taskRecorder.currentStepSucceeded("Saved license.")
    drmHandle.copyInAxisUserKey(fulfillment.userKey)
    context.taskRecorder.currentStepSucceeded("Saved user key.")

    context.bookDownloadSucceeded()
  }
}
