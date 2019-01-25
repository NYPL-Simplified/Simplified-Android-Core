package org.nypl.simplified.tests.books.book_database

import android.content.Context
import com.io7m.jfunctional.Option
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.book_database.BookDatabase
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.reader.ReaderBookLocation
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
    val bookID = BookID.create("abcd")
    val databaseEntry0 = database0.createOrUpdate(bookID, feedEntry)

    this.run {
      val formatHandle0 =
        databaseEntry0.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

      Assert.assertTrue(
        "Format is present", formatHandle0 != null)

      formatHandle0!!
      Assert.assertEquals(null, formatHandle0.format.lastReadLocation)

      val location =
        ReaderBookLocation.create(Option.some("xyz"), "abc")

      formatHandle0.setLastReadLocation(location)
      Assert.assertEquals(location, formatHandle0.format.lastReadLocation)
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
