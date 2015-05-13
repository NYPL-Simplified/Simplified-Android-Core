package org.nypl.simplified.app.catalog;

import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.io7m.jnull.NullCheck;

/**
 * Utility functions for configuring a set of acquisition buttons.
 */

public final class CatalogAcquisitionButtons
{
  public static void addButtons(
    final Activity in_act,
    final ViewGroup in_vg,
    final BooksType in_books,
    final FeedEntryOPDS in_e)
  {
    NullCheck.notNull(in_act);
    NullCheck.notNull(in_vg);
    NullCheck.notNull(in_books);
    NullCheck.notNull(in_e);

    in_vg.setVisibility(View.VISIBLE);
    in_vg.removeAllViews();

    final BookID book_id = in_e.getBookID();
    final OPDSAcquisitionFeedEntry eo = in_e.getFeedEntry();
    for (final OPDSAcquisition a : eo.getAcquisitions()) {
      switch (a.getType()) {
        case ACQUISITION_BORROW:
        case ACQUISITION_GENERIC:
        {
          final CatalogAcquisitionButton b =
            new CatalogAcquisitionButton(in_act, in_books, book_id, a, in_e);
          in_vg.addView(b);
          break;
        }
        case ACQUISITION_BUY:
        case ACQUISITION_OPEN_ACCESS:
        case ACQUISITION_SAMPLE:
        case ACQUISITION_SUBSCRIBE:
        {
          break;
        }
      }
    }
  }
}
