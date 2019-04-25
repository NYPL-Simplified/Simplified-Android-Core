package org.nypl.simplified.books.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.SettableFuture
import com.io7m.jfunctional.None
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.junreachable.UnimplementedCodeException
import com.io7m.junreachable.UnreachableCodeException
import org.joda.time.LocalDateTime
import org.nypl.drm.core.AdobeAdeptACSMException
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptFulfillmentToken
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.DRMUnsupportedException
import org.nypl.simplified.books.accounts.AccountAuthenticatedHTTP
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.Book
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.BookDatabaseException
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed
import org.nypl.simplified.books.book_registry.BookStatusDownloadInProgress
import org.nypl.simplified.books.book_registry.BookStatusHeld
import org.nypl.simplified.books.book_registry.BookStatusHeldReady
import org.nypl.simplified.books.book_registry.BookStatusHoldable
import org.nypl.simplified.books.book_registry.BookStatusRequestingDownload
import org.nypl.simplified.books.book_registry.BookStatusRequestingLoan
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.bundled_content.BundledContentResolverType
import org.nypl.simplified.books.bundled_content.BundledURIs
import org.nypl.simplified.books.exceptions.BookBorrowExceptionBadBorrowFeed
import org.nypl.simplified.books.exceptions.BookBorrowExceptionFetchingACSMFailed
import org.nypl.simplified.books.exceptions.BookBorrowExceptionFetchingBookFailed
import org.nypl.simplified.books.exceptions.BookBorrowExceptionNoCredentials
import org.nypl.simplified.books.exceptions.BookBorrowExceptionNoUsableAcquisition
import org.nypl.simplified.books.exceptions.BookUnexpectedTypeException
import org.nypl.simplified.books.exceptions.BookUnsupportedTypeException
import org.nypl.simplified.books.feeds.Feed
import org.nypl.simplified.books.feeds.FeedEntry
import org.nypl.simplified.books.feeds.FeedGroup
import org.nypl.simplified.books.feeds.FeedLoaderResult
import org.nypl.simplified.books.feeds.FeedLoaderType
import org.nypl.simplified.books.logging.LogUtilities
import org.nypl.simplified.downloader.core.DownloadListenerType
import org.nypl.simplified.downloader.core.DownloadType
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.http.core.HTTPAuthOAuth
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPOAuthToken
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
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A book borrowing task.
 */

