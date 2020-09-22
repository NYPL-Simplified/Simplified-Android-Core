package org.nypl.simplified.tests.books.book_database

import android.content.Context
import com.io7m.jfunctional.Option
import one.irradia.mime.vanilla.MIMEParser
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.BookDRMInformationHandleACS
import org.nypl.simplified.books.book_database.BookDRMInformationHandleLCP
import org.nypl.simplified.books.book_database.BookDRMInformationHandleNone
import org.nypl.simplified.books.book_database.BookDatabase
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
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
    val bookDatabase = BookDatabase.open(context(), parser, serializer, accountID, directory)

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

  /**
   * Setting and unsetting DRM works.
   *
   * @throws Exception On errors
   */

  @Test
  fun testEntryDRM() {
    val parser = OPDSJSONParser.newParser()
    val serializer = OPDSJSONSerializer.newSerializer()
    val directory = DirectoryUtilities.directoryCreateTemporary()
    val database0 = BookDatabase.open(context(), parser, serializer, accountID, directory)

    val feedEntry: OPDSAcquisitionFeedEntry = this.acquisitionFeedEntryWithPDF()
    val bookID = BookIDs.newFromText("abcd")
    val entry = database0.createOrUpdate(bookID, feedEntry)

    this.run {
      val formatHandle = entry.findFormatHandle(BookDatabaseEntryFormatHandlePDF::class.java)!!
      formatHandle.drmInformationHandle as BookDRMInformationHandleNone

      this.logger.debug("setting DRM to LCP")
      formatHandle.setDRMKind(BookDRMKind.LCP)
      formatHandle.drmInformationHandle as BookDRMInformationHandleLCP
      formatHandle.format.drmInformation as BookDRMInformation.LCP
      this.logger.debug("setting DRM to ACS")
      formatHandle.setDRMKind(BookDRMKind.ACS)
      formatHandle.drmInformationHandle as BookDRMInformationHandleACS
      formatHandle.format.drmInformation as BookDRMInformation.ACS
      this.logger.debug("setting DRM to NONE")
      formatHandle.setDRMKind(BookDRMKind.NONE)
      formatHandle.drmInformationHandle as BookDRMInformationHandleNone
      formatHandle.format.drmInformation as BookDRMInformation.None
    }
  }

  private fun acquisitionFeedEntryWithPDF(): OPDSAcquisitionFeedEntry {
    val revoke = Option.none<URI>()
    val eb = OPDSAcquisitionFeedEntry.newBuilder(
      "abcd",
      "Title",
      DateTime.now(),
      OPDSAvailabilityOpenAccess.get(revoke)
    )

    eb.addAcquisition(
      OPDSAcquisition(
        ACQUISITION_BORROW,
        URI.create("http://example.com"),
        Option.some(MIMEParser.parseRaisingException("application/pdf")),
        emptyList()
      )
    )
    return eb.build()
  }
}
