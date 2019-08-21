package org.nypl.simplified.app.catalog

import android.text.TextUtils
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.app.R
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.profiles.api.ProfileReadableType

/**
 * A button that opens a given book for reading.
 */

class CatalogBookReadButton(
    val activity: AppCompatActivity,
    val analytics: AnalyticsType,
    val profile: ProfileReadableType,
    val account: AccountType,
    val bookID: BookID,
    val entry: FeedEntryOPDS)
  : AppCompatButton(activity), CatalogBookButtonType {

  init {
    this.text = resources.getString(R.string.catalog_book_read)
    this.ellipsize = TextUtils.TruncateAt.END
    this.maxLines = 1
    this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f)

    this.setOnClickListener(CatalogBookReadController(
      this.activity,
      this.analytics,
      this.profile,
      this.account,
      this.bookID,
      this.entry))
  }
}
