package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.content.res.Resources;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.app.R;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;

import java.util.Calendar;

/**
 * An acquisition button.
 */

public final class CatalogAcquisitionButton extends CatalogLeftPaddedButton
  implements CatalogBookButtonType
{
  /**
   * Construct an acquisition button.
   *
   * @param in_activity The host activity
   * @param in_books    The books database
   * @param in_book_id  The ID of the book to be acquired
   * @param in_acq      The acquisition
   * @param in_entry    The associated feed entry
   */

  public CatalogAcquisitionButton(
    final Activity in_activity,
    final BooksType in_books,
    final BookID in_book_id,
    final OPDSAcquisition in_acq,
    final FeedEntryOPDS in_entry)
  {
    super(in_activity);

    final Resources rr = NullCheck.notNull(in_activity.getResources());

    final OPDSAvailabilityType availability = in_entry.getFeedEntry().getAvailability();

    switch (in_acq.getType()) {
      case ACQUISITION_OPEN_ACCESS:
      case ACQUISITION_BORROW: {
        if (availability instanceof OPDSAvailabilityHoldable) {
          this.getTextView().setText(
            NullCheck.notNull(
              rr.getString(R.string.catalog_book_reserve)));
        } else {
          this.getTextView().setText(
            NullCheck.notNull(
              rr.getString(R.string.catalog_book_borrow)));
        }
        break;
      }
      case ACQUISITION_BUY:
      case ACQUISITION_GENERIC:
      case ACQUISITION_SAMPLE:
      case ACQUISITION_SUBSCRIBE: {
        this.getTextView().setText(
          NullCheck.notNull(
            rr.getString(R.string.catalog_book_download)));
        break;
      }
    }

    OptionType<Calendar> end_date = Option.none();
    if (availability instanceof OPDSAvailabilityLoaned) {
      end_date = ((OPDSAvailabilityLoaned) availability).getEndDate();
    } else if (availability instanceof OPDSAvailabilityHeldReady) {
      end_date = ((OPDSAvailabilityHeldReady) availability).getEndDate();
    }
    if (end_date.isSome()) {
      final CatalogButtonExpiryView expiry_view = new CatalogButtonExpiryView(in_activity);
      expiry_view.setDate(((Some<Calendar>) end_date).get());
      this.addView(expiry_view, 0);
    }

    this.getTextView().setTextSize(12.0f);
    this.setBackground(rr.getDrawable(R.drawable.simplified_button));
    this.getTextView().setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));
    this.setOnClickListener(
      new CatalogAcquisitionButtonController(
        in_activity, in_books, in_book_id, in_acq, in_entry));
  }
}
