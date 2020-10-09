package org.nypl.simplified.tests.books.borrowing

import okhttp3.mockwebserver.MockWebServer
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.tests.books.borrowing.BorrowTestFeeds.Status.HELD
import org.nypl.simplified.tests.books.borrowing.BorrowTestFeeds.Status.LOANABLE
import org.nypl.simplified.tests.books.borrowing.BorrowTestFeeds.Status.LOANED
import java.net.URI

object BorrowTestFeeds {

  enum class Status {
    LOANED,
    LOANABLE,
    HELD
  }

  data class PathElement(
    val type: String,
    val path: String
  )

  data class FeedRequirements(
    val status: Status,
    val base: URI,
    val path: List<PathElement>
  ) {
    init {
      check(this.path.isNotEmpty()) {
        "Path must be non-empty"
      }
    }
  }

  fun feed(
    requirements: FeedRequirements
  ): OPDSAcquisitionFeedEntry {
    return OPDSAcquisitionFeedEntryParser.newParser()
      .parseEntryStream(URI.create("urn:stdin"), this.feedText(requirements).byteInputStream())
  }

  fun feedText(
    requirements: FeedRequirements
  ): String {
    return buildString {
      this.append(
        """
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:opds="http://opds-spec.org/2010/catalog">
<title>Example</title>
<updated>2020-01-01T00:00:00+0000</updated>
<id>7264f7f8-7bea-4ce6-906e-615406ca38cb</id>
"""
      )
      val first = requirements.path.first()
      val rest = requirements.path.drop(1)
      this.append(
        """
<link
  href="${requirements.base.resolve(first.path)}"
  type="${first.type}"
  rel="${this@BorrowTestFeeds.acquisitionURIOf(requirements.status)}">
"""
      )
      this.append(this@BorrowTestFeeds.indirects(rest))
      this.append(this@BorrowTestFeeds.statusElements(requirements.status))
      this.append(
        """
</link>
</entry>
"""
      )
    }
  }

  private fun statusElements(status: Status): String {
    return when (status) {
      LOANED -> buildString {
        this.append(
          """
  <opds:availability since="2020-01-01T00:00:00+0000" status="available" until="2020-01-01T00:00:00+0000" />
  <opds:holds total="0" />
  <opds:copies available="5" total="5" />
        """
        )
      }
      LOANABLE -> buildString {
        this.append(
          """
  <opds:availability since="2020-01-01T00:00:00+0000" status="available" until="2020-01-01T00:00:00+0000" />
  <opds:holds total="0" />
  <opds:copies available="5" total="5" />
        """
        )
      }
      HELD -> buildString {
        TODO()
      }
    }
  }

  private fun indirects(rest: List<PathElement>): String {
    if (rest.isEmpty()) {
      return ""
    }

    val current = rest.first()
    return buildString {
      this.append("""<opds:indirectAcquisition type="${current.type}">""")
      this.append(this@BorrowTestFeeds.indirects(rest.drop(1)))
      this.append("""</opds:indirectAcquisition>""")
    }
  }

  private fun acquisitionURIOf(status: Status): String {
    return when (status) {
      LOANED -> "http://opds-spec.org/acquisition"
      LOANABLE -> "http://opds-spec.org/acquisition/borrow"
      HELD -> TODO()
    }
  }

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

  fun opdsLoanedFeedEntryOfType(
    webServer: MockWebServer,
    mime: String
  ): OPDSAcquisitionFeedEntry {
    val parsedEntry =
      this.opdsFeedEntryOf(this.opdsLoanedTextOfType(webServer, mime))
    check(parsedEntry.availability is OPDSAvailabilityLoaned) { "Feed entry must be Loaned" }
    return parsedEntry
  }

  private fun opdsLoanedTextOfType(
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
