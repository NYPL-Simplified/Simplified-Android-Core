package org.nypl.simplified.viewer.epub.readium1.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import org.librarysimplified.services.api.Services
import org.nypl.simplified.ui.theme.ThemeServiceType
import org.nypl.simplified.viewer.epub.readium1.R
import org.slf4j.LoggerFactory

/**
 * Activity for displaying the ViewPager which contains Fragments
 * for the Table of Contents and User-Saved Bookmarks.
 */

class ReaderTOCActivity : AppCompatActivity(), ReaderTOCSelectionListenerType {

  private val logger = LoggerFactory.getLogger(ReaderTOCActivity::class.java)

  private lateinit var tocParameters: ReaderTOCParameters

  override fun finish() {
    super.finish()
    this.overridePendingTransition(0, 0)
  }

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)
    logger.debug("onCreate")

    this.setTheme(
      Services.serviceDirectory()
        .requireService(ThemeServiceType::class.java)
        .findCurrentTheme()
        .themeWithActionBar
    )

    this.setTitle(R.string.reader_toc)

    val input = this.intent!!
    val args = input.extras!!
    this.tocParameters = args.getSerializable(PARAMETERS_ID) as ReaderTOCParameters

    this.setContentView(R.layout.reader_toc_tab_layout)

    val pager =
      findViewById<ViewPager>(R.id.reader_toc_view_pager)
    val pagerAdapter =
      ReaderTOCFragmentPagerAdapter(this.supportFragmentManager, this.tocParameters)

    pager.adapter = pagerAdapter
    val tabLayout = findViewById<TabLayout>(R.id.reader_toc_tab_layout)
    tabLayout.setupWithViewPager(pager)
  }

  override fun onTOCItemSelected(selection: ReaderTOCSelection) {
    val intent = Intent()
    intent.putExtra(TOC_SELECTED_ID, selection)
    this.setResult(Activity.RESULT_OK, intent)
    this.finish()
  }

  companion object {

    /**
     * The name of the argument containing the selected TOC item.
     */

    const val PARAMETERS_ID: String =
      "org.nypl.simplified.app.reader.ReaderTOCActivity.parameters"

    /**
     * The name of the argument containing the selected TOC item.
     */

    const val TOC_SELECTED_ID: String =
      "org.nypl.simplified.app.reader.ReaderTOCActivity.toc_selected"

    /**
     * The activity request code (for retrieving the result of executing the
     * activity).
     */

    const val TOC_SELECTION_REQUEST_CODE: Int = 23

    /**
     * Start a TOC activity. The user will be shown a pager view.
     * If they select a TOC or Bookmark item, the results of that selection
     * will be reported using the request code [.TOC_SELECTION_REQUEST_CODE].
     *
     * @param from The parent activity
     * @param toc The table of contents
     */

    fun startActivityForResult(
      from: Activity,
      parameters: ReaderTOCParameters
    ) {
      val i = Intent(Intent.ACTION_PICK)
      i.setClass(from, ReaderTOCActivity::class.java)
      i.putExtra(PARAMETERS_ID, parameters)
      from.startActivityForResult(i, TOC_SELECTION_REQUEST_CODE)
    }
  }
}
