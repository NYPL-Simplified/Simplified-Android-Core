package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import com.google.common.util.concurrent.ListeningExecutorService
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.controller.BooksControllerType
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.core.BookAcquisitionSelection
import org.nypl.simplified.books.document_store.DocumentStoreType
import org.nypl.simplified.books.feeds.FeedEntry
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
     */

    fun addButtons(
      activity: AppCompatActivity,
      viewGroup: ViewGroup,
      books: BooksControllerType,
      profiles: ProfilesControllerType,
      bookRegistry: BookRegistryReadableType,
      entry: FeedEntry.FeedEntryOPDS,
      networkConnectivity: NetworkConnectivityType,
      backgroundExecutor: ListeningExecutorService,
      documents: DocumentStoreType) {

      viewGroup.visibility = View.VISIBLE
      viewGroup.removeAllViews()

      val bookID = entry.bookID
      val opdsEntry = entry.feedEntry

      val acquisitionOpt = BookAcquisitionSelection.preferredAcquisition(opdsEntry.acquisitions)
      if (acquisitionOpt is Some<OPDSAcquisition>) {
        val acquisition = acquisitionOpt.get()
        viewGroup.addView(
          CatalogAcquisitionButton(
            activity = activity,
            books = books,
            profiles = profiles,
            bookRegistry = bookRegistry,
            bookId = bookID,
            acquisition = acquisition,
            entry = entry,
            networkConnectivity = networkConnectivity,
            backgroundExecutor = backgroundExecutor,
            documents = documents))
      } else {
        this.log.error("[{}]: no available acquisition for book ({})", bookID.brief(), opdsEntry.title)
      }
    }
  }
}
