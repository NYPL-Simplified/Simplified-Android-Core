package org.nypl.simplified.tests.sandbox

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.io7m.jfunctional.Option
import org.json.JSONObject
import org.nypl.audiobook.android.tests.sandbox.R
import org.nypl.simplified.app.catalog.CatalogFeedBookCellView
import org.nypl.simplified.books.core.BookStatusDownloadFailed
import org.nypl.simplified.books.core.BookStatusLoaned
import org.nypl.simplified.books.core.BookStatusRevokeFailed
import org.nypl.simplified.books.core.BooksType
import org.nypl.simplified.books.core.FeedEntryOPDS
import org.nypl.simplified.books.core.FeedEntryType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.mime.MIMEParser
import org.nypl.simplified.mime.MIMEType
import org.nypl.simplified.multilibrary.Account
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import java.net.URI
import java.util.Calendar

class BookCellViewsActivity : FragmentActivity() {

  private lateinit var covers: BookCoverProviderType
  private lateinit var booksStatus: MockedBookStatusCache
  private lateinit var books: BooksType
  private lateinit var account_json: JSONObject
  private lateinit var account: Account
  private lateinit var view: ViewGroup

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    this.setContentView(R.layout.sandbox_cell_views)

    this.view =
      this.findViewById(R.id.sandbox_cell_view)

    this.account_json =
      mockAccount()
    this.account =
      Account(this.account_json)
    this.booksStatus =
      MockedBookStatusCache()
    this.covers =
      MockedBookCoverProvider()
    this.books =
      MockedBooks(this.booksStatus)

    val entry0Builder =
      OPDSAcquisitionFeedEntry.newBuilder(
      "entry_0",
      "Entry 0",
      Calendar.getInstance(),
      OPDSAvailabilityOpenAccess.get(Option.none()))

    val entry0 =
      FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(entry0Builder.build())

    val entry1Builder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "entry_1",
        "Entry 1",
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))

    entry1Builder.addAcquisition(OPDSAcquisition(
      ACQUISITION_OPEN_ACCESS,
      URI.create("http://www.example.com"),
      Option.some(MIMEParser.parseRaisingException("application/epub+zip")),
      ArrayList()))

    val entry1 =
      FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(entry1Builder.build())

    val entry2Builder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "entry_2",
        "Entry 2",
        Calendar.getInstance(),
        OPDSAvailabilityOpenAccess.get(Option.none()))

    entry2Builder.addAcquisition(OPDSAcquisition(
      ACQUISITION_OPEN_ACCESS,
      URI.create("http://www.example.com"),
      Option.some(MIMEParser.parseRaisingException("application/epub+zip")),
      ArrayList()))

    val entry2 =
      FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(entry2Builder.build())

    this.booksStatus.statusMap.put(
      entry0.bookID, BookStatusLoaned(entry0.bookID, Option.none(), false))
    this.booksStatus.statusMap.put(
      entry1.bookID, BookStatusDownloadFailed(entry1.bookID, Option.none(), Option.none()))
    this.booksStatus.statusMap.put(
      entry2.bookID, BookStatusRevokeFailed(entry2.bookID, Option.none()))

    val cell0 = makeCell(entry0)
    val cell1 = makeCell(entry1)
    val cell2 = makeCell(entry2)

    this.view.addView(cell0)
    this.view.addView(cell1)
    this.view.addView(cell2)
  }

  private fun makeCell(entry: FeedEntryType): CatalogFeedBookCellView {
    val cell =
      CatalogFeedBookCellView(this, this.account, this.covers, this.books)
    val coverLayout =
      cell.findViewById<View>(org.nypl.simplified.app.R.id.cell_cover_layout)

    val coverHeight = coverLayout.getLayoutParams().height
    val coverWidth = (coverHeight.toDouble() / 4.0 * 3.0).toInt()
    val layoutParams = LinearLayout.LayoutParams(coverWidth, coverHeight)
    coverLayout.setLayoutParams(layoutParams)

    cell.viewConfigure(entry, { _, _ -> })
    return cell
  }

  private fun mockAccount(): JSONObject {
    val json = JSONObject()
    json.put("id", 0)
    json.put("name", "A Name")
    json.put("catalogUrl", "http://www.example.com")
    json.put("mainColor", "red")
    return json
  }

}
