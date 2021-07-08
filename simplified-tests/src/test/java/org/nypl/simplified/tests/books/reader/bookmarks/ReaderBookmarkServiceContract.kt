package org.nypl.simplified.tests.books.reader.bookmarks

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.subjects.Subject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookLocation
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkKind
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCalls
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkHTTPCallsType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableResult.SYNC_DISABLED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableResult.SYNC_ENABLED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableResult.SYNC_ENABLE_NOT_SUPPORTED
import org.nypl.simplified.tests.EventAssertions
import org.nypl.simplified.tests.EventLogging
import org.nypl.simplified.tests.mocking.MockProfilesController
import org.slf4j.Logger
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

abstract class ReaderBookmarkServiceContract {

  val fakeAccountID =
    AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))

  protected abstract val logger: Logger

  protected abstract fun bookmarkService(
    threads: (Runnable) -> Thread,
    events: Subject<ReaderBookmarkEvent>,
    httpCalls: ReaderBookmarkHTTPCallsType,
    profilesController: ProfilesControllerType
  ): ReaderBookmarkServiceType

  private val objectMapper = ObjectMapper()
  private var readerBookmarkService: ReaderBookmarkServiceType? = null
  private lateinit var server: MockWebServer
  private lateinit var http: LSHTTPClientType
  private lateinit var annotationsURI: URI
  private lateinit var patronURI: URI

  private val annotationsEmpty = """
{
   "id" : "http://www.example.com/annotations/",
   "type" : [
      "BasicContainer",
      "AnnotationCollection"
   ],
   "@context" : [
      "http://www.w3.org/ns/anno.jsonld",
      "http://www.w3.org/ns/ldp.jsonld"
   ],
   "total" : 0,
   "first" : {
      "items" : [],
      "type" : "AnnotationPage",
      "id" : "http://www.example.com/annotations/"
   }
}
"""

  private val patronSettingsWithAnnotationsEnabled = """
{
  "settings": {
    "simplified:synchronize_annotations": true
  }
}
"""

  private val patronSettingsWithAnnotationsDisabled = """
{
  "settings": {
    "simplified:synchronize_annotations": false
  }
}
"""

  private val accountCredentials =
    AccountAuthenticationCredentials.Basic(
      userName = AccountUsername("abcd"),
      password = AccountPassword("1234"),
      adobeCredentials = null,
      authenticationDescription = null,
      annotationsURI = null
    )

  private fun addResponse(
    uri: String,
    response: String
  ) {
    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(response)
    )
  }

  @BeforeEach
  fun setup() {
    this.http =
      LSHTTPClients()
        .create(
          context = Mockito.mock(Context::class.java),
          configuration = LSHTTPClientConfiguration(
            applicationName = "simplified-test",
            applicationVersion = "0.0.1",
            tlsOverrides = null,
            timeout = Pair(5L, TimeUnit.SECONDS)
          )
        )

    this.server = MockWebServer()
    this.server.start()
    this.annotationsURI =
      this.server.url("annotations").toUri()
    this.patronURI =
      this.server.url("patron").toUri()
  }

  @AfterEach
  fun tearDown() {
    this.readerBookmarkService?.close()
    this.server.close()
  }

  /**
   * Initializing the bookmarks controller with a single account that permits and supports syncing
   * but has no books, succeeds quietly.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testInitializeEmpty() {
    this.addResponse("http://www.example.com/patron", this.patronSettingsWithAnnotationsEnabled)
    this.addResponse("http://www.example.com/annotations", this.annotationsEmpty)

    val httpCalls = ReaderBookmarkHTTPCalls(this.objectMapper, this.http)

    val profileEvents =
      EventLogging.create<ProfileEvent>(this.logger, 1)
    val bookmarkEvents =
      EventLogging.create<ReaderBookmarkEvent>(this.logger, 2)
    val accountEvents =
      EventLogging.create<AccountEvent>(this.logger, 1)

    val books =
      Mockito.mock(BookDatabaseType::class.java)

    Mockito.`when`(books.books())
      .thenReturn(sortedSetOf())

    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(accountProvider.patronSettingsURI)
      .thenReturn(this.patronURI)

    val accountPreferences =
      AccountPreferences(
        bookmarkSyncingPermitted = true,
        catalogURIOverride = null,
        announcementsAcknowledged = listOf()
      )

    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoggedIn(
          this.accountCredentials.copy(annotationsURI = this.annotationsURI)
        )
      )
    Mockito.`when`(account.id)
      .thenReturn(this.fakeAccountID)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(books)
    Mockito.`when`(account.preferences)
      .thenReturn(accountPreferences)

    val profile =
      Mockito.mock(ProfileType::class.java)

    Mockito.`when`(profile.account(this.fakeAccountID))
      .thenReturn(account)
    Mockito.`when`(profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.fakeAccountID, account)))
    Mockito.`when`(profile.id)
      .thenReturn(ProfileID.generate())

    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(profiles.profileEvents())
      .thenReturn(profileEvents.events)
    Mockito.`when`(profiles.accountEvents())
      .thenReturn(accountEvents.events)
    Mockito.`when`(profiles.profileCurrent())
      .thenReturn(profile)

    this.readerBookmarkService =
      this.bookmarkService(::Thread, bookmarkEvents.events, httpCalls, profiles)

    bookmarkEvents.latch.await()

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncStarted::class.java,
      bookmarkEvents.eventLog,
      0,
      { event -> Assertions.assertEquals(this.fakeAccountID, event.accountID) }
    )

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncFinished::class.java,
      bookmarkEvents.eventLog,
      1,
      { event -> Assertions.assertEquals(this.fakeAccountID, event.accountID) }
    )

    Assertions.assertEquals(2, this.server.requestCount)
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.patronURI, request.requestUrl?.toUri())
    }
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.annotationsURI, request.requestUrl?.toUri())
    }
  }

  /**
   * Initializing the bookmarks controller with a single account that permits and supports syncing
   * but has no books, succeeds quietly.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testInitializeReceive() {
    this.addResponse("http://www.example.com/patron", this.patronSettingsWithAnnotationsEnabled)

    this.addResponse(
      "http://www.example.com/annotations",
      """
    {
       "id" : "http://www.example.com/annotations/",
       "type" : [
          "BasicContainer",
          "AnnotationCollection"
       ],
       "@context" : [
          "http://www.w3.org/ns/anno.jsonld",
          "http://www.w3.org/ns/ldp.jsonld"
       ],
       "total" : 0,
       "first" : {
          "items" : [
             {
                "body" : {
                   "http://librarysimplified.org/terms/device" : "urn:uuid:253c7cbc-4fdf-430e-81b9-18bea90b6026",
                   "http://librarysimplified.org/terms/time" : "2018-12-03T16:29:03"
                },
                "id" : "http://www.example.com/annotations/100000",
                "type" : "Annotation",
                "motivation" : "http://www.w3.org/ns/oa#bookmarking",
                "target" : {
                   "selector" : {
                      "value" : "{\"idref\":\"n-1\",\"contentCFI\":\"/4/14,/1:0,/1:1\"}",
                      "type" : "FragmentSelector"
                   },
                   "source" : "urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a"
                }
             }
          ],
          "type" : "AnnotationPage",
          "id" : "http://www.example.com/annotations/"
       }
    }
    """
    )

    val httpCalls = ReaderBookmarkHTTPCalls(this.objectMapper, this.http)

    val profileEvents =
      EventLogging.create<ProfileEvent>(this.logger, 1)
    val bookmarkEvents =
      EventLogging.create<ReaderBookmarkEvent>(this.logger, 3)
    val accountEvents =
      EventLogging.create<AccountEvent>(this.logger, 1)

    val bookID =
      BookID.create("fab6e4ebeb3240676b3f7585f8ee4faecccbe1f9243a652153f3071e90599325")

    val receivedBookmarks =
      mutableListOf<Bookmark>()

    val format =
      BookFormat.BookFormatEPUB(
        drmInformation = BookDRMInformation.None,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(formatHandle.format)
      .thenReturn(format)

    Mockito.`when`(formatHandle.setBookmarks(Mockito.anyList()))
      .then { input ->
        val bookmarks: List<Bookmark> = input.arguments[0] as List<Bookmark>
        receivedBookmarks.addAll(bookmarks)
        Unit
      }

    val bookEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)

    Mockito.`when`(bookEntry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java))
      .thenReturn(formatHandle)

    val books =
      Mockito.mock(BookDatabaseType::class.java)

    Mockito.`when`(books.books())
      .thenReturn(sortedSetOf())
    Mockito.`when`(books.entry(bookID))
      .thenReturn(bookEntry)

    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(accountProvider.patronSettingsURI)
      .thenReturn(this.patronURI)

    val accountPreferences =
      AccountPreferences(
        bookmarkSyncingPermitted = true,
        catalogURIOverride = null,
        announcementsAcknowledged = listOf()
      )

    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoggedIn(
          this.accountCredentials.copy(annotationsURI = this.annotationsURI)
        )
      )
    Mockito.`when`(account.id)
      .thenReturn(this.fakeAccountID)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(books)
    Mockito.`when`(account.preferences)
      .thenReturn(accountPreferences)

    val profile =
      Mockito.mock(ProfileType::class.java)

    Mockito.`when`(profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.fakeAccountID, account)))
    Mockito.`when`(profile.id)
      .thenReturn(ProfileID.generate())
    Mockito.`when`(profile.account(this.fakeAccountID))
      .thenReturn(account)

    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(profiles.profileEvents())
      .thenReturn(profileEvents.events)
    Mockito.`when`(profiles.accountEvents())
      .thenReturn(accountEvents.events)
    Mockito.`when`(profiles.profileCurrent())
      .thenReturn(profile)

    this.readerBookmarkService =
      this.bookmarkService(::Thread, bookmarkEvents.events, httpCalls, profiles)

    bookmarkEvents.latch.await()

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncStarted::class.java,
      bookmarkEvents.eventLog,
      0,
      { event -> Assertions.assertEquals(this.fakeAccountID, event.accountID) }
    )

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSaved::class.java,
      bookmarkEvents.eventLog,
      1,
      { event -> Assertions.assertEquals(this.fakeAccountID, event.accountID) }
    )

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncFinished::class.java,
      bookmarkEvents.eventLog,
      2,
      { event -> Assertions.assertEquals(this.fakeAccountID, event.accountID) }
    )

    Assertions.assertEquals(1, receivedBookmarks.size)
    Assertions.assertEquals("urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a", receivedBookmarks[0].opdsId)

    Assertions.assertEquals(2, this.server.requestCount)
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.patronURI, request.requestUrl?.toUri())
    }
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.annotationsURI, request.requestUrl?.toUri())
    }
  }

  /**
   * Initializing the bookmarks controller with a single account that permits and supports syncing
   * and has bookmarks, succeeds quietly.
   */

  @Test
  @Timeout(value = 5L, unit = TimeUnit.SECONDS)
  fun testInitializeSendBookmarks() {
    this.addResponse("http://www.example.com/patron", this.patronSettingsWithAnnotationsEnabled)

    val responseText = """
    {
       "id" : "http://www.example.com/annotations/",
       "type" : [
          "BasicContainer",
          "AnnotationCollection"
       ],
       "@context" : [
          "http://www.w3.org/ns/anno.jsonld",
          "http://www.w3.org/ns/ldp.jsonld"
       ],
       "total" : 0,
       "first" : {
          "items" : [
             {
                "body" : {
                   "http://librarysimplified.org/terms/device" : "urn:uuid:253c7cbc-4fdf-430e-81b9-18bea90b6026",
                   "http://librarysimplified.org/terms/time" : "2018-12-03T16:29:03"
                },
                "id" : "http://www.example.com/annotations/100000",
                "type" : "Annotation",
                "motivation" : "http://www.w3.org/ns/oa#bookmarking",
                "target" : {
                   "selector" : {
                      "value" : "{\"idref\":\"n-1\",\"contentCFI\":\"/4/14,/1:0,/1:1\"}",
                      "type" : "FragmentSelector"
                   },
                   "source" : "urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a"
                }
             }
          ],
          "type" : "AnnotationPage",
          "id" : "http://www.example.com/annotations/"
       }
    }
    """

    this.addResponse("http://www.example.com/annotations", responseText)

    val httpCalls = ReaderBookmarkHTTPCalls(this.objectMapper, this.http)

    val profileEvents =
      EventLogging.create<ProfileEvent>(this.logger, 1)
    val bookmarkEvents =
      EventLogging.create<ReaderBookmarkEvent>(this.logger, 3)
    val accountEvents =
      EventLogging.create<AccountEvent>(this.logger, 1)

    val bookID =
      BookID.create("fab6e4ebeb3240676b3f7585f8ee4faecccbe1f9243a652153f3071e90599325")

    val receivedBookmarks =
      mutableListOf<Bookmark>()

    val startingBookmarks =
      listOf(
        Bookmark.create(
          opdsId = "urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a",
          location = BookLocation.BookLocationR1(0.5, null, "x"),
          kind = BookmarkKind.ReaderBookmarkLastReadLocation,
          time = DateTime.now(DateTimeZone.UTC),
          chapterTitle = "A Title",
          bookProgress = 0.5,
          deviceID = "urn:uuid:253c7cbc-4fdf-430e-81b9-18bea90b6026",
          uri = null
        )
      )

    val format =
      BookFormat.BookFormatEPUB(
        drmInformation = BookDRMInformation.None,
        file = null,
        lastReadLocation = null,
        bookmarks = startingBookmarks,
        contentType = BookFormats.epubMimeTypes().first()
      )

    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(formatHandle.format)
      .thenReturn(format)

    Mockito.`when`(formatHandle.setBookmarks(Mockito.anyList()))
      .then { input ->
        val bookmarks: List<Bookmark> = input.arguments[0] as List<Bookmark>
        receivedBookmarks.addAll(bookmarks)
        Unit
      }

    val bookEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)

    Mockito.`when`(bookEntry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java))
      .thenReturn(formatHandle)

    val books =
      Mockito.mock(BookDatabaseType::class.java)

    Mockito.`when`(books.books())
      .thenReturn(sortedSetOf())
    Mockito.`when`(books.entry(bookID))
      .thenReturn(bookEntry)

    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)

    Mockito.`when`(accountProvider.patronSettingsURI)
      .thenReturn(this.patronURI)

    val accountPreferences =
      AccountPreferences(
        bookmarkSyncingPermitted = true,
        catalogURIOverride = null,
        announcementsAcknowledged = listOf()
      )

    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoggedIn(
          this.accountCredentials.copy(annotationsURI = this.annotationsURI)
        )
      )
    Mockito.`when`(account.id)
      .thenReturn(this.fakeAccountID)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(account.bookDatabase)
      .thenReturn(books)
    Mockito.`when`(account.preferences)
      .thenReturn(accountPreferences)

    val profile =
      Mockito.mock(ProfileType::class.java)

    Mockito.`when`(profile.accounts())
      .thenReturn(sortedMapOf(Pair(this.fakeAccountID, account)))
    Mockito.`when`(profile.id)
      .thenReturn(ProfileID.generate())
    Mockito.`when`(profile.account(this.fakeAccountID))
      .thenReturn(account)

    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(profiles.profileEvents())
      .thenReturn(profileEvents.events)
    Mockito.`when`(profiles.accountEvents())
      .thenReturn(accountEvents.events)
    Mockito.`when`(profiles.profileCurrent())
      .thenReturn(profile)

    this.readerBookmarkService =
      this.bookmarkService(::Thread, bookmarkEvents.events, httpCalls, profiles)

    bookmarkEvents.latch.await()

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncStarted::class.java,
      bookmarkEvents.eventLog,
      0,
      { event -> Assertions.assertEquals(this.fakeAccountID, event.accountID) }
    )

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSaved::class.java,
      bookmarkEvents.eventLog,
      1,
      { event -> Assertions.assertEquals(this.fakeAccountID, event.accountID) }
    )

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncFinished::class.java,
      bookmarkEvents.eventLog,
      2,
      { event -> Assertions.assertEquals(this.fakeAccountID, event.accountID) }
    )

    Assertions.assertEquals(2, receivedBookmarks.size)
    Assertions.assertEquals("urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a", receivedBookmarks[0].opdsId)
    Assertions.assertEquals("urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a", receivedBookmarks[1].opdsId)

    Assertions.assertEquals(2, this.server.requestCount)
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.patronURI, request.requestUrl?.toUri())
    }
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.annotationsURI, request.requestUrl?.toUri())
    }
  }

  /**
   * Trying to enable syncing on an account that doesn't support it, returns an appropriate status
   * code.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testEnableBookmarkSyncingNotSupported() {
    val httpCalls =
      ReaderBookmarkHTTPCalls(this.objectMapper, this.http)
    val bookmarkEvents =
      EventLogging.create<ReaderBookmarkEvent>(this.logger, 3)
    val profiles =
      MockProfilesController(1, 1)

    this.readerBookmarkService =
      this.bookmarkService(
        threads = ::Thread,
        events = bookmarkEvents.events,
        httpCalls = httpCalls,
        profilesController = profiles
      )

    val service =
      this.readerBookmarkService!!
    val result =
      service.bookmarkSyncEnable(profiles.profileList[0].accountList[0].id, true).get()

    Assertions.assertEquals(SYNC_ENABLE_NOT_SUPPORTED, result)
  }

  /**
   * Trying to enable syncing on an account that supports it, makes the appropriate HTTP calls, and
   * returns an appropriate status code.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testEnableBookmarkSyncingSupportedEnable() {
    val httpCalls =
      ReaderBookmarkHTTPCalls(this.objectMapper, this.http)
    val bookmarkEvents =
      EventLogging.create<ReaderBookmarkEvent>(this.logger, 3)
    val profiles =
      MockProfilesController(1, 1)

    val account = profiles.profileList[0].accountList[0]
    account.setAccountProvider(
      (account.provider as AccountProvider).copy(patronSettingsURI = this.patronURI)
    )
    account.setPreferences(account.preferences.copy(bookmarkSyncingPermitted = false))
    account.setLoginState(
      AccountLoggedIn(
        AccountAuthenticationCredentials.Basic(
          userName = AccountUsername("durandal"),
          password = AccountPassword("tycho"),
          adobeCredentials = null,
          authenticationDescription = null,
          annotationsURI = this.annotationsURI
        )
      )
    )

    /*
     * The service checks to see if the patron has syncing enabled.
     */

    this.addResponse(
      this.patronURI.toString(),
      this.patronSettingsWithAnnotationsDisabled
    )

    /*
     * The service then sends a request to turn syncing on.
     */

    this.addResponse(
      this.patronURI.toString(),
      this.patronSettingsWithAnnotationsEnabled
    )

    /*
     * The service then checks again to see if the patron has syncing enabled.
     */

    this.addResponse(
      this.patronURI.toString(),
      this.patronSettingsWithAnnotationsEnabled
    )

    this.readerBookmarkService =
      this.bookmarkService(
        threads = ::Thread,
        events = bookmarkEvents.events,
        httpCalls = httpCalls,
        profilesController = profiles
      )

    val service =
      this.readerBookmarkService!!
    val result =
      service.bookmarkSyncEnable(
        accountID = profiles.profileList[0].accountList[0].id,
        enabled = true
      ).get()

    Assertions.assertEquals(3, this.server.requestCount)
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.patronURI, request.requestUrl?.toUri())
    }
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.patronURI, request.requestUrl?.toUri())
    }
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.patronURI, request.requestUrl?.toUri())
    }
    Assertions.assertEquals(SYNC_ENABLED, result)
    Assertions.assertEquals(true, account.preferences.bookmarkSyncingPermitted)
  }

  /**
   * Trying to disable syncing on an account that supports it, makes the appropriate HTTP calls, and
   * returns an appropriate status code.
   */

  @Test
  @Timeout(value = 10L, unit = TimeUnit.SECONDS)
  fun testEnableBookmarkSyncingSupportedDisable() {
    val httpCalls =
      ReaderBookmarkHTTPCalls(this.objectMapper, this.http)
    val bookmarkEvents =
      EventLogging.create<ReaderBookmarkEvent>(this.logger, 3)
    val profiles =
      MockProfilesController(1, 1)

    val account = profiles.profileList[0].accountList[0]
    account.setAccountProvider(
      (account.provider as AccountProvider).copy(patronSettingsURI = this.patronURI)
    )
    account.setPreferences(account.preferences.copy(bookmarkSyncingPermitted = true))
    account.setLoginState(
      AccountLoggedIn(
        AccountAuthenticationCredentials.Basic(
          userName = AccountUsername("durandal"),
          password = AccountPassword("tycho"),
          adobeCredentials = null,
          authenticationDescription = null,
          annotationsURI = this.annotationsURI
        )
      )
    )

    /*
     * The service checks to see if the patron has syncing enabled. As it is initially enabled,
     * there'll be a request for bookmarks right after.
     */

    this.addResponse(
      this.patronURI.toString(),
      this.patronSettingsWithAnnotationsEnabled
    )
    this.addResponse(
      this.annotationsURI.toString(),
      this.annotationsEmpty
    )

    /*
     * The service then sends a request to turn syncing off, then checks again to see if the patron
     * has syncing disabled.
     */

    this.addResponse(
      this.patronURI.toString(),
      this.patronSettingsWithAnnotationsDisabled
    )
    this.addResponse(
      this.patronURI.toString(),
      this.patronSettingsWithAnnotationsDisabled
    )

    this.readerBookmarkService =
      this.bookmarkService(
        threads = ::Thread,
        events = bookmarkEvents.events,
        httpCalls = httpCalls,
        profilesController = profiles
      )

    val service =
      this.readerBookmarkService!!
    val result =
      service.bookmarkSyncEnable(
        accountID = profiles.profileList[0].accountList[0].id,
        enabled = false
      ).get()

    Assertions.assertEquals(4, this.server.requestCount)
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.patronURI, request.requestUrl?.toUri())
    }
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.annotationsURI, request.requestUrl?.toUri())
    }
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.patronURI, request.requestUrl?.toUri())
    }
    this.run {
      val request = this.server.takeRequest()
      Assertions.assertEquals(this.patronURI, request.requestUrl?.toUri())
    }
    Assertions.assertEquals(SYNC_DISABLED, result)
    Assertions.assertEquals(false, account.preferences.bookmarkSyncingPermitted)
  }
}
