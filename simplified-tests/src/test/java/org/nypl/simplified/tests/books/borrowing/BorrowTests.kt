package org.nypl.simplified.tests.books.borrowing

import okhttp3.mockwebserver.MockWebServer
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import java.net.URI

object BorrowTests {

  fun opdsEmptyFeedEntryOfType(): OPDSAcquisitionFeedEntry {
    val parsedEntry = this.opdsFeedEntryOf(
      """
    <entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
      <title>Example</title>
      <updated>2020-09-17T16:48:51+0000</updated>
      <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
      <link href="http://www.example.com" rel="unrecognized" type="text/html"/>
    </entry>
    """
    )
    return parsedEntry
  }

  fun opdsOpenAccessFeedEntryOfType(
    webServer: MockWebServer,
    mime: String
  ): OPDSAcquisitionFeedEntry {
    val parsedEntry = this.opdsFeedEntryOf(
      """
    <entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
      <title>Example</title>
      <updated>2020-09-17T16:48:51+0000</updated>
      <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
      <link href="${webServer.url("/next")}" rel="http://opds-spec.org/acquisition/open-access" type="$mime">
        <opds:availability since="2020-09-17T16:48:51+0000" status="available" until="2020-09-17T16:48:51+0000" />
        <opds:holds total="0" />
        <opds:copies available="5" total="5" />
      </link>
    </entry>
    """
    )
    check(parsedEntry.availability is OPDSAvailabilityOpenAccess) { "Feed entry must be OpenAccess" }
    return parsedEntry
  }

  fun opdsLoanableIndirectFeedEntryOfType(
    webServer: MockWebServer,
    mime: String
  ): OPDSAcquisitionFeedEntry {
    val parsedEntry =
      this.opdsFeedEntryOf(this.opdsLoanableIndirectFeedEntryText(webServer, mime))
    check(parsedEntry.availability is OPDSAvailabilityLoanable) { "Feed entry must be Loanable" }
    return parsedEntry
  }

  fun opdsLoanableIndirectFeedEntryText(
    webServer: MockWebServer,
    mime: String
  ): String {
    return """
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
  <title>Example</title>
  <updated>2020-09-17T16:48:51+0000</updated>
  <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
  <link 
     href="${webServer.url("/next")}"
     type="application/atom+xml;relation=entry;profile=opds-catalog"
     rel="http://opds-spec.org/acquisition/borrow">
    <opds:indirectAcquisition type="$mime" />
    <opds:availability since="2020-09-17T16:48:51+0000" status="available" until="2020-09-17T16:48:51+0000" />
    <opds:holds total="0" />
    <opds:copies available="5" total="5" />
  </link>
</entry>
      """
  }

  fun opdsLoanedFeedEntryOfType(
    webServer: MockWebServer,
    mime: String
  ): OPDSAcquisitionFeedEntry {
    val parsedEntry =
      this.opdsFeedEntryOf(this.opdsLoanedTextOfType(webServer, mime))
    check(parsedEntry.availability is OPDSAvailabilityLoaned) { "Feed entry must be Loaned" }
    return parsedEntry
  }

  fun opdsLoanedTextOfType(
    webServer: MockWebServer,
    mime: String
  ): String {
    return """
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
  }

  fun opdsContentURILoanedFeedEntryOfType(
    mime: String
  ): OPDSAcquisitionFeedEntry {
    val parsedEntry = this.opdsFeedEntryOf(
      """
    <entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
      <title>Example</title>
      <updated>2020-09-17T16:48:51+0000</updated>
      <id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
      <link href="content://com.example/d1d21a41-8932-4b15-9be9-d43a0c3ea66f/book.epub" rel="http://opds-spec.org/acquisition" type="$mime">
        <opds:availability since="2020-09-17T16:48:51+0000" status="available" until="2020-09-17T16:48:51+0000" />
        <opds:holds total="0" />
        <opds:copies available="5" total="5" />
      </link>
    </entry>
    """
    )
    check(parsedEntry.availability is OPDSAvailabilityLoaned) { "Feed entry must be Loaned" }
    return parsedEntry
  }

  fun opdsFeedEntryOf(text: String): OPDSAcquisitionFeedEntry {
    return OPDSAcquisitionFeedEntryParser.newParser()
      .parseEntryStream(URI.create("urn:stdin"), text.byteInputStream())
  }
}
