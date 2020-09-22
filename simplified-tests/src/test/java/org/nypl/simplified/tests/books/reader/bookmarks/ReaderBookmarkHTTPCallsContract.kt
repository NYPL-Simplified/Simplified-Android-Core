package org.nypl.simplified.tests.books.reader.bookmarks

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCalls
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPResultType
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotation
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationBodyNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationSelectorNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationTargetNode
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI

abstract class ReaderBookmarkHTTPCallsContract {

  @JvmField
  @Rule
  val expectedException = ExpectedException.none()

  private fun checkGetSyncing(expected: Boolean, serverResponseText: String) {
    val objectMapper = ObjectMapper()
    val http = JSONParsingHTTP(objectMapper)
    val calls = ReaderBookmarkHTTPCalls(objectMapper, http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null
      )

    val targetURI =
      URI.create("https://example.com/me/")

    http.responses[targetURI] = this.httpOKOfData(objectMapper, serverResponseText)
    val enabled0 = calls.syncingIsEnabled(targetURI, credentials)
    Assert.assertEquals(expected, enabled0)
  }

  private fun checkGetBookmarks(
    expectedBookmarks: List<BookmarkAnnotation>,
    serverResponseText: String
  ) {
    val objectMapper = ObjectMapper()
    val http = JSONParsingHTTP(objectMapper)
    val calls = ReaderBookmarkHTTPCalls(objectMapper, http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null
      )

    val targetURI =
      URI.create("https://example.com/annotations/")

    http.responses[targetURI] = this.httpOKOfData(objectMapper, serverResponseText)
    val receivedBookmarks = calls.bookmarksGet(targetURI, credentials)
    Assert.assertEquals(expectedBookmarks, receivedBookmarks)
  }

  private fun httpOKOfData(
    objectMapper: ObjectMapper,
    jsonText: String
  ): HTTPResultType<InputStream> {
    val node = objectMapper.readTree(jsonText)
    val data = objectMapper.writeValueAsBytes(node)
    val stream = ByteArrayInputStream(data)
    return HTTPResultOK(
      "OK",
      200,
      stream,
      data.size.toLong(),
      mutableMapOf(),
      0L
    )
  }

  class JSONParsingHTTP(private val objectMapper: ObjectMapper) : HTTPType {

    lateinit var mostRecentlyParsed: ObjectNode

    val responses: MutableMap<URI, HTTPResultType<*>> = mutableMapOf()

    override fun get(
      auth: OptionType<HTTPAuthType>,
      uri: URI,
      offset: Long
    ): HTTPResultType<InputStream> {
      return (this.responses[uri] as HTTPResultType<InputStream>)
    }

    override fun get(
      auth: OptionType<HTTPAuthType>?,
      uri: URI?,
      offset: Long,
      noCache: Boolean?
    ): HTTPResultType<InputStream> {
      return (this.responses[uri] as HTTPResultType<InputStream>)
    }

    override fun put(
      auth: OptionType<HTTPAuthType>,
      uri: URI
    ): HTTPResultType<InputStream> {
      return (this.responses[uri] as HTTPResultType<InputStream>)
    }

    override fun put(
      auth: OptionType<HTTPAuthType>,
      uri: URI,
      data: ByteArray,
      content_type: String
    ): HTTPResultType<InputStream> {
      this.mostRecentlyParsed = this.objectMapper.readTree(data) as ObjectNode
      return (this.responses[uri] as HTTPResultType<InputStream>)
    }

    override fun post(
      auth: OptionType<HTTPAuthType>,
      uri: URI,
      data: ByteArray,
      content_type: String
    ): HTTPResultType<InputStream> {
      this.mostRecentlyParsed = this.objectMapper.readTree(data) as ObjectNode
      return (this.responses[uri] as HTTPResultType<InputStream>)
    }

    override fun delete(
      auth: OptionType<HTTPAuthType>,
      uri: URI,
      content_type: String
    ): HTTPResultType<InputStream> {
      return (this.responses[uri] as HTTPResultType<InputStream>)
    }

    override fun head(
      auth: OptionType<HTTPAuthType>,
      uri: URI
    ): HTTPResultType<InputStream> {
      return (this.responses[uri] as HTTPResultType<InputStream>)
    }
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
          chapterProgress = null,
          bookProgress = null
        ),
      id = "https://example.com/annotations/book0",
      type = "Annotation",
      motivation = "http://librarysimplified.org/terms/annotation/idling",
      target = BookmarkAnnotationTargetNode(
        source = "urn:book0",
        selector = BookmarkAnnotationSelectorNode(
          type = "FragmentSelector",
          value = "zoom!"
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
          chapterProgress = null,
          bookProgress = null
        ),
      id = "https://example.com/annotations/book0",
      type = "Annotation",
      motivation = "http://librarysimplified.org/terms/annotation/idling",
      target = BookmarkAnnotationTargetNode(
        source = "urn:book0",
        selector = BookmarkAnnotationSelectorNode(
          type = "FragmentSelector",
          value = "zoom!"
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
                      "value" : "zoom!",
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
                      "value" : "zoom!",
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
    val http = JSONParsingHTTP(objectMapper)
    val calls = ReaderBookmarkHTTPCalls(objectMapper, http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null
      )

    val targetURI =
      URI.create("https://example.com/me/")

    http.responses[targetURI] =
      HTTPResultError<InputStream>(
        401,
        "UNAUTHORIZED",
        0L,
        mutableMapOf(),
        0L,
        ByteArrayInputStream(ByteArray(1)),
        Option.none()
      )

    this.expectedException.expect(IOException::class.java)
    calls.syncingIsEnabled(targetURI, credentials)
  }

  @Test
  fun testGetBookmarksFailure0() {
    val objectMapper = ObjectMapper()
    val http = JSONParsingHTTP(objectMapper)
    val calls = ReaderBookmarkHTTPCalls(objectMapper, http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("io7mtest"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null
      )

    val targetURI =
      URI.create("https://example.com/annotations/")

    http.responses[targetURI] =
      HTTPResultError<InputStream>(
        401,
        "UNAUTHORIZED",
        0L,
        mutableMapOf(),
        0L,
        ByteArrayInputStream(ByteArray(1)),
        Option.none()
      )

    this.expectedException.expect(IOException::class.java)
    calls.bookmarksGet(targetURI, credentials)
  }

  @Test
  fun testAddBookmarksFailure0() {
    val objectMapper = ObjectMapper()
    val http = JSONParsingHTTP(objectMapper)
    val calls = ReaderBookmarkHTTPCalls(objectMapper, http)

    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("io7mtest"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null
      )

    val targetURI =
      URI.create("https://example.com/annotations/")

    http.responses[targetURI] =
      HTTPResultError<InputStream>(
        401,
        "UNAUTHORIZED",
        0L,
        mutableMapOf(),
        0L,
        ByteArrayInputStream(ByteArray(1)),
        Option.none()
      )

    this.expectedException.expect(IOException::class.java)
    calls.bookmarkAdd(targetURI, credentials, this.bookmark0)
  }
}