class BookBorrowTask(
  private val adobeDRM: AdobeAdeptExecutorType?,
  private val downloader: DownloaderType,
  private val downloads: ConcurrentHashMap<BookID, DownloadType>,
  private val feedLoader: FeedLoaderType,
  private val bundledContent: BundledContentResolverType,
  private val bookRegistry: BookRegistryType,
  private val bookId: BookID,
  private val account: AccountType,
  private val acquisition: OPDSAcquisition,
  private val entry: OPDSAcquisitionFeedEntry) : Callable<Unit> {

  @Throws(Exception::class)
  override fun call(): Unit {
    return execute()
  }

  @Volatile
  private var adobeLoan: AdobeAdeptLoan? = null

  @Volatile
  private var downloadRunningTotal: Long = 0L

  @Volatile
  private lateinit var fulfillURI: URI

  private lateinit var databaseEntry: BookDatabaseEntryType

  @Volatile
  private lateinit var bookInitial: Book

  private fun execute(): Unit {
    try {
      LOG.debug("[{}]: starting borrow", this.bookId.brief())
      LOG.debug("[{}]: creating feed entry", this.bookId.brief())

      /*
       * Create a new book database entry and publish the status of the book.
       */

      val database = this.account.bookDatabase()
      this.databaseEntry = database.createOrUpdate(this.bookId, this.entry)
      this.bookInitial = this.databaseEntry.book

      this.bookRegistry.update(
        BookWithStatus.create(this.databaseEntry.book, BookStatusRequestingLoan(this.bookId)))

      /*
       * If the requested URI appears to refer to bundled content, serve the book from there.
       */

      if (BundledURIs.isBundledURI(this.acquisition.uri)) {
        LOG.debug("[{}]: acquisition is bundled", this.bookId.brief())
        return this.runAcquisitionBundled()
      }

      val type = this.acquisition.relation
      return when (type) {
        ACQUISITION_BORROW -> {
          LOG.debug("[{}]: acquisition type is {}, performing borrow", this.bookId.brief(), type)
          this.runAcquisitionBorrow()
        }
        ACQUISITION_GENERIC -> {
          LOG.debug("[{}]: acquisition type is {}, performing generic procedure", this.bookId.brief(), type)
          this.runAcquisitionFulfill(this.entry)
        }
        ACQUISITION_OPEN_ACCESS -> {
          LOG.debug("[{}]: acquisition type is {}, performing fulfillment", this.bookId.brief(), type)
          this.runAcquisitionFulfill(this.entry)
        }
        ACQUISITION_BUY, ACQUISITION_SAMPLE, ACQUISITION_SUBSCRIBE -> {
          LOG.debug("[{}]: acquisition type is {}, cannot continue!", this.bookId.brief(), type)
          throw UnsupportedOperationException()
        }
      }

    } catch (e: Exception) {
      LOG.error("[{}]: error: ", this.bookId.brief(), e)
      return this.downloadFailed(Option.some(e))
    } finally {
      LOG.debug("[{}]: finished", this.bookId.brief())
    }
  }

  /**
   * Hit a "borrow" link, read the resulting feed, download the book if it is
   * available.
   */

  private fun runAcquisitionBorrow(): Unit {
    LOG.debug("[{}]: borrowing", this.bookId.brief())

    val httpAuth = createHttpAuthIfRequired()

    /*
     * Grab the feed for the borrow link.
     */

    LOG.debug("[{}]: fetching item feed: {}", this.bookId.brief(), this.acquisition.uri)

    val feedResult =
      this.feedLoader.fetchURIRefreshing(this.acquisition.uri, httpAuth, "PUT")
        .get(1L, TimeUnit.MINUTES)

    return when (feedResult) {
      is FeedLoaderResult.FeedLoaderSuccess -> {
        when (feedResult.feed) {
          is Feed.FeedWithoutGroups -> {
            val entries =
              checkFeedHasEntries(this.acquisition.uri, feedResult.feed.entriesInOrder)

            val feedEntry = entries[0]
            when (feedEntry) {
              is FeedEntry.FeedEntryCorrupt -> {
                LOG.error("[{}]: unexpectedly received corrupt feed entry", bookId.brief())
                this.downloadFailed(Option.some(BookBorrowExceptionBadBorrowFeed(feedEntry.error)))
              }
              is FeedEntry.FeedEntryOPDS ->
                this.runAcquisitionBorrowGotOPDSEntry(feedEntry)
            }
          }

          is Feed.FeedWithGroups -> {
            val groups =
              checkFeedHasGroups(this.acquisition.uri, feedResult.feed.feedGroupsInOrder)
            val entries =
              checkFeedHasEntries(this.acquisition.uri, groups[0].groupEntries)

            val feedEntry = entries[0]
            when (feedEntry) {
              is FeedEntry.FeedEntryCorrupt -> {
                LOG.error("[{}]: unexpectedly received corrupt feed entry", bookId.brief())
                this.downloadFailed(Option.some(BookBorrowExceptionBadBorrowFeed(feedEntry.error)))
              }
              is FeedEntry.FeedEntryOPDS ->
                this.runAcquisitionBorrowGotOPDSEntry(feedEntry)
            }
          }
        }
      }

      is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral ->
        throw feedResult.exception
      is FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication ->
        throw feedResult.exception
    }
  }

  private fun checkFeedHasGroups(
    uri: URI,
    groups: List<FeedGroup>): List<FeedGroup> {
    if (groups.isEmpty()) {
      LOG.error("[{}]: unexpectedly received feed with zero groups", bookId.brief())
      throw BookBorrowExceptionBadBorrowFeed(IOException("No groups in feed: $uri"))
    }
    return groups
  }

  private fun checkFeedHasEntries(
    uri: URI,
    entries: List<FeedEntry>): List<FeedEntry> {
    if (entries.isEmpty()) {
      LOG.error("[{}]: unexpectedly received feed with no entries", bookId.brief())
      throw BookBorrowExceptionBadBorrowFeed(IOException("No entries in feed: $uri"))
    }
    return entries
  }

  @Throws(IOException::class, BookBorrowExceptionNoUsableAcquisition::class)
  private fun runAcquisitionBorrowGotOPDSEntry(feedEntry: FeedEntry.FeedEntryOPDS): Unit {
    val sid = this.bookId.brief()

    LOG.debug("[{}]: received OPDS feed entry", sid)
    LOG.debug("[{}]: book availability is {}", sid, feedEntry.feedEntry.availability)

    /*
     * Update the database.
     */

    LOG.debug("[{}]: saving state to database", sid)
    this.databaseEntry.writeOPDSEntry(feedEntry.feedEntry)

    /*
     * Then, work out what to do based on the latest availability data.
     * If the book is loaned, attempt to download it. If it is held, notify
     * the user.
     */

    LOG.debug("[{}]: continuing borrow based on availability", sid)

    val wantFulfill = feedEntry.feedEntry.availability.matchAvailability(
      object : OPDSAvailabilityMatcherType<Boolean, UnreachableCodeException> {

        /**
         * If the book is held but is ready for download, just notify
         * the user of this fact by setting the status.
         */

        override fun onHeldReady(a: OPDSAvailabilityHeldReady): Boolean? {
          LOG.debug("[{}]: book is held but is ready, nothing more to do", sid)

          val status = BookStatusHeldReady(bookId, a.endDate, a.revoke.isSome)
          bookRegistry.update(BookWithStatus.create(databaseEntry.book, status))
          return java.lang.Boolean.FALSE
        }

        /**
         * If the book is held, just notify the user of this fact by
         * setting the status.
         */

        override fun onHeld(a: OPDSAvailabilityHeld): Boolean? {
          LOG.debug("[{}]: book is held, nothing more to do", sid)

          val status = BookStatusHeld(bookId, a.position, a.startDate, a.endDate, a.revoke.isSome)
          bookRegistry.update(BookWithStatus.create(databaseEntry.book, status))
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

          val status = BookStatusHoldable(bookId)
          bookRegistry.update(BookWithStatus.create(databaseEntry.book, status))
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

          val status = BookStatusRequestingDownload(bookId, a.endDate)
          bookRegistry.update(BookWithStatus.create(databaseEntry.book, status))
          return java.lang.Boolean.TRUE
        }

        /**
         * If the book is "open-access", then attempt to fulfill the
         * book.
         */

        override fun onOpenAccess(a: OPDSAvailabilityOpenAccess): Boolean? {
          LOG.debug("[{}]: book is open access, fulfilling", sid)

          val status = BookStatusRequestingDownload(bookId, Option.none())
          bookRegistry.update(BookWithStatus.create(databaseEntry.book, status))
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

    return if (wantFulfill) {
      this.runAcquisitionFulfill(feedEntry.feedEntry)
    } else {
      Unit.unit()
    }
  }

  /**
   * Fulfill a book by hitting the generic or open access links.
   */

  @Throws(BookBorrowExceptionNoUsableAcquisition::class)
  private fun runAcquisitionFulfill(ee: OPDSAcquisitionFeedEntry): Unit {
    LOG.debug("[{}]: fulfilling book", this.bookId.brief())

    for (ea in ee.acquisitions) {
      when (ea.relation) {
        ACQUISITION_GENERIC,
        ACQUISITION_OPEN_ACCESS ->
          return this.runAcquisitionFulfillDoDownload(ea, createHttpAuthIfRequired())
        ACQUISITION_BORROW,
        ACQUISITION_BUY,
        ACQUISITION_SAMPLE,
        ACQUISITION_SUBSCRIBE -> {

        }
      }
    }

    throw BookBorrowExceptionNoUsableAcquisition()
  }

  private fun runAcquisitionFulfillDoDownload(
    acquisition: OPDSAcquisition,
    httpAuth: OptionType<HTTPAuthType>): Unit {

    this.fulfillURI = acquisition.uri

    /*
     * Point the downloader at the acquisition link. The result will be an
     * EPUB, ACSM file, or Simplified bearer token. ACSM files have to be
     * "fulfilled" after downloading by passing them to the Adobe DRM
     * connector. Bearer token documents need an additional request to
     * actually get the book in question.
     */


    val downloadFuture = SettableFuture.create<File>()
    val download =
      this.downloader.download(acquisition.uri, httpAuth, object : DownloadListenerType {
        override fun onDownloadStarted(
          download: DownloadType,
          expectedTotal: Long) {
          downloadDataReceived(0L, expectedTotal)
        }

        override fun onDownloadDataReceived(
          download: DownloadType,
          runningTotal: Long,
          expectedTotal: Long) {
          downloadDataReceived(runningTotal, expectedTotal)
        }

        override fun onDownloadCancelled(d: DownloadType) {
          downloadFuture.setException(CancellationException())
        }

        override fun onDownloadFailed(
          download: DownloadType,
          status: Int,
          runningTotal: Long,
          exception: OptionType<Throwable>) {
          downloadFuture.setException(captureDownloadException(download, exception))
        }

        override fun onDownloadCompleted(
          download: DownloadType,
          file: File) {
          downloadFuture.set(file)
        }
      })

    this.downloads[this.bookId] = download

    val file = try {
      downloadFuture.get(3L, TimeUnit.MINUTES)
    } catch (ex: TimeoutException) {
      download.cancel()
      downloadFuture.cancel(true)
      throw IOException("Timed out", ex)
    } finally {
      this.downloads.remove(this.bookId)
    }

    LOG.debug("[{}]: download {} completed for {}", this.bookId.brief(), download, file)
    val contentType = download.contentType
    LOG.debug("[{}]: content type is {}", this.bookId.brief(), contentType)

    return if (ACSM_CONTENT_TYPE == contentType) {
      runFulfillACSM(file)
    } else if (SIMPLIFIED_BEARER_TOKEN_CONTENT_TYPE == contentType) {
      runFulfillSimplifiedBearerToken(acquisition, file)
    } else {
      saveFinalContent(
        file = file,
        expectedContentTypes = acquisition.availableFinalContentTypes(),
        receivedContentType = contentType)
    }
  }

  private fun saveFinalContent(
    file: File,
    expectedContentTypes: Set<String>,
    receivedContentType: String): Unit {

    LOG.debug(
      "[{}]: saving content {} (expected one of {}, received {})",
      this.bookId.brief(),
      file,
      expectedContentTypes,
      receivedContentType)

    LOG.debug("[{}]: saving adobe rights {}", this.bookId.brief(), this.adobeLoan)
    LOG.debug("[{}]: saving fulfill URI  {}", this.bookId.brief(), this.fulfillURI)

    val handleContentType =
      checkExpectedContentType(expectedContentTypes, receivedContentType)
    val formatHandle =
      this.databaseEntry.findFormatHandleForContentType(handleContentType)

    fun updateStatus(): Unit {
      val book = this.databaseEntry.book
      this.bookRegistry.update(BookWithStatus.create(book, BookStatus.fromBook(book)))
      return Unit.unit()
    }

    return if (formatHandle != null) {
      when (formatHandle) {
        is BookDatabaseEntryFormatHandleEPUB -> {
          formatHandle.copyInBook(file)
          formatHandle.setAdobeRightsInformation(this.adobeLoan)
          updateStatus()
        }
        is BookDatabaseEntryFormatHandlePDF -> {
          formatHandle.copyInBook(file)
          updateStatus()
        }
        is BookDatabaseEntryFormatHandleAudioBook -> {
          formatHandle.copyInManifestAndURI(file, this.fulfillURI)
          updateStatus()
        }
      }
    } else {
      LOG.error("[{}]: database entry does not have a format handle for {}",
        this.bookId.brief(), handleContentType)
      throw BookUnsupportedTypeException(handleContentType)
    }
  }

  /*
   * If we expect a specific content type, but the server actually delivers application/octet-stream,
   * then assume that the server delivered the expected type. Otherwise, check that the received
   * type matches the expected type.
   */

  private fun checkExpectedContentType(
    expectedContentTypes: Set<String>,
    receivedContentType: String): String {

    Preconditions.checkArgument(
      !expectedContentTypes.isEmpty(),
      "At least one expected content type")

    return when (receivedContentType) {
      "application/octet-stream" -> {
        LOG.debug("[{}]: expected one of {} but received {} (acceptable)",
          this.bookId.brief(),
          expectedContentTypes,
          receivedContentType)
        expectedContentTypes.first()
      }

      else -> {
        if (expectedContentTypes.contains(receivedContentType)) {
          return receivedContentType
        }

        LOG.debug("[{}]: expected {} but received {} (unacceptable)",
          this.bookId.brief(), expectedContentTypes, receivedContentType)

        throw BookUnexpectedTypeException(
          message =
          StringBuilder("Unexpected content type\n")
            .append("  Expected: One of ")
            .append(expectedContentTypes)
            .append('\n')
            .append("  Received: ")
            .append(receivedContentType)
            .append('\n')
            .toString(),
          expected = expectedContentTypes,
          received = receivedContentType)
      }
    }
  }

  /**
   * Fulfill the given ACSM file, if Adobe DRM is supported. Otherwise, fail.
   *
   * @param file The ACSM file
   * @throws IOException On I/O errors
   */

  @Throws(IOException::class, AdobeAdeptACSMException::class, BookUnsupportedTypeException::class)
  private fun runFulfillACSM(file: File): Unit {
    LOG.debug("[{}]: fulfilling ACSM file", this.bookId.brief())

    /*
     * The ACSM file will typically have downloaded almost instantly, leaving
     * the download progress bar at 100%. The Adobe library will then take up
     * to roughly ten seconds to start fulfilling the ACSM. This call
     * effectively sets the download progress bar to 0% so that it doesn't look
     * as if the user is waiting for no good reason.
     */

    this.downloadDataReceived(0L, 100L)

    return if (this.adobeDRM != null) {
      LOG.debug("[{}]: DRM support is available, using DRM connector", this.bookId.brief())
      this.runFulfillACSMWithConnector(this.adobeDRM, file)
    } else {
      LOG.debug("[{}]: DRM support is unavailable, cannot continue!", this.bookId.brief())
      this.downloadFailed(Option.some(DRMUnsupportedException("DRM support is not available")))
    }
  }

  @Throws(IOException::class, AdobeAdeptACSMException::class, BookUnsupportedTypeException::class)
  private fun runFulfillACSMWithConnector(adobe: AdobeAdeptExecutorType, file: File): Unit {

    val acsm = FileUtilities.fileReadBytes(file)
    val parsed = AdobeAdeptFulfillmentToken.parseFromBytes(acsm)
    val contentType = parsed.format
    if ("application/epub+zip" != contentType) {
      throw BookUnsupportedTypeException(contentType)
    }

    throw UnimplementedCodeException()
  }

  @Throws(IOException::class)
  private fun runFulfillSimplifiedBearerToken(
    acquisition: OPDSAcquisition,
    file: File): Unit {
    LOG.debug("[{}]: fulfilling Simplified bearer token file", this.bookId.brief())

    /*
     * The bearer token file will typically have downloaded almost instantly, leaving
     * the download progress bar at 100%. This call effectively sets the download progress bar
     * to 0% so that it doesn't look as if the user is waiting for no good reason.
     */

    this.downloadDataReceived(0L, 100L)

    return try {
      val token =
        SimplifiedBearerTokenJSON.deserializeFromFile(ObjectMapper(), LocalDateTime.now(), file)

      val nextAcquisition =
        OPDSAcquisition(
          ACQUISITION_GENERIC,
          token.location,
          acquisition.type,
          acquisition.indirectAcquisitions)

      val auth = HTTPAuthOAuth.create(HTTPOAuthToken.create(token.accessToken))
      this.runAcquisitionFulfillDoDownload(nextAcquisition, Option.some(auth))
    } catch (ex: Exception) {
      LOG.error("[{}]: failed to parse bearer token: {}: ",
        this.bookId.brief(), acquisition.uri, ex)
      this.downloadFailed(Option.of(ex))
    }
  }

  private fun captureDownloadException(
    download: DownloadType,
    exception: OptionType<Throwable>): Throwable {

    /*
     * If the content type indicates that the file was an ACSM file,
     * explicitly indicate that it was fetching an ACSM that failed.
     * This allows the UI to assign blame!
     */

    val ex: Throwable
    val acsmType = ACSM_CONTENT_TYPE
    if (acsmType == download.contentType) {
      ex = BookBorrowExceptionFetchingACSMFailed.newException(exception)
    } else {
      ex = BookBorrowExceptionFetchingBookFailed.newException(exception)
    }
    return ex
  }

  /**
   * If the account requires credentials, create HTTP auth details. If no credentials
   * are provided, throw an exception.
   */

  private fun createHttpAuthIfRequired(): OptionType<HTTPAuthType> {
    return if (this.account.requiresCredentials()) {
      Option.some(AccountAuthenticatedHTTP.createAuthenticatedHTTP(getRequiredAccountCredentials()))
    } else {
      Option.none<HTTPAuthType>()
    }
  }

  /**
   * Assume that account credentials are required and fetch them. If they're not present, fail
   * loudly.
   */

  private fun getRequiredAccountCredentials(): AccountAuthenticationCredentials {
    val loginState = this.account.loginState()
    val credentials = loginState.credentials
    if (credentials != null) {
      return credentials
    } else {
      LOG.error("[{}] borrowing requires credentials, but none are available", this.bookId.brief())
      throw BookBorrowExceptionNoCredentials()
    }
  }

  /**
   * Copy data out of the bundled resources.
   */

  @Throws(IOException::class, BookDatabaseException::class)
  private fun runAcquisitionBundled(): Unit {

    val file = this.databaseEntry.temporaryFile()
    val buffer = ByteArray(2048)

    try {
      return FileOutputStream(file).use { output ->
        this.bundledContent.resolve(this.acquisition.uri).use { stream ->
          val size = stream.available().toLong()
          var consumed = 0L
          this.downloadDataReceived(consumed, size)

          while (true) {
            val r = stream.read(buffer)
            if (r == -1) {
              break
            }
            consumed += r.toLong()
            output.write(buffer, 0, r)
            this.downloadDataReceived(consumed, size)
          }
          output.flush()
        }

        this.saveEPUBAndRights(file, Option.none<AdobeAdeptLoan>())
        val book = this.databaseEntry.book
        this.bookRegistry.update(BookWithStatus.create(book, BookStatus.fromBook(book)))
        Unit.unit()
      }
    } catch (e: IOException) {
      FileUtilities.fileDelete(file)
      throw e
    } catch (e: BookDatabaseException) {
      FileUtilities.fileDelete(file)
      throw e
    }
  }

  @Throws(BookDatabaseException::class)
  private fun saveEPUBAndRights(
    file: File,
    loanOpt: OptionType<AdobeAdeptLoan>) {

    val format =
      this.databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)
    if (format != null) {
      format.copyInBook(file)
      format.setAdobeRightsInformation(
        if (loanOpt is Some<AdobeAdeptLoan>) {
          loanOpt.get()
        } else {
          null
        })
    } else {
      throw UnreachableCodeException()
    }
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
      val status = BookStatusDownloadInProgress(
        this.bookId, runningTotal, expectedTotal, Option.none())
      this.bookRegistry.update(BookWithStatus.create(this.databaseEntry.book, status))
      this.downloadRunningTotal = runningTotal
    }
  }

  private fun downloadFailed(exception: OptionType<Throwable>): Unit {
    LogUtilities.errorWithOptionalException(LOG, "download failed", exception)

    this.bookRegistry.update(
      BookWithStatus.create(this.bookInitial,
        BookStatusDownloadFailed(this.bookId, exception, Option.none())))

    return Unit.unit()
  }

  companion object {

    const val ACSM_CONTENT_TYPE =
      "application/vnd.adobe.adept+xml"
    const val SIMPLIFIED_BEARER_TOKEN_CONTENT_TYPE =
      "application/vnd.librarysimplified.bearer-token+json"
    private val LOG =
      LoggerFactory.getLogger(BookBorrowTask::class.java)
  }
}
