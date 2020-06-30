package org.nypl.simplified.reader.bookmarks.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.format.ISODateTimeFormat
import org.nypl.simplified.books.api.BookLocationJSON
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkKind
import java.net.URI

data class BookmarkAnnotationSelectorNode(
  val type: String,
  val value: String
)

data class BookmarkAnnotationTargetNode(
  val source: String,
  val selector: BookmarkAnnotationSelectorNode
)

data class BookmarkAnnotationBodyNode(
  val timestamp: String,
  val device: String,
  val chapterTitle: String?,
  val chapterProgress: Float?,
  val bookProgress: Float?
)

data class BookmarkAnnotation(
  val context: String?,
  val body: BookmarkAnnotationBodyNode,
  val id: String?,
  val type: String,
  val motivation: String,
  val target: BookmarkAnnotationTargetNode
) {
  override fun equals(other: Any?): Boolean {
    return this.target.selector.value == (other as BookmarkAnnotation).target.selector.value
  }

  override fun hashCode(): Int {
    return this.target.selector.value.hashCode()
  }

  val kind: BookmarkKind =
    BookmarkKind.ofMotivation(this.motivation)
}

data class BookmarkAnnotationFirstNode(
  val items: List<BookmarkAnnotation>,
  val type: String,
  val id: String
)

data class BookmarkAnnotationResponse(
  val context: List<String>,
  val total: Int,
  val type: List<String>,
  val id: String,
  val first: BookmarkAnnotationFirstNode
)

object BookmarkAnnotations {

  fun toBookmark(
    objectMapper: ObjectMapper,
    annotation: BookmarkAnnotation
  ): Bookmark {

    val locationJSON =
      BookLocationJSON.deserializeFromString(objectMapper, annotation.target.selector.value)

    return Bookmark(
      opdsId = annotation.target.source,
      location = locationJSON,
      kind = BookmarkKind.ofMotivation(annotation.motivation),
      time = ISODateTimeFormat.dateTimeParser().parseLocalDateTime(annotation.body.timestamp),
      chapterTitle = annotation.body.chapterTitle ?: "",
      bookProgress = annotation.body.bookProgress?.toDouble() ?: 0.0,
      uri = if (annotation.id != null) URI.create(annotation.id) else null,
      deviceID = annotation.body.device
    )
  }

  fun fromBookmark(
    objectMapper: ObjectMapper,
    bookmark: Bookmark
  ): BookmarkAnnotation {

    val bodyAnnotation =
      BookmarkAnnotationBodyNode(
        timestamp = bookmark.time.toString(),
        device = bookmark.deviceID ?: "null",
        chapterProgress = bookmark.chapterProgress.toFloat(),
        chapterTitle = bookmark.chapterTitle,
        bookProgress = bookmark.bookProgress.toFloat()
      )

    val locationJSON =
      BookLocationJSON.serializeToString(objectMapper, bookmark.location)

    val target =
      BookmarkAnnotationTargetNode(
        bookmark.opdsId,
        BookmarkAnnotationSelectorNode("oa:FragmentSelector", locationJSON)
      )

    return BookmarkAnnotation(
      context = "http://www.w3.org/ns/anno.jsonld",
      body = bodyAnnotation,
      id = null,
      type = "Annotation",
      motivation = bookmark.kind.motivationURI,
      target = target
    )
  }
}
