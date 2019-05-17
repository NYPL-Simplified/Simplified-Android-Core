package org.nypl.simplified.app.catalog

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import com.io7m.jnull.Nullable
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS

/**
 * An activity showing options for reporting
 */

class CatalogBookReportActivity : CatalogActivity() {

  private lateinit var typeButtons: List<CheckBox>
  private lateinit var container: ViewGroup
  private lateinit var submitButton: Button
  private lateinit var feedEntry: FeedEntryOPDS
  private lateinit var account: AccountType

  override fun navigationDrawerShouldShowIndicator(): Boolean {
    return this.upStack.isEmpty
  }

  override fun navigationDrawerGetActivityTitle(resources: Resources): String {
    return resources.getString(R.string.catalog_book_report)
  }

  override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
    when (menuItem.itemId) {
      android.R.id.home -> {
        this.onBackPressed()
        return true
      }

      else -> {
        return super.onOptionsItemSelected(menuItem)
      }
    }
  }

  override fun onCreate(@Nullable state: Bundle?) {
    super.onCreate(state)

    val intent = this.intent
    val a = intent.extras!!

    this.feedEntry =
      a.getSerializable(FEED_ENTRY) as FeedEntryOPDS
    this.account =
      Simplified.getProfilesController()
        .profileAccountForBook(this.feedEntry.bookID)

    val layout =
      this.layoutInflater.inflate(R.layout.catalog_book_report, this.contentFrame, false) as ViewGroup
    this.contentFrame.addView(layout)
    this.contentFrame.requestLayout()

    this.submitButton =
      layout.findViewById(R.id.report_submit)
    this.container =
      layout.findViewById(R.id.options_container)

    val buttons = ArrayList<CheckBox>()
    for (i in 0 until this.container.childCount) {
      val child = this.container.getChildAt(i)
      if (child is CheckBox) {
        buttons.add(child)
        child.setOnClickListener {
          this.typeButtons.forEach { view -> view.isChecked = false }
          child.isChecked = true
        }
      }
    }

    this.typeButtons = buttons.toList()
    this.submitButton.setOnClickListener { view -> this.submitReport() }
  }

  override fun onStart() {
    super.onStart()
    this.navigationDrawerShowUpIndicatorUnconditionally()
  }

  private fun submitReport() {
    val button = this.typeButtons.find { button -> button.isChecked }
    if (button != null) {
      Simplified.getBooksController()
        .bookReport(this.account, this.feedEntry, button.tag as String)
      this.finish()
    }
  }

  companion object {

    private const val FEED_ENTRY =
      "org.nypl.simplified.app.CatalogBookReportActivity.feedEntry"

    /**
     * Start a new reader for the given book.
     *
     * @param from       The parent activity
     * @param feedEntry Feed entry of the book to report a problem with
     */

    fun startActivity(
      from: Activity,
      feedEntry: FeedEntryOPDS) {
      val b = Bundle()
      b.putSerializable(this.FEED_ENTRY, feedEntry)
      val i = Intent(from, CatalogBookReportActivity::class.java)
      i.putExtras(b)
      i.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
      from.startActivity(i)
    }
  }
}
