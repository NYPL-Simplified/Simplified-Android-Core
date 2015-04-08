package org.nypl.simplified.app;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookStatusCancelled;
import org.nypl.simplified.books.core.BookStatusDone;
import org.nypl.simplified.books.core.BookStatusDownloading;
import org.nypl.simplified.books.core.BookStatusFailed;
import org.nypl.simplified.books.core.BookStatusLoaned;
import org.nypl.simplified.books.core.BookStatusLoanedMatcherType;
import org.nypl.simplified.books.core.BookStatusLoanedType;
import org.nypl.simplified.books.core.BookStatusMatcherType;
import org.nypl.simplified.books.core.BookStatusPaused;
import org.nypl.simplified.books.core.BookStatusRequesting;
import org.nypl.simplified.books.core.BookStatusType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.app.Activity;
import android.content.Context;
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
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>
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
        Log.e(CatalogAcquisitionCellView.TAG, "unable to load image");
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
            status.matchBookStatus(CatalogAcquisitionCellView.this);
          }
        });
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

      if (stat.isSome()) {
        final BookStatusType some = ((Some<BookStatusType>) stat).get();
        some.matchBookStatus(this);
      } else {
        this.onBookStatusNone(in_e, book_id);
      }
    }
  }

  private void onBookStatusNone(
    final OPDSAcquisitionFeedEntry e,
    final BookID id)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status none");
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    this.loadImageAndSetVisibility(e);

    CatalogAcquisitionButtons.configureAllAcquisitionButtonsForLayout(
      this.activity,
      this.books,
      this.cell_buttons,
      e,
      id);
  }

  @Override public Unit onBookStatusLoanedType(
    final BookStatusLoanedType o)
  {
    return o.matchBookLoanedStatus(this);
  }

  @Override public Unit onBookStatusRequesting(
    final BookStatusRequesting s)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status requesting");
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    this.loadImageAndSetVisibility(NullCheck.notNull(this.entry.get()));
    this.cell_buttons.removeAllViews();
    return Unit.unit();
  }

  @Override public Unit onBookStatusCancelled(
    final BookStatusCancelled c)
  {
    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry.get());
    final BookID id = c.getID();
    this.books.bookDownloadAcknowledge(id);
    this.onBookStatusNone(e, id);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDone(
    final BookStatusDone d)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status done");
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(e);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();
    final Button b = new Button(this.activity);
    b.setText("Read");
    b.setTextSize(12.0f);
    this.cell_buttons.addView(b);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloading(
    final BookStatusDownloading d)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status downloading");
    this.cell_downloading.setVisibility(View.VISIBLE);
    this.cell_book.setVisibility(View.GONE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry.get());
    final BookID id = d.getID();

    this.cell_downloading_title.setText(CatalogAcquisitionCellView
      .makeTitleText(e));
    this.cell_downloading_authors.setText(CatalogAcquisitionCellView
      .makeAuthorText(e));

    final DownloadSnapshot snap = d.getSnapshot();
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

  @Override public Unit onBookStatusFailed(
    final BookStatusFailed f)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status failed");
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.VISIBLE);

    final DownloadSnapshot snap = f.getSnapshot();
    this.cell_downloading_failed_title.setText("Download failed");
    this.cell_downloading_failed_text.setText(snap.getError().toString());
    return Unit.unit();
  }

  @Override public Unit onBookStatusLoaned(
    final BookStatusLoaned o)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status loaned");
    this.cell_downloading.setVisibility(View.GONE);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(e);

    this.cell_buttons.setVisibility(View.VISIBLE);
    CatalogAcquisitionButtons.configureAllAcquisitionButtonsForLayout(
      this.activity,
      this.books,
      this.cell_buttons,
      e,
      o.getID());

    return Unit.unit();
  }

  @Override public Unit onBookStatusPaused(
    final BookStatusPaused p)
  {
    Log.d(CatalogAcquisitionCellView.TAG, "status paused");
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.GONE);

    return Unit.unit();
  }
}
