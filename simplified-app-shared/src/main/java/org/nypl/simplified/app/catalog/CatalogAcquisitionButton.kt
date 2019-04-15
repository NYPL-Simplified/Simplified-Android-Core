package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import com.google.common.util.concurrent.ListeningExecutorService
import com.io7m.jnull.NullCheck
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.R
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.controller.BooksControllerType
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.document_store.DocumentStoreType
import org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BORROW
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_BUY
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_GENERIC
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SAMPLE
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation.ACQUISITION_SUBSCRIBE
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable

/**
 * An acquisition button.
 */

class CatalogAcquisitionButton(
  activity: AppCompatActivity,
  books: BooksControllerType,
  profiles: ProfilesControllerType,
  bookRegistry: BookRegistryReadableType,
  bookId: BookID,
  acquisition: OPDSAcquisition,
  entry: FeedEntryOPDS,
  networkConnectivity: NetworkConnectivityType,
  backgroundExecutor: ListeningExecutorService,
  documents: DocumentStoreType)
  : AppCompatButton(activity), CatalogBookButtonType {

  init {
    val resources = NullCheck.notNull(activity.resources)

    val availability = entry.feedEntry.availability
    if (bookRegistry.book(bookId).isSome) {
      this.text = resources.getString(R.string.catalog_book_download)
      this.contentDescription = resources.getString(R.string.catalog_accessibility_book_download)
    } else {
      when (acquisition.relation) {
        ACQUISITION_OPEN_ACCESS -> {
          this.text = resources.getString(R.string.catalog_book_download)
          this.contentDescription = resources.getString(R.string.catalog_accessibility_book_download)
        }
        ACQUISITION_BORROW -> {
          if (availability is OPDSAvailabilityHoldable) {
            this.text = resources.getString(R.string.catalog_book_reserve)
            this.contentDescription = resources.getString(R.string.catalog_accessibility_book_reserve)
          } else {
            this.text =  resources.getString(R.string.catalog_book_borrow)
            this.contentDescription = resources.getString(R.string.catalog_accessibility_book_borrow)
          }
        }
        ACQUISITION_BUY,
        ACQUISITION_GENERIC,
        ACQUISITION_SAMPLE,
        ACQUISITION_SUBSCRIBE -> {
          this.text = resources.getString(R.string.catalog_book_download)
          this.contentDescription = resources.getString(R.string.catalog_accessibility_book_download)
        }
      }
    }

    this.setOnClickListener(
      CatalogAcquisitionButtonController(
        acquisition = acquisition,
        activity = activity,
        books = books,
        entry = entry,
        id = bookId,
        profiles = profiles,
        bookRegistry = bookRegistry,
        networkConnectivity = networkConnectivity,
        backgroundExecutor = backgroundExecutor,
        documents = documents))
  }
}
