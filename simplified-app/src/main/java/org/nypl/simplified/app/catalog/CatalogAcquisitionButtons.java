package org.nypl.simplified.app.catalog;

import java.util.List;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Utility functions for configuring a set of acquisition buttons.
 */

public final class CatalogAcquisitionButtons
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogAcquisitionButtons.class);
  }

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

    final OPDSAcquisition a =
      CatalogAcquisitionButtons.getPreferredAcquisition(eo.getAcquisitions());
    final CatalogAcquisitionButton b =
      new CatalogAcquisitionButton(in_act, in_books, book_id, a, in_e);
    in_vg.addView(b);
  }

  public static OPDSAcquisition getPreferredAcquisition(
    final List<OPDSAcquisition> acquisitions)
  {
    NullCheck.notNull(acquisitions);

    Assertions.checkPrecondition(
      acquisitions.isEmpty() == false,
      "Acquisitions list is non-empty");

    OPDSAcquisition best = NullCheck.notNull(acquisitions.get(0));
    for (final OPDSAcquisition current : acquisitions) {
      final OPDSAcquisition nn_current = NullCheck.notNull(current);
      if (CatalogAcquisitionButtons.priority(nn_current) > CatalogAcquisitionButtons
        .priority(best)) {
        best = nn_current;
      }
    }

    CatalogAcquisitionButtons.LOG.debug(
      "best acquisition of {} was {}",
      acquisitions,
      best);
    return best;
  }

  private static int priority(
    final OPDSAcquisition a)
  {
    switch (a.getType()) {
      case ACQUISITION_BORROW:
        return 6;
      case ACQUISITION_OPEN_ACCESS:
        return 4;
      case ACQUISITION_GENERIC:
        return 5;
      case ACQUISITION_SAMPLE:
        return 3;
      case ACQUISITION_BUY:
        return 2;
      case ACQUISITION_SUBSCRIBE:
        return 1;
    }

    return 0;
  }

  private CatalogAcquisitionButtons()
  {
    throw new UnreachableCodeException();
  }
}
