package org.nypl.simplified.app.catalog;

import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;

import com.io7m.jnull.NullCheck;

import org.nypl.simplified.app.R;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryReadableType;
import org.nypl.simplified.books.controller.BooksControllerType;
import org.nypl.simplified.books.controller.ProfilesControllerType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;

import static org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS;

/**
 * An acquisition button.
 */

public final class CatalogAcquisitionButton extends CatalogLeftPaddedButton implements CatalogBookButtonType {

  /**
   * Construct an acquisition button.
   */

  public CatalogAcquisitionButton(
    final AppCompatActivity in_activity,
    final BooksControllerType in_books,
    final ProfilesControllerType in_profiles,
    final BookRegistryReadableType in_book_registry,
    final BookID in_book_id,
    final OPDSAcquisition in_acquisition,
    final FeedEntryOPDS in_entry) {

    super(in_activity);
    final Resources resources = NullCheck.notNull(in_activity.getResources());

    final OPDSAvailabilityType availability = in_entry.getFeedEntry().getAvailability();
    this.getTextView().setTextSize(12.0f);

    if (in_book_registry.book(in_book_id).isSome()) {
      this.getTextView().setText(
        NullCheck.notNull(resources.getString(R.string.catalog_book_download)));
      this.setContentDescription(
        NullCheck.notNull(resources.getString(R.string.catalog_accessibility_book_download)));
    } else {
      switch (in_acquisition.getRelation()) {
        case ACQUISITION_OPEN_ACCESS:
          this.getTextView().setText(
            NullCheck.notNull(resources.getString(R.string.catalog_book_download)));
          this.setContentDescription(
            NullCheck.notNull(resources.getString(R.string.catalog_accessibility_book_download)));
          break;
        case ACQUISITION_BORROW: {
          if (availability instanceof OPDSAvailabilityHoldable) {
            this.getTextView().setText(NullCheck.notNull(resources.getString(R.string.catalog_book_reserve)));
            this.setContentDescription(NullCheck.notNull(resources.getString(R.string.catalog_accessibility_book_reserve)));
          } else {
            this.getTextView().setText(NullCheck.notNull(resources.getString(R.string.catalog_book_borrow)));
            this.setContentDescription(NullCheck.notNull(resources.getString(R.string.catalog_accessibility_book_borrow)));
          }
          break;
        }
        case ACQUISITION_BUY:
        case ACQUISITION_GENERIC:
        case ACQUISITION_SAMPLE:
        case ACQUISITION_SUBSCRIBE: {
          this.getTextView().setText(NullCheck.notNull(resources.getString(R.string.catalog_book_download)));
          this.setContentDescription(NullCheck.notNull(resources.getString(R.string.catalog_accessibility_book_download)));
          break;
        }
      }
    }

    this.setOnClickListener(
      new CatalogAcquisitionButtonController(
        in_activity,
        in_profiles,
        in_books,
        in_book_registry,
        in_book_id,
        in_acquisition,
        in_entry));
  }
}
