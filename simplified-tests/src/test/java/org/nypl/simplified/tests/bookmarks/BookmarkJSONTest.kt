package org.nypl.simplified.tests.bookmarks

import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nypl.simplified.books.api.BookLocation
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkJSON
import org.nypl.simplified.books.api.BookmarkKind
import org.nypl.simplified.json.core.JSONParseException
import java.io.FileNotFoundException
import java.io.InputStream

class BookmarkJSONTest {

  private lateinit var objectMapper: ObjectMapper
  private lateinit var formatter: DateTimeFormatter

  @BeforeEach
  fun testSetup() {
    this.objectMapper = ObjectMapper()
    this.formatter = ISODateTimeFormat.dateTime().withZoneUTC()
  }

  @AfterEach
  fun tearDown() {
    DateTimeZone.setDefault(DateTimeZone.getDefault())
  }

  /**
   * Deserialize JSON representing a bookmark with a top-level chapterProgress property. Older
   * bookmarks had this structure. The top-level chapterProgress should be deserialized into
   * location.progress.chapterProgress.
   */

  @Test
  fun testDeserializeJSONWithTopLevelChapterProgress() {
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = """
        {
          "opdsId" : "urn:isbn:9781683609438",
          "location" : {
            "contentCFI" : "/4/2[is-that-you-walt-whitman]/4[is-that-you-walt-whitman-text]/78/1:287",
            "idref" : "is-that-you-walt-whitman-xhtml"
          },
          "time" : "2020-09-16T14:51:46.238",
          "chapterTitle" : "Is That You, Walt Whitman?",
          "chapterProgress" : 0.4736842215061188,
          "bookProgress" : 0.49,
          "deviceID" : "null"
        }
      """
    )

    assertEquals(0.4736842215061188, bookmark.chapterProgress, .0001)

