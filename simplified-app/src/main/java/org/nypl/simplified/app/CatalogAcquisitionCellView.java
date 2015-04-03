package org.nypl.simplified.app;

import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookStatusCancelled;
import org.nypl.simplified.books.core.BookStatusDone;
import org.nypl.simplified.books.core.BookStatusDownloading;
import org.nypl.simplified.books.core.BookStatusFailed;
import org.nypl.simplified.books.core.BookStatusMatcherType;
import org.nypl.simplified.books.core.BookStatusOwned;
import org.nypl.simplified.books.core.BookStatusPaused;
import org.nypl.simplified.books.core.BookStatusType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * A single cell in an acquisition list or grid.
 */

@SuppressWarnings("synthetic-access") public final class CatalogAcquisitionCellView extends
  FrameLayout implements
  BitmapCacheListenerType<OPDSAcquisitionFeedEntry>,
  Observer
{
  private static final String TAG;

  static {
    TAG = "CACV";
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
    return TextUtilities.ellipsize(NullCheck.notNull(sb.toString()), 48);
  }

  private static String makeTitleText(
    final OPDSAcquisitionFeedEntry in_e)
  {
    return TextUtilities.ellipsize(in_e.getTitle(), 48);
  }

  private final Activity                                  activity;
  private final BooksType                                 books;
  private final TextView                                  cell_authors;
  private final ViewGroup                                 cell_book;
  private final ViewGroup                                 cell_buttons;
  private final ImageView                                 cell_cover_image;
  private final ViewGroup                                 cell_cover_layout;
  private final ProgressBar                               cell_cover_progress;
  private final ViewGroup                                 cell_downloading;
  private final TextView                                  cell_downloading_authors;
  private final Button                                    cell_downloading_cancel;
  private final ViewGroup                                 cell_downloading_failed;
  private final TextView                                  cell_downloading_failed_text;
  private final TextView                                  cell_downloading_percent_text;
  private final ProgressBar                               cell_downloading_progress;
  private final TextView                                  cell_downloading_title;
  private final ViewGroup                                 cell_text_layout;
  private final TextView                                  cell_title;
  private final AtomicReference<OPDSAcquisitionFeedEntry> entry;
  private @Nullable ListenableFuture<Bitmap>              loading;
  private final TextView                                  cell_downloading_failed_title;
  private final Map<BookID, Unit>                         requesting;

  public CatalogAcquisitionCellView(
    final Activity in_activity,
    final BooksType in_books,
    final Map<BookID, Unit> in_requesting)
  {
    super(in_activity.getApplicationContext(), null);

    this.activity = NullCheck.notNull(in_activity);
    this.requesting = NullCheck.notNull(in_requesting);
    this.books = NullCheck.notNull(in_books);

    final Context context =
      NullCheck.notNull(in_activity.getApplicationContext());
    final LayoutInflater inflater =
      (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.catalog_acquisition_cell, this, true);

    /**
     * Receive book status updates.
     */

    this.books.addObserver(this);

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
  }

  private boolean isCellBeingReusedForSame(
    final OPDSAcquisitionFeedEntry in_e)
  {
    final String i_id = in_e.getID();
    final OPDSAcquisitionFeedEntry cell_e = this.entry.get();
    if (cell_e != null) {
      final String c_id = cell_e.getID();
      if (c_id.equals(i_id)) {
        Log.d(
          CatalogAcquisitionCellView.TAG,
          String.format(
            "cell same %s : %s (%s)",
            i_id,
            BookID.newFromText(i_id),
            in_e.getTitle()));
        return true;
      }
    }

    Log.d(
      CatalogAcquisitionCellView.TAG,
      String.format(
        "cell new  %s : %s (%s)",
        in_e.getID(),
        BookID.newFromText(i_id),
        in_e.getTitle()));
    return false;
  }

  @Override public void onBitmapLoadingFailure(
    final OPDSAcquisitionFeedEntry key,
    final Throwable x)
  {
    if (x instanceof CancellationException) {
      return;
    }

    Log.e(CatalogAcquisitionCellView.TAG, x.getMessage(), x);
  }

  @Override public void onBitmapLoadingSuccess(
    final OPDSAcquisitionFeedEntry key,
    final Bitmap b)
  {
    final OPDSAcquisitionFeedEntry current = this.entry.get();
    final String current_name;
    final String current_id;
    if (current != null) {
      current_name = current.getTitle();
      current_id = current.getID();
    } else {
      current_name = "(null)";
      current_id = "(null)";
    }

    /**
     * If the received acquisition entry ID matches that of the current ID,
     * then the cell is being reused for the same entry and so the bitmap
     * should not be replaced.
     */

    final Boolean should_set =
      Boolean.valueOf(current_id.equals(key.getID()));

    Log.d(CatalogAcquisitionCellView.TAG, String.format(
      "image received, setting: %s (current '%s' / received '%s')",
      should_set,
      current_name,
      key.getTitle()));

    if (should_set.booleanValue()) {
      final ImageView image_view = this.cell_cover_image;
      final ProgressBar progress = this.cell_cover_progress;

      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          image_view.setImageBitmap(b);
          image_view.setVisibility(View.VISIBLE);
          Fade.fadeIn(image_view, Fade.DEFAULT_FADE_DURATION);
          progress.setVisibility(View.INVISIBLE);
        }
      });
    }
  }

  @Override public void update(
    final @Nullable Observable observable,
    final @Nullable Object data)
  {
    assert observable == this.books;
    assert data instanceof BookStatusType;

    Log.d(
      CatalogAcquisitionCellView.TAG,
      String.format("update %s %s", observable, data));

    final BookStatusType status = (BookStatusType) data;
    final OPDSAcquisitionFeedEntry in_entry = this.entry.get();

    if (in_entry != null) {
      final BookID id = BookID.newIDFromEntry(in_entry);
      if (id.equals(status.getID())) {
        UIThread.runOnUIThread(new Runnable() {
          @Override public void run()
          {
            CatalogAcquisitionCellView.this.viewConfigureCellViewForStatus(
              CatalogAcquisitionCellView.this.activity,
              in_entry,
              id,
              Option.some(status));
          }
        });
      }
    }
  }

  public void viewConfigure(
    final OPDSAcquisitionFeedEntry in_e,
    final CatalogAcquisitionThumbnailCacheType in_image_loader,
    final CatalogAcquisitionFeedListenerType in_listener)
  {
    NullCheck.notNull(in_e);
    NullCheck.notNull(in_image_loader);
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

    final BitmapDisplaySizeType size =
      new BitmapDisplayHeightPreserveAspect(
        this.cell_cover_layout.getLayoutParams().height);

    final boolean cell_being_reused = this.isCellBeingReusedForSame(in_e);
    if (cell_being_reused == false) {
      this.entry.set(in_e);
      final ListenableFuture<Bitmap> l = this.loading;
      if (l != null) {
        l.cancel(true);
      }
      this.cell_cover_image.setVisibility(View.INVISIBLE);
      this.cell_cover_progress.setVisibility(View.VISIBLE);
      this.loading =
        in_image_loader.getThumbnailAsynchronous(in_e, size, this);

      final BookID book_id = BookID.newIDFromEntry(in_e);
      final OptionType<BookStatusType> stat =
        this.books.booksStatusGet(book_id);

      this.viewConfigureCellViewForStatus(this.activity, in_e, book_id, stat);
    }
  }

  private void viewConfigureCellAcquisitionButton(
    final Context ctx,
    final OPDSAcquisitionFeedEntry e,
    final BookID id,
    final OPDSAcquisition a)
  {
    switch (a.getType()) {
      case ACQUISITION_BORROW:
      case ACQUISITION_OPEN_ACCESS:
      {
        final CatalogAcquisitionButton b =
          new CatalogAcquisitionButton(ctx, NullCheck.notNull(a));
        b.setTextSize(12.0f);

        final CatalogAcquisitionController b_controller =
          new CatalogAcquisitionController(
            this.activity,
            this.books,
            id,
            a,
            e.getTitle());

        b.setOnClickListener(new OnClickListener() {
          @Override public void onClick(
            final @Nullable View v)
          {
            CatalogAcquisitionCellView.this.requestingAdd(id);
            b.setEnabled(false);
            b_controller.onClick(v);
          }
        });

        b.setEnabled(this.requestingGet(id) == false);
        this.cell_buttons.addView(b);
        break;
      }
      case ACQUISITION_SAMPLE:
      case ACQUISITION_SUBSCRIBE:
      case ACQUISITION_BUY:
      case ACQUISITION_GENERIC:
      {
        /**
         * TODO: Not yet supported.
         */

        break;
      }
    }
  }

  private void viewConfigureCellAcquisitionButtons(
    final Context ctx,
    final OPDSAcquisitionFeedEntry e,
    final BookID id)
  {
    UIThread.checkIsUIThread();

    this.cell_buttons.removeAllViews();

    for (final OPDSAcquisition a : e.getAcquisitions()) {
      this.viewConfigureCellAcquisitionButton(ctx, e, id, a);
    }
  }

  /**
   * Configure the overall status of the cell. The cell displays a number of
   * different layouts depending on whether the current book is loaned, fully
   * downloaded, currently downloading, not loaned, etc.
   */

  private void viewConfigureCellViewForStatus(
    final Context ctx,
    final OPDSAcquisitionFeedEntry in_e,
    final BookID book_id,
    final OptionType<BookStatusType> in_stat_opt)
  {
    UIThread.checkIsUIThread();

    if (in_stat_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) in_stat_opt;
      final BookStatusType status = some.get();
      status
        .matchBookStatus(new BookStatusMatcherType<Unit, UnreachableCodeException>() {
          @Override public Unit onBookStatusCancelled(
            final BookStatusCancelled c)
          {

            CatalogAcquisitionCellView.this
              .viewConfigureCellViewForStatusCancelled(ctx, in_e, book_id, c);
            return Unit.unit();
          }

          @Override public Unit onBookStatusDone(
            final BookStatusDone d)
          {
            CatalogAcquisitionCellView.this
              .viewConfigureCellViewForStatusDone(ctx, in_e, book_id, d);
            return Unit.unit();
          }

          @Override public Unit onBookStatusDownloading(
            final BookStatusDownloading d)
          {
            CatalogAcquisitionCellView.this
              .viewConfigureCellViewForStatusDownloading(
                ctx,
                in_e,
                book_id,
                d);
            return Unit.unit();
          }

          @Override public Unit onBookStatusFailed(
            final BookStatusFailed f)
          {
            CatalogAcquisitionCellView.this
              .viewConfigureCellViewForStatusFailed(ctx, in_e, book_id, f);
            return Unit.unit();
          }

          @Override public Unit onBookStatusOwned(
            final BookStatusOwned o)
          {
            CatalogAcquisitionCellView.this
              .viewConfigureCellViewForStatusOwned(ctx, in_e, book_id, o);
            return Unit.unit();
          }

          @Override public Unit onBookStatusPaused(
            final BookStatusPaused p)
          {
            CatalogAcquisitionCellView.this
              .viewConfigureCellViewForStatusPaused(ctx, in_e, book_id, p);
            return Unit.unit();
          }
        });
    } else {
      this.viewConfigureCellViewForStatusNone(ctx, in_e, book_id);
    }
  }

  private boolean requestingGet(
    final BookID id)
  {
    final boolean r = this.requesting.containsKey(id);
    Log.d(CatalogAcquisitionCellView.TAG, String.format("requesting: %s", r));
    return r;
  }

  private void requestingAdd(
    final BookID id)
  {
    Log.d(
      CatalogAcquisitionCellView.TAG,
      String.format("request added %s", id));
    this.requesting.put(id, Unit.unit());
  }

  private void requestingRemove(
    final BookID id)
  {
    Log.d(
      CatalogAcquisitionCellView.TAG,
      String.format("request removed %s", id));
    this.requesting.remove(id);
  }

  private void viewConfigureCellViewForStatusCancelled(
    final Context ctx,
    final OPDSAcquisitionFeedEntry in_e,
    final BookID book_id,
    final BookStatusCancelled c)
  {
    this.books.bookDownloadAcknowledge(book_id);
    this.requestingRemove(book_id);
    this.viewConfigureCellViewForStatusNone(ctx, in_e, book_id);
  }

  private void viewConfigureCellViewForStatusDone(
    final Context ctx,
    final OPDSAcquisitionFeedEntry in_e,
    final BookID book_id,
    final BookStatusDone d)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status done");
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    this.requestingRemove(book_id);

    this.cell_buttons.removeAllViews();
    final Button b = new Button(ctx);
    b.setText("Read");
    b.setTextSize(12.0f);
    this.cell_buttons.addView(b);
  }

  private void viewConfigureCellViewForStatusDownloading(
    final Context ctx,
    final OPDSAcquisitionFeedEntry in_e,
    final BookID book_id,
    final BookStatusDownloading d)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status downloading");
    this.cell_downloading.setVisibility(View.VISIBLE);
    this.cell_book.setVisibility(View.GONE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    this.cell_downloading_title.setText(CatalogAcquisitionCellView
      .makeTitleText(in_e));
    this.cell_downloading_authors.setText(CatalogAcquisitionCellView
      .makeAuthorText(in_e));

    final DownloadSnapshot snap = d.getSnapshot();
    final long max = snap.statusGetMaximumBytes();
    final long cur = snap.statusGetCurrentBytes();

    if (max < 0) {
      this.cell_downloading_progress.setIndeterminate(true);
    } else {
      final double perc = ((double) cur / (double) max) * 100.0;
      final int iperc = (int) perc;
      this.cell_downloading_progress.setIndeterminate(false);
      this.cell_downloading_progress.setMax(100);
      this.cell_downloading_progress.setProgress(iperc);
      this.cell_downloading_percent_text
        .setText(String.format("%d%%", iperc));
    }

    this.cell_downloading_cancel.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        CatalogAcquisitionCellView.this.books.bookDownloadCancel(book_id);
      }
    });
  }

  private void viewConfigureCellViewForStatusFailed(
    final Context ctx,
    final OPDSAcquisitionFeedEntry in_e,
    final BookID book_id,
    final BookStatusFailed f)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status failed");
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.VISIBLE);

    final String text;
    final DownloadSnapshot snap = f.getSnapshot();
    if (snap.getError().isSome()) {
      final Some<Throwable> some = (Some<Throwable>) snap.getError();
      final Throwable e = some.get();
      text = String.format("Download failed: %s", e);
    } else {
      text = "Unknown error";
    }

    this.cell_downloading_failed_title.setText(CatalogAcquisitionCellView
      .makeTitleText(in_e));
    this.cell_downloading_failed_text.setText(text);
  }

  private void viewConfigureCellViewForStatusNone(
    final Context ctx,
    final OPDSAcquisitionFeedEntry in_e,
    final BookID book_id)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status none");
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    this.viewConfigureCellAcquisitionButtons(ctx, in_e, book_id);
  }

  private void viewConfigureCellViewForStatusOwned(
    final Context ctx,
    final OPDSAcquisitionFeedEntry in_e,
    final BookID book_id,
    final BookStatusOwned o)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status owned");
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    this.viewConfigureCellAcquisitionButtons(ctx, in_e, book_id);
  }

  private void viewConfigureCellViewForStatusPaused(
    final Context ctx,
    final OPDSAcquisitionFeedEntry in_e,
    final BookID book_id,
    final BookStatusPaused p)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status paused");
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    throw new UnimplementedCodeException();
  }
}
