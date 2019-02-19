package org.nypl.simplified.tests.books.reader.bookmarks

import com.fasterxml.jackson.databind.ObjectMapper
import com.io7m.jfunctional.Option
import org.joda.time.LocalDateTime
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountBarcode
import org.nypl.simplified.books.accounts.AccountEvent
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.accounts.AccountPIN
import org.nypl.simplified.books.accounts.AccountPreferences
import org.nypl.simplified.books.accounts.AccountProvider
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.BookDatabaseType
import org.nypl.simplified.books.book_database.BookFormat
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.profiles.ProfileEvent
import org.nypl.simplified.books.profiles.ProfileID
import org.nypl.simplified.books.profiles.ProfileType
import org.nypl.simplified.books.reader.ReaderBookLocation
import org.nypl.simplified.books.reader.ReaderBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkControllerType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCalls
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCallsType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkKind
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPResultType
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.tests.EventAssertions
import org.nypl.simplified.tests.EventLogging
import org.nypl.simplified.tests.http.MockingHTTP
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

abstract class ReaderBookmarkControllerContract {

  protected abstract val logger: Logger

  protected abstract fun bookmarkController(
    threads: (Runnable) -> Thread,
    events: ObservableType<ReaderBookmarkEvent>,
    httpCalls: ReaderBookmarkHTTPCallsType,
    profilesController: ProfilesControllerType): ReaderBookmarkControllerType

  private val objectMapper = ObjectMapper()
  private var readerBookmarkController: ReaderBookmarkControllerType? = null

  private val accountCredentials =
    AccountAuthenticationCredentials.builder(
      AccountPIN.create("abcd"), AccountBarcode.create("1234"))
      .build()

  private fun okResponse(text: String): HTTPResultType<InputStream> {
    val bytes = text.toByteArray(Charset.forName("UTF-8"))
    val stream = ByteArrayInputStream(bytes)
    return HTTPResultOK<InputStream>(
      "OK",
      200,
      stream,
      bytes.size.toLong(),
      mutableMapOf(),
      0L)
  }

  @After
  fun tearDown() {
    this.readerBookmarkController?.close()
  }

  /**
   * Initializing the bookmarks controller with a single account that permits and supports syncing
   * but has no books, succeeds quietly.
   */

  @Test
  fun testInitializeEmpty() {
    val http = MockingHTTP()
    http.addResponse(
      "http://www.example.com/patron",
      this.okResponse("""
    {
      "settings": {
        "simplified:synchronize_annotations": true
      }
    }
    """))

    http.addResponse(
      "http://www.example.com/annotations",
      this.okResponse("""
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
    """))

    val httpCalls = ReaderBookmarkHTTPCalls(this.objectMapper, http)

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
      Mockito.mock(AccountProvider::class.java)

    Mockito.`when`(accountProvider.supportsSimplyESynchronization())
      .thenReturn(true)
    Mockito.`when`(accountProvider.annotationsURI())
      .thenReturn(Option.some(URI.create("http://www.example.com/annotations")))
    Mockito.`when`(accountProvider.patronSettingsURI())
      .thenReturn(Option.some(URI.create("http://www.example.com/patron")))

    val accountPreferences =
      AccountPreferences(bookmarkSyncingPermitted = true)

    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.credentials())
      .thenReturn(Option.some(this.accountCredentials))
    Mockito.`when`(account.id())
      .thenReturn(AccountID.create(2323))
    Mockito.`when`(account.provider())
      .thenReturn(accountProvider)
    Mockito.`when`(account.bookDatabase())
      .thenReturn(books)
    Mockito.`when`(account.preferences())
      .thenReturn(accountPreferences)

    val profile =
      Mockito.mock(ProfileType::class.java)

    Mockito.`when`(profile.account(AccountID.create(2323)))
      .thenReturn(account)
    Mockito.`when`(profile.accounts())
      .thenReturn(sortedMapOf(Pair(AccountID.create(2323), account)))
    Mockito.`when`(profile.id())
      .thenReturn(ProfileID.create(23))

    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(profiles.profileEvents())
      .thenReturn(profileEvents.events)
    Mockito.`when`(profiles.accountEvents())
      .thenReturn(accountEvents.events)
    Mockito.`when`(profiles.profileCurrent())
      .thenReturn(profile)

    this.readerBookmarkController =
      this.bookmarkController(::Thread, bookmarkEvents.events, httpCalls, profiles)

    bookmarkEvents.latch.await()

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncStarted::class.java,
      bookmarkEvents.eventLog,
      0,
      { event -> Assert.assertEquals(2323, event.accountID.id()) })

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncFinished::class.java,
      bookmarkEvents.eventLog,
      1,
      { event -> Assert.assertEquals(2323, event.accountID.id()) })
  }

  /**
   * Initializing the bookmarks controller with a single account that permits and supports syncing
   * but has no books, succeeds quietly.
   */

  @Test(timeout = 5_000L)
  fun testInitializeReceive() {
    val http = MockingHTTP()
    http.addResponse(
      "http://www.example.com/patron",
      this.okResponse("""
    {
      "settings": {
        "simplified:synchronize_annotations": true
      }
    }
    """))

    http.addResponse(
      "http://www.example.com/annotations",
      this.okResponse("""
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
                "motivation" : "http://librarysimplified.org/terms/annotation/idling",
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
    """))

    val httpCalls = ReaderBookmarkHTTPCalls(this.objectMapper, http)

    val profileEvents =
      EventLogging.create<ProfileEvent>(this.logger, 1)
    val bookmarkEvents =
      EventLogging.create<ReaderBookmarkEvent>(this.logger, 3)
    val accountEvents =
      EventLogging.create<AccountEvent>(this.logger, 1)

    val bookID =
      BookID.create("fab6e4ebeb3240676b3f7585f8ee4faecccbe1f9243a652153f3071e90599325")

    val receivedBookmarks =
      mutableListOf<ReaderBookmark>()

    val format =
      BookFormat.BookFormatEPUB(
        adobeRightsFile = null,
        adobeRights = null,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf())

    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(formatHandle.format)
      .thenReturn(format)

    Mockito.`when`(formatHandle.setBookmarks(Mockito.anyList()))
      .then { input ->
        val bookmarks: List<ReaderBookmark> = input.arguments[0] as List<ReaderBookmark>
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
      Mockito.mock(AccountProvider::class.java)

    Mockito.`when`(accountProvider.supportsSimplyESynchronization())
      .thenReturn(true)
    Mockito.`when`(accountProvider.annotationsURI())
      .thenReturn(Option.some(URI.create("http://www.example.com/annotations")))
    Mockito.`when`(accountProvider.patronSettingsURI())
      .thenReturn(Option.some(URI.create("http://www.example.com/patron")))

    val accountPreferences =
      AccountPreferences(bookmarkSyncingPermitted = true)

    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.credentials())
      .thenReturn(Option.some(this.accountCredentials))
    Mockito.`when`(account.id())
      .thenReturn(AccountID.create(2323))
    Mockito.`when`(account.provider())
      .thenReturn(accountProvider)
    Mockito.`when`(account.bookDatabase())
      .thenReturn(books)
    Mockito.`when`(account.preferences())
      .thenReturn(accountPreferences)

    val profile =
      Mockito.mock(ProfileType::class.java)

    Mockito.`when`(profile.accounts())
      .thenReturn(sortedMapOf(Pair(AccountID.create(2323), account)))
    Mockito.`when`(profile.id())
      .thenReturn(ProfileID.create(23))
    Mockito.`when`(profile.account(AccountID.create(2323)))
      .thenReturn(account)

    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(profiles.profileEvents())
      .thenReturn(profileEvents.events)
    Mockito.`when`(profiles.accountEvents())
      .thenReturn(accountEvents.events)
    Mockito.`when`(profiles.profileCurrent())
      .thenReturn(profile)

    this.readerBookmarkController =
      this.bookmarkController(::Thread, bookmarkEvents.events, httpCalls, profiles)

    bookmarkEvents.latch.await()

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncStarted::class.java,
      bookmarkEvents.eventLog,
      0,
      { event -> Assert.assertEquals(2323, event.accountID.id()) })

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSaved::class.java,
      bookmarkEvents.eventLog,
      1,
      { event -> Assert.assertEquals(2323, event.accountID.id()) })

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncFinished::class.java,
      bookmarkEvents.eventLog,
      2,
      { event -> Assert.assertEquals(2323, event.accountID.id()) })

    Assert.assertEquals(1, receivedBookmarks.size)
    Assert.assertEquals(
      "urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a",
      receivedBookmarks[0].opdsId)
  }

  /**
   * Initializing the bookmarks controller with a single account that permits and supports syncing
   * and has bookmarks, succeeds quietly.
   */

  @Test(timeout = 5_000L)
  fun testInitializeSendBookmarks() {
    val http = MockingHTTP()
    http.addResponse(
      "http://www.example.com/patron",
      this.okResponse("""
    {
      "settings": {
        "simplified:synchronize_annotations": true
      }
    }
    """))

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

    http.addResponse("http://www.example.com/annotations", this.okResponse(responseText))

    val httpCalls = ReaderBookmarkHTTPCalls(this.objectMapper, http)

    val profileEvents =
      EventLogging.create<ProfileEvent>(this.logger, 1)
    val bookmarkEvents =
      EventLogging.create<ReaderBookmarkEvent>(this.logger, 3)
    val accountEvents =
      EventLogging.create<AccountEvent>(this.logger, 1)

    val bookID =
      BookID.create("fab6e4ebeb3240676b3f7585f8ee4faecccbe1f9243a652153f3071e90599325")

    val receivedBookmarks =
      mutableListOf<ReaderBookmark>()

    val startingBookmarks =
      listOf(ReaderBookmark(
        opdsId = "urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a",
        location = ReaderBookLocation.create(Option.none(), "x"),
        kind = ReaderBookmarkKind.ReaderBookmarkLastReadLocation,
        time = LocalDateTime.now(),
        chapterTitle = "A Title",
        chapterProgress = 0.5,
        bookProgress = 0.5,
        deviceID = "urn:uuid:253c7cbc-4fdf-430e-81b9-18bea90b6026",
        uri = null))

    val format =
      BookFormat.BookFormatEPUB(
        adobeRightsFile = null,
        adobeRights = null,
        file = null,
        lastReadLocation = null,
        bookmarks = startingBookmarks)

    val formatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    Mockito.`when`(formatHandle.format)
      .thenReturn(format)

    Mockito.`when`(formatHandle.setBookmarks(Mockito.anyList()))
      .then { input ->
        val bookmarks: List<ReaderBookmark> = input.arguments[0] as List<ReaderBookmark>
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
      Mockito.mock(AccountProvider::class.java)

    Mockito.`when`(accountProvider.supportsSimplyESynchronization())
      .thenReturn(true)
    Mockito.`when`(accountProvider.annotationsURI())
      .thenReturn(Option.some(URI.create("http://www.example.com/annotations")))
    Mockito.`when`(accountProvider.patronSettingsURI())
      .thenReturn(Option.some(URI.create("http://www.example.com/patron")))

    val accountPreferences =
      AccountPreferences(bookmarkSyncingPermitted = true)

    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(account.credentials())
      .thenReturn(Option.some(this.accountCredentials))
    Mockito.`when`(account.id())
      .thenReturn(AccountID.create(2323))
    Mockito.`when`(account.provider())
      .thenReturn(accountProvider)
    Mockito.`when`(account.bookDatabase())
      .thenReturn(books)
    Mockito.`when`(account.preferences())
      .thenReturn(accountPreferences)

    val profile =
      Mockito.mock(ProfileType::class.java)

    Mockito.`when`(profile.accounts())
      .thenReturn(sortedMapOf(Pair(AccountID.create(2323), account)))
    Mockito.`when`(profile.id())
      .thenReturn(ProfileID.create(23))
    Mockito.`when`(profile.account(AccountID.create(2323)))
      .thenReturn(account)

    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(profiles.profileEvents())
      .thenReturn(profileEvents.events)
    Mockito.`when`(profiles.accountEvents())
      .thenReturn(accountEvents.events)
    Mockito.`when`(profiles.profileCurrent())
      .thenReturn(profile)

    this.readerBookmarkController =
      this.bookmarkController(::Thread, bookmarkEvents.events, httpCalls, profiles)

    bookmarkEvents.latch.await()

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncStarted::class.java,
      bookmarkEvents.eventLog,
      0,
      { event -> Assert.assertEquals(2323, event.accountID.id()) })

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSaved::class.java,
      bookmarkEvents.eventLog,
      1,
      { event -> Assert.assertEquals(2323, event.accountID.id()) })

    EventAssertions.isTypeAndMatches(
      ReaderBookmarkEvent.ReaderBookmarkSyncFinished::class.java,
      bookmarkEvents.eventLog,
      2,
      { event -> Assert.assertEquals(2323, event.accountID.id()) })

    Assert.assertEquals(2, receivedBookmarks.size)
    Assert.assertEquals(
      "urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a",
      receivedBookmarks[0].opdsId)
    Assert.assertEquals(
      "urn:example.com/terms/id/c083c0a6-54c6-4cc5-9d3a-425317da662a",
      receivedBookmarks[1].opdsId)
  }
}
