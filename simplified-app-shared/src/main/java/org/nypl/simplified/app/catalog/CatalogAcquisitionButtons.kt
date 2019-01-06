package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.controller.BooksControllerType
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.core.BookAcquisitionSelection
import org.nypl.simplified.books.feeds.FeedEntryOPDS
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
      activity: AppCompatActivity,
      account: AccountType,
      viewGroup: ViewGroup,
      books: BooksControllerType,
      profiles: ProfilesControllerType,
      bookRegistry: BookRegistryReadableType,
      entry: FeedEntryOPDS) {

      viewGroup.visibility = View.VISIBLE
      viewGroup.removeAllViews()

      val bookID = entry.bookID
      val opdsEntry = entry.feedEntry

      val acquisitionOpt = BookAcquisitionSelection.preferredAcquisition(opdsEntry.acquisitions)
      if (acquisitionOpt is Some<OPDSAcquisition>) {
        val acquisition = acquisitionOpt.get()
        viewGroup.addView(
          CatalogAcquisitionButton(activity, books, profiles, bookRegistry, bookID, acquisition, entry))
      } else {
        this.log.error("[{}]: no available acquisition for book ({})", bookID.brief(), opdsEntry.title)
      }
    }
  }
}
