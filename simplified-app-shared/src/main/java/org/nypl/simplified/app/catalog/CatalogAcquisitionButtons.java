package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.books.core.BookAcquisitionSelection;
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
  private static final Logger LOG = LogUtilities.getLog(CatalogAcquisitionButtons.class);

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
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(eo.getAcquisitions());

    if (a_opt.isSome()) {
      final OPDSAcquisition a = ((Some<OPDSAcquisition>) a_opt).get();
      final CatalogAcquisitionButton b =
        new CatalogAcquisitionButton(in_act, in_books, book_id, a, in_e);
      in_vg.addView(b);
    }
  }
}
