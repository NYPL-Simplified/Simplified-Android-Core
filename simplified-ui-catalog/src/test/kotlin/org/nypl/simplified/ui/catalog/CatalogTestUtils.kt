package org.nypl.simplified.ui.catalog

import one.irradia.mime.api.MIMEType
import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import java.net.URI

object CatalogTestUtils {
  /*
  * We can't easily mock parts of FeedEntries (BookFormatDefinition) due to
  * https://github.com/mockk/mockk/issues/473
  * so here's a quick way to get a relatively-useable epub FeedEntry
  */
  fun buildTestFeedEntryOPDS(): FeedEntry.FeedEntryOPDS {
    val entry = FeedEntry.FeedEntryOPDS(
      AccountID.generate(),
      OPDSAcquisitionFeedEntry.newBuilder(
        "in_id",
        "in_title",
        DateTime.now(),
        OPDSAvailabilityLoanable.get(),
      )
        .addAcquisition(
          OPDSAcquisition(
            OPDSAcquisition.Relation.ACQUISITION_BORROW,
            URI.create("some-uri"),
            MIMEType(
              "application",
              "epub+zip",
              emptyMap()
            ),
            emptyList()
          )
        )
        .build()
    )
    return entry
  }
}