    this.checkRoundTrip(bookmark)
  }

  /**
   * Deserialize JSON representing a bookmark with chapterProgress nested in location.progress.
   */

  @Test
  fun testDeserializeJSONWithNestedChapterProgress() {
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = """
        {
          "opdsId" : "urn:isbn:9781683601111",
          "location" : {
            "contentCFI" : "/4/2[the-end-of-coney-island-avenue]/4[the-end-of-coney-island-avenue-text]/84/1:325",
            "idref" : "the-end-of-coney-island-avenue-xhtml",
            "progress" : {
              "chapterIndex" : 9,
              "chapterProgress" : 0.4285714328289032
            }
          },
          "time" : "2020-09-16T19:07:21.455",
          "chapterTitle" : "The End of Coney Island Avenue",
          "chapterProgress" : 0.4285714328289032,
          "bookProgress" : 0.34,
          "deviceID" : "null"
        }
      """
    )

    assertEquals(0.4285714328289032, bookmark.chapterProgress, .0001)

    this.checkRoundTrip(bookmark)
  }

  @Test
  fun testBookmark20210317_r1_0() {
    DateTimeZone.setDefault(DateTimeZone.getDefault())

    val text = this.resourceText("bookmark-20210317-r1-0.json")
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = text
    )

    assertEquals("2021-01-21T19:16:54.066Z", this.formatter.print(bookmark.time))
    assertEquals("urn:isbn:9781683607144", bookmark.opdsId)
    assertEquals("A title!", bookmark.chapterTitle)
    assertEquals("fc4f5d19-43a2-4181-99a0-7579e0a4935b", bookmark.deviceID)
    assertEquals(BookmarkKind.ReaderBookmarkExplicit, bookmark.kind)

    val location = bookmark.location as BookLocation.BookLocationR1
    assertEquals("/4/2[title-page]/2/2/1:0", location.contentCFI)
    assertEquals("title-page-xhtml", location.idRef)
    assertEquals(0.25, location.progress)

    this.checkRoundTrip(bookmark)
  }

  @Test
  fun testBookmark20210317_r2_0() {
    DateTimeZone.setDefault(DateTimeZone.getDefault())

    val text = this.resourceText("bookmark-20210317-r2-0.json")
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = text
    )

    assertEquals("2021-01-21T19:16:54.066Z", this.formatter.print(bookmark.time))
    assertEquals("urn:isbn:9781683607144", bookmark.opdsId)
    assertEquals("Another title", bookmark.chapterTitle)
    assertEquals("null", bookmark.deviceID)
    assertEquals(BookmarkKind.ReaderBookmarkExplicit, bookmark.kind)

    val location = bookmark.location as BookLocation.BookLocationR2
    assertEquals(0.25, location.progress.chapterProgress, 0.0)
    assertEquals("/title-page.xhtml", location.progress.chapterHref)

    this.checkRoundTrip(bookmark)
  }

  @Test
  fun testBookmarkLegacyR1_0() {
    DateTimeZone.setDefault(DateTimeZone.getDefault())

    val text = this.resourceText("bookmark-legacy-r1-0.json")
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = text
    )

    assertEquals("2021-01-21T19:16:54.066Z", this.formatter.print(bookmark.time))
    assertEquals("urn:isbn:9781683607144", bookmark.opdsId)
    assertEquals("Some title", bookmark.chapterTitle)
    assertEquals("70c47074-c048-48c0-8eae-286b9738c108", bookmark.deviceID)
    assertEquals(BookmarkKind.ReaderBookmarkExplicit, bookmark.kind)

    val location = bookmark.location as BookLocation.BookLocationR1
    assertEquals("/4/2[title-page]/2/2/1:0", location.contentCFI)
    assertEquals("title-page-xhtml", location.idRef)
    assertEquals(0.30, location.progress)

    this.checkRoundTrip(bookmark)
  }

  @Test
  fun testBookmarkLegacyR1_1() {
    DateTimeZone.setDefault(DateTimeZone.getDefault())

    val text = this.resourceText("bookmark-legacy-r1-1.json")
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = text
    )

    assertEquals("2021-03-17T15:19:56.465Z", this.formatter.print(bookmark.time))
    assertEquals("urn:isbn:9781683606123", bookmark.opdsId)
    assertEquals("Unknown", bookmark.chapterTitle)
    assertEquals("null", bookmark.deviceID)
    assertEquals(BookmarkKind.ReaderBookmarkExplicit, bookmark.kind)

    val location = bookmark.location as BookLocation.BookLocationR1
    assertEquals("/4/2[cover-image]/2", location.contentCFI)
    assertEquals("Cover", location.idRef)
    assertEquals(0.3, location.progress)

    this.checkRoundTrip(bookmark)
  }

  private fun checkRoundTrip(bookmark: Bookmark) {
    val serializedText =
      BookmarkJSON.serializeToString(this.objectMapper, bookmark)
    val serialized =
      BookmarkJSON.deserializeFromString(
        objectMapper = this.objectMapper,
        kind = bookmark.kind,
        serialized = serializedText
      )
    assertEquals(bookmark, serialized)
  }

  @Test
  fun testBookmarkLegacyR2_0() {
    DateTimeZone.setDefault(DateTimeZone.getDefault())

    val text = this.resourceText("bookmark-legacy-r2-0.json")

    val ex = assertThrows(JSONParseException::class.java) {
      BookmarkJSON.deserializeFromString(
        objectMapper = this.objectMapper,
        kind = BookmarkKind.ReaderBookmarkExplicit,
        serialized = text
      )
    }
    assertTrue(ex.message!!.contains("Unsupported book location format version: (unspecified)"))
  }

  private fun resourceText(
    name: String
  ): String {
    return this.resource(name).readBytes().decodeToString()
  }

  private fun resource(
    name: String
  ): InputStream {
    val fileName =
      "/org/nypl/simplified/tests/bookmarks/$name"
    val url =
      BookmarkAnnotationsJSONTest::class.java.getResource(fileName)
        ?: throw FileNotFoundException("No such resource: $fileName")
    return url.openStream()
  }

  @Test
  fun testBookmark20210317_r1_0_UTC() {
    DateTimeZone.setDefault(DateTimeZone.UTC)

    val text = this.resourceText("bookmark-20210317-r1-0.json")
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = text
    )

    assertEquals("2021-01-21T19:16:54.066Z", this.formatter.print(bookmark.time))
    assertEquals("urn:isbn:9781683607144", bookmark.opdsId)
    assertEquals("A title!", bookmark.chapterTitle)
    assertEquals("fc4f5d19-43a2-4181-99a0-7579e0a4935b", bookmark.deviceID)
    assertEquals(BookmarkKind.ReaderBookmarkExplicit, bookmark.kind)

    val location = bookmark.location as BookLocation.BookLocationR1
    assertEquals("/4/2[title-page]/2/2/1:0", location.contentCFI)
    assertEquals("title-page-xhtml", location.idRef)
    assertEquals(0.25, location.progress)

    this.checkRoundTrip(bookmark)
  }

  @Test
  fun testBookmark20210317_r2_0_UTC() {
    DateTimeZone.setDefault(DateTimeZone.UTC)

    val text = this.resourceText("bookmark-20210317-r2-0.json")
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = text
    )

    assertEquals("2021-01-21T19:16:54.066Z", this.formatter.print(bookmark.time))
    assertEquals("urn:isbn:9781683607144", bookmark.opdsId)
    assertEquals("Another title", bookmark.chapterTitle)
    assertEquals("null", bookmark.deviceID)
    assertEquals(BookmarkKind.ReaderBookmarkExplicit, bookmark.kind)

    val location = bookmark.location as BookLocation.BookLocationR2
    assertEquals(0.25, location.progress.chapterProgress, 0.0)
    assertEquals("/title-page.xhtml", location.progress.chapterHref)

    this.checkRoundTrip(bookmark)
  }

  @Test
  fun testBookmarkLegacyR1_0_UTC() {
    DateTimeZone.setDefault(DateTimeZone.UTC)

    val text = this.resourceText("bookmark-legacy-r1-0.json")
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = text
    )

    assertEquals("2021-01-21T19:16:54.066Z", this.formatter.print(bookmark.time))
    assertEquals("urn:isbn:9781683607144", bookmark.opdsId)
    assertEquals("Some title", bookmark.chapterTitle)
    assertEquals("70c47074-c048-48c0-8eae-286b9738c108", bookmark.deviceID)
    assertEquals(BookmarkKind.ReaderBookmarkExplicit, bookmark.kind)

    val location = bookmark.location as BookLocation.BookLocationR1
    assertEquals("/4/2[title-page]/2/2/1:0", location.contentCFI)
    assertEquals("title-page-xhtml", location.idRef)
    assertEquals(0.30, location.progress)

    this.checkRoundTrip(bookmark)
  }

  @Test
  fun testBookmarkLegacyR1_1_UTC() {
    DateTimeZone.setDefault(DateTimeZone.UTC)

    val text = this.resourceText("bookmark-legacy-r1-1.json")
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = this.objectMapper,
      kind = BookmarkKind.ReaderBookmarkExplicit,
      serialized = text
    )

    assertEquals("2021-03-17T15:19:56.465Z", this.formatter.print(bookmark.time))
    assertEquals("urn:isbn:9781683606123", bookmark.opdsId)
    assertEquals("Unknown", bookmark.chapterTitle)
    assertEquals("null", bookmark.deviceID)
    assertEquals(BookmarkKind.ReaderBookmarkExplicit, bookmark.kind)

    val location = bookmark.location as BookLocation.BookLocationR1
    assertEquals("/4/2[cover-image]/2", location.contentCFI)
    assertEquals("Cover", location.idRef)
    assertEquals(0.3, location.progress)

    this.checkRoundTrip(bookmark)
  }
}
