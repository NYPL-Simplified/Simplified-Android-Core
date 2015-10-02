package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

import java.util.List;

/**
 * Utility functions for configuring a set of acquisition buttons.
 */

public final class CatalogAcquisitionButtons
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogAcquisitionButtons.class);
  }

  private CatalogAcquisitionButtons()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Given a feed entry, add all the required acquisition buttons to the given
   * view group.
   *
   * @param in_act   The activity hosting the view
   * @param in_vg    The view group
   * @param in_books The books database
   * @param in_e     The feed entry
   */

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

    final OptionType<OPDSAcquisition> a_opt =
      CatalogAcquisitionButtons.getPreferredAcquisition(
        book_id, eo.getAcquisitions());
    if (a_opt.isSome()) {
      final OPDSAcquisition a = ((Some<OPDSAcquisition>) a_opt).get();
      final CatalogAcquisitionButton b =
        new CatalogAcquisitionButton(in_act, in_books, book_id, a, in_e);
      in_vg.addView(b);
    }
  }

  /**
   * Return the preferred acquisition type, from the list of types.
   *
   * @param book_id      The book ID
   * @param acquisitions The list of acquisition types
   *
   * @return The preferred acquisition, if any
   */

  public static OptionType<OPDSAcquisition> getPreferredAcquisition(
    final BookID book_id,
    final List<OPDSAcquisition> acquisitions)
  {
    NullCheck.notNull(acquisitions);

    if (acquisitions.isEmpty()) {
      CatalogAcquisitionButtons.LOG.debug(
        "[{}]: no acquisitions, so no best acquisition!", book_id);
      return Option.none();
    }

    OPDSAcquisition best = NullCheck.notNull(acquisitions.get(0));
    for (final OPDSAcquisition current : acquisitions) {
      final OPDSAcquisition nn_current = NullCheck.notNull(current);
      if (CatalogAcquisitionButtons.priority(nn_current)
          > CatalogAcquisitionButtons.priority(best)) {
        best = nn_current;
      }
    }

    CatalogAcquisitionButtons.LOG.debug(
      "[{}]: best acquisition of {} was {}", book_id, acquisitions, best);

    return Option.some(best);
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
}
