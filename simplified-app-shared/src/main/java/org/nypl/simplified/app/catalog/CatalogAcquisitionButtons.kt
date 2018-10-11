package org.nypl.simplified.app.catalog

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException

import org.nypl.simplified.books.core.BookAcquisitionSelection
import org.nypl.simplified.books.core.BooksType
import org.nypl.simplified.books.core.FeedEntryOPDS
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.slf4j.LoggerFactory

/**
 * Utility functions for configuring a set of acquisition buttons.
 */

class CatalogAcquisitionButtons private constructor() {

  init {
    throw UnreachableCodeException()
  }

  companion object {

    private val log = LoggerFactory.getLogger(CatalogAcquisitionButtons::class.java)

    /**
     * Given a feed entry, add all the required acquisition buttons to the given
     * view group.
     *
     * @param activity   The activity hosting the view
     * @param viewGroup    The view group
     * @param books The books database
     * @param entry     The feed entry
     */

    fun addButtons(
      activity: Activity,
      viewGroup: ViewGroup,
      books: BooksType,
      entry: FeedEntryOPDS) {

      viewGroup.visibility = View.VISIBLE
      viewGroup.removeAllViews()

      val bookID = entry.bookID
      val opdsEntry = entry.feedEntry

      val acquisitionOpt = BookAcquisitionSelection.preferredAcquisition(opdsEntry.acquisitions)
      if (acquisitionOpt is Some<OPDSAcquisition>) {
        val acquisition = acquisitionOpt.get()
        viewGroup.addView(CatalogAcquisitionButton(activity, books, bookID, acquisition, entry))
      } else {
        this.log.error("[{}]: no available acquisition for book ({})",
          bookID.shortID, opdsEntry.title)
      }
    }
  }
}
