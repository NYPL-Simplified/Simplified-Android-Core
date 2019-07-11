package org.nypl.simplified.app.pdf

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.nypl.pdf.android.api.PdfFragmentListenerType
import org.nypl.pdf.android.api.TableOfContentsFragmentListenerType
import org.nypl.pdf.android.api.TableOfContentsItem
import org.nypl.pdf.android.pdfviewer.PdfViewerFragment
import org.nypl.pdf.android.pdfviewer.TableOfContentsFragment
import org.nypl.simplified.app.R
import org.nypl.simplified.app.profiles.ProfileTimeOutActivity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream

class PdfReaderActivity : ProfileTimeOutActivity(), PdfFragmentListenerType, TableOfContentsFragmentListenerType {

    companion object {
        const val KEY_PAGE_INDEX = "page_index"
        const val TABLE_OF_CONTENTS = "table_of_contents"

        private const val PARAMS_ID = "org.nypl.pdf.android.pdfreader.PdfReaderActivity.params"

        /**
         * Factory method to start a [PdfReaderActivity]
         */
        fun startActivity(
                from: Activity,
                parameters: PdfReaderParameters
        ) {

            val b = Bundle()
            b.putSerializable(this.PARAMS_ID, parameters)
            val i = Intent(from, PdfReaderActivity::class.java)
            i.putExtras(b)
            from.startActivity(i)
        }
    }

    private val log: Logger = LoggerFactory.getLogger(PdfReaderActivity::class.java)

    // vars assigned in onCreate and passed with the intent
    private lateinit var documentTitle: String
    private lateinit var pdfFile: File

    // vars for the activity to pass back to the reader or table of contents fragment
    private var documentPageIndex: Int = 0
    private var tableOfContentsList: ArrayList<TableOfContentsItem> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        log.debug("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_reader)

        val intentParams = intent?.getSerializableExtra(PARAMS_ID) as PdfReaderParameters
        this.documentTitle = intentParams.documentTile
        this.pdfFile = intentParams.pdfFile

        if (savedInstanceState != null) {
            this.documentPageIndex = savedInstanceState.getInt(KEY_PAGE_INDEX, 0)
            this.tableOfContentsList = savedInstanceState.getParcelableArrayList(TABLE_OF_CONTENTS) ?: arrayListOf()
        } else {
            this.documentPageIndex = 0

            // Get the new instance of the reader you want to load here.
            val readerFragment = PdfViewerFragment.newInstance()

            this.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.pdf_reader_fragment_holder, readerFragment, "READER")
                    .commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        log.debug("onSaveInstanceState")
        outState.putInt(KEY_PAGE_INDEX, documentPageIndex)
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
