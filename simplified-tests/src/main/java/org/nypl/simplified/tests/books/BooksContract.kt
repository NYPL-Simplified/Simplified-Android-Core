package org.nypl.simplified.tests.books

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jnull.NullCheck
import com.io7m.junreachable.UnreachableCodeException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito
import org.nypl.audiobook.android.api.PlayerAudioBookProviderType
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.audiobook.android.mocking.MockingAudioBook
import org.nypl.audiobook.android.mocking.MockingDownloadProvider
import org.nypl.audiobook.android.mocking.MockingPlayer
import org.nypl.drm.core.AdobeAdeptConnectorType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeAdeptLoanReturnListenerType
import org.nypl.drm.core.AdobeLoanID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.books.core.AccountAdobeToken
import org.nypl.simplified.books.core.AccountAuthProvider
import org.nypl.simplified.books.core.AccountAuthToken
import org.nypl.simplified.books.core.AccountBarcode
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountDataLoadListenerType
import org.nypl.simplified.books.core.AccountLoginListenerType
import org.nypl.simplified.books.core.AccountLogoutListenerType
import org.nypl.simplified.books.core.AccountPIN
import org.nypl.simplified.books.core.AccountPatron
import org.nypl.simplified.books.core.AccountsDatabase
import org.nypl.simplified.books.core.BookDatabase
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot
import org.nypl.simplified.books.core.BookDatabaseReadableType
import org.nypl.simplified.books.core.BookFormats
import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.core.BookRevokeExceptionDRMWorkflowError
import org.nypl.simplified.books.core.BookStatusDownloaded
import org.nypl.simplified.books.core.BookStatusLoaned
import org.nypl.simplified.books.core.BookStatusRevokeFailed
import org.nypl.simplified.books.core.BookStatusType
import org.nypl.simplified.books.core.BooksController
import org.nypl.simplified.books.core.BooksControllerConfigurationType
import org.nypl.simplified.books.core.FeedHTTPTransport
import org.nypl.simplified.books.core.FeedLoader
import org.nypl.simplified.books.core.FeedLoaderType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSIndirectAcquisition
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.opds.core.OPDSJSONParserType
import org.nypl.simplified.opds.core.OPDSJSONSerializer
import org.nypl.simplified.opds.core.OPDSJSONSerializerType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.util.Calendar
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class BooksContract {

  protected abstract val context: Context

  private lateinit var jsonSerializer: OPDSJSONSerializerType
  private lateinit var jsonParser: OPDSJSONParserType
  private lateinit var executor: ListeningExecutorService
  private lateinit var downloadExecutor: ListeningExecutorService

  @Rule
  @JvmField
  val expectedException = ExpectedException.none()

  @Before
  fun setUp() {
    MockedAudioEngineProvider.onNextRequest = null

    this.jsonSerializer = OPDSJSONSerializer.newSerializer()
    this.jsonParser = OPDSJSONParser.newParser()
    this.executor =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4))
    this.downloadExecutor =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4))
  }

  @After
  fun tearDown() {
    MockedAudioEngineProvider.onNextRequest = null

    this.executor.shutdown()
    this.downloadExecutor.shutdown()
  }

  /**
   * Trying to load books from a directory that isn't accessible (because it's not a directory,
   * for example), fails.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksLoadFileNotDirectory() {

    val tmp = File.createTempFile("books", "")

    val booksConfig = BooksControllerConfiguration()
    val http = CrashingHTTP()

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val loadOk = AtomicBoolean(false)
    val future = booksController.accountLoadBooks(
      object : AccountDataLoadListenerType {
        override fun onAccountDataBookLoadFailed(
          id: BookID,
          error: OptionType<Throwable>,
          message: String) {
          LOG.debug("testBooksLoadFileNotDirectory: load failed")
          loadOk.set(false)
        }

        override fun onAccountDataBookLoadFinished() {
          LOG.debug("testBooksLoadFileNotDirectory: load finished")
        }

        override fun onAccountDataBookLoadSucceeded(
          book: BookID,
          snap: BookDatabaseEntrySnapshot) {
          LOG.debug("testBooksLoadFileNotDirectory: load succeeded")
          loadOk.set(false)
        }

        override fun onAccountDataLoadFailedImmediately(
          error: Throwable) {
          LOG.debug("testBooksLoadFileNotDirectory: load failed")
          loadOk.set(false)
        }

        override fun onAccountUnavailable() {
          LOG.debug("testBooksLoadFileNotDirectory: account unavailable")
          loadOk.set(true)
        }
      }, true)

    future.get()
    Assert.assertEquals("Load must have succeeded", true, loadOk.get())
  }

  /**
   * Loading books whilst not logged in works.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksLoadNotLoggedIn() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()
    val http = CrashingHTTP()

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val loadOk = AtomicBoolean(false)
    val future = booksController.accountLoadBooks(
      object : AccountDataLoadListenerType {
        override fun onAccountDataBookLoadFailed(
          id: BookID,
          error: OptionType<Throwable>,
          message: String) {
          LOG.debug("testBooksLoadNotLoggedIn: load failed")
          loadOk.set(false)
        }

        override fun onAccountDataBookLoadFinished() {
          LOG.debug("testBooksLoadNotLoggedIn: load finished")
        }

        override fun onAccountDataBookLoadSucceeded(
          book: BookID,
          snap: BookDatabaseEntrySnapshot) {
          LOG.debug("testBooksLoadNotLoggedIn: load succeeded")
          loadOk.set(false)
        }

        override fun onAccountDataLoadFailedImmediately(
          error: Throwable) {
          LOG.debug("testBooksLoadNotLoggedIn: load failed")
          loadOk.set(false)
        }

        override fun onAccountUnavailable() {
          LOG.debug("testBooksLoadNotLoggedIn: account unavailable")
          loadOk.set(true)
        }
      }, true)

    future.get()
    Assert.assertEquals("Load must have succeeded", true, loadOk.get())
  }

  /**
   * Logging in works.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksLoginAcceptedFirst() {

    val tmp =
      DirectoryUtilities.directoryCreateTemporary()
    val booksConfig =
      BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = AuthenticatedHTTP(LOG, LOANS_URI, barcode, pin)

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val future = booksController.accountLogin(creds, LoggingAccountLoginListener(LOG))
    future.get()
  }

  /**
   * Logging in when the books directory is not a directory fails.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksLoginFileNotDirectory() {

    val tmp = File.createTempFile("books", "")
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))
    val http = AuthenticatedHTTP(LOG, LOANS_URI, barcode, pin)

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val failed = AtomicBoolean(false)
    val loginListener = object : AccountLoginListenerType {
      override fun onAccountSyncAuthenticationFailure(message: String) {
        // Nothing
      }

      override fun onAccountSyncBook(book: BookID) {
        // Nothing
      }

      override fun onAccountSyncFailure(
        error: OptionType<Throwable>,
        message: String) {
        // Nothing
      }

      override fun onAccountSyncSuccess() {
        // Nothing
      }

      override fun onAccountSyncBookDeleted(book: BookID) {
        // Nothing
      }

      override fun onAccountLoginFailureCredentialsIncorrect() {
        // Nothing
      }

      override fun onAccountLoginFailureServerError(code: Int) {
        // Nothing
      }

      override fun onAccountLoginFailureLocalError(
        error: OptionType<Throwable>,
        message: String) {
        LOG.debug("testBooksLoginFileNotDirectory: login failed: $message")
        (error as Some<Throwable>).get().printStackTrace()
        failed.set(true)
      }

      override fun onAccountLoginSuccess(credentials: AccountCredentials) {
        throw UnreachableCodeException()
      }

      override fun onAccountLoginFailureDeviceActivationError(
        message: String) {
        // Nothing
      }
    }

    val future = booksController.accountLogin(creds, loginListener)
    try {
      this.expectedException.expect(IOException::class.java)
      future.get(10L, TimeUnit.SECONDS)
    } catch (ex: ExecutionException) {
      Assert.assertEquals("Login must fail", true, failed.get())
      throw ex.cause!!
    }
  }

  /**
   * Logging out works.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksSyncLoadLogoutOK() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))
    val http = AuthenticatedHTTP(LOG, LOANS_URI, barcode, pin)

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val loginLatch = CountDownLatch(1)
    val loginSyncLatch = CountDownLatch(1)
    val syncedOk = AtomicBoolean(false)
    val syncedBookCount = AtomicInteger(0)

    val loginListener =
      LatchedAccountLoginListener(LOG, loginSyncLatch, syncedBookCount, syncedOk, loginLatch)

    booksController.accountLogin(creds, loginListener)
    loginLatch.await(10L, TimeUnit.SECONDS)
    loginSyncLatch.await(10L, TimeUnit.SECONDS)

    Assert.assertTrue("Sync must succeed", syncedOk.get())
    Assert.assertEquals("Must have synced correct number of books", 2, syncedBookCount.get().toLong())

    val loadBookCount = AtomicInteger()
    val loadOk = AtomicBoolean()

    val loadListener = object : AccountDataLoadListenerType {
      override fun onAccountDataBookLoadFailed(
        id: BookID,
        error: OptionType<Throwable>,
        message: String) {
        loadOk.set(false)
      }

      override fun onAccountDataBookLoadFinished() {
        // Nothing
      }

      override fun onAccountDataBookLoadSucceeded(
        book: BookID,
        snap: BookDatabaseEntrySnapshot) {
        loadBookCount.incrementAndGet()
        loadOk.set(true)
      }

      override fun onAccountDataLoadFailedImmediately(
        error: Throwable) {
        loadOk.set(false)
      }

      override fun onAccountUnavailable() {
        loadOk.set(false)
      }
    }

    LOG.debug("loading books")
    val loadFuture = booksController.accountLoadBooks(loadListener, true)
    LOG.debug("waiting for book load completion")
    loadFuture.get()
    LOG.debug("book load completed")

    Assert.assertEquals("Loading must succeed", true, loadOk.get())
    Assert.assertEquals("Must have loaded the correct number of books", 2, loadBookCount.get().toLong())

    val logoutListener = object : AccountLogoutListenerType {
      override fun onAccountLogoutFailure(error: OptionType<Throwable>, message: String) {

      }

      override fun onAccountLogoutSuccess() {

      }

      override fun onAccountLogoutFailureServerError(code: Int) {

      }
    }

    LOG.debug("logging out")
    val logoutFuture =
      booksController.accountLogout(creds, logoutListener, LoggingSyncListener(LOG), LoggingDeviceListener(LOG))
    LOG.debug("awaiting logout completion")
    logoutFuture.get()
    LOG.debug("logged out")
  }

  /**
   * Syncing books works.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksSyncOK() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = AuthenticatedHTTP(LOG, LOANS_URI, barcode, pin)

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val loginListener = LoggingAccountLoginListener(LOG)
    val loginFuture = booksController.accountLogin(creds, loginListener)
    loginFuture.get()

    Assert.assertEquals(
      "Must have synced the correct number of books",
      2,
      loginListener.syncedBookCount.get().toLong())

    /*
     * Assert status of each book.
     */

    val statusCache = booksController.bookGetStatusCache()

    this.run {
      val opt = statusCache.booksStatusGet(
        BookID.exactString("2925d691731c018650422a6d8463cd1fb880e2a8d0d8741a9652e6fb5a56783f"))
      Assert.assertTrue("Book must have status", opt.isSome)
      val o = (opt as Some<BookStatusType>).get()
      Assert.assertTrue(
        "Book 2925d691731c018650422a6d8463cd1fb880e2a8d0d8741a9652e6fb5a56783f is loaned",
        o is BookStatusLoaned)
    }

    this.run {
      val opt = statusCache.booksStatusGet(
        BookID.exactString("31d2a6c5a6aa3065e25a7373167d734d72e72cdd843d1474d807dce2bf6de834"))
      Assert.assertTrue("Book must have status", opt.isSome)
      val o = (opt as Some<BookStatusType>).get()
      Assert.assertTrue(
        "Book 31d2a6c5a6aa3065e25a7373167d734d72e72cdd843d1474d807dce2bf6de834 is loaned",
        o is BookStatusLoaned)
    }
  }

  /**
   * Borrowing and then revoking a book works.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksBorrowRevokeAudioBookOK() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = MappedHTTP(LOG)
    http.addResource("http://example.com/loans/", httpResource("loans.xml"), "text/xml")
    http.addResource("http://example.com/borrow/0", httpResource("borrow-audiobook-0.xml"), "text/xml")
    http.addResource("http://example.com/fulfill/0", httpResource("basic-manifest.json"), "application/audiobook+json")
    http.addResource("http://example.com/revoke/0", httpResource("revoke-audiobook-0.xml"), "text/xml")

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database, http),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val booksStatus = booksController.bookGetStatusCache()

    val loginListener = LoggingAccountLoginListener(LOG)
    val loginFuture = booksController.accountLogin(creds, loginListener)
    loginFuture.get()

    LOG.debug("borrowing book")

    val bookID =
      BookID.exactString("2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881")

    /*
     * Borrow the book.
     *
     * XXX: The book borrowing code needs to be completely rewritten. It is currently essentially
     * impossible to convert the borrowing code into a task that can be observed synchronously.
     */

    booksController.bookBorrow(bookID, makeOPDSEntryAudioBook(), true)
    TimeUnit.SECONDS.sleep(2L)

    val downloadedStatus =
      (booksStatus.booksStatusGet(bookID) as Some<BookStatusDownloaded>).get()
    Assert.assertEquals(bookID, downloadedStatus.id)

    /*
     * Tell the mocked audio engine provider to return a fake audio book that pretends to have
     * deleted all local data successfully.
     */

    MockedAudioEngineProvider.onNextRequest = { request ->
      object : PlayerAudioBookProviderType {
        override fun create(context: Context): PlayerResult<PlayerAudioBookType, Exception> {
          return PlayerResult.Success(
            MockingAudioBook(
              id = PlayerBookID.transform("x"),
              downloadStatusExecutor = downloadExecutor,
              downloadProvider = MockingDownloadProvider(
                executorService = downloadExecutor,
                shouldFail = { _ -> false }),
              players = { book -> MockingPlayer(book) }))
        }
      }
    }

    /*
     * Revoke the book.
     */

    val revokeFuture = booksController.bookRevoke(bookID, true)
    revokeFuture.get()
    LOG.debug("revocation task finished")

    Assert.assertTrue(
      "Book has no status after revocation",
      booksStatus.booksStatusGet(bookID).isNone)

    Assert.assertFalse(
      "Database entry should be gone",
      booksController.bookGetDatabase().databaseEntryExists(bookID))
  }

  /**
   * Borrowing a book and then trying to revoke it, but encountering a server error, indicates
   * failure.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksBorrowRevokeAudioBookServerFailed() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = MappedHTTP(LOG)
    http.addResource("http://example.com/loans/", httpResource("loans.xml"), "text/xml")
    http.addResource("http://example.com/borrow/0", httpResource("borrow-audiobook-1.xml"), "text/xml")
    http.addResource("http://example.com/fulfill/0", httpResource("basic-manifest.json"), "application/audiobook+json")

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database, http),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val booksStatus = booksController.bookGetStatusCache()

    val loginListener = LoggingAccountLoginListener(LOG)
    val loginFuture = booksController.accountLogin(creds, loginListener)
    loginFuture.get()

    LOG.debug("borrowing book")

    val bookID =
      BookID.exactString("2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881")

    /*
     * Borrow the book.
     *
     * XXX: The book borrowing code needs to be completely rewritten. It is currently essentially
     * impossible to convert the borrowing code into a task that can be observed synchronously.
     */

    booksController.bookBorrow(bookID, makeOPDSEntryAudioBook(), true)
    TimeUnit.SECONDS.sleep(2L)

    val downloadedStatus =
      (booksStatus.booksStatusGet(bookID) as Some<BookStatusDownloaded>).get()
    Assert.assertEquals(bookID, downloadedStatus.id)

    /*
     * Revoke the book, but expect to receive a server error.
     */

    val revokeFuture = booksController.bookRevoke(bookID, true)

    try {
      this.expectedException.expect(ExecutionException::class.java)
      revokeFuture.get()
    } catch (ex: ExecutionException) {
      val revokedStatus =
        (booksStatus.booksStatusGet(bookID) as Some<BookStatusRevokeFailed>).get()
      Assert.assertEquals(bookID, revokedStatus.id)
      Assert.assertTrue(
        "Database entry should not be gone",
        booksController.bookGetDatabase().databaseEntryExists(bookID))
      throw ex
    }
  }

  /**
   * Borrowing a book and then trying to revoke it, but encountering a wrong feed type, indicates
   * failure.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksBorrowRevokeAudioBookServerWrongFeedType() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = MappedHTTP(LOG)
    http.addResource("http://example.com/loans/", httpResource("loans.xml"), "text/xml")
    http.addResource("http://example.com/borrow/0", httpResource("revoke-error-bad-feed-type.xml"), "text/xml")
    http.addResource("http://example.com/fulfill/0", httpResource("basic-manifest.json"), "application/audiobook+json")
    http.addResource("http://example.com/revoke/fails", httpResource("groups.xml"), "text/xml")

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database, http),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val booksStatus = booksController.bookGetStatusCache()

    val loginListener = LoggingAccountLoginListener(LOG)
    val loginFuture = booksController.accountLogin(creds, loginListener)
    loginFuture.get()

    LOG.debug("borrowing book")

    val bookID =
      BookID.exactString("2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881")

    /*
     * Borrow the book.
     *
     * XXX: The book borrowing code needs to be completely rewritten. It is currently essentially
     * impossible to convert the borrowing code into a task that can be observed synchronously.
     */

    booksController.bookBorrow(bookID, makeOPDSEntryAudioBook(), true)
    TimeUnit.SECONDS.sleep(2L)

    val downloadedStatus =
      (booksStatus.booksStatusGet(bookID) as Some<BookStatusDownloaded>).get()
    Assert.assertEquals(bookID, downloadedStatus.id)

    /*
     * Revoke the book, but expect to receive a server error.
     */

    val revokeFuture = booksController.bookRevoke(bookID, true)

    try {
      this.expectedException.expect(ExecutionException::class.java)
      revokeFuture.get()
    } catch (ex: ExecutionException) {
      val revokedStatus =
        (booksStatus.booksStatusGet(bookID) as Some<BookStatusRevokeFailed>).get()
      Assert.assertEquals(bookID, revokedStatus.id)
      Assert.assertTrue(
        "Database entry should not be gone",
        booksController.bookGetDatabase().databaseEntryExists(bookID))
      throw ex
    }
  }

  /**
   * Borrowing a book and then trying to revoke it, but encountering an empty feed, indicates
   * failure.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksBorrowRevokeAudioBookServerEmptyFeed() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = MappedHTTP(LOG)
    http.addResource("http://example.com/loans/", httpResource("loans.xml"), "text/xml")
    http.addResource("http://example.com/borrow/0", httpResource("revoke-error-empty-feed-borrow.xml"), "text/xml")
    http.addResource("http://example.com/fulfill/0", httpResource("basic-manifest.json"), "application/audiobook+json")
    http.addResource("http://example.com/revoke/0", httpResource("revoke-error-empty-feed-revoke.xml"), "text/xml")

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database, http),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val booksStatus = booksController.bookGetStatusCache()

    val loginListener = LoggingAccountLoginListener(LOG)
    val loginFuture = booksController.accountLogin(creds, loginListener)
    loginFuture.get()

    LOG.debug("borrowing book")

    val bookID =
      BookID.exactString("2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881")

    /*
     * Borrow the book.
     *
     * XXX: The book borrowing code needs to be completely rewritten. It is currently essentially
     * impossible to convert the borrowing code into a task that can be observed synchronously.
     */

    booksController.bookBorrow(bookID, makeOPDSEntryAudioBook(), true)
    TimeUnit.SECONDS.sleep(2L)

    val downloadedStatus =
      (booksStatus.booksStatusGet(bookID) as Some<BookStatusDownloaded>).get()
    Assert.assertEquals(bookID, downloadedStatus.id)

    /*
     * Revoke the book, but expect to receive a server error.
     */

    val revokeFuture = booksController.bookRevoke(bookID, true)

    try {
      this.expectedException.expect(ExecutionException::class.java)
      revokeFuture.get()
    } catch (ex: ExecutionException) {
      val revokedStatus =
        (booksStatus.booksStatusGet(bookID) as Some<BookStatusRevokeFailed>).get()
      Assert.assertEquals(bookID, revokedStatus.id)
      Assert.assertTrue(
        "Database entry should not be gone",
        booksController.bookGetDatabase().databaseEntryExists(bookID))
      throw ex
    }
  }

  /**
   * Borrowing a book and then trying to revoke it, but removing the book database before
   * the revocation happens, indicates failure.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksBorrowRevokeAudioBookDatabaseFailed() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = MappedHTTP(LOG)
    http.addResource("http://example.com/loans/", httpResource("loans.xml"), "text/xml")
    http.addResource("http://example.com/borrow/0", httpResource("borrow-audiobook-0.xml"), "text/xml")
    http.addResource("http://example.com/fulfill/0", httpResource("basic-manifest.json"), "application/audiobook+json")
    http.addResource("http://example.com/revoke/0", httpResource("revoke-audiobook-0.xml"), "text/xml")

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database, http),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val booksStatus = booksController.bookGetStatusCache()

    val loginListener = LoggingAccountLoginListener(LOG)
    val loginFuture = booksController.accountLogin(creds, loginListener)
    loginFuture.get()

    LOG.debug("borrowing book")

    val bookID =
      BookID.exactString("2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881")

    /*
     * Borrow the book.
     *
     * XXX: The book borrowing code needs to be completely rewritten. It is currently essentially
     * impossible to convert the borrowing code into a task that can be observed synchronously.
     */

    booksController.bookBorrow(bookID, makeOPDSEntryAudioBook(), true)
    TimeUnit.SECONDS.sleep(2L)

    val downloadedStatus =
      (booksStatus.booksStatusGet(bookID) as Some<BookStatusDownloaded>).get()
    Assert.assertEquals(bookID, downloadedStatus.id)

    booksController.bookGetDatabase().databaseDestroy()

    /*
     * Revoke the book, but expect to receive a local error.
     */

    val revokeFuture = booksController.bookRevoke(bookID, true)

    try {
      this.expectedException.expect(ExecutionException::class.java)
      revokeFuture.get()
    } catch (ex: ExecutionException) {
      val revokedStatus =
        (booksStatus.booksStatusGet(bookID) as Some<BookStatusRevokeFailed>).get()
      Assert.assertEquals(bookID, revokedStatus.id)
      throw ex
    }
  }

  /**
   * Borrowing and then revoking a book works.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksBorrowRevokeEPUBOK() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = MappedHTTP(LOG)
    http.addResource("http://example.com/loans/", httpResource("loans.xml"), "text/xml")
    http.addResource("http://example.com/borrow/0", httpResource("borrow-epub-0.xml"), "text/xml")
    http.addResource("http://example.com/fulfill/0", httpResource("empty.epub"), "application/epub+zip")
    http.addResource("http://example.com/revoke/0", httpResource("revoke-epub-0.xml"), "text/xml")

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database, http),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.none(),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val booksStatus = booksController.bookGetStatusCache()

    val loginListener = LoggingAccountLoginListener(LOG)
    val loginFuture = booksController.accountLogin(creds, loginListener)
    loginFuture.get()

    LOG.debug("borrowing book")

    val bookID =
      BookID.exactString("2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881")

    /*
     * Borrow the book.
     *
     * XXX: The book borrowing code needs to be completely rewritten. It is currently essentially
     * impossible to convert the borrowing code into a task that can be observed synchronously.
     */

    booksController.bookBorrow(bookID, makeOPDSEntryEPUB(), true)
    TimeUnit.SECONDS.sleep(2L)

    val downloadedStatus =
      (booksStatus.booksStatusGet(bookID) as Some<BookStatusDownloaded>).get()
    Assert.assertEquals(bookID, downloadedStatus.id)

    /*
     * Revoke the book.
     */

    val revokeFuture = booksController.bookRevoke(bookID, true)
    revokeFuture.get()
    LOG.debug("revocation task finished")

    Assert.assertTrue(
      "Book has no status after revocation",
      booksStatus.booksStatusGet(bookID).isNone)

    Assert.assertFalse(
      "Database entry should be gone",
      booksController.bookGetDatabase().databaseEntryExists(bookID))
  }

  /**
   * Borrowing and then revoking a book (with mocked Adobe DRM) works.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksBorrowRevokeEPUBMockedAdobeDRMOK() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = MappedHTTP(LOG)
    http.addResource("http://example.com/loans/", httpResource("loans.xml"), "text/xml")
    http.addResource("http://example.com/borrow/0", httpResource("borrow-epub-0.xml"), "text/xml")
    http.addResource("http://example.com/fulfill/0", httpResource("empty.epub"), "application/epub+zip")
    http.addResource("http://example.com/revoke/0", httpResource("revoke-epub-0.xml"), "text/xml")

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    /*
     * Create a mocked Adobe connector.
     */

    val adobeConnector =
      Mockito.mock(AdobeAdeptConnectorType::class.java)

    /*
     * When the code calls the "loanReturn" method, tell the passed-in listener that the
     * return succeeded.
     */

    Mockito.`when`(adobeConnector.loanReturn(Mockito.any(), Mockito.any(), Mockito.any()))
      .thenAnswer { invocation ->
        val listener =
          invocation.getArgument(0) as AdobeAdeptLoanReturnListenerType
        listener.onLoanReturnSuccess()
      }

    val adobeDRM = AdobeAdeptExecutorType { procedure ->
      LOG.debug("execute {}", procedure)
      procedure.executeWith(adobeConnector)
    }

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database, http),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.some(adobeDRM),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val booksStatus = booksController.bookGetStatusCache()

    val loginListener = LoggingAccountLoginListener(LOG)
    val loginFuture = booksController.accountLogin(creds, loginListener)
    loginFuture.get()

    LOG.debug("borrowing book")

    val bookID =
      BookID.exactString("2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881")

    /*
     * Borrow the book.
     *
     * XXX: The book borrowing code needs to be completely rewritten. It is currently essentially
     * impossible to convert the borrowing code into a task that can be observed synchronously.
     */

    booksController.bookBorrow(bookID, makeOPDSEntryEPUB(), true)
    TimeUnit.SECONDS.sleep(2L)

    val downloadedStatus =
      (booksStatus.booksStatusGet(bookID) as Some<BookStatusDownloaded>).get()
    Assert.assertEquals(bookID, downloadedStatus.id)

    /*
     * Insert some fake Adobe rights information into the databases.
     */

    val accountCredentials =
      AccountCredentials(
        Option.of(AdobeVendorID("FAKE")),
        barcode,
        pin,
        Option.of(AccountAuthProvider("FAKE")),
        Option.of(AccountAuthToken("FAKE")),
        Option.of(AccountAdobeToken("FAKE")),
        Option.of(AccountPatron("FAKE")))

    accountCredentials.adobeUserID = Option.of(AdobeUserID("FAKE"))
    accounts.accountSetCredentials(accountCredentials)

    val formatHandle =
      (booksController.bookGetDatabase()
        .databaseOpenExistingEntry(bookID)
        .entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)
        as Some<BookDatabaseEntryFormatHandleEPUB>)
        .get()

    formatHandle.setAdobeRightsInformation(
      Option.of(AdobeAdeptLoan(
        AdobeLoanID("xxxx"),
        ByteBuffer.allocate(4),
        true)))

    /*
     * Revoke the book.
     */

    val revokeFuture = booksController.bookRevoke(bookID, true)
    revokeFuture.get()
    LOG.debug("revocation task finished")

    Assert.assertTrue(
      "Book has no status after revocation",
      booksStatus.booksStatusGet(bookID).isNone)

    Assert.assertFalse(
      "Database entry should be gone",
      booksController.bookGetDatabase().databaseEntryExists(bookID))
  }

  /**
   * Borrowing and then revoking a book (with mocked Adobe DRM) correctly reports failures when
   * the DRM fails.
   */

  @Test(timeout = 10_000L)
  @Throws(Exception::class)
  fun testBooksBorrowRevokeEPUBMockedAdobeDRMFailedDRM() {

    val tmp = DirectoryUtilities.directoryCreateTemporary()
    val booksConfig = BooksControllerConfiguration()

    val barcode = AccountBarcode("barcode")
    val pin = AccountPIN("pin")
    val creds = AccountCredentials(Option.none(), barcode, pin, Option.some(AccountAuthProvider("Library")))

    val http = MappedHTTP(LOG)
    http.addResource("http://example.com/loans/", httpResource("loans.xml"), "text/xml")
    http.addResource("http://example.com/borrow/0", httpResource("borrow-epub-0.xml"), "text/xml")
    http.addResource("http://example.com/fulfill/0", httpResource("empty.epub"), "application/epub+zip")
    http.addResource("http://example.com/revoke/0", httpResource("revoke-epub-0.xml"), "text/xml")

    val downloader =
      DownloaderHTTP.newDownloader(this.executor, DirectoryUtilities.directoryCreateTemporary(), http)
    val accounts =
      AccountsDatabase.openDatabase(File(tmp, "accounts"))
    val database =
      BookDatabase.newDatabase(this.jsonSerializer, this.jsonParser, File(tmp, "data"))

    /*
     * Create a mocked Adobe connector.
     */

    val adobeConnector =
      Mockito.mock(AdobeAdeptConnectorType::class.java)

    /*
     * When the code calls the "loanReturn" method, tell the passed-in listener that the
     * return failed.
     */

    Mockito.`when`(adobeConnector.loanReturn(Mockito.any(), Mockito.any(), Mockito.any()))
      .thenAnswer { invocation ->
        val listener =
          invocation.getArgument(0) as AdobeAdeptLoanReturnListenerType
        listener.onLoanReturnFailure("E_DEFECTIVE")
      }

    val adobeDRM = AdobeAdeptExecutorType { procedure ->
      LOG.debug("execute {}", procedure)
      procedure.executeWith(adobeConnector)
    }

    val booksController =
      BooksController.newBooks(
        this.context,
        this.executor,
        BooksContract.newParser(database, http),
        http,
        downloader,
        this.jsonSerializer,
        this.jsonParser,
        Option.some(adobeDRM),
        EmptyDocumentStore(),
        database,
        accounts,
        booksConfig,
        booksConfig.currentRootFeedURI.resolve("loans/"))

    val booksStatus = booksController.bookGetStatusCache()

    val loginListener = LoggingAccountLoginListener(LOG)
    val loginFuture = booksController.accountLogin(creds, loginListener)
    loginFuture.get()

    LOG.debug("borrowing book")

    val bookID =
      BookID.exactString("2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881")

    /*
     * Borrow the book.
     *
     * XXX: The book borrowing code needs to be completely rewritten. It is currently essentially
     * impossible to convert the borrowing code into a task that can be observed synchronously.
     */

    booksController.bookBorrow(bookID, makeOPDSEntryEPUB(), true)
    TimeUnit.SECONDS.sleep(2L)

    val downloadedStatus =
      (booksStatus.booksStatusGet(bookID) as Some<BookStatusDownloaded>).get()
    Assert.assertEquals(bookID, downloadedStatus.id)

    /*
     * Insert some fake Adobe rights information into the databases.
     */

    val accountCredentials =
      AccountCredentials(
        Option.of(AdobeVendorID("FAKE")),
        barcode,
        pin,
        Option.of(AccountAuthProvider("FAKE")),
        Option.of(AccountAuthToken("FAKE")),
        Option.of(AccountAdobeToken("FAKE")),
        Option.of(AccountPatron("FAKE")))

    accountCredentials.adobeUserID = Option.of(AdobeUserID("FAKE"))
    accounts.accountSetCredentials(accountCredentials)

    val formatHandle =
      (booksController.bookGetDatabase()
        .databaseOpenExistingEntry(bookID)
        .entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)
        as Some<BookDatabaseEntryFormatHandleEPUB>)
        .get()

    formatHandle.setAdobeRightsInformation(
      Option.of(AdobeAdeptLoan(
        AdobeLoanID("xxxx"),
        ByteBuffer.allocate(4),
        true)))

    /*
     * Revoke the book, but expect to receive a local error.
     */

    val revokeFuture = booksController.bookRevoke(bookID, true)

    try {
      this.expectedException.expect(BookRevokeExceptionDRMWorkflowError::class.java)
      revokeFuture.get()
    } catch (ex: ExecutionException) {
      val revokedStatus =
        (booksStatus.booksStatusGet(bookID) as Some<BookStatusRevokeFailed>).get()
      Assert.assertEquals(bookID, revokedStatus.id)
      Assert.assertTrue(
        "Database entry should not be gone",
        booksController.bookGetDatabase().databaseEntryExists(bookID))
      throw ex.cause!!
    }
  }

  /**
   * Make an OPDS entry that allows borrowing something that looks like an audio book.
   */

  private fun makeOPDSEntryAudioBook(): OPDSAcquisitionFeedEntry {
    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "x",
        "Book",
        Calendar.getInstance(),
        OPDSAvailabilityLoanable.get())

    val opdsAcquisition =
      OPDSAcquisition(OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://example.com/borrow/0"),
        Option.none(),
        listOf(OPDSIndirectAcquisition(
          "application/audiobook+json",
          listOf(OPDSIndirectAcquisition(
            "audio/mpeg",
            listOf())))))

    opdsEntryBuilder.addAcquisition(opdsAcquisition)
    return opdsEntryBuilder.build()
  }

  /**
   * Make an OPDS entry that allows borrowing something that looks like an EPUB.
   */

  private fun makeOPDSEntryEPUB(): OPDSAcquisitionFeedEntry {
    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "x",
        "Book",
        Calendar.getInstance(),
        OPDSAvailabilityLoanable.get())

    val opdsAcquisition =
      OPDSAcquisition(OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://example.com/borrow/0"),
        Option.none(),
        listOf(OPDSIndirectAcquisition(
          "application/epub+zip",
          listOf())))

    opdsEntryBuilder.addAcquisition(opdsAcquisition)
    return opdsEntryBuilder.build()
  }

  private class BooksControllerConfiguration : BooksControllerConfigurationType {
    var currentRoot: URI = BooksContract.ROOT_URI

    @Synchronized
    override fun getCurrentRootFeedURI(): URI {
      return this.currentRoot
    }

    @Synchronized
    override fun setCurrentRootFeedURI(u: URI) {
      this.currentRoot = NullCheck.notNull(u)
    }

    override fun getAdobeAuthURI(): URI? {
      return null
    }

    override fun setAdobeAuthURI(u: URI) {

    }

    override fun getAlternateRootFeedURI(): URI? {
      return null
    }

    override fun setAlternateRootFeedURI(u: URI) {

    }
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(BooksContract::class.java)
    private val LOANS_URI = URI.create("http://example.com/loans/")
    private val ROOT_URI = URI.create("http://example.com/")

    private fun httpResource(name: String): MappedHTTP.MappedResource {
      val stream = BooksContract.javaClass.getResourceAsStream(name)
      ByteArrayOutputStream().use { outStream ->
        val size = stream.copyTo(outStream, 1024)
        val copyStream = ByteArrayInputStream(outStream.toByteArray())
        return MappedHTTP.MappedResource(copyStream, size)
      }
    }

    private fun newParser(
      db: BookDatabaseReadableType,
      http: HTTPType = HTTP.newHTTP()): FeedLoaderType {
      val parser =
        OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes()))
      val exec = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
      val transport = FeedHTTPTransport.newTransport(http)
      val searchParser = OPDSSearchParser.newParser()
      return FeedLoader.newFeedLoader(exec, db, parser, transport, searchParser)
    }
  }
}
