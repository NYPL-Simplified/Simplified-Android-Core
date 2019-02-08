package org.nypl.simplified.tests.books.reader.bookmarks

import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.reader.ReaderBookmarkID
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEngineOperation
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEngineOperation.Companion.evaluateInput
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEngineState
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkInput
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkInput.Event.*
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkOutput
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkOutput.*
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkOutput.Event.*

open class ReaderBookmarkEngineContract {

  @Test
  fun testBookmarkCreatedLocal()
  {
    val bookmark0 = ReaderBookmarkID("a")

    val state =
      ReaderBookmarkEngineState.create(locallySaved = setOf())

    val result =
      evaluateInput(BookmarkLocalCreated(bookmark0))
        .evaluateFor(state)

    Assert.assertTrue(result.state.bookmarks.containsKey(bookmark0))
    Assert.assertEquals(Command.SendBookmark(bookmark0), result.outputs[0])
  }

  @Test
  fun testBookmarkCreatedLocalTwice()
  {
    val bookmark0 = ReaderBookmarkID("a")

    val state =
      ReaderBookmarkEngineState.create(locallySaved = setOf())

    val result =
      evaluateInput(BookmarkLocalCreated(bookmark0))
        .andThen(evaluateInput(BookmarkLocalCreated(bookmark0)))
        .evaluateFor(state)

    Assert.assertTrue(result.state.bookmarks.containsKey(bookmark0))
    Assert.assertEquals(Command.SendBookmark(bookmark0), result.outputs[0])
    Assert.assertEquals(Event.LocalBookmarkAlreadyExists(bookmark0), result.outputs[1])
  }

}