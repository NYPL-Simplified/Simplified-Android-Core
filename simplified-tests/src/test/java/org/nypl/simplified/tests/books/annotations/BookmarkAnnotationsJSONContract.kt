package org.nypl.simplified.tests.books.annotations

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.reader.bookmarks.BookmarkAnnotationsJSON
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotation
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationBodyNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationFirstNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationResponse
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationSelectorNode
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotationTargetNode

abstract class BookmarkAnnotationsJSONContract {

  private val mapper = ObjectMapper()

  private val bookmarkBody0 =
    BookmarkAnnotationBodyNode(
      timestamp = "2019-01-25T20:00:37+0000",
      device = "cca80416-3168-4e58-b621-7964b9265ac9",
      chapterProgress = 25.0f,
      chapterTitle = "A Title",
      bookProgress = 50.0f
    )

  private val bookmarkBody1 =
    BookmarkAnnotationBodyNode(
      timestamp = "2019-01-25T20:00:37+0000",
      device = "cca80416-3168-4e58-b621-7964b9265ac9",
      chapterProgress = 10.0f,
      chapterTitle = "A Title",
      bookProgress = 50.0f
    )

  private val bookmarkBody2 =
    BookmarkAnnotationBodyNode(
      timestamp = "2019-01-25T20:00:37+0000",
      device = "cca80416-3168-4e58-b621-7964b9265ac9",
      chapterProgress = 50.0f,
      chapterTitle = "A Title",
      bookProgress = 50.0f
    )

  private val bookmarkBodyBadDate =
    BookmarkAnnotationBodyNode(
      timestamp = "2019-01-25T20:00:37Z",
      device = "cca80416-3168-4e58-b621-7964b9265ac9",
      chapterProgress = 25.0f,
      chapterTitle = "A Title",
      bookProgress = 50.0f
    )

  private val bookmark0 =
    BookmarkAnnotation(
      context = "http://www.w3.org/ns/anno.jsonld",
      body = bookmarkBody0,
      id = "x",
      type = "Annotation",
      motivation = "http://www.w3.org/ns/oa#bookmarking",
      target = BookmarkAnnotationTargetNode(
        "z0",
        BookmarkAnnotationSelectorNode("x0", "y0")
      )
    )

  private val bookmark1 =
    BookmarkAnnotation(
      context = "http://www.w3.org/ns/anno.jsonld",
      body = bookmarkBody1,
      id = "x",
      type = "Annotation",
      motivation = "http://www.w3.org/ns/oa#bookmarking",
      target = BookmarkAnnotationTargetNode(
        "z1",
        BookmarkAnnotationSelectorNode("x1", "y1")
      )
    )

  private val bookmark2 =
    BookmarkAnnotation(
      context = "http://www.w3.org/ns/anno.jsonld",
      body = bookmarkBody2,
      id = "x",
      type = "Annotation",
      motivation = "http://www.w3.org/ns/oa#bookmarking",
      target = BookmarkAnnotationTargetNode(
        "z2",
        BookmarkAnnotationSelectorNode("x2", "y2")
      )
    )

  private val bookmarkAnnotationResponse =
    BookmarkAnnotationResponse(
      context = listOf("c0", "c1", "c2"),
      total = 20,
      type = listOf("t0", "t1", "t2"),
      id = "id0",
      first = BookmarkAnnotationFirstNode(
        items = listOf(bookmark0, bookmark1, bookmark2),
        type = "Annotation",
        id = "id"
      )
    )

  @Test
  fun testSelector() {
    val input =
      BookmarkAnnotationSelectorNode("x", "y")
    val node =
      BookmarkAnnotationsJSON.serializeSelectorNodeToJSON(mapper, input)

    Assert.assertEquals("x", node["type"].textValue())
    Assert.assertEquals("y", node["value"].textValue())

    Assert.assertEquals(input, BookmarkAnnotationsJSON.deserializeSelectorNodeFromJSON(node))
  }

  @Test
  fun testTarget() {
    val input =
      BookmarkAnnotationTargetNode("z", BookmarkAnnotationSelectorNode("x", "y"))
    val node =
      BookmarkAnnotationsJSON.serializeTargetNodeToJSON(mapper, input)

    Assert.assertEquals("z", node["source"].textValue())
    Assert.assertEquals("x", node["selector"]["type"].textValue())
    Assert.assertEquals("y", node["selector"]["value"].textValue())

    Assert.assertEquals(input, BookmarkAnnotationsJSON.deserializeTargetNodeFromJSON(node))
  }

  @Test
  fun testBody() {
    val node =
      BookmarkAnnotationsJSON.serializeBodyNodeToJSON(mapper, bookmarkBody0)

    Assert.assertEquals(
      "2019-01-25T20:00:37+0000",
      node["http://librarysimplified.org/terms/time"].textValue()
    )
    Assert.assertEquals(
      "cca80416-3168-4e58-b621-7964b9265ac9",
      node["http://librarysimplified.org/terms/device"].textValue()
    )
    Assert.assertEquals(
      25.0,
      node["http://librarysimplified.org/terms/progressWithinChapter"].doubleValue(),
      0.0
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

    Assert.assertEquals(bookmarkBody0, BookmarkAnnotationsJSON.deserializeBodyNodeFromJSON(node))
  }

  @Test
  fun testBookmark() {
    val target =
      BookmarkAnnotationTargetNode("z", BookmarkAnnotationSelectorNode("x", "y"))

    val input =
      BookmarkAnnotation(
        context = "http://www.w3.org/ns/anno.jsonld",
        body = bookmarkBody0,
        id = "x",
        type = "Annotation",
        motivation = "http://www.w3.org/ns/oa#bookmarking",
        target = target
      )

    val node =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationToJSON(mapper, input)

    Assert.assertEquals(input, BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(node))
  }

  @Test
  fun testBookmarkAnnotationFirstNode() {
    val input =
      BookmarkAnnotationFirstNode(
        type = "x",
        id = "z",
        items = listOf(bookmark0, bookmark1, bookmark2)
      )

    val node =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationFirstNodeToJSON(mapper, input)

    Assert.assertEquals(
      input,
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFirstNodeFromJSON(node)
    )
  }

  @Test
  fun testBookmarkAnnotationResponse() {
    val node =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationResponseToJSON(
        mapper, bookmarkAnnotationResponse
      )

    Assert.assertEquals(
      bookmarkAnnotationResponse,
      BookmarkAnnotationsJSON.deserializeBookmarkAnnotationResponseFromJSON(node)
    )
  }

  @Test
  fun testBookmarkBadDateSIMPLY_1938() {
    val target =
      BookmarkAnnotationTargetNode("z", BookmarkAnnotationSelectorNode("x", "y"))

    val input =
      BookmarkAnnotation(
        context = "http://www.w3.org/ns/anno.jsonld",
        body = bookmarkBodyBadDate,
        id = "x",
        type = "Annotation",
        motivation = "http://www.w3.org/ns/oa#bookmarking",
        target = target
      )

    val node =
      BookmarkAnnotationsJSON.serializeBookmarkAnnotationToJSON(mapper, input)

    Assert.assertEquals(input, BookmarkAnnotationsJSON.deserializeBookmarkAnnotationFromJSON(node))
  }
}
