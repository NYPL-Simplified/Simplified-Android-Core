package org.nypl.simplified.viewer.pdf

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import edu.umn.minitex.pdf.android.api.PdfFragmentListenerType
import edu.umn.minitex.pdf.android.api.TableOfContentsFragmentListenerType
import edu.umn.minitex.pdf.android.api.TableOfContentsItem
import edu.umn.minitex.pdf.android.pdfviewer.PdfViewerFragment
import edu.umn.minitex.pdf.android.pdfviewer.TableOfContentsFragment
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream

class PdfReaderActivity :
  AppCompatActivity(),
  PdfFragmentListenerType,
  TableOfContentsFragmentListenerType {

  companion object {
    const val TABLE_OF_CONTENTS = "table_of_contents"

    private const val PARAMS_ID = "edu.umn.minitex.pdf.android.pdfreader.PdfReaderActivity.params"

    /**
     * Factory method to start a [PdfReaderActivity]
     */
    fun startActivity(
      from: Activity,
      parameters: PdfReaderParameters
    ) {
      val b = Bundle()
      b.putSerializable(PARAMS_ID, parameters)
      val i = Intent(from, PdfReaderActivity::class.java)
      i.putExtras(b)
      from.startActivity(i)
    }
  }

  private val log: Logger = LoggerFactory.getLogger(PdfReaderActivity::class.java)

  // vars assigned in onCreate and passed with the intent
  private lateinit var documentTitle: String
  private lateinit var pdfFile: File
  private lateinit var accountId: AccountID
  private lateinit var id: BookID
  private lateinit var currentProfile: ProfileReadableType
  private lateinit var account: AccountType
  private lateinit var books: BookDatabaseType
  private lateinit var entry: BookDatabaseEntryType
  private lateinit var handle: BookDatabaseEntryFormatHandlePDF

  // vars for the activity to pass back to the reader or table of contents fragment
  private var documentPageIndex: Int = 0
  private var tableOfContentsList: ArrayList<TableOfContentsItem> = arrayListOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    log.debug("onCreate")
    super.onCreate(savedInstanceState)
    setContentView(R.layout.pdf_reader)

    val intentParams = intent?.getSerializableExtra(PARAMS_ID) as PdfReaderParameters
    this.documentTitle = intentParams.documentTile
    this.pdfFile = intentParams.pdfFile
    this.accountId = intentParams.accountId
    this.id = intentParams.id

    val services =
      Services.serviceDirectory()

    this.currentProfile =
      services.requireService(ProfilesControllerType::class.java).profileCurrent()
    this.account = currentProfile.account(accountId)
    this.books = account.bookDatabase

    try {
      this.entry = books.entry(id)
      this.handle = entry.findFormatHandle(BookDatabaseEntryFormatHandlePDF::class.java)!!
      this.documentPageIndex = handle.format.lastReadLocation!!
    } catch (e: Exception) {
      log.error("Could not get lastReadLocation, defaulting to the 1st page", e)
    }

    if (savedInstanceState == null) {
      // Get the new instance of the reader you want to load here.
      val readerFragment = PdfViewerFragment.newInstance()

      this.supportFragmentManager
        .beginTransaction()
        .replace(R.id.pdf_reader_fragment_holder, readerFragment, "READER")
        .commit()
    } else {
      this.tableOfContentsList =
        savedInstanceState.getParcelableArrayList(TABLE_OF_CONTENTS) ?: arrayListOf()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    log.debug("onSaveInstanceState")
    outState.putParcelableArrayList(TABLE_OF_CONTENTS, tableOfContentsList)
    super.onSaveInstanceState(outState)
  }

  //region [PdfFragmentListenerType]
  override fun onReaderWantsInputStream(): InputStream {
    log.debug("onReaderWantsInputStream")
    return pdfFile.inputStream()
  }

  override fun onReaderWantsTitle(): String {
    log.debug("onReaderWantsTitle")
    return this.documentTitle
  }

  override fun onReaderWantsCurrentPage(): Int {
    log.debug("onReaderWantsCurrentPage")
    return this.documentPageIndex
  }

  override fun onReaderPageChanged(pageIndex: Int) {
    log.debug("onReaderPageChanged")
    this.documentPageIndex = pageIndex
    handle.setLastReadLocation(pageIndex)
  }

  override fun onReaderLoadedTableOfContents(tableOfContentsList: ArrayList<TableOfContentsItem>) {
    log.debug("onReaderLoadedTableOfContents. tableOfContentsList: $tableOfContentsList")
    this.tableOfContentsList = tableOfContentsList
  }

  override fun onReaderWantsTableOfContentsFragment() {
    log.debug("onReaderWantsTableOfContentsFragment")

    // Get the new instance of the [TableOfContentsFragment] you want to load here.
    val readerFragment = TableOfContentsFragment.newInstance()

    this.supportFragmentManager
      .beginTransaction()
      .replace(R.id.pdf_reader_fragment_holder, readerFragment, "READER")
      .addToBackStack(null)
      .commit()
  }
  //endregion

  //region [TableOfContentsFragmentListenerType]
  override fun onTableOfContentsWantsItems(): ArrayList<TableOfContentsItem> {
    log.debug("onTableOfContentsWantsItems")
    return this.tableOfContentsList
  }

  override fun onTableOfContentsWantsTitle(): String {
    log.debug("onTableOfContentsWantsTitle")
    return getString(R.string.table_of_contents_title)
  }

  override fun onTableOfContentsWantsEmptyDataText(): String {
    log.debug("onTableOfContentsWantsEmptyDataText")
    return getString(R.string.table_of_contents_empty_message)
  }

  override fun onTableOfContentsItemSelected(pageSelected: Int) {
    log.debug("onTableOfContentsItemSelected. pageSelected: $pageSelected")

    // the reader fragment should be on the backstack and will ask for the page index when `onResume` is called
    this.documentPageIndex = pageSelected
    onBackPressed()
  }
  //endregion
}
