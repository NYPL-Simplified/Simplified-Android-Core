package org.nypl.simplified.tests.bookmarks

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.joda.time.DateTimeUtils
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.books.api.BookLocation
import org.nypl.simplified.books.api.BookmarkKind
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotation
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationBodyNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationFirstNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationResponse
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationSelectorNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationTargetNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotations
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationsJSON
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.InputStream

class BookmarkAnnotationsJSONTest {

  private val logger =
    LoggerFactory.getLogger(BookmarkAnnotationsJSONTest::class.java)

  @Rule
  @JvmField
  var expectedException: ExpectedException = ExpectedException.none()

  private val objectMapper: ObjectMapper = ObjectMapper()

  private val targetValue0 =
    "{\n  \"@type\": \"LocatorHrefProgression\",\n  \"idref\": \"/0.html\",\n  \"progressWithinChapter\": 0.5\n}\n"
  private val targetValue1 =
    "{\n  \"@type\": \"LocatorHrefProgression\",\n  \"idref\": \"/1.html\",\n  \"progressWithinChapter\": 0.5\n}\n"
  private val targetValue2 =
    "{\n  \"@type\": \"LocatorHrefProgression\",\n  \"idref\": \"/2.html\",\n  \"progressWithinChapter\": 0.5\n}\n"

  private val bookmarkBody0 =
    BookmarkAnnotationBodyNode(
      timestamp = "2019-01-25T20:00:37+0000",
      device = "cca80416-3168-4e58-b621-7964b9265ac9",
      chapterTitle = "A Title",
      bookProgress = 50.0f
    )

  private val bookmarkBody1 =
    BookmarkAnnotationBodyNode(
      timestamp = "2019-01-25T20:00:37+0000",
      device = "cca80416-3168-4e58-b621-7964b9265ac9",
      chapterTitle = "A Title",
      bookProgress = 50.0f
    )

  private val bookmarkBody2 =
    BookmarkAnnotationBodyNode(
      timestamp = "2019-01-25T20:00:37+0000",
      device = "cca80416-3168-4e58-b621-7964b9265ac9",
      chapterTitle = "A Title",
      bookProgress = 50.0f
    )

  private val bookmarkBodyBadDate =
    BookmarkAnnotationBodyNode(
      timestamp = "2019-01-25T20:00:37Z",
      device = "cca80416-3168-4e58-b621-7964b9265ac9",
      chapterTitle = "A Title",
      bookProgress = 50.0f
    )

  private val bookmark0 =
    BookmarkAnnotation(
      context = "http://www.w3.org/ns/anno.jsonld",
      body = this.bookmarkBody0,
      id = "x",
      type = "Annotation",
      motivation = "http://www.w3.org/ns/oa#bookmarking",
      target = BookmarkAnnotationTargetNode(
        "z0",
        BookmarkAnnotationSelectorNode("oa:FragmentSelector", this.targetValue0)
      )
    )

  private val bookmark1 =
    BookmarkAnnotation(
      context = "http://www.w3.org/ns/anno.jsonld",
      body = this.bookmarkBody1,
      id = "x",
      type = "Annotation",
      motivation = "http://www.w3.org/ns/oa#bookmarking",
      target = BookmarkAnnotationTargetNode(
        "z1",
        BookmarkAnnotationSelectorNode("oa:FragmentSelector", this.targetValue1)
      )
    )

  private val bookmark2 =
    BookmarkAnnotation(
      context = "http://www.w3.org/ns/anno.jsonld",
      body = this.bookmarkBody2,
      id = "x",
      type = "Annotation",
      motivation = "http://www.w3.org/ns/oa#bookmarking",
      target = BookmarkAnnotationTargetNode(
        "z2",
        BookmarkAnnotationSelectorNode("oa:FragmentSelector", this.targetValue2)
      )
    )

  private val bookmarkAnnotationResponse =
    BookmarkAnnotationResponse(
      context = listOf("c0", "c1", "c2"),
      total = 20,
      type = listOf("t0", "t1", "t2"),
      id = "id0",
      first = BookmarkAnnotationFirstNode(
        items = listOf(this.bookmark0, this.bookmark1, this.bookmark2),
        type = "Annotation",
        id = "id"
      )
    )

