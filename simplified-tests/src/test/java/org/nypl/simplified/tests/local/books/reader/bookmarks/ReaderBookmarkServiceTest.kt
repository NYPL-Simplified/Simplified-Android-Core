package org.nypl.simplified.tests.local.books.reader.bookmarks

import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCallsType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkService
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServiceProviderType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServiceType
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.tests.books.reader.bookmarks.ReaderBookmarkServiceContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ReaderBookmarkServiceTest : ReaderBookmarkServiceContract() {

  override val logger: Logger = LoggerFactory.getLogger(ReaderBookmarkServiceTest::class.java)

  override fun bookmarkService(
    threads: (Runnable) -> Thread,
    events: ObservableType<ReaderBookmarkEvent>,
    httpCalls: ReaderBookmarkHTTPCallsType,
    profilesController: ProfilesControllerType): ReaderBookmarkServiceType {

    return ReaderBookmarkService.createService(
      ReaderBookmarkServiceProviderType.Requirements(
        threads = threads,
        events = events,
        httpCalls = httpCalls,
        profilesController = profilesController))
  }
}
