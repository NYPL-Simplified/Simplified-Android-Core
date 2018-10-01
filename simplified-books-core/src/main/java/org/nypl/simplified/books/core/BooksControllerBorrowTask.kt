package org.nypl.simplified.books.core

import com.io7m.jfunctional.None
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.junreachable.UnimplementedCodeException
import com.io7m.junreachable.UnreachableCodeException
import org.json.JSONException
import org.json.JSONObject
import org.nypl.drm.core.AdobeAdeptACSMException
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptFulfillmentListenerType
import org.nypl.drm.core.AdobeAdeptFulfillmentToken
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.DRMUnsupportedException
import org.nypl.simplified.assertions.Assertions
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.downloader.core.DownloadListenerType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.http.core.HTTPAuthBasic
import org.nypl.simplified.http.core.HTTPAuthOAuth
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPProblemReport
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BUY
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_GENERIC
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SAMPLE
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SUBSCRIBE
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.opds.core.OPDSParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.UnsupportedOperationException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 *
 * The logic for borrowing and/or fulfilling a book.
 */

internal class BooksControllerBorrowTask(
  private val booksDatabase: BookDatabaseType,
  private val accountsDatabase: AccountsDatabaseType,
  private val booksStatus: BooksStatusCacheType,
  private val downloader: DownloaderType,
  private val http: HTTPType,
  private val downloads: ConcurrentHashMap<BookID, DownloadType>,
  private val bookID: BookID,
  private val feedEntry: OPDSAcquisitionFeedEntry,
  private val feedLoader: FeedLoaderType,
  private val adobeDRM: OptionType<AdobeAdeptExecutorType>,
  private val needsAuthentication: Boolean) : Runnable {

  private val shortID: String = this.bookID.shortID
  private var downloadRunningTotal: Long = 0
  private lateinit var databaseEntry: BookDatabaseEntryType
  private lateinit var acquisition: OPDSAcquisition

  private val accountCredentials: AccountCredentials
    get() {
      val credentialsOpt =
        this.accountsDatabase.accountGetCredentials()
      if (credentialsOpt.isNone) {
        throw IllegalStateException("Not logged in!")
      }

      return (credentialsOpt as Some<AccountCredentials>).get()
    }

  private fun downloadFailed(exception: OptionType<Throwable>) {
    LOG.error("[{}]: download failed", this.shortID)

    exception.map_ { x -> LOG.error("[{}]: download failed: ", this.shortID, x) }

    val failed = BookStatusDownloadFailed(this.bookID, exception, Option.none())
    this.booksStatus.booksStatusUpdate(failed)
    this.downloadRemoveFromCurrent()
  }

  private fun downloadRemoveFromCurrent(): DownloadType? {
    LOG.debug("removing download of {} from list", this.bookID)
    return this.downloads.remove(this.bookID)
  }

  private fun downloadCancelled() {
    try {
      val snap = this.databaseEntry.entryGetSnapshot()
      val status = BookStatus.fromSnapshot(this.bookID, snap)
      this.booksStatus.booksStatusUpdate(status)
    } catch (e: IOException) {
      LOG.error("i/o error reading snapshot: ", e)
    } finally {
      this.downloadRemoveFromCurrent()
    }
  }

  /**
   * Fulfill the given ACSM file, if Adobe DRM is supported. Otherwise, fail.
   *
   * @param file The ACSM file
   * @throws IOException On I/O errors
   */

  @Throws(IOException::class, AdobeAdeptACSMException::class, BookUnsupportedTypeException::class)
  private fun runFulfillACSM(file: File) {
    LOG.debug("[{}]: fulfilling ACSM file", this.shortID)

    /*
     * The ACSM file will typically have downloaded almost instantly, leaving
     * the download progress bar at 100%. The Adobe library will then take up
     * to roughly ten seconds to start fulfilling the ACSM. This call
     * effectively sets the download progress bar to 0% so that it doesn't look
     * as if the user is waiting for no good reason.
     */

    this.downloadDataReceived(0L, 100L)

    if (this.adobeDRM is Some<AdobeAdeptExecutorType>) {
      LOG.debug("[{}]: DRM support is available, using DRM connector", this.shortID)
      this.runFulfillACSMWithConnector(this.adobeDRM.get(), file)
    } else {
      LOG.debug("[{}]: DRM support is unavailable, cannot continue!", this.shortID)
      this.downloadFailed(Option.some(DRMUnsupportedException("DRM support is not available")))
    }
  }

  @Throws(IOException::class, AdobeAdeptACSMException::class, BookUnsupportedTypeException::class)
  private fun runFulfillACSMWithConnector(adobe: AdobeAdeptExecutorType, file: File) {

    val acsm = FileUtilities.fileReadBytes(file)
    val parsed = AdobeAdeptFulfillmentToken.parseFromBytes(acsm)
    val contentType = parsed.format
    if (!("application/epub+zip" == contentType)) {
      throw BookUnsupportedTypeException(contentType)
    }

    adobe.execute { connector ->

      /*
       * Create a fake download that cancels the Adobe download via
       * the net provider. There can only be one Adobe download in progress
       * at a time (the {@link AdobeAdeptExecutorType} interface
       * guarantees this),
       * so the download must refer to the current one.
       */

      this.downloads[this.bookID] = object : DownloadType {
        override fun cancel() {
          val net = connector.netProvider
          net.cancel()
        }

        override fun getContentType(): String {
          return "application/octet-stream"
        }
      }

      /*
       * In rare instances, there is a bug that will put the app in a state where
       * a user is signed in, but has no valid Adobe User ID associated with DRM activation.
       * If this occurs, cancel the attempted DRM fulfillment.
       */

      val user = this.accountCredentials.adobeUserID
      if (user is None<AdobeUserID>) {
        connector.netProvider.cancel()
        return@execute
      }

      if (user is Some<AdobeUserID>) {
        connector.fulfillACSM(AdobeFulfillmentListener(this, contentType), acsm, user.get())
        return@execute
      }
    }
  }

  @Throws(IOException::class)
  private fun runFulfillSimplifiedBearerToken(file: File) {
    LOG.debug("[{}]: fulfilling Simplified bearer token file", this.shortID)

    /*
     * The bearer token file will typically have downloaded almost instantly,
     * leaving the download progress bar at 100%. This call effectively sets
     * the download progress bar to 0% so that it doesn't look as if the user
     * is waiting for no good reason.
     */

    this.downloadDataReceived(0L, 100L)

    val jsonObject: JSONObject
    try {
      jsonObject = JSONObject(FileUtilities.fileReadUTF8(file))
    } catch (ex: JSONException) {
      this.downloadFailed(Option.some(ex))
      return
    }

    val token = SimplifiedBearerToken.withJSONObject(jsonObject)
    if (token == null) {
      this.downloadFailed(Option.some(Throwable("failed to parse Simplified bearer token")))
      return
    }

    val acquisition = OPDSAcquisition(
      ACQUISITION_GENERIC,
      token.location,
      Option.some(BooksControllerBorrowTask.SIMPLIFIED_BEARER_TOKEN_CONTENT_TYPE),
      emptyList())

    val auth = HTTPAuthOAuth(token.accessToken)
    this.runAcquisitionFulfillDoDownload(acquisition, Option.some(auth))
  }

  private fun downloadDataReceived(
    runningTotal: Long,
    expectedTotal: Long) {

    /*
     * Because "data received" updates happen at such a huge rate, we want
     * to ensure that updates to the book status are rate limited to avoid
     * overwhelming the UI. Book updates are only published at the start of
     * downloads, or if a large enough chunk of data has now been received
     * to justify a UI update.
     */

    val atStart = runningTotal == 0L
    val divider = expectedTotal.toDouble() / 10.0
    val longEnough = runningTotal.toDouble() > this.downloadRunningTotal.toDouble() + divider

    if (longEnough || atStart) {
      val status =
        BookStatusDownloadInProgress(this.bookID, runningTotal, expectedTotal, Option.none())
      this.booksStatus.booksStatusUpdate(status)
      this.downloadRunningTotal = runningTotal
    }
  }

  override fun run() {
    try {
      LOG.debug("[{}]: starting borrow (full id {})", this.shortID, this.bookID)
      LOG.debug("[{}]: creating feed entry", this.shortID)

      /*
       * First, create the on-disk database entry for the book. Write
       * the feed data to it, fetch the cover image (if any).
       */

      this.databaseEntry =
        this.booksDatabase.databaseCreateEntry(this.bookID, this.feedEntry)

      this.databaseEntry.entryUpdateAll(this.feedEntry, this.booksStatus, this.http)
      this.booksStatus.booksStatusUpdate(BookStatusRequestingLoan(this.bookID))

      /*
       * Now, pick the "best" acquisition out of all of the available acquisitions
       * in the feed.
       */

      val preferredAcquisitionOpt =
        BookAcquisitionSelection.preferredAcquisition(this.feedEntry.acquisitions)
      if (preferredAcquisitionOpt is Some<OPDSAcquisition>) {
        this.acquisition = preferredAcquisitionOpt.get()
      } else {
        throw UnsupportedOperationException("No usable acquisition!")
      }

      /*
       * Then, run the appropriate acquisition type for the book.
       */

      when (this.acquisition.relation) {
        ACQUISITION_BORROW -> {
          LOG.debug("[{}]: acquisition type is {}, performing borrow",
            this.shortID, this.acquisition.relation)
          this.runAcquisitionBorrow()
        }

        ACQUISITION_OPEN_ACCESS -> {
          LOG.debug("[{}]: acquisition type is {}, performing fulfillment",
            this.shortID, this.acquisition.relation)
          this.runAcquisitionFulfill(this.feedEntry)
        }

        ACQUISITION_GENERIC -> {
          if (this.adobeDRM.isSome && this.needsAuthentication) {
            val credentialsOpt = this.accountsDatabase.accountGetCredentials()
            if (credentialsOpt is Some<AccountCredentials>) {

              val adobeCredentials = credentialsOpt.get()
              val adobeUserID = adobeCredentials.adobeUserID
              if (adobeUserID is Some<AdobeUserID>) {
                LOG.debug("[{}]: acquisition type is {}, performing fulfillment",
                  this.shortID, this.acquisition.relation)
                this.runAcquisitionFulfill(this.feedEntry)
              } else {
                val activationTask = BooksControllerDeviceActivationTask(
                  this.adobeDRM,
                  adobeCredentials,
                  this.accountsDatabase,
                  this.booksDatabase,
                  object : DeviceActivationListenerType {
                    override fun onDeviceActivationFailure(message: String) {
                      LOG.debug("device activation failed: {}", message)

                      val task = this@BooksControllerBorrowTask
                      val failed =
                        BookStatusDownloadFailed(
                          task.bookID,
                          Option.some(AccountTooManyActivationsException(message)),
                          Option.none())
                      task.booksStatus.booksStatusUpdate(failed)
                      task.downloadRemoveFromCurrent()
                    }

                    override fun onDeviceActivationSuccess() {
                      LOG.debug("device activation succeeded")
                    }
                  })
                activationTask.run()
              }
            }
          } else {
            LOG.debug("[{}]: acquisition type is {}, performing fulfillment",
              this.shortID, this.acquisition.relation)
            this.runAcquisitionFulfill(this.feedEntry)
          }
        }

        ACQUISITION_BUY,
        ACQUISITION_SAMPLE,
        ACQUISITION_SUBSCRIBE -> {
          LOG.debug("[{}]: acquisition type is {}, cannot continue!",
            this.shortID, this.acquisition.relation)
          throw UnimplementedCodeException()
        }
      }
    } catch (e: Throwable) {
      LOG.error("[{}]: error: ", this.shortID, e)
      this.downloadFailed(Option.some(e))
    }
  }

  /**
   * Hit a "borrow" link, read the resulting feed, download the book if it is
   * available.
   */

  private fun runAcquisitionBorrow() {
    LOG.debug("[{}]: borrowing", this.shortID)

    /*
     * Borrowing requires authentication.
     */

    val credentials = this.accountCredentials
    val barcode = credentials.barcode
    val pin = credentials.pin
    var auth: HTTPAuthType = HTTPAuthBasic(barcode.toString(), pin.toString())

    val authTokenOpt = credentials.authToken
    if (authTokenOpt is Some<AccountAuthToken>) {
      val token = authTokenOpt.get()
      if (token != null) {
        auth = HTTPAuthOAuth(token.toString())
      }
    }

    /*
     * Grab the feed for the borrow link.
     */

    LOG.debug("[{}]: fetching item feed: {}", this.shortID, this.acquisition.uri)

    val task = this
    val feedEntryMatcher = object : FeedEntryMatcherType<Unit, UnreachableCodeException> {
      override fun onFeedEntryOPDS(e: FeedEntryOPDS): Unit {
        try {
          task.runAcquisitionBorrowGotOPDSEntry(e)
        } catch (x: IOException) {
          task.downloadFailed(Option.some(x))
        } catch (x: BookBorrowExceptionNoUsableAcquisition) {
          task.downloadFailed(Option.some(x))
        }

        return Unit.unit()
      }

      override fun onFeedEntryCorrupt(e: FeedEntryCorrupt): Unit {
        LOG.error("[{}]: unexpectedly received corrupt feed entry", task.shortID)
        task.downloadFailed(Option.some(BookBorrowExceptionBadBorrowFeed(e.error)))
        return Unit.unit()
      }
    }

    val feedMatcher = object : FeedMatcherType<Unit, UnreachableCodeException> {
      override fun onFeedWithGroups(f: FeedWithGroups): Unit {
        LOG.debug("[{}]: received feed with groups, using first entry", task.shortID)
        return f[0].groupEntries[0].matchFeedEntry(feedEntryMatcher)
      }

      override fun onFeedWithoutGroups(f: FeedWithoutGroups): Unit {
        LOG.debug("[{}]: received feed without groups, using first entry", task.shortID)
        return f[0].matchFeedEntry(feedEntryMatcher)
      }
    }

    this.feedLoader.fromURIRefreshing(
      this.acquisition.uri, Option.some(auth), "PUT", object : FeedLoaderListenerType {
      override fun onFeedLoadSuccess(u: URI, f: FeedType) {
        try {
          LOG.debug("[{}]: loaded feed from {}", task.shortID, u)
          f.matchFeed(feedMatcher)
        } catch (e: Throwable) {
          LOG.error("[{}]: failure after receiving feed: {}: ", task.shortID, u, e)
          task.downloadFailed(Option.some(e))
        }
      }

      override fun onFeedRequiresAuthentication(
        u: URI,
        attempts: Int,
        listener: FeedLoaderAuthenticationListenerType) {
        LOG.debug("[{}]: feed {} requires authentication but none can be provided", task.shortID, u)

        /*
         * XXX: If the feed resulting from borrowing a book requires
         * authentication, then the user should be notified somehow and given
         * a chance to log in.  The app currently has the user log in prior
         * to attempting an operation that requires credentials, but those
         * credentials could have become stale in between "logging in" and
         * attempting to borrow a book. We have no way to notify the user that
         * their credentials are incorrect from here, however.
         */

        listener.onAuthenticationNotProvided()
      }

      override fun onFeedLoadFailure(u: URI, x: Throwable) {
        LOG.debug("[{}]: failed to load feed", task.shortID)

        var ex: Throwable = BookBorrowExceptionFetchingBorrowFeedFailed(x)

        if (x is OPDSParseException) {
          ex = BookBorrowExceptionBadBorrowFeed(x)
        } else if (x is FeedHTTPTransportException) {
          val problemReportOpt = x.problemReport
          if (problemReportOpt.isSome) {
            val problemReport = (problemReportOpt as Some<HTTPProblemReport>).get()
            val problemType = problemReport.problemType
            if (problemType == HTTPProblemReport.ProblemType.LoanLimitReached) {
              ex = BookBorrowExceptionLoanLimitReached(x)
            }
            if (HTTPProblemReport.ProblemStatus.Unauthorized == problemReport.problemStatus) {
              try {
                task.accountsDatabase.accountRemoveCredentials()
              } catch (e: IOException) {
                e.printStackTrace()
              }

            }
          }
        }

        task.downloadFailed(Option.some(ex))
      }
    })
  }

  @Throws(IOException::class, BookBorrowExceptionNoUsableAcquisition::class)
  private fun runAcquisitionBorrowGotOPDSEntry(e: FeedEntryOPDS) {
    val sid = this.shortID

    LOG.debug("[{}]: received OPDS feed entry", sid)
    LOG.debug("[{}]: book availability is {}", sid, e.feedEntry.availability)

    /*
     * Update the database.
     */

    LOG.debug("[{}]: saving state to database", sid)
    this.databaseEntry.entrySetFeedData(e.feedEntry)

    /*
     * Then, work out what to do based on the latest availability data.
     * If the book is loaned, attempt to download it. If it is held, notify
     * the user.
     */

    LOG.debug("[{}]: continuing borrow based on availability", sid)

    val bookID = this.bookID
    val stat = this.booksStatus

    val wantFulfill = e.feedEntry.availability.matchAvailability(
      object : OPDSAvailabilityMatcherType<Boolean, UnreachableCodeException> {
        /**
         * If the book is held but is ready for download, just notify
         * the user of this fact by setting the status.
         */

        override fun onHeldReady(a: OPDSAvailabilityHeldReady): Boolean? {
          LOG.debug("[{}]: book is held but is ready, nothing more to do", sid)

          stat.booksStatusUpdate(BookStatusHeldReady(bookID, a.endDate, a.revoke.isSome))
          return java.lang.Boolean.FALSE
        }

        /**
         * If the book is held, just notify the user of this fact by
         * setting the status.
         */

        override fun onHeld(a: OPDSAvailabilityHeld): Boolean? {
          LOG.debug("[{}]: book is held, nothing more to do", sid)

          stat.booksStatusUpdate(
            BookStatusHeld(bookID, a.position, a.startDate, a.endDate, a.revoke.isSome))
          return java.lang.Boolean.FALSE
        }

        /**
         * If the book is available to be placed on hold, set the
         * appropriate status.
         *
         * XXX: This should not occur in practice! Should this code be
         * unreachable?
         */

        override fun onHoldable(a: OPDSAvailabilityHoldable): Boolean? {
          LOG.debug("[{}]: book is holdable, cannot continue!", sid)

          stat.booksStatusUpdate(BookStatusHoldable(bookID))
          return java.lang.Boolean.FALSE
        }

        /**
         * If the book claims to be only "loanable", then something is
         * definitely wrong.
         *
         * XXX: This should not occur in practice! Should this code be
         * unreachable?
         */

        override fun onLoanable(a: OPDSAvailabilityLoanable): Boolean? {
          LOG.debug("[{}]: book is loanable, this is a server bug!", sid)
          throw UnreachableCodeException()
        }

        /**
         * If the book is "loaned", then attempt to fulfill the book.
         */

        override fun onLoaned(a: OPDSAvailabilityLoaned): Boolean? {
          LOG.debug("[{}]: book is loaned, fulfilling", sid)

          stat.booksStatusUpdate(BookStatusRequestingDownload(bookID, a.endDate))
          return java.lang.Boolean.TRUE
        }

        /**
         * If the book is "open-access", then attempt to fulfill the
         * book.
         */

        override fun onOpenAccess(a: OPDSAvailabilityOpenAccess): Boolean? {
          LOG.debug("[{}]: book is open access, fulfilling", sid)

          val status = BookStatusRequestingDownload(bookID, Option.none())
          stat.booksStatusUpdate(status)
          return java.lang.Boolean.TRUE
        }

        /**
         * The server cannot return a "revoked" representation. Reaching
         * this code indicates a serious bug in the application.
         */

        override fun onRevoked(a: OPDSAvailabilityRevoked): Boolean? {
          throw UnreachableCodeException()
        }
      })

    if (wantFulfill) {
      this.downloads[bookID] = this.runAcquisitionFulfill(e.feedEntry)
    }
  }

  private fun runAcquisitionFulfillDoDownload(
    a: OPDSAcquisition,
    predeterminedAuth: OptionType<HTTPAuthType>): DownloadType {

    /*
     * Downloading requires authentication.
     */
    var auth = predeterminedAuth
    if (predeterminedAuth.isNone && this.needsAuthentication) {
      val credentials = this.accountCredentials
      val barcode = credentials.barcode
      val pin = credentials.pin
      auth = Option.some(HTTPAuthBasic(barcode.toString(), pin.toString()))

      val authTokenOpt = credentials.authToken
      if (authTokenOpt is Some<AccountAuthToken>) {
        val token = authTokenOpt.get()
        if (token != null) {
          auth = Option.some(HTTPAuthOAuth(token.toString()))
        }
      }
    }

    val sid = this.shortID
    LOG.debug("[{}]: starting download", sid)

    /*
     * Point the downloader at the acquisition link. The result will be an
     * EPUB, ACSM file, or Simplified bearer token. ACSM files have to be
     * "fulfilled" after downloading by passing them to the Adobe DRM
     * connector. Bearer token documents need an additional request to
     * actually get the book in question.
     */

    val task = this
    return this.downloader.download(a.uri, auth, object : DownloadListenerType {
      override fun onDownloadStarted(
        download: DownloadType,
        expectedTotal: Long) {
        task.downloadDataReceived(0L, expectedTotal)
      }

      override fun onDownloadDataReceived(
        download: DownloadType,
        runningTotal: Long,
        expectedTotal: Long) {
        task.downloadDataReceived(runningTotal, expectedTotal)
      }

      override fun onDownloadCancelled(d: DownloadType) {
        task.downloadCancelled()
      }

      override fun onDownloadFailed(
        download: DownloadType,
        status: Int,
        runningTotal: Long,
        exception: OptionType<Throwable>) {

        /*
         * If the content type indicates that the file was an ACSM file,
         * explicitly indicate that it was fetching an ACSM that failed.
         * This allows the UI to assign blame!
         */

        val ex: Throwable
        val acsmType = BooksControllerBorrowTask.ACSM_CONTENT_TYPE
        if (acsmType == download.contentType) {
          ex = BookBorrowExceptionFetchingACSMFailed.newException(exception)
        } else {
          ex = BookBorrowExceptionFetchingBookFailed.newException(exception)
        }

        task.downloadFailed(Option.some(ex))
      }

      override fun onDownloadCompleted(
        download: DownloadType,
        file: File) {
        try {
          LOG.debug("[{}]: download {} completed for {}", sid, download, file)

          task.downloadRemoveFromCurrent()

          val contentType = download.contentType
          LOG.debug("[{}]: content type is {}", sid, contentType)

          if (BooksControllerBorrowTask.ACSM_CONTENT_TYPE == contentType) {
            task.runFulfillACSM(file)
          } else if (BooksControllerBorrowTask.SIMPLIFIED_BEARER_TOKEN_CONTENT_TYPE == contentType) {
            task.runFulfillSimplifiedBearerToken(file)
          } else {
            task.saveFinalContent(
              file = file,
              contentType = contentType,
              adobeRights = Option.none())
          }
        } catch (e: IOException) {
          LOG.error("onDownloadCompleted: i/o exception: ", e)
          task.downloadFailed(Option.some(e))
        } catch (e: BookUnsupportedTypeException) {
          LOG.error("onDownloadCompleted: unsupported book exception: ", e)
          task.downloadFailed(Option.some(e))
        } catch (e: AdobeAdeptACSMException) {
          LOG.error("onDownloadCompleted: acsm exception: ", e)
          task.downloadFailed(Option.some(e))
        }
      }
    })
  }

  private fun saveFinalContent(
    file: File,
    contentType: String,
    adobeRights: OptionType<AdobeAdeptLoan>) {

    LOG.debug("[{}]: saving content {} ({})", this.shortID, file, contentType)
    LOG.debug("[{}]: saving rights {}", this.shortID, adobeRights)

    val formatHandleOpt: OptionType<BookDatabaseEntryFormatHandle> =
      this.databaseEntry.entryFindFormatHandleForContentType(contentType)

    fun updateStatus() {
      val downloadedSnap = this.databaseEntry.entryGetSnapshot()
      val downloadedStatus = BookStatus.fromSnapshot(this.bookID, downloadedSnap)

      Assertions.checkPrecondition(
        downloadedStatus is BookStatusDownloaded,
        "Downloaded book status must be Downloaded (is %s)",
        downloadedStatus)

      this.booksStatus.booksStatusUpdate(downloadedStatus)
    }

    if (formatHandleOpt is Some<BookDatabaseEntryFormatHandle>) {
      val format = formatHandleOpt.get()
      return when (format) {
        is BookDatabaseEntryFormatHandleEPUB -> {
          format.copyInBook(file)
          format.setAdobeRightsInformation(adobeRights)
          updateStatus()
        }
        is BookDatabaseEntryFormatHandleAudioBook -> {
          throw UnimplementedCodeException()
        }
      }
    } else {
      LOG.error("[{}]: database entry does not have a format handle for {}",
        this.shortID, contentType)
      throw BookUnsupportedTypeException(contentType)
    }
  }

  /**
   * Fulfill a book by hitting the generic or open access links.
   */

  @Throws(BookBorrowExceptionNoUsableAcquisition::class)
  private fun runAcquisitionFulfill(ee: OPDSAcquisitionFeedEntry): DownloadType {
    LOG.debug("[{}]: fulfilling book", this.shortID)

    for (ea in ee.acquisitions) {
      when (ea.relation) {
        ACQUISITION_GENERIC,
        ACQUISITION_OPEN_ACCESS -> {
          return this.runAcquisitionFulfillDoDownload(ea, Option.none())
        }
        ACQUISITION_BORROW,
        ACQUISITION_BUY,
        ACQUISITION_SAMPLE,
        ACQUISITION_SUBSCRIBE -> {
        }
      }
    }

    throw BookBorrowExceptionNoUsableAcquisition()
  }

  /**
   * The listener passed to the Adobe library in order to perform fulfillment of
   * tokens delivered in ACSM files.
   */

  private class AdobeFulfillmentListener internal constructor(
    private val task: BooksControllerBorrowTask,
    private val contentType: String) : AdobeAdeptFulfillmentListenerType {

    override fun onFulfillmentFailure(message: String) {
      val error: OptionType<Throwable>

      if (message.startsWith("NYPL_UNSUPPORTED requestPasshash")) {
        error = Option.some(BookUnsupportedPasshashException())
      } else if (message.startsWith("E_ACT_NOT_READY")) {
        error = Option.some(AccountNotReadyException(message))
      } else if (message.startsWith("E_ACT_TOO_MANY_ACTIVATIONS")) {
        error = Option.some(AccountTooManyActivationsException(message))
      } else {
        error = Option.some(BookBorrowExceptionDRMWorkflowError(message))
      }
      //      else if (message.startsWith("E_LIC_ALREADY_FULFILLED_BY_ANOTHER_USER")) {
      //        error = Option.some((Throwable) new LicenceAlreadyFulfilledByAnotherUserException(message));
      //      }

      this.task.downloadFailed(error)
    }

    override fun onFulfillmentSuccess(file: File, loan: AdobeAdeptLoan) {
      try {
        this.task.saveFinalContent(file, contentType, Option.some(loan))
      } catch (x: Throwable) {
        LOG.error("failure saving content/rights: ", x)
        this.task.downloadFailed(Option.some(x))
      }
    }

    override fun onFulfillmentProgress(progress: Double) {

      /*
       * The Adobe library won't give exact numbers when it comes to bytes,
       * but the app doesn't actually need to display those anyway. We therefore
       * assume that an ebook is 10000 bytes long, and calculate byte values
       * as if this were true!
       */

      this.task.downloadDataReceived((10000.0 * progress).toLong(), 10000L)
    }

    override fun onFulfillmentCancelled() {
      this.task.downloadCancelled()
    }
  }

  companion object {

    const val ACSM_CONTENT_TYPE =
      "application/vnd.adobe.adept+xml"
    const val SIMPLIFIED_BEARER_TOKEN_CONTENT_TYPE =
      "application/vnd.librarysimplified.bearer-token+json"

    private val LOG: Logger =
      LoggerFactory.getLogger(BooksControllerBorrowTask::class.java)
  }
}
