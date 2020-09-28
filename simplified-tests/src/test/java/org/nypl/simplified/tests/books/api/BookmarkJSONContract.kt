package org.nypl.simplified.tests.books.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.api.BookmarkJSON
import org.nypl.simplified.books.api.BookmarkKind

abstract class BookmarkJSONContract {

  /**
   * Deserialize JSON representing a bookmark with a top-level chapterProgress prooperty. Older
   * bookmarks had this structure. The top-level chapterProgress should be deserialized into
   * location.progress.chapterProgress.
   */
  @Test
  fun testDeserializeJSONWithTopLevelChapterProgress() {
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = ObjectMapper(),
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

    Assert.assertEquals(0.4736842215061188, bookmark.location.progress!!.chapterProgress, .0001)
  }

  /**
   * Deserialize JSON representing a bookmark with chapterProgress nested in location.progress.
   */
  @Test
  fun testDeserializeJSONWithNestedChapterProgress() {
    val bookmark = BookmarkJSON.deserializeFromString(
      objectMapper = ObjectMapper(),
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

    Assert.assertEquals(9, bookmark.location.progress!!.chapterIndex)
    Assert.assertEquals(0.4285714328289032, bookmark.location.progress!!.chapterProgress, .0001)
  }
}
