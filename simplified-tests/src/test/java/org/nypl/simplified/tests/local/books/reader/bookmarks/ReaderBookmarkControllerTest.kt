package org.nypl.simplified.tests.local.books.reader.bookmarks

import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkController
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkControllerType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCallsType
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.tests.books.reader.bookmarks.ReaderBookmarkControllerContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ReaderBookmarkControllerTest : ReaderBookmarkControllerContract() {

  override val logger: Logger = LoggerFactory.getLogger(ReaderBookmarkControllerTest::class.java)

  override fun bookmarkController(
    threads: (Runnable) -> Thread,
    events: ObservableType<ReaderBookmarkEvent>,
    httpCalls: ReaderBookmarkHTTPCallsType,
    profilesController: ProfilesControllerType): ReaderBookmarkControllerType {
    return ReaderBookmarkController.create(
      threads = threads,
      events = events,
      httpCalls = httpCalls,
      profilesController = profilesController)
  }

}
