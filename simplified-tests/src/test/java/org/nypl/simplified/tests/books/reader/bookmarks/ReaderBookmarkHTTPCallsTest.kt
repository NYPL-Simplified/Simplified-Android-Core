package org.nypl.simplified.tests.books.reader.bookmarks

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCalls
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotation
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationBodyNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationSelectorNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationTargetNode
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

class ReaderBookmarkHTTPCallsTest {

  private lateinit var http: LSHTTPClientType
  private lateinit var server: MockWebServer

  private fun checkGetSyncing(
    expected: Boolean,
    serverResponseText: String
  ) {
    val objectMapper = ObjectMapper()
    val calls = ReaderBookmarkHTTPCalls(objectMapper, this.http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null,
        annotationsURI = URI("https://www.example.com")
      )

    val targetURI =
      this.server.url("me").toUri()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(serverResponseText)
    )

    val enabled0 = calls.syncingIsEnabled(targetURI, credentials)
    Assertions.assertEquals(expected, enabled0)
  }

  private fun checkGetBookmarks(
    expectedBookmarks: List<BookmarkAnnotation>,
    serverResponseText: String
  ) {
    val objectMapper = ObjectMapper()
    val calls = ReaderBookmarkHTTPCalls(objectMapper, this.http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null,
        annotationsURI = URI("https://www.example.com")
      )

    val targetURI =
      this.server.url("annotations").toUri()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(serverResponseText)
    )

    val receivedBookmarks = calls.bookmarksGet(targetURI, credentials)
    Assertions.assertEquals(expectedBookmarks, receivedBookmarks)
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
  }

  @AfterEach
  fun tearDown() {
    this.server.close()
  }

  @Test
  fun testGetSyncingFalse0() {
    this.checkGetSyncing(
      expected = false,
      serverResponseText = """
        {
          "settings": {
            "simplified:synchronize_annotations": "false"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testGetSyncingFalse1() {
    this.checkGetSyncing(
      expected = false,
      serverResponseText = """
        {
          "settings": {
            "simplified:synchronize_annotations": null
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testGetSyncingFalse2() {
    this.checkGetSyncing(
      expected = false,
      serverResponseText = """
        {
          "settings": {

          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testGetSyncingTrue0() {
    this.checkGetSyncing(
      expected = true,
      serverResponseText = """
        {
          "settings": {
            "simplified:synchronize_annotations": true
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testGetBookmarksEmpty() {
    this.checkGetBookmarks(
      expectedBookmarks = listOf(),
      serverResponseText = """
        {
          "@context": ["http://www.w3.org/ns/anno.jsonld", "http://www.w3.org/ns/ldp.jsonld"],
          "total": 0,
          "type": ["BasicContainer", "AnnotationCollection"],
          "id": "https://example.com/annotations/",
          "first": {
            "items": [],
            "type": "AnnotationPage",
             "id": "https://example.com/annotations/"
          }
        }
      """.trimIndent()
    )
  }

  val bookmark0 =
    BookmarkAnnotation(
      context = null,
      body =
        BookmarkAnnotationBodyNode(
          timestamp = "2019-02-08T15:37:46+0000",
          device = "urn:uuid:d8c5a487-646b-4c75-a83f-80599e8cf9d1",
          chapterTitle = null,
          bookProgress = null
        ),
      id = "https://example.com/annotations/book0",
      type = "Annotation",
      motivation = "http://librarysimplified.org/terms/annotation/idling",
      target = BookmarkAnnotationTargetNode(
        source = "urn:book0",
        selector = BookmarkAnnotationSelectorNode(
          type = "FragmentSelector",
          value = "{\n  \"@type\": \"LocatorHrefProgression\",\n  \"href\": \"/xyz.html\",\n  \"progressWithinChapter\": 0.5\n}\n"
        )
      )
    )

  val bookmark1 =
    BookmarkAnnotation(
      context = null,
      body =
        BookmarkAnnotationBodyNode(
          timestamp = "2019-02-08T15:37:47+0000",
          device = "urn:uuid:d8c5a487-646b-4c75-a83f-80599e8cf9d1",
          chapterTitle = null,
          bookProgress = null
        ),
      id = "https://example.com/annotations/book0",
      type = "Annotation",
      motivation = "http://librarysimplified.org/terms/annotation/idling",
      target = BookmarkAnnotationTargetNode(
        source = "urn:book0",
        selector = BookmarkAnnotationSelectorNode(
          type = "FragmentSelector",
          value = "{\n  \"@type\": \"LocatorHrefProgression\",\n  \"href\": \"/xyz.html\",\n  \"progressWithinChapter\": 0.5\n}\n"
        )
      )
    )

  @Test
  fun testGetBookmarksSimple() {
    this.checkGetBookmarks(
      expectedBookmarks = listOf(this.bookmark0, this.bookmark1),
      serverResponseText = """
        {
          "@context": ["http://www.w3.org/ns/anno.jsonld", "http://www.w3.org/ns/ldp.jsonld"],
          "total": 0,
          "type": ["BasicContainer", "AnnotationCollection"],
          "id": "https://example.com/annotations/",
          "first": {
            "items": [
              {
                "motivation" : "http://librarysimplified.org/terms/annotation/idling",
                "type" : "Annotation",
                "id" : "https://example.com/annotations/book0",
                "body" : {
                   "http://librarysimplified.org/terms/device" : "urn:uuid:d8c5a487-646b-4c75-a83f-80599e8cf9d1",
                   "http://librarysimplified.org/terms/time" : "2019-02-08T15:37:46+0000"
                },
                "target" : {
                   "source" : "urn:book0",
                   "selector" : {
                      "value" : "{\n  \"@type\": \"LocatorHrefProgression\",\n  \"href\": \"/xyz.html\",\n  \"progressWithinChapter\": 0.5\n}\n",
                      "type" : "FragmentSelector"
                   }
                }
              },
              {
                "motivation" : "http://librarysimplified.org/terms/annotation/idling",
                "type" : "Annotation",
                "id" : "https://example.com/annotations/book0",
                "body" : {
                   "http://librarysimplified.org/terms/device" : "urn:uuid:d8c5a487-646b-4c75-a83f-80599e8cf9d1",
                   "http://librarysimplified.org/terms/time" : "2019-02-08T15:37:47+0000"
                },
                "target" : {
                   "source" : "urn:book0",
                   "selector" : {
                      "value" : "{\n  \"@type\": \"LocatorHrefProgression\",\n  \"href\": \"/xyz.html\",\n  \"progressWithinChapter\": 0.5\n}\n",
                      "type" : "FragmentSelector"
                   }
                }
              }
            ],
            "type": "AnnotationPage",
             "id": "https://example.com/annotations/"
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun testGetSyncingFailure0() {
    val objectMapper = ObjectMapper()
    val calls = ReaderBookmarkHTTPCalls(objectMapper, this.http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null,
        annotationsURI = URI("https://www.example.com")
      )

    val targetURI = this.server.url("me").toUri()
    this.server.enqueue(
      MockResponse()
        .setResponseCode(401)
    )

    Assertions.assertThrows(
      IOException::class.java,
      Executable {
        calls.syncingIsEnabled(targetURI, credentials)
      }
    )
  }

  @Test
  fun testGetBookmarksFailure0() {
    val objectMapper = ObjectMapper()
    val calls = ReaderBookmarkHTTPCalls(objectMapper, this.http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("io7mtest"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null,
        annotationsURI = URI("https://www.example.com")
      )

    val targetURI = this.server.url("annotations").toUri()
    this.server.enqueue(
      MockResponse()
        .setResponseCode(401)
    )

    Assertions.assertThrows(
      IOException::class.java,
      Executable {
        calls.bookmarksGet(targetURI, credentials)
      }
    )
  }

  @Test
  fun testAddBookmarksFailure0() {
    val objectMapper = ObjectMapper()
    val calls = ReaderBookmarkHTTPCalls(objectMapper, this.http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("io7mtest"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null,
        annotationsURI = URI("https://www.example.com")
      )

    val targetURI = this.server.url("annotations").toUri()
    this.server.enqueue(
      MockResponse()
        .setResponseCode(401)
    )

    Assertions.assertThrows(
      IOException::class.java,
      Executable {
        calls.bookmarkAdd(targetURI, credentials, this.bookmark0)
      }
    )
  }
}
