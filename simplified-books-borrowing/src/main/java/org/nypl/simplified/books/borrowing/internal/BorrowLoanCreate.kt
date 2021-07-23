package org.nypl.simplified.books.borrowing.internal

import com.io7m.junreachable.UnreachableCodeException
import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMECompatibility.applicationOctetStream
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.api.LSHTTPRequestBuilderType.Method.Put
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.book_registry.BookStatus.Held.HeldInQueue
import org.nypl.simplified.books.book_registry.BookStatus.Held.HeldReady
import org.nypl.simplified.books.book_registry.BookStatus.Loaned.LoanedNotDownloaded
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpConnectionFailed
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.httpRequestFailed
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.opdsFeedEntryHoldable
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.opdsFeedEntryLoanable
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.opdsFeedEntryNoNext
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.opdsFeedEntryParseError
import org.nypl.simplified.books.borrowing.internal.BorrowHTTP.authorizationOf
import org.nypl.simplified.books.borrowing.internal.BorrowHTTP.isMimeTypeAcceptable
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskHaltedEarly
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import org.nypl.simplified.books.formats.api.StandardFormatNames.allOPDSFeeds
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement
import org.nypl.simplified.opds.core.OPDSAcquisitionPaths
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.opds.core.OPDSParseException
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI

/**
 * A task that creates an OPDS loan by hitting an acquisition URI.
 *
 * XXX: Use [FeedLoading.loadSingleEntryFeed]
 */

class BorrowLoanCreate private constructor() : BorrowSubtaskType {

  companion object : BorrowSubtaskFactoryType {
    override val name: String
      get() = "Create OPDS Loan"

    override fun createSubtask(): BorrowSubtaskType {
      return BorrowLoanCreate()
    }

    override fun isApplicableFor(
      type: MIMEType,
      target: URI?,
      account: AccountReadableType?
    ): Boolean {
      for (opdsType in allOPDSFeeds) {
        if (MIMECompatibility.isCompatibleStrictWithoutAttributes(opdsType, type)) {
          return true
        }
      }
      return false
    }
  }

