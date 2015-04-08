package org.nypl.simplified.app;

import java.util.Observable;
import java.util.Observer;

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
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
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

/**
 * A book detail dialog fragment used on tablets or devices with large
 * screens.
 */

public final class CatalogBookDialog extends DialogFragment implements
  Observer,
  BookStatusMatcherType<Unit, UnreachableCodeException>,
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>
{
  private static final String ACQUISITION_ENTRY_ID;
  private static final String TAG;

  static {
    ACQUISITION_ENTRY_ID = "org.nypl.simplified.app.CatalogBookDialog.entry";
    TAG = "CBD";
  }

  public static CatalogBookDialog newDialog(
    final OPDSAcquisitionFeedEntry e)
  {
    final CatalogBookDialog c = new CatalogBookDialog();
    final Bundle b = new Bundle();
    b.putSerializable(
      CatalogBookDialog.ACQUISITION_ENTRY_ID,
      NullCheck.notNull(e));
    c.setArguments(b);
    return c;
  }

  private @Nullable ViewGroup                book_buttons;
  private @Nullable ViewGroup                book_downloading;
  private @Nullable Button                   book_downloading_cancel;
  private @Nullable TextView                 book_downloading_percent_text;
  private @Nullable ProgressBar              book_downloading_progress;
  private @Nullable OPDSAcquisitionFeedEntry entry;

  public CatalogBookDialog()
  {
    // Fragments must have no-arg constructors.
  }

  @Override public Unit onBookStatusCancelled(
    final BookStatusCancelled c)
  {
    final Simplified app = Simplified.get();
    final BooksType books = app.getBooks();
    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry);
    final BookID id = c.getID();
    books.bookDownloadAcknowledge(id);
    this.onStatusNone(books, id, e);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDone(
    final BookStatusDone d)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);

    bb.setVisibility(View.VISIBLE);
    bd.setVisibility(View.GONE);

    final Button b = new Button(this.getActivity());
    b.setText("Read");
    b.setTextSize(12.0f);
    bb.addView(b);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloading(
    final BookStatusDownloading d)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.VISIBLE);

    final DownloadSnapshot snap = d.getSnapshot();
    CatalogAcquisitionDownloadProgressBar.setProgressBar(
      snap,
      NullCheck.notNull(this.book_downloading_percent_text),
      NullCheck.notNull(this.book_downloading_progress));

    final Simplified app = Simplified.get();
    final BooksType books = app.getBooks();

    final Button dc = NullCheck.notNull(this.book_downloading_cancel);
    dc.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        books.bookDownloadCancel(d.getID());
      }
    });

    return Unit.unit();
  }

  @Override public Unit onBookStatusFailed(
    final BookStatusFailed f)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.GONE);

    return Unit.unit();
  }

  @Override public Unit onBookStatusLoaned(
    final BookStatusLoaned o)
  {
    final Simplified app = Simplified.get();
    final BooksType books = app.getBooks();

    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);

    bb.setVisibility(View.VISIBLE);
    bd.setVisibility(View.GONE);

    CatalogAcquisitionButtons.configureAllAcquisitionButtonsForLayout(
      NullCheck.notNull(this.getActivity()),
      books,
      bb,
      NullCheck.notNull(this.entry),
      o.getID());
    return Unit.unit();
  }

  @Override public Unit onBookStatusLoanedType(
    final BookStatusLoanedType o)
  {
    return o.matchBookLoanedStatus(this);
  }

  @Override public Unit onBookStatusPaused(
    final BookStatusPaused p)
  {
    return Unit.unit();
  }

  @Override public Unit onBookStatusRequesting(
    final BookStatusRequesting s)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.GONE);
    bb.removeAllViews();
    return Unit.unit();
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedBookDialog);

    final Bundle b = NullCheck.notNull(this.getArguments());
    final OPDSAcquisitionFeedEntry e =
      NullCheck.notNull((OPDSAcquisitionFeedEntry) b
        .getSerializable(CatalogBookDialog.ACQUISITION_ENTRY_ID));
    Log.d(CatalogBookDialog.TAG, "showing dialog for " + e.getID());
    this.entry = e;

    final Simplified app = Simplified.get();
    final BooksType books = app.getBooks();
    books.addObserver(this);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final Resources rr = NullCheck.notNull(this.getResources());
    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry);

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.book_dialog,
        container,
        false));

    final ViewGroup header =
      NullCheck.notNull((ViewGroup) layout.findViewById(R.id.book_header));
    final ViewGroup header_left =
      NullCheck.notNull((ViewGroup) header
        .findViewById(R.id.book_header_left));
    final TextView header_title =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_title));
    final TextView header_subtitle =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_subtitle));
    final ImageView header_cover =
      NullCheck.notNull((ImageView) header
        .findViewById(R.id.book_header_cover));
    final TextView header_authors =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_authors));
    final TextView header_meta =
      NullCheck
        .notNull((TextView) header.findViewById(R.id.book_header_meta));

    final ViewGroup hold_notification =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_hold_notification));

    final ViewGroup bd =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_downloading));
    this.book_downloading = bd;
    this.book_downloading_percent_text =
      NullCheck.notNull((TextView) bd
        .findViewById(R.id.book_downloading_percent_text));
    this.book_downloading_progress =
      NullCheck.notNull((ProgressBar) bd
        .findViewById(R.id.book_downloading_progress));
    this.book_downloading_cancel =
      NullCheck.notNull((Button) bd
        .findViewById(R.id.book_downloading_cancel));

    this.book_buttons =
      NullCheck.notNull((ViewGroup) layout.findViewById(R.id.book_buttons));

    final ViewGroup summary =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_summary_layout));
    final TextView summary_publisher =
      NullCheck.notNull((TextView) summary
        .findViewById(R.id.book_summary_publisher));
    final WebView summary_text =
      NullCheck.notNull((WebView) summary
        .findViewById(R.id.book_summary_text));

    final ViewGroup related_layout =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_related_layout));

    /**
     * Assuming a roughly fixed height for cover images, assume a 4:3 aspect
     * ratio and set the width of the cover layout.
     */

    final int cover_height = header_cover.getLayoutParams().height;
    final int cover_width = (int) ((cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams cp =
      new LinearLayout.LayoutParams(cover_width, LayoutParams.WRAP_CONTENT);
    header_left.setLayoutParams(cp);

    /**
     * Configure detail texts.
     */

    CatalogBookDetail.configureSummaryPublisher(e, summary_publisher);
    CatalogBookDetail.configureSummaryWebView(e, summary_text);

    final Simplified app = Simplified.get();
    final BooksType books = app.getBooks();
    final BookID book_id = BookID.newIDFromEntry(e);
    final OptionType<BookStatusType> status_opt =
      books.booksStatusGet(book_id);
    if (status_opt.isSome()) {
      final BookStatusType status = ((Some<BookStatusType>) status_opt).get();
      status.matchBookStatus(this);
    } else {
      this.onStatusNone(books, book_id, e);
    }

    hold_notification.setVisibility(View.GONE);
    header_title.setText(e.getTitle());

    if (e.getSubtitle().isEmpty() == false) {
      header_subtitle.setText(e.getSubtitle());
    } else {
      header_subtitle.setVisibility(View.GONE);
    }

    CatalogBookDetail.configureViewTextAuthor(e, header_authors);
    CatalogBookDetail.configureViewTextMeta(rr, e, header_meta);

    related_layout.setVisibility(View.GONE);

    final CoverProviderType cover_provider = app.getCoverProvider();
    cover_provider.loadCoverInto(e, header_cover, cover_width, cover_height);

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }
    return layout;
  }

  @Override public void onDestroy()
  {
    super.onDestroy();

    final Simplified app = Simplified.get();
    final BooksType books = app.getBooks();
    books.deleteObserver(this);
  }

  @Override public void onResume()
  {
    super.onResume();

    /**
     * Force the dialog to always appear at the same size, with a decent
     * amount of empty space around it.
     */

    final Activity act = NullCheck.notNull(this.getActivity());
    final WindowManager window_manager =
      NullCheck.notNull((WindowManager) act
        .getSystemService(Context.WINDOW_SERVICE));
    final Display display =
      NullCheck.notNull(window_manager.getDefaultDisplay());

    final DisplayMetrics m = new DisplayMetrics();
    display.getMetrics(m);

    final int width = (int) (m.widthPixels * 0.75);
    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(width, window.getAttributes().height);
  }

  private void onStatusNone(
    final BooksType books,
    final BookID book_id,
    final OPDSAcquisitionFeedEntry e)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    bd.setVisibility(View.GONE);

    if (e.getAcquisitions().isEmpty()) {
      bb.setVisibility(View.GONE);
    } else {
      bb.setVisibility(View.VISIBLE);
      CatalogAcquisitionButtons.configureAllAcquisitionButtonsForLayout(
        NullCheck.notNull(this.getActivity()),
        books,
        bb,
        e,
        book_id);
    }
  }

  @Override public void update(
    final @Nullable Observable observable,
    final @Nullable Object data)
  {
    NullCheck.notNull(observable);

    Log.d(
      CatalogBookDialog.TAG,
      String.format("update %s %s", observable, data));

    final BookStatusType status = NullCheck.notNull((BookStatusType) data);
    final OPDSAcquisitionFeedEntry e = this.entry;

    if (e != null) {
      final BookID id = BookID.newIDFromEntry(e);
      if (id.equals(status.getID())) {
        UIThread.runOnUIThread(new Runnable() {
          @Override public void run()
          {
            status.matchBookStatus(CatalogBookDialog.this);
          }
        });
      }
    }
  }
}
