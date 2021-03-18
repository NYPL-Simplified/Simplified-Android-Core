package org.nypl.simplified.viewer.epub.readium2

import org.librarysimplified.r2.api.SR2BookChapter
import org.librarysimplified.r2.api.SR2BookMetadata
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Locator
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookChapterProgress
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookLocation
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkKind
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceUsableType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarks
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Functions to convert between SimplyE and SR2 bookmarks.
 */

object Reader2Bookmarks {

  private val logger =
    LoggerFactory.getLogger(Reader2Bookmarks::class.java)

  private fun loadRawBookmarks(
    bookmarkService: ReaderBookmarkServiceUsableType,
    accountID: AccountID,
    bookID: BookID
  ): ReaderBookmarks {
    return try {
      bookmarkService
        .bookmarkLoad(accountID, bookID)
        .get(10L, TimeUnit.SECONDS)
    } catch (e: Exception) {
      this.logger.error("could not load bookmarks: ", e)
      ReaderBookmarks(null, emptyList())
    }
  }

  /**
   * Load bookmarks from the given bookmark service.
   */

  fun loadBookmarks(
    bookmarkService: ReaderBookmarkServiceUsableType,
    accountID: AccountID,
    bookID: BookID,
    bookMetadata: SR2BookMetadata
  ): List<SR2Bookmark> {
    val rawBookmarks =
      this.loadRawBookmarks(
        bookmarkService = bookmarkService,
        accountID = accountID,
        bookID = bookID
      )
    val lastRead =
      rawBookmarks.lastRead?.let { this.toSR2Bookmark(bookMetadata, it) }
    val explicits =
      rawBookmarks.bookmarks.mapNotNull { this.toSR2Bookmark(bookMetadata, it) }

    val results = mutableListOf<SR2Bookmark>()
    lastRead?.let(results::add)
    results.addAll(explicits)
    return results.toList()
  }

  /**
   * Convert an SR2 bookmark to a SimplyE bookmark.
   */

  fun fromSR2Bookmark(
    bookEntry: FeedEntry.FeedEntryOPDS,
    deviceId: String,
    source: SR2Bookmark
  ): Bookmark {
    val progress = BookChapterProgress(
      chapterHref = source.locator.chapterHref,
      chapterProgress = when (val locator = source.locator) {
        is SR2Locator.SR2LocatorPercent -> locator.chapterProgress
        is SR2Locator.SR2LocatorChapterEnd -> 1.0
      }
    )

    val location =
      BookLocation.BookLocationR2(progress)

    val kind = when (source.type) {
      SR2Bookmark.Type.EXPLICIT ->
        BookmarkKind.ReaderBookmarkExplicit
      SR2Bookmark.Type.LAST_READ ->
        BookmarkKind.ReaderBookmarkLastReadLocation
    }

    return Bookmark(
      opdsId = bookEntry.feedEntry.id,
      location = location,
      time = source.date.toLocalDateTime(),
      kind = kind,
      chapterTitle = source.title,
      bookProgress = source.bookProgress,
      deviceID = deviceId,
      uri = null
    )
  }

  /**
   * Convert a SimplyE bookmark to an SR2 bookmark.
   */

  fun toSR2Bookmark(
    bookMetadata: SR2BookMetadata,
    source: Bookmark
  ): SR2Bookmark? {
    return when (val location = source.location) {
      is BookLocation.BookLocationR2 ->
        this.r2ToSR2Bookmark(source, location)
      is BookLocation.BookLocationR1 ->
        this.r1ToSR2Bookmark(bookMetadata, source, location)
    }
  }

  private fun r1ToSR2Bookmark(
    bookMetadata: SR2BookMetadata,
    source: Bookmark,
    location: BookLocation.BookLocationR1
  ): SR2Bookmark? {
    val chapter = bookMetadata.readingOrder.find { chapter -> chapter.title == location.idRef }
    if (chapter != null) {
      return toSR2Chapter(chapter, source)
    }
    return null
  }

  private fun toSR2Chapter(
    chapter: SR2BookChapter,
    source: Bookmark
  ): SR2Bookmark {
    return SR2Bookmark(
      date = source.time.toDateTime(),
      type = when (source.kind) {
        BookmarkKind.ReaderBookmarkLastReadLocation ->
          SR2Bookmark.Type.LAST_READ
        BookmarkKind.ReaderBookmarkExplicit ->
          SR2Bookmark.Type.EXPLICIT
      },
      title = source.chapterTitle,
      locator = SR2Locator.SR2LocatorPercent(
        chapterHref = chapter.chapterHref,
        chapterProgress = source.chapterProgress
      ),
      bookProgress = source.bookProgress
    )
  }

  private fun r2ToSR2Bookmark(
    source: Bookmark,
    location: BookLocation.BookLocationR2
  ): SR2Bookmark =
    SR2Bookmark(
      date = source.time.toDateTime(),
      type = when (source.kind) {
        BookmarkKind.ReaderBookmarkLastReadLocation ->
          SR2Bookmark.Type.LAST_READ
        BookmarkKind.ReaderBookmarkExplicit ->
          SR2Bookmark.Type.EXPLICIT
      },
      title = source.chapterTitle,
      locator = SR2Locator.SR2LocatorPercent(
        chapterHref = location.progress.chapterHref,
        chapterProgress = location.progress.chapterProgress
      ),
      bookProgress = source.bookProgress
    )
}
