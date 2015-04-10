package org.nypl.simplified.app;

import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

public final class CatalogAcquisitionButtons
{
  public static void configureAllAcquisitionButtonsForLayout(
    final Activity in_activity,
    final BooksType in_books,
    final ViewGroup cell_buttons,
    final OPDSAcquisitionFeedEntry e,
    final BookID id)
  {
    UIThread.checkIsUIThread();
    cell_buttons.removeAllViews();

    for (final OPDSAcquisition a : e.getAcquisitions()) {
      CatalogAcquisitionButtons.configureOneAcquisitionButtonForLayout(
        in_activity,
        in_books,
        cell_buttons,
        e,
        id,
        NullCheck.notNull(a));
    }
  }

  public static void configureOneAcquisitionButtonForLayout(
    final Activity in_activity,
    final BooksType in_books,
    final ViewGroup in_buttons_layout,
    final OPDSAcquisitionFeedEntry e,
    final BookID id,
    final OPDSAcquisition a)
  {
    switch (a.getType()) {
      case ACQUISITION_GENERIC:
      case ACQUISITION_BORROW:
      {
        final CatalogAcquisitionButton b =
          new CatalogAcquisitionButton(in_activity, NullCheck.notNull(a));
        b.setTextSize(12.0f);

        final CatalogAcquisitionController b_controller =
          new CatalogAcquisitionController(
            in_activity,
            in_books,
            id,
            a,
            e.getTitle());

        b.setOnClickListener(new OnClickListener() {
          @Override public void onClick(
            final @Nullable View v)
          {
            b.setEnabled(false);
            b_controller.onClick(v);
          }
        });

        in_buttons_layout.addView(b);
        break;
      }

      case ACQUISITION_OPEN_ACCESS:
      case ACQUISITION_SAMPLE:
      case ACQUISITION_SUBSCRIBE:
      case ACQUISITION_BUY:
      {
        /**
         * TODO: Not yet supported.
         */

        break;
      }
    }
  }

  private CatalogAcquisitionButtons()
  {
    throw new UnreachableCodeException();
  }

}
