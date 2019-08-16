package org.nypl.simplified.tests.books.book_database

import android.content.Context
import com.io7m.jfunctional.Option
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.mime.MIMEParser
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionRelation
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.opds.core.OPDSJSONSerializer
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

abstract class BookDatabasePDFContract {

  private val logger =
    LoggerFactory.getLogger(BookDatabasePDFContract::class.java)
  private val accountID =
    AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))
  private val MIME_TYPE_PDF =
    MIMEParser.parseRaisingException("application/pdf")

  protected abstract fun context(): Context

  /**
   * Tests that saving a PDF Book's last read location can be saved and restored when a book
   * is opened again.
   *
   * @throws Exception On errors
   */
  @Test
  fun testEntryLastReadLocation() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val bookDatabase =
      org.nypl.simplified.books.book_database.BookDatabase.open(
        context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithPDF()
    val bookID = BookIDs.newFromText("abcd")

    val databaseEntry: BookDatabaseEntryType = bookDatabase.createOrUpdate(bookID, feedEntry)

    this.run {
      val formatHandle =
        databaseEntry.findFormatHandle(BookDatabaseEntryFormatHandlePDF::class.java)

      Assert.assertTrue("Format is present", formatHandle != null)

      formatHandle!!
      Assert.assertEquals(null, formatHandle.format.lastReadLocation)

      val pageNumber = 25

      formatHandle.setLastReadLocation(pageNumber)
      Assert.assertEquals(pageNumber, formatHandle.format.lastReadLocation)

      formatHandle.setLastReadLocation(null)
      Assert.assertEquals(null, formatHandle.format.lastReadLocation)
    }

  }

  private fun acquisitionFeedEntryWithPDF(): OPDSAcquisitionFeedEntry {
    val revoke = Option.none<URI>()
    val eb = OPDSAcquisitionFeedEntry.newBuilder(
      "abcd",
      "Title",
      DateTime.now(),
      OPDSAvailabilityOpenAccess.get(revoke))

    eb.addAcquisition(
      OPDSAcquisition(
        OPDSAcquisitionRelation.ACQUISITION_BORROW,
        URI.create("http://example.com"),
        Option.some(MIME_TYPE_PDF),
        emptyList()))
    return eb.build()
  }
}
