package org.nypl.simplified.tests.books.controller

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import junit.framework.Assert
import org.joda.time.LocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mockito
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.BookDatabaseType
import org.nypl.simplified.books.book_database.BookEvent
import org.nypl.simplified.books.book_database.BookFormat
import org.nypl.simplified.books.book_database.BookFormats
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.bundled_content.BundledContentResolverType
import org.nypl.simplified.books.controller.BookmarksControllerType.Bookmarks
import org.nypl.simplified.books.controller.BookmarksLoadTask
import org.nypl.simplified.books.controller.BookmarksUpdateTask
import org.nypl.simplified.books.feeds.FeedHTTPTransport
import org.nypl.simplified.books.feeds.FeedLoader
import org.nypl.simplified.books.feeds.FeedLoaderType
import org.nypl.simplified.books.reader.ReaderBookLocation
import org.nypl.simplified.books.reader.ReaderBookmark
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.tests.http.MockingHTTP
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.Executors

open class BookmarksLoadTaskContract {

  @JvmField
  @Rule
  val expected = ExpectedException.none()

  /**
   * Trying to update bookmarks for a format that doesn't support bookmarks silently fails.
   */

  @Test(timeout = 5_000L)
  fun testLoadWrongFormat() {

    val entry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val books =
      Mockito.mock(BookDatabaseType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)

    val bookID =
      BookID.create("x")

    Mockito.`when`(account.bookDatabase())
      .thenReturn(books)
    Mockito.`when`(books.entry(bookID))
      .thenReturn(entry)
    Mockito.`when`(entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java))
      .thenReturn(null)

    val task =
      BookmarksLoadTask(
        account = account,
        bookID = bookID)

    val result = task.call()
    Assert.assertEquals(null, result.lastRead)
    Assert.assertEquals(listOf<ReaderBookmark>(), result.bookmarks)
  }

  /**
   * Trying to load bookmarks for an EPUB works.
   */

  @Test(timeout = 5_000L)
  fun testLoadEPUB() {

    val handle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    val entry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val books =
      Mockito.mock(BookDatabaseType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)

    val bookID =
      BookID.create("x")

    val bookmark0 =
      ReaderBookmark(
        book = bookID,
        location = ReaderBookLocation.create(Option.some("xyz"), "abc"),
        time = LocalDateTime.now(),
        chapterTitle = "A title",
        chapterProgress = 0.5,
        bookProgress = 0.25,
        deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

    val bookmark1 =
      ReaderBookmark(
        book = bookID,
        location = ReaderBookLocation.create(Option.some("xyz"), "abc"),
        time = LocalDateTime.now(),
        chapterTitle = "A title",
        chapterProgress = 0.6,
        bookProgress = 0.25,
        deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

    val bookmark2 =
      ReaderBookmark(
        book = bookID,
        location = ReaderBookLocation.create(Option.some("xyz"), "abc"),
        time = LocalDateTime.now(),
        chapterTitle = "A title",
        chapterProgress = 0.7,
        bookProgress = 0.25,
        deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

    val bookmarks0 =
      listOf(bookmark0, bookmark1, bookmark2)

    val format =
      BookFormat.BookFormatEPUB(
        adobeRights = null,
        adobeRightsFile = null,
        file = null,
        lastReadLocation = bookmark0,
        bookmarks = bookmarks0)

    Mockito.`when`(account.bookDatabase())
      .thenReturn(books)
    Mockito.`when`(books.entry(bookID))
      .thenReturn(entry)
    Mockito.`when`(entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java))
      .thenReturn(handle)
    Mockito.`when`(handle.format)
      .thenReturn(format)

    val task =
      BookmarksLoadTask(
        account = account,
        bookID = bookID)

    val result = task.call()
    Assert.assertEquals(bookmark0, result.lastRead)
    Assert.assertEquals(bookmarks0, result.bookmarks)
  }
}