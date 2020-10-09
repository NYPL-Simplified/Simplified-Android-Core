package org.nypl.simplified.tests.books.borrowing

import org.junit.Assert.assertEquals
import org.junit.Test
import org.nypl.simplified.books.formats.api.StandardFormatNames
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.tests.books.borrowing.BorrowTestFeeds.Status.LOANABLE
import org.nypl.simplified.tests.books.borrowing.BorrowTestFeeds.Status.LOANED
import java.net.URI

class BorrowTestFeedsTest {

  @Test
  fun testLoaned() {
    val requirements =
      BorrowTestFeeds.FeedRequirements(
        status = LOANED,
        base = URI.create("http://www.example.com/"),
        path = listOf(
          BorrowTestFeeds.PathElement(
            type = StandardFormatNames.opdsAcquisitionFeedEntry.fullType,
            path = "/loan"
          ),
          BorrowTestFeeds.PathElement(
            type = StandardFormatNames.adobeACSMFiles.fullType,
            path = "/acsm"
          ),
          BorrowTestFeeds.PathElement(
            type = StandardFormatNames.genericEPUBFiles.fullType,
            path = "/epub"
          )
        )
      )

    val entry = BorrowTestFeeds.feed(requirements)
    assertEquals(OPDSAvailabilityLoaned::class.java, entry.availability.javaClass)
  }

  @Test
  fun testLoanable() {
    val requirements =
      BorrowTestFeeds.FeedRequirements(
        status = LOANABLE,
        base = URI.create("http://www.example.com/"),
        path = listOf(
          BorrowTestFeeds.PathElement(
            type = StandardFormatNames.opdsAcquisitionFeedEntry.fullType,
            path = "/loan"
          ),
          BorrowTestFeeds.PathElement(
            type = StandardFormatNames.adobeACSMFiles.fullType,
            path = "/acsm"
          ),
          BorrowTestFeeds.PathElement(
            type = StandardFormatNames.genericEPUBFiles.fullType,
            path = "/epub"
          )
        )
      )

    val entry = BorrowTestFeeds.feed(requirements)
    assertEquals(OPDSAvailabilityLoanable::class.java, entry.availability.javaClass)
  }
}