  @Test
  fun testSelector() {
    val input =
      BookmarkAnnotationSelectorNode("oa:FragmentSelector", this.targetValue0)
    val node =
      BookmarkAnnotationsJSON.serializeSelectorNodeToJSON(this.objectMapper, input)

    Assert.assertEquals("oa:FragmentSelector", node["type"].textValue())
    Assert.assertEquals(this.targetValue0, node["value"].textValue())

    Assert.assertEquals(input, BookmarkAnnotationsJSON.deserializeSelectorNodeFromJSON(this.objectMapper, node))
  }

  @Test
  fun testTarget() {
    val input =
      BookmarkAnnotationTargetNode("z", BookmarkAnnotationSelectorNode("oa:FragmentSelector", this.targetValue0))
    val node =
      BookmarkAnnotationsJSON.serializeTargetNodeToJSON(this.objectMapper, input)

    Assert.assertEquals("z", node["source"].textValue())
    Assert.assertEquals("oa:FragmentSelector", node["selector"]["type"].textValue())
    Assert.assertEquals(this.targetValue0, node["selector"]["value"].textValue())

    Assert.assertEquals(input, BookmarkAnnotationsJSON.deserializeTargetNodeFromJSON(this.objectMapper, node))
  }

  @Test
  fun testBody() {
    val node =
      BookmarkAnnotationsJSON.serializeBodyNodeToJSON(this.objectMapper, this.bookmarkBody0)

    Assert.assertEquals(
      "2019-01-25T20:00:37+0000",
      node["http://librarysimplified.org/terms/time"].textValue()
    )
    Assert.assertEquals(
      "cca80416-3168-4e58-b621-7964b9265ac9",
      node["http://librarysimplified.org/terms/device"].textValue()
    )
    Assert.assertEquals(
      null,
      node["http://librarysimplified.org/terms/progressWithinChapter"]
    )
    Assert.assertEquals(
      50.0,
      node["http://librarysimplified.org/terms/progressWithinBook"].doubleValue(),
      0.0
    )
    Assert.assertEquals(
      "A Title",
      node["http://librarysimplified.org/terms/chapter"].textValue()
    )

    Assert.assertEquals(this.bookmarkBody0, BookmarkAnnotationsJSON.deserializeBodyNodeFromJSON(node))
  }

  @Test
  fun testBookmark() {
    val target =
      BookmarkAnnotationTargetNode("z", BookmarkAnnotationSelectorNode("oa:FragmentSelector", this.targetValue0))

    val input =
      BookmarkAnnotation(
        context = "http://www.w3.org/ns/anno.jsonld",
        body = this.bookmarkBody0,
        id = "x",
        type = "Annotation",
        motivation = "http://www.w3.org/ns/oa#bookmarking",
        target = target
      )

    val node =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationToJSON(this.objectMapper, input)

    this.compareAnnotations(input, BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(this.objectMapper, node))
  }

  @Test
  fun testBookmarkAnnotationFirstNode() {
    val input =
      BookmarkAnnotationFirstNode(
        type = "x",
        id = "z",
        items = listOf(this.bookmark0, this.bookmark1, this.bookmark2)
      )

    val node =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationFirstNodeToJSON(this.objectMapper, input)

    Assert.assertEquals(
      input,
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFirstNodeFromJSON(this.objectMapper, node)
    )
  }

  @Test
  fun testBookmarkAnnotationResponse() {
    val node =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationResponseToJSON(
        this.objectMapper, this.bookmarkAnnotationResponse
      )

    Assert.assertEquals(
      this.bookmarkAnnotationResponse,
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationResponseFromJSON(
        objectMapper = this.objectMapper,
        node = node
      )
    )
  }

  @Test
  fun testBookmarkBadDateSIMPLY_1938() {
    val target =
      BookmarkAnnotationTargetNode("z", BookmarkAnnotationSelectorNode("oa:FragmentSelector", this.targetValue0))

    val input =
      BookmarkAnnotation(
        context = "http://www.w3.org/ns/anno.jsonld",
        body = this.bookmarkBodyBadDate,
        id = "x",
        type = "Annotation",
        motivation = "http://www.w3.org/ns/oa#bookmarking",
        target = target
      )

    val node =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationToJSON(this.objectMapper, input)

    this.compareAnnotations(input, BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(this.objectMapper, node))
  }

  @Test
  fun testSpecValidBookmark0() {
    val annotation =
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(
        objectMapper = this.objectMapper,
        node = this.resourceNode("valid-bookmark-0.json")
      )

    val bookmark = BookmarkAnnotations.toBookmark(this.objectMapper, annotation)
    Assert.assertEquals("urn:uuid:1daa8de6-94e8-4711-b7d1-e43b572aa6e0", bookmark.opdsId)
    Assert.assertEquals("urn:uuid:c83db5b1-9130-4b86-93ea-634b00235c7c", bookmark.deviceID)
    Assert.assertEquals(BookmarkKind.ReaderBookmarkLastReadLocation, bookmark.kind)
    Assert.assertEquals("2021-03-12T16:32:49.000Z", bookmark.time.toString())
    Assert.assertEquals("", bookmark.chapterTitle)

    val location = bookmark.location as BookLocation.BookLocationR2
    Assert.assertEquals(0.5, location.progress.chapterProgress, 0.0)
    Assert.assertEquals("/xyz.html", location.progress.chapterHref)

    this.checkRoundTrip(annotation)
  }

  @Test
  fun testSpecValidBookmark1() {
    val annotation =
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(
        objectMapper = this.objectMapper,
        node = this.resourceNode("valid-bookmark-1.json")
      )

    DateTimeUtils.setCurrentMillisFixed(0L)

    val bookmark = BookmarkAnnotations.toBookmark(this.objectMapper, annotation)
    Assert.assertEquals("urn:uuid:1daa8de6-94e8-4711-b7d1-e43b572aa6e0", bookmark.opdsId)
    Assert.assertEquals(null, bookmark.deviceID)
    Assert.assertEquals(BookmarkKind.ReaderBookmarkLastReadLocation, bookmark.kind)
    Assert.assertEquals("1970-01-01T00:00:00.000Z", bookmark.time.toString())
    Assert.assertEquals("", bookmark.chapterTitle)

    val location = bookmark.location as BookLocation.BookLocationR2
    Assert.assertEquals(0.5, location.progress.chapterProgress, 0.0)
    Assert.assertEquals("/xyz.html", location.progress.chapterHref)

    this.checkRoundTrip(annotation)
  }

  @Test
  fun testSpecValidBookmark2() {
    val annotation =
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(
        objectMapper = this.objectMapper,
        node = this.resourceNode("valid-bookmark-2.json")
      )

    DateTimeUtils.setCurrentMillisFixed(0L)

    val bookmark = BookmarkAnnotations.toBookmark(this.objectMapper, annotation)
    Assert.assertEquals("urn:uuid:1daa8de6-94e8-4711-b7d1-e43b572aa6e0", bookmark.opdsId)
    Assert.assertEquals(null, bookmark.deviceID)
    Assert.assertEquals(BookmarkKind.ReaderBookmarkExplicit, bookmark.kind)
    Assert.assertEquals("1970-01-01T00:00:00.000Z", bookmark.time.toString())
    Assert.assertEquals("", bookmark.chapterTitle)

    val location = bookmark.location as BookLocation.BookLocationR2
    Assert.assertEquals(0.5, location.progress.chapterProgress, 0.0)
    Assert.assertEquals("/xyz.html", location.progress.chapterHref)

    this.checkRoundTrip(annotation)
  }

  @Test
  fun testSpecValidLocator0() {
    val location =
      BookmarkAnnotationsJSON.deserializeLocation(
        objectMapper = this.objectMapper,
        value = this.resourceText("valid-locator-0.json")
      )

    val locationHP = location as BookLocation.BookLocationR2
    Assert.assertEquals(0.5, locationHP.progress.chapterProgress, 0.0)
    Assert.assertEquals("/xyz.html", locationHP.progress.chapterHref)
  }

  @Test
  fun testSpecValidLocator1() {
    val location =
      BookmarkAnnotationsJSON.deserializeLocation(
        objectMapper = this.objectMapper,
        value = this.resourceText("valid-locator-1.json")
      )

    val locationR1 = location as BookLocation.BookLocationR1
    Assert.assertEquals(0.25, locationR1.progress!!, 0.0)
    Assert.assertEquals("xyz-html", locationR1.idRef)
    Assert.assertEquals("/4/2/2/2", locationR1.contentCFI)
  }

  @Test
  fun testSpecInvalidBookmark0() {
    this.expectedException.expect(JSONParseException::class.java)
    this.expectedException.expectMessage("Expected: A key 'body'")
    BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(
      objectMapper = this.objectMapper,
      node = this.resourceNode("invalid-bookmark-0.json")
    )
  }

