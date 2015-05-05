package org.nypl.simplified.app.catalog;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.app.CoverProviderType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.TextUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookStatusDownloadCancelled;
import org.nypl.simplified.books.core.BookStatusDownloadFailed;
import org.nypl.simplified.books.core.BookStatusDownloadInProgress;
import org.nypl.simplified.books.core.BookStatusDownloaded;
import org.nypl.simplified.books.core.BookStatusDownloadingMatcherType;
import org.nypl.simplified.books.core.BookStatusDownloadingPaused;
import org.nypl.simplified.books.core.BookStatusDownloadingType;
import org.nypl.simplified.books.core.BookStatusLoaned;
import org.nypl.simplified.books.core.BookStatusLoanedMatcherType;
import org.nypl.simplified.books.core.BookStatusLoanedType;
import org.nypl.simplified.books.core.BookStatusMatcherType;
import org.nypl.simplified.books.core.BookStatusRequestingDownload;
import org.nypl.simplified.books.core.BookStatusRequestingLoan;
import org.nypl.simplified.books.core.BookStatusType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import com.squareup.picasso.Callback;

/**
 * A single cell in an acquisition list or grid.
 */

@SuppressWarnings("synthetic-access") public final class CatalogAcquisitionCellView extends
  FrameLayout implements
  Observer,
  BookStatusMatcherType<Unit, UnreachableCodeException>,
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>,
  BookStatusDownloadingMatcherType<Unit, UnreachableCodeException>
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogAcquisitionCellView.class);
  }

  private static String makeAuthorText(
    final OPDSAcquisitionFeedEntry in_e)
  {
    final StringBuilder sb = new StringBuilder();
    final List<String> as = in_e.getAuthors();
    final int max = as.size();
    for (int index = 0; index < max; ++index) {
      final String a = NullCheck.notNull(as.get(index));
      sb.append(a);
      if ((index + 1) < max) {
        sb.append(", ");
      }
    }
    return TextUtilities.ellipsize(NullCheck.notNull(sb.toString()), 32);
  }

  private static String makeTitleText(
    final OPDSAcquisitionFeedEntry in_e)
  {
    return TextUtilities.ellipsize(in_e.getTitle(), 32);
  }

  private final Activity                                  activity;
  private final BooksType                                 books;
  private final TextView                                  cell_authors;
  private final ViewGroup                                 cell_book;
  private final ViewGroup                                 cell_buttons;
  private final ImageView                                 cell_cover_image;
  private final ViewGroup                                 cell_cover_layout;
  private final ProgressBar                               cell_cover_progress;
  private final TextView                                  cell_debug;
  private final ViewGroup                                 cell_downloading;
  private final TextView                                  cell_downloading_authors;
  private final Button                                    cell_downloading_cancel;
  private final ViewGroup                                 cell_downloading_failed;
  private final TextView                                  cell_downloading_failed_text;
  private final TextView                                  cell_downloading_failed_title;
  private final TextView                                  cell_downloading_percent_text;
  private final ProgressBar                               cell_downloading_progress;
  private final TextView                                  cell_downloading_title;
  private final ViewGroup                                 cell_text_layout;
  private final TextView                                  cell_title;
  private final CoverProviderType                         cover_provider;
  private final boolean                                   debug_cell_state;
  private final AtomicReference<OPDSAcquisitionFeedEntry> entry;

  public CatalogAcquisitionCellView(
    final Activity in_activity,
    final CoverProviderType in_cover_provider,
    final BooksType in_books)
  {
    super(in_activity.getApplicationContext(), null);

    this.activity = NullCheck.notNull(in_activity);
    this.cover_provider = NullCheck.notNull(in_cover_provider);
    this.books = NullCheck.notNull(in_books);

    final Context context =
      NullCheck.notNull(in_activity.getApplicationContext());
    final Resources rr = NullCheck.notNull(context.getResources());

    final LayoutInflater inflater =
      (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.catalog_acquisition_cell, this, true);

    /**
     * Receive book status updates.
     */

    this.books.booksObservableAddObserver(this);

    this.cell_downloading =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.cell_downloading));
    this.cell_downloading_progress =
      NullCheck.notNull((ProgressBar) this.cell_downloading
        .findViewById(R.id.cell_downloading_progress));
    this.cell_downloading_percent_text =
      NullCheck.notNull((TextView) this.cell_downloading
        .findViewById(R.id.cell_downloading_percent_text));
    this.cell_downloading_title =
      NullCheck.notNull((TextView) this.cell_downloading
        .findViewById(R.id.cell_downloading_title));
    this.cell_downloading_authors =
      NullCheck.notNull((TextView) this.cell_downloading
        .findViewById(R.id.cell_downloading_authors));
    this.cell_downloading_cancel =
      NullCheck.notNull((Button) this.cell_downloading
        .findViewById(R.id.cell_downloading_cancel));

    this.cell_downloading_failed =
      NullCheck.notNull((ViewGroup) this
        .findViewById(R.id.cell_downloading_failed));
    this.cell_downloading_failed_text =
      NullCheck.notNull((TextView) this.cell_downloading_failed
        .findViewById(R.id.cell_downloading_failed_text));
    this.cell_downloading_failed_title =
      NullCheck.notNull((TextView) this.cell_downloading_failed
        .findViewById(R.id.cell_downloading_failed_title));

    this.cell_book =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.cell_book));

    this.cell_debug =
      NullCheck.notNull((TextView) this.findViewById(R.id.cell_debug));
    this.debug_cell_state =
      rr.getBoolean(R.bool.debug_catalog_cell_view_states);
    if (this.debug_cell_state == false) {
      this.cell_debug.setVisibility(View.GONE);
    }

    this.cell_text_layout =
      NullCheck.notNull((ViewGroup) this.cell_book
        .findViewById(R.id.cell_text_layout));
    this.cell_title =
      NullCheck.notNull((TextView) this.cell_text_layout
        .findViewById(R.id.cell_title));
    this.cell_authors =
      NullCheck.notNull((TextView) this.cell_text_layout
        .findViewById(R.id.cell_authors));
    this.cell_buttons =
      NullCheck.notNull((ViewGroup) this.cell_text_layout
        .findViewById(R.id.cell_buttons));

    this.cell_cover_layout =
      NullCheck
        .notNull((ViewGroup) this.findViewById(R.id.cell_cover_layout));
    this.cell_cover_image =
      NullCheck.notNull((ImageView) this.cell_cover_layout
        .findViewById(R.id.cell_cover_image));
    this.cell_cover_progress =
      NullCheck.notNull((ProgressBar) this.cell_cover_layout
        .findViewById(R.id.cell_cover_loading));

    /**
     * The height of the row is known, so assume a roughly 4:3 aspect ratio
     * for cover images and calculate the width of the cover layout in pixels.
     */

    final int cover_height = this.cell_cover_layout.getLayoutParams().height;
    final int cover_width = (int) ((cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams ccl_p =
      new LinearLayout.LayoutParams(cover_width, cover_height);
    this.cell_cover_layout.setLayoutParams(ccl_p);

    this.entry = new AtomicReference<OPDSAcquisitionFeedEntry>();

    /**
     * Hide everything by default.
     */

    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
  }

  private boolean isCellBeingReusedForSame(
    final OPDSAcquisitionFeedEntry in_e)
  {
    final String i_id = in_e.getID();
    final OPDSAcquisitionFeedEntry cell_e = this.entry.get();
    if (cell_e != null) {
      final String c_id = cell_e.getID();
      if (c_id.equals(i_id)) {
        CatalogAcquisitionCellView.LOG.debug(
          "cell same {}: {} ({})",
          i_id,
          BookID.newFromText(i_id),
          in_e.getTitle());
        return true;
      }
    }

    CatalogAcquisitionCellView.LOG.debug(
      "cell new {}: {} ({})",
      in_e.getID(),
      BookID.newFromText(i_id),
      in_e.getTitle());
    return false;
  }

  private void loadImageAndSetVisibility(
    final OPDSAcquisitionFeedEntry in_e)
  {
    final int in_image_height =
      this.cell_cover_layout.getLayoutParams().height;

    final ImageView ci = this.cell_cover_image;
    final ProgressBar cp = this.cell_cover_progress;

    ci.setVisibility(View.INVISIBLE);
    cp.setVisibility(View.VISIBLE);

    final Callback callback = new Callback() {
      @Override public void onError()
      {
        CatalogAcquisitionCellView.LOG.error("unable to load image");
      }

      @Override public void onSuccess()
      {
        ci.setVisibility(View.VISIBLE);
        cp.setVisibility(View.INVISIBLE);
      }
    };

    this.cover_provider.loadThumbnailIntoWithCallback(
      in_e,
      this.cell_cover_image,
      (int) (in_image_height * 0.75),
      in_image_height,
      callback);
  }

  @Override public Unit onBookStatusDownloadCancelled(
    final BookStatusDownloadCancelled c)
  {
    CatalogAcquisitionCellView.LOG.debug("{}: cancelled", c.getID());
    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry.get());
    final BookID id = c.getID();
    this.books.bookDownloadAcknowledge(id);
    this.onStatus(e, id, this.books.booksStatusGet(id));
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloaded(
    final BookStatusDownloaded d)
  {
    CatalogAcquisitionCellView.LOG.debug("{}: downloaded", d.getID());
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);
    this.setDebugCellText("downloaded");

    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(e);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

    this.cell_buttons.addView(new CatalogBookDeleteButton(this.activity, d
      .getID()));
    this.cell_buttons.addView(new CatalogBookReadButton(this.activity, d
      .getID()));
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadFailed(
    final BookStatusDownloadFailed f)
  {
    CatalogAcquisitionCellView.LOG.debug("{}: download failed", f.getID());
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.VISIBLE);
    this.setDebugCellText("download-failed");

    final DownloadSnapshot snap = f.getDownloadSnapshot();
    this.cell_downloading_failed_title.setText("Download failed");
    this.cell_downloading_failed_text.setText(snap.getError().toString());
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloading(
    final BookStatusDownloadingType o)
  {
    return o.matchBookDownloadingStatus(this);
  }

  @Override public Unit onBookStatusDownloadingPaused(
    final BookStatusDownloadingPaused p)
  {
    CatalogAcquisitionCellView.LOG.debug("{}: paused", p.getID());
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);
    this.setDebugCellText("download-paused");
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadInProgress(
    final BookStatusDownloadInProgress d)
  {
    CatalogAcquisitionCellView.LOG.debug("{}: downloading", d.getID());
    this.cell_downloading.setVisibility(View.VISIBLE);
    this.cell_book.setVisibility(View.GONE);
    this.cell_downloading_failed.setVisibility(View.GONE);
    this.setDebugCellText("download-in-progress");

    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry.get());
    final BookID id = d.getID();

    this.cell_downloading_title.setText(CatalogAcquisitionCellView
      .makeTitleText(e));
    this.cell_downloading_authors.setText(CatalogAcquisitionCellView
      .makeAuthorText(e));

    final DownloadSnapshot snap = d.getDownloadSnapshot();
    CatalogAcquisitionDownloadProgressBar.setProgressBar(
      snap,
      this.cell_downloading_percent_text,
      this.cell_downloading_progress);

    this.cell_downloading_cancel.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        CatalogAcquisitionCellView.this.books.bookDownloadCancel(id);
      }
    });

    return Unit.unit();
  }

  @Override public Unit onBookStatusLoaned(
    final BookStatusLoaned o)
  {
    CatalogAcquisitionCellView.LOG.debug("{}: loaned", o.getID());
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);
    this.setDebugCellText("loaned");

    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(e);

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.cell_buttons,
      this.books,
      o.getID(),
      e);

    return Unit.unit();
  }

  @Override public Unit onBookStatusLoanedType(
    final BookStatusLoanedType o)
  {
    return o.matchBookLoanedStatus(this);
  }

  private void onBookStatusNone(
    final OPDSAcquisitionFeedEntry e,
    final BookID id)
  {
    CatalogAcquisitionCellView.LOG.debug("{}: none", id);
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);
    this.setDebugCellText("none");

    this.loadImageAndSetVisibility(e);

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.cell_buttons,
      this.books,
      id,
      e);
  }

  @Override public Unit onBookStatusRequestingDownload(
    final BookStatusRequestingDownload d)
  {
    CatalogAcquisitionCellView.LOG
      .debug("{}: requesting download", d.getID());
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);
    this.setDebugCellText("requesting-download");

    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(e);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();
    return Unit.unit();
  }

  @Override public Unit onBookStatusRequestingLoan(
    final BookStatusRequestingLoan s)
  {
    CatalogAcquisitionCellView.LOG.debug("{}: requesting loan", s.getID());
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);
    this.setDebugCellText("requesting-loan");

    this.loadImageAndSetVisibility(NullCheck.notNull(this.entry.get()));

    this.cell_buttons.setVisibility(View.GONE);
    this.cell_buttons.removeAllViews();
    return Unit.unit();
  }

  private void onStatus(
    final OPDSAcquisitionFeedEntry in_entry,
    final BookID id,
    final OptionType<BookStatusType> status_opt)
  {
    if (status_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) status_opt;
      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          some.get().matchBookStatus(CatalogAcquisitionCellView.this);
        }
      });
    } else {
      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          CatalogAcquisitionCellView.this.onBookStatusNone(in_entry, id);
        }
      });
    }
  }

  private void setDebugCellText(
    final String text)
  {
    if (this.debug_cell_state) {
      this.cell_debug.setText(text);
    }
  }

  @Override public void update(
    final @Nullable Observable observable,
    final @Nullable Object data)
  {
    assert observable == this.books;
    assert data instanceof BookID;

    CatalogAcquisitionCellView.LOG.debug("update: {}", data);

    final BookID update_id = (BookID) data;
    final OPDSAcquisitionFeedEntry in_entry = this.entry.get();

    if (in_entry != null) {
      final BookID id = BookID.newIDFromEntry(in_entry);
      if (id.equals(update_id)) {
        final OptionType<BookStatusType> status_opt =
          this.books.booksStatusGet(id);
        this.onStatus(in_entry, id, status_opt);
      }
    }
  }

  /**
   * Configure the overall status of the cell. The cell displays a number of
   * different layouts depending on whether the current book is loaned, fully
   * downloaded, currently downloading, not loaned, etc.
   */

  public void viewConfigure(
    final OPDSAcquisitionFeedEntry in_e,
    final CatalogAcquisitionFeedListenerType in_listener)
  {
    NullCheck.notNull(in_e);
    NullCheck.notNull(in_listener);

    UIThread.checkIsUIThread();

    this.cell_title.setText(CatalogAcquisitionCellView.makeTitleText(in_e));
    this.cell_authors
      .setText(CatalogAcquisitionCellView.makeAuthorText(in_e));

    this.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        in_listener.onSelectBook(CatalogAcquisitionCellView.this, in_e);
      }
    });

    final boolean reusing = this.isCellBeingReusedForSame(in_e);
    if (reusing == false) {
      this.entry.set(in_e);
      this.cell_cover_image.setVisibility(View.INVISIBLE);
      this.cell_cover_progress.setVisibility(View.VISIBLE);

      final BookID book_id = BookID.newIDFromEntry(in_e);
      final OptionType<BookStatusType> stat =
        this.books.booksStatusGet(book_id);
      this.onStatus(in_e, book_id, stat);
    }
  }
}
