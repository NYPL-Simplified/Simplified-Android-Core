package org.nypl.simplified.app.catalog

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.books.core.BookFormats
import org.nypl.simplified.books.core.FeedEntryOPDS

object CatalogCoverBadges {

  /*
   * Configure the visibility and content of the given image view based on the given OPDS
   * feed entry.
   */

  fun configureBadgeForEntry(
    entryNow: FeedEntryOPDS,
    badgeView: ImageView,
    sizeDp: Int): Unit {

    /*
     * If the format can't be inferred, don't show a badge. It's not clear why the book
     * would even be in the feed in the first place...
     */

    val formatOpt = entryNow.probableFormat
    return if (formatOpt is Some<BookFormats.BookFormatDefinition>) {
      val format = formatOpt.get()
      when (format) {
        null,
        BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB -> {
          badgeView.visibility = View.GONE
          Unit.unit()
        }

        /*
         * Show badges for audio books.
         */

        BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO -> {
          val catalogAppServices = Simplified.getCatalogAppServices()

          val badgeLayoutParams =
            RelativeLayout.LayoutParams(
              catalogAppServices.screenDPToPixels(sizeDp).toInt(),
              catalogAppServices.screenDPToPixels(sizeDp).toInt())

          badgeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
          badgeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
          badgeLayoutParams.rightMargin = 0
          badgeLayoutParams.bottomMargin = 0

          badgeView.layoutParams = badgeLayoutParams
          badgeView.isClickable = false
          badgeView.isFocusable = false
          badgeView.visibility = View.VISIBLE
          badgeView.setImageResource(R.drawable.audiobook_icon)
          return Unit.unit()
        }
      }
    } else {
      badgeView.visibility = View.GONE
      Unit.unit()
    }
  }

}