  @Test
  fun testSpecInvalidBookmark1() {
    this.expectedException.expect(JSONParseException::class.java)
    this.expectedException.expectMessage("Expected: A key 'motivation'")
    BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(
      objectMapper = this.objectMapper,
      node = this.resourceNode("invalid-bookmark-1.json")
    )
  }

  @Test
  fun testSpecInvalidBookmark2() {
    this.expectedException.expect(JSONParseException::class.java)
    this.expectedException.expectMessage("Expected: A key 'target'")
    BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(
      objectMapper = this.objectMapper,
      node = this.resourceNode("invalid-bookmark-2.json")
    )
  }

  @Test
  fun testSpecInvalidBookmark3() {
    this.expectedException.expect(JSONParseException::class.java)
    this.expectedException.expectMessage("Unrecognized selector node type: What?")
    BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(
      objectMapper = this.objectMapper,
      node = this.resourceNode("invalid-bookmark-3.json")
    )
  }

  @Test
  fun testSpecInvalidBookmark4() {
    this.expectedException.expect(JSONParseException::class.java)
    this.expectedException.expectMessage("Unexpected character ('['")
    BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(
      objectMapper = this.objectMapper,
      node = this.resourceNode("invalid-bookmark-4.json")
    )
  }

  private fun resourceText(
    name: String
  ): String {
    return this.resource(name).readBytes().decodeToString()
  }

  private fun resourceNode(
    name: String
  ): ObjectNode {
    return this.objectMapper.readTree(this.resourceText(name)) as ObjectNode
  }

  private fun checkRoundTrip(bookmarkAnnotation: BookmarkAnnotation) {
    val serialized =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationToBytes(this.objectMapper, bookmarkAnnotation)
    val serializedText =
      serialized.decodeToString()

    this.logger.debug("serialized: {}", serializedText)

    val deserialized =
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(
        this.objectMapper,
        this.objectMapper.readTree(serialized) as ObjectNode
      )

    this.compareAnnotations(bookmarkAnnotation, deserialized)

    val toBookmark =
      BookmarkAnnotations.toBookmark(this.objectMapper, deserialized)
    val fromBookmark =
      BookmarkAnnotations.fromBookmark(this.objectMapper, toBookmark)
    val toBookmarkAgain =
      BookmarkAnnotations.toBookmark(this.objectMapper, fromBookmark)

    this.compareAnnotations(bookmarkAnnotation, deserialized)
    this.compareAnnotations(bookmarkAnnotation, fromBookmark)
    Assert.assertEquals(toBookmark, toBookmarkAgain)
  }

  private fun compareAnnotations(
    x: BookmarkAnnotation,
    y: BookmarkAnnotation
  ) {
    this.logger.debug("compareAnnotations: x: {}", x)
    this.logger.debug("compareAnnotations: y: {}", y)

    Assert.assertEquals(x.body.bookProgress, y.body.bookProgress)
    Assert.assertEquals(x.body.chapterTitle, y.body.chapterTitle)
    Assert.assertEquals(x.body.device, y.body.device)
    Assert.assertEquals(x.body.timestamp, y.body.timestamp)
    Assert.assertEquals(x.context, y.context)
    Assert.assertEquals(x.id, y.id)
    Assert.assertEquals(x.kind, y.kind)
    Assert.assertEquals(x.motivation, y.motivation)
    Assert.assertEquals(x.target.selector.type, y.target.selector.type)

    val xSelectorValue =
      BookmarkAnnotationsJSON.deserializeLocation(this.objectMapper, x.target.selector.value)
    val ySelectorValue =
      BookmarkAnnotationsJSON.deserializeLocation(this.objectMapper, y.target.selector.value)

    Assert.assertEquals(xSelectorValue, ySelectorValue)
    Assert.assertEquals(x.target.source, y.target.source)
    Assert.assertEquals(x.type, y.type)
  }

  private fun resource(
    name: String
  ): InputStream {
    val fileName =
      "/org/nypl/simplified/tests/bookmark_annotations/spec/$name"
    val url =
      BookmarkAnnotationsJSONTest::class.java.getResource(fileName)
        ?: throw FileNotFoundException("No such resource: $fileName")
    return url.openStream()
  }
}
