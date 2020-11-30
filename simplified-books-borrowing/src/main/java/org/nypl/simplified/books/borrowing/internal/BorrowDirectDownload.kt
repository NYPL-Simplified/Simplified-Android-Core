package org.nypl.simplified.books.borrowing.internal

import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMEType
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericEPUBFiles
import org.nypl.simplified.books.formats.api.StandardFormatNames.genericPDFFiles
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
      target: URI?,
      account: AccountReadableType?
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

    BorrowHTTP.download(context)
  }
}
