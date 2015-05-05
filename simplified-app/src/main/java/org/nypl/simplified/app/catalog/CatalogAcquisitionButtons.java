package org.nypl.simplified.app.catalog;

import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
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
    final BookID in_book_id,
    final OPDSAcquisitionFeedEntry in_e)
  {
    NullCheck.notNull(in_act);
    NullCheck.notNull(in_vg);
    NullCheck.notNull(in_books);
    NullCheck.notNull(in_book_id);
    NullCheck.notNull(in_e);

    in_vg.setVisibility(View.VISIBLE);
    in_vg.removeAllViews();

    for (final OPDSAcquisition a : in_e.getAcquisitions()) {
      switch (a.getType()) {
        case ACQUISITION_BORROW:
        case ACQUISITION_GENERIC:
        {
          final CatalogAcquisitionButton b =
            new CatalogAcquisitionButton(
              in_act,
              in_books,
              in_book_id,
              a,
              in_e);
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
