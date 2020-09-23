package org.nypl.simplified.tests.books.borrowing

import okhttp3.mockwebserver.MockWebServer
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import java.net.URI

object BorrowTests {

  fun opdsLoanedFeedEntryOfType(
    webServer: MockWebServer,
    mime: String
  ): OPDSAcquisitionFeedEntry {
    val parsedEntry = this.opdsLoanedFeedEntryOf(
      """
    <entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
      <title>Example</title>
      <updated>2020-09-17T16:48:51+0000</updated>
      <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
      <link href="${webServer.url("/next")}" rel="http://opds-spec.org/acquisition" type="$mime">
        <opds:availability since="2020-09-17T16:48:51+0000" status="available" until="2020-09-17T16:48:51+0000" />
        <opds:holds total="0" />
        <opds:copies available="5" total="5" />
      </link>
    </entry>
    """
    )
    check(parsedEntry.availability is OPDSAvailabilityLoaned) { "Feed entry must be Loanable" }
    return parsedEntry
  }

  fun opdsLoanedFeedEntryOf(text: String): OPDSAcquisitionFeedEntry {
    return OPDSAcquisitionFeedEntryParser.newParser()
      .parseEntryStream(URI.create("urn:stdin"), text.byteInputStream())
  }
}
