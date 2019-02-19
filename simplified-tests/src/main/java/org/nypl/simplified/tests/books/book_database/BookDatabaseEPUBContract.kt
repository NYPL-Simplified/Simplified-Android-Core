package org.nypl.simplified.tests.books.book_database

import android.content.Context
import com.io7m.jfunctional.Option
import org.joda.time.LocalDateTime
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.book_database.BookDatabase
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_database.BookIDs
import org.nypl.simplified.books.reader.ReaderBookLocation
import org.nypl.simplified.books.reader.ReaderBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkKind
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkKind.*
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.opds.core.OPDSJSONSerializer
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Calendar

abstract class BookDatabaseEPUBContract {

  private val logger = LoggerFactory.getLogger(BookDatabaseEPUBContract::class.java)

  protected abstract fun context(): Context

  /**
   * Creating a book database entry for a feed that contains an EPUB acquisition results in an
   * EPUB format. Reopening the database shows that the data is preserved.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryLastReadLocation() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, AccountID.create(1), directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithEPUB()
    val bookID = BookIDs.newFromText("abcd")
    val databaseEntry0 = database0.createOrUpdate(bookID, feedEntry)

    this.run {
      val formatHandle =
        databaseEntry0.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      Assert.assertTrue(
        "Format is present", formatHandle != null)

      formatHandle!!
      Assert.assertEquals(null, formatHandle.format.lastReadLocation)

      val bookmark =
        ReaderBookmark(
          opdsId = "abcd",
          location = ReaderBookLocation.create(Option.some("xyz"), "abc"),
          time = LocalDateTime.now(),
          chapterTitle = "A title",
          kind = ReaderBookmarkExplicit,
          chapterProgress = 0.5,
          bookProgress = 0.25,
          uri = null,
          deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

      formatHandle.setLastReadLocation(bookmark)
      Assert.assertEquals(bookmark, formatHandle.format.lastReadLocation)

      formatHandle.setLastReadLocation(null)
      Assert.assertEquals(null, formatHandle.format.lastReadLocation)
    }
  }

  /**
   * Saving and restoring bookmarks works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testBookmarks() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 =
      BookDatabase.open(context(), parser, serializer, AccountID.create(1), directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithEPUB()
    val bookID = BookIDs.newFromText("abcd")
    val databaseEntry0 = database0.createOrUpdate(bookID, feedEntry)

    val bookmark0 =
      ReaderBookmark(
        opdsId = "abcd",
        location = ReaderBookLocation.create(Option.some("xyz"), "abc"),
        time = LocalDateTime.now(),
        kind = ReaderBookmarkExplicit,
        chapterTitle = "A title",
        chapterProgress = 0.5,
        bookProgress = 0.25,
        uri = null,
        deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

    val bookmark1 =
      ReaderBookmark(
        opdsId = "abcd",
        location = ReaderBookLocation.create(Option.some("xyz"), "abc"),
        time = LocalDateTime.now(),
        kind = ReaderBookmarkExplicit,
        chapterTitle = "A title",
        chapterProgress = 0.6,
        bookProgress = 0.25,
        uri = null,
        deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

    val bookmark2 =
      ReaderBookmark(
        opdsId = "abcd",
        location = ReaderBookLocation.create(Option.some("xyz"), "abc"),
        time = LocalDateTime.now(),
        kind = ReaderBookmarkExplicit,
        chapterTitle = "A title",
        chapterProgress = 0.7,
        bookProgress = 0.25,
        uri = null,
        deviceID = "3475fa24-25ca-4ddb-9d7b-762358d5f83a")

    val bookmarks0 = listOf(bookmark0)
    val bookmarks1 = listOf(bookmark0, bookmark1, bookmark2)

    this.run {
      val formatHandle =
        databaseEntry0.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      Assert.assertTrue(
        "Format is present", formatHandle != null)

      formatHandle!!
      Assert.assertEquals(listOf<ReaderBookmark>(), formatHandle.format.bookmarks)

      formatHandle.setBookmarks(bookmarks0)
      Assert.assertEquals(bookmarks0, formatHandle.format.bookmarks)

      formatHandle.setBookmarks(bookmarks1)
      Assert.assertEquals(bookmarks1, formatHandle.format.bookmarks)
    }

    val database1 =
      BookDatabase.open(context(), parser, serializer, AccountID.create(1), directory)
    val databaseEntry1 =
      database1.createOrUpdate(bookID, feedEntry)

    this.run {
      val formatHandle =
        databaseEntry1.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      Assert.assertTrue(
        "Format is present", formatHandle != null)

      formatHandle!!
      Assert.assertEquals(bookmarks1, formatHandle.format.bookmarks)
    }
  }

  private fun acquisitionFeedEntryWithEPUB(): OPDSAcquisitionFeedEntry {
    val revoke = Option.none<URI>()
    val eb = OPDSAcquisitionFeedEntry.newBuilder(
      "abcd",
      "Title",
      Calendar.getInstance(),
      OPDSAvailabilityOpenAccess.get(revoke))

    eb.addAcquisition(
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://example.com"),
        Option.some("application/epub+zip"),
        emptyList()))
    return eb.build()
  }
}
