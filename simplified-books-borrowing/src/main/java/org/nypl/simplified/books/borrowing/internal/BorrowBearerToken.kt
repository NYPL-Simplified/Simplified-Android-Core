package org.nypl.simplified.books.borrowing.internal

import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import org.nypl.simplified.books.formats.api.StandardFormatNames
import java.net.URI

/**
 * A task that pretends to negotiate a Simplified bearer token. Bearer token handling is
 * transparent in the HTTP client now, so this task trivially succeeds.
 */

class BorrowBearerToken : BorrowSubtaskType {

  companion object : BorrowSubtaskFactoryType {
    override val name: String
      get() = "Bearer Token Negotiation"

    override fun createSubtask(): BorrowSubtaskType {
      return BorrowBearerToken()
    }

    override fun isApplicableFor(
      type: MIMEType,
      target: URI?
    ): Boolean {
      return MIMECompatibility.isCompatibleStrictWithoutAttributes(
        type,
        StandardFormatNames.simplifiedBearerToken
      )
    }
  }

  override fun execute(context: BorrowContextType) {
    context.taskRecorder.beginNewStep("Handling bearer token negotiation...")
    context.bookDownloadIsRunning(null, 0L, 0L, "Requesting download...")

    return try {
      val currentURI = context.currentURICheck()
      context.receivedNewURI(currentURI)
      context.taskRecorder.currentStepSucceeded("Bearer token negotiation is transparent.")
      Unit
    } catch (e: BorrowSubtaskException.BorrowSubtaskFailed) {
      context.bookDownloadFailed()
      throw e
    }
  }
}
