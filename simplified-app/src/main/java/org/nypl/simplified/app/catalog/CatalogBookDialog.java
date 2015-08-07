package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.slf4j.Logger;

/**
 * A book detail dialog fragment used on tablets or devices with large screens.
 */

public final class CatalogBookDialog extends DialogFragment
{
  private static final String ACQUISITION_ENTRY_ID;
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookDialog.class);
  }

  static {
    ACQUISITION_ENTRY_ID = "org.nypl.simplified.app.CatalogBookDialog.entry";
  }

  private @Nullable FeedEntryOPDS         entry;
  private @Nullable CatalogBookDetailView view;

  /**
   * Construct a book dialog.
   */

  public CatalogBookDialog()
  {
    // Fragments must have no-arg constructors.
  }

  /**
   * Construct a new book dialog.
   *
   * @param e The feed entry
   *
   * @return A new dialog
   */

  public static CatalogBookDialog newDialog(
    final FeedEntryOPDS e)
  {
    final CatalogBookDialog c = new CatalogBookDialog();
    final Bundle b = new Bundle();
    b.putSerializable(
      CatalogBookDialog.ACQUISITION_ENTRY_ID, NullCheck.notNull(e));
    c.setArguments(b);
    return c;
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedBookDialog);

    final Bundle b = NullCheck.notNull(this.getArguments());
    final FeedEntryOPDS e = NullCheck.notNull(
      (FeedEntryOPDS) b.getSerializable(
        CatalogBookDialog.ACQUISITION_ENTRY_ID));

    CatalogBookDialog.LOG.debug("showing dialog for id: {}", e.getBookID());
    this.entry = e;
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);
    final FeedEntryOPDS e = NullCheck.notNull(this.entry);

    final CatalogBookDetailView detail_view = new CatalogBookDetailView(
      NullCheck.notNull(this.getActivity()), inflater, e);
    this.view = detail_view;

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.booksObservableAddObserver(detail_view);

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }

    return detail_view.getScrollView();
  }

  @Override public void onDestroy()
  {
    super.onDestroy();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final CatalogBookDetailView detail_view = NullCheck.notNull(this.view);
    books.booksObservableDeleteObserver(detail_view);
  }

  @Override public void onResume()
  {
    super.onResume();

    /**
     * Force the dialog to always appear at the same size, with a decent
     * amount of empty space around it.
     */

    final Activity act = NullCheck.notNull(this.getActivity());
    final WindowManager window_manager = NullCheck.notNull(
      (WindowManager) act.getSystemService(Context.WINDOW_SERVICE));
    final Display display =
      NullCheck.notNull(window_manager.getDefaultDisplay());

    final DisplayMetrics m = new DisplayMetrics();
    display.getMetrics(m);

    final int width = (int) (m.widthPixels * 0.75);
    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(width, window.getAttributes().height);
  }
}