  override fun execute(context: BorrowContextType) {
    context.taskRecorder.beginNewStep("Creating an OPDS loan...")

    try {
      context.bookLoanIsRequesting("Requesting a loan...")

      val currentURI = context.currentURICheck()
      context.taskRecorder.beginNewStep("Using $currentURI to create a loan...")
      context.taskRecorder.addAttribute("Loan URI", currentURI.toString())
      context.checkCancelled()

      val request =
        context.httpClient.newRequest(currentURI)
          .setMethod(Put(ByteArray(0), applicationOctetStream))
          .setAuthorization(authorizationOf(context.account))
          .build()

      return request.execute().use { response ->
        when (val status = response.status) {
          is LSHTTPResponseStatus.Responded.OK ->
            this.handleOKRequest(context, currentURI, status)
          is LSHTTPResponseStatus.Responded.Error ->
            this.handleHTTPError(context, status)
          is LSHTTPResponseStatus.Failed ->
            this.handleHTTPFailure(context, status)
        }
      }
    } catch (e: BorrowSubtaskFailed) {
      context.bookLoanFailed()
      throw e
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
    val report = status.properties.problemReport
    if (report != null) {
      context.taskRecorder.addAttributes(report.toMap())

      if (report.type == "http://librarysimplified.org/terms/problem/loan-already-exists") {
        context.taskRecorder.currentStepSucceeded("It turns out we already had a loan for this book.")
        context.bookPublishStatus(
          LoanedNotDownloaded(
            id = context.bookCurrent.id,
            loanExpiryDate = null,
            returnable = false
          )
        )
        return
      }
    }

    context.taskRecorder.currentStepFailed(
      message = "HTTP request failed: ${status.properties.originalStatus} ${status.properties.message}",
      errorCode = httpRequestFailed,
      exception = null
    )
    throw BorrowSubtaskFailed()
  }

  private fun handleOKRequest(
    context: BorrowContextType,
    uri: URI,
    status: LSHTTPResponseStatus.Responded.OK
  ) {
    if (!isMimeTypeAcceptable(context, status.properties.contentType)) {
      throw BorrowSubtaskFailed()
    }

    val inputStream = status.bodyStream ?: ByteArrayInputStream(ByteArray(0))
    val entry = this.parseOPDSFeedEntry(context, inputStream, uri)
    context.bookDatabaseEntry.writeOPDSEntry(entry)

    this.checkAvailability(context, entry)
    val nextURI = this.findNextURI(context, entry)
    context.receivedNewURI(nextURI)
  }

  private fun checkAvailability(
    context: BorrowContextType,
    entry: OPDSAcquisitionFeedEntry
  ) {
    context.taskRecorder.beginNewStep("Checking OPDS availability...")

    return entry.availability.matchAvailability(
      object : OPDSAvailabilityMatcherType<Unit, BorrowSubtaskException> {
        override fun onHeldReady(a: OPDSAvailabilityHeldReady) {
          context.taskRecorder.currentStepSucceeded("Book is held and ready.")
          context.bookPublishStatus(
            HeldReady(
              id = context.bookCurrent.id,
              endDate = a.endDateOrNull,
              isRevocable = a.revoke.isSome
            )
          )
          throw BorrowSubtaskHaltedEarly()
        }

        override fun onHeld(a: OPDSAvailabilityHeld) {
          context.taskRecorder.currentStepSucceeded("Book is held.")
          context.bookPublishStatus(
            HeldInQueue(
              id = context.bookCurrent.id,
              queuePosition = a.positionOrNull,
              startDate = a.startDateOrNull,
              isRevocable = a.revoke.isSome,
              endDate = a.endDateOrNull
            )
          )
          throw BorrowSubtaskHaltedEarly()
        }

        /**
         * If the book is available to be placed on hold, set the
         * appropriate status.
         *
         * XXX: This should not occur in practice! Should this code be
         * unreachable?
         */

        override fun onHoldable(a: OPDSAvailabilityHoldable) {
          context.taskRecorder.currentStepFailed("Book is unexpectedly holdable.", opdsFeedEntryHoldable)
          throw BorrowSubtaskFailed()
        }

        override fun onLoaned(a: OPDSAvailabilityLoaned) {
          context.taskRecorder.currentStepSucceeded("Book is loaned.")
          context.bookPublishStatus(
            LoanedNotDownloaded(
              id = context.bookCurrent.id,
              loanExpiryDate = a.endDateOrNull,
              returnable = a.revoke.isSome
            )
          )
        }

        /**
         * If the book claims to be only "loanable", then something is
         * definitely wrong.
         *
         * XXX: This should not occur in practice! Should this code be
         * unreachable?
         */

        override fun onLoanable(a: OPDSAvailabilityLoanable) {
          context.taskRecorder.currentStepFailed("Book is unexpectedly loanable.", opdsFeedEntryLoanable)
          throw BorrowSubtaskFailed()
        }

        override fun onOpenAccess(a: OPDSAvailabilityOpenAccess) {
          context.taskRecorder.currentStepSucceeded("Book is open access.")
          context.bookPublishStatus(
            LoanedNotDownloaded(
              id = context.bookCurrent.id,
              loanExpiryDate = a.endDateOrNull,
              returnable = a.revoke.isSome
            )
          )
        }

        /**
         * The server cannot return a "revoked" representation. Reaching
         * this code indicates a serious bug in the application.
         */

        override fun onRevoked(a: OPDSAvailabilityRevoked) {
          throw UnreachableCodeException()
        }
      })
  }

  private fun parseOPDSFeedEntry(
    context: BorrowContextType,
    inputStream: InputStream,
    uri: URI
  ): OPDSAcquisitionFeedEntry {
    context.taskRecorder.beginNewStep("Parsing the OPDS feed entry...")
    val parser = OPDSAcquisitionFeedEntryParser.newParser()

    return try {
      inputStream.use {
        parser.parseEntryStream(uri, it)
      }
    } catch (e: OPDSParseException) {
      context.logError("OPDS feed parse error: ", e)
      context.taskRecorder.currentStepFailed(
        message = "Failed to parse the OPDS feed entry (${e.message}).",
        errorCode = opdsFeedEntryParseError,
        exception = e
      )
      throw BorrowSubtaskFailed()
    }
  }

  private fun findNextURI(
    context: BorrowContextType,
    entry: OPDSAcquisitionFeedEntry
  ): URI {
    context.taskRecorder.beginNewStep("Finding the next URI in the OPDS feed entry...")

    val remaining = context.opdsAcquisitionPathRemaining()
    val nextPaths = OPDSAcquisitionPaths.linearize(entry)
    for (nextPath in nextPaths) {
      val nextElements = nextPath.elements
      if (nextElements.size == remaining.size) {
        if (nextElements.zip(remaining).all(this::typesAreCompatible)) {
          val nextURI = nextElements[0].target
          if (nextURI != null) {
            context.taskRecorder.currentStepSucceeded("Found a usable URI ($nextURI).")
            return nextURI
          }
        }
      }
    }

    context.taskRecorder.currentStepFailed(
      message = "The OPDS feed entry did not provide a 'next' URI.",
      errorCode = opdsFeedEntryNoNext
    )
    throw BorrowSubtaskFailed()
  }

  private fun typesAreCompatible(pair: Pair<OPDSAcquisitionPathElement, OPDSAcquisitionPathElement>) =
    MIMECompatibility.isCompatibleStrictWithoutAttributes(pair.first.mimeType, pair.second.mimeType)
}
