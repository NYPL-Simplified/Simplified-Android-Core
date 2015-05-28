package org.nypl.simplified.app.catalog;

import java.util.Observable;
import java.util.Observer;

import org.nypl.simplified.app.BookCoverProviderType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.utilities.LogUtilities;
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
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
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

@SuppressWarnings("synthetic-access") public final class CatalogBookDialog extends
  DialogFragment implements
  Observer,
  BookStatusMatcherType<Unit, UnreachableCodeException>,
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>,
  BookStatusDownloadingMatcherType<Unit, UnreachableCodeException>
{
  private static final String ACQUISITION_ENTRY_ID;
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookDialog.class);
  }

  static {
    ACQUISITION_ENTRY_ID = "org.nypl.simplified.app.CatalogBookDialog.entry";
  }

  public static CatalogBookDialog newDialog(
    final FeedEntryOPDS e)
  {
    final CatalogBookDialog c = new CatalogBookDialog();
    final Bundle b = new Bundle();
    b.putSerializable(
      CatalogBookDialog.ACQUISITION_ENTRY_ID,
      NullCheck.notNull(e));
    c.setArguments(b);
    return c;
  }

  private @Nullable ViewGroup     book_buttons;
  private @Nullable ViewGroup     book_downloading;
  private @Nullable Button        book_downloading_cancel;
  private @Nullable ViewGroup     book_downloading_failed;
  private @Nullable Button        book_downloading_failed_dismiss;
  private @Nullable TextView      book_downloading_failed_text;
  private @Nullable TextView      book_downloading_percent_text;
  private @Nullable ProgressBar   book_downloading_progress;
  private @Nullable TextView      debug_status;
  private @Nullable FeedEntryOPDS entry;

  public CatalogBookDialog()
  {
    // Fragments must have no-arg constructors.
  }

  @Override public Unit onBookStatusDownloadCancelled(
    final BookStatusDownloadCancelled c)
  {
    this.setStatus("cancelled");

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    final FeedEntryOPDS e = NullCheck.notNull(this.entry);
    books.bookDownloadAcknowledge(e.getBookID());
    this.onStatusNone(books, e);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloaded(
    final BookStatusDownloaded d)
  {
    this.setStatus("downloaded");

    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bb.setVisibility(View.VISIBLE);
    bd.setVisibility(View.GONE);
    bdf.setVisibility(View.GONE);

    final Activity act = NullCheck.notNull(this.getActivity());
    bb.addView(new CatalogBookDeleteButton(act, d.getID()));
    bb.addView(new CatalogBookReadButton(act, d.getID()));
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadFailed(
    final BookStatusDownloadFailed f)
  {
    this.setStatus("download-failed");

    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.GONE);
    bdf.setVisibility(View.VISIBLE);

    final TextView ft = NullCheck.notNull(this.book_downloading_failed_text);
    final DownloadSnapshot snap = f.getDownloadSnapshot();
    final OptionType<Throwable> e_opt = snap.getError();
    if (e_opt.isSome()) {
      final Throwable e = ((Some<Throwable>) e_opt).get();
      ft.setText(e.getMessage());
    } else {
      ft.setText("");
    }

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final Button button =
      NullCheck.notNull(this.book_downloading_failed_dismiss);
    button.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        books.bookDownloadAcknowledge(f.getID());
      }
    });

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
    this.setStatus("downloading-paused");
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadInProgress(
    final BookStatusDownloadInProgress d)
  {
    this.setStatus("download-in-progress");

    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.VISIBLE);
    bdf.setVisibility(View.GONE);

    final DownloadSnapshot snap = d.getDownloadSnapshot();
    CatalogDownloadProgressBar.setProgressBar(
      snap,
      NullCheck.notNull(this.book_downloading_percent_text),
      NullCheck.notNull(this.book_downloading_progress));

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
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

  @Override public Unit onBookStatusLoaned(
    final BookStatusLoaned o)
  {
    this.setStatus("loaned");

    final SimplifiedCatalogAppServicesType cs =
      Simplified.getCatalogAppServices();

    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bd.setVisibility(View.GONE);
    bdf.setVisibility(View.GONE);

    CatalogAcquisitionButtons.addButtons(
      NullCheck.notNull(this.getActivity()),
      bb,
      cs.getBooks(),
      NullCheck.notNull(this.entry));
    return Unit.unit();
  }

  @Override public Unit onBookStatusLoanedType(
    final BookStatusLoanedType o)
  {
    return o.matchBookLoanedStatus(this);
  }

  @Override public Unit onBookStatusRequestingDownload(
    final BookStatusRequestingDownload d)
  {
    this.setStatus("requesting-download");

    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.GONE);
    bdf.setVisibility(View.GONE);

    bb.removeAllViews();
    return Unit.unit();
  }

  @Override public Unit onBookStatusRequestingLoan(
    final BookStatusRequestingLoan s)
  {
    this.setStatus("requesting-loan");

    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.GONE);
    bdf.setVisibility(View.GONE);

    bb.removeAllViews();
    return Unit.unit();
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedBookDialog);

    final Bundle b = NullCheck.notNull(this.getArguments());
    final FeedEntryOPDS e =
      NullCheck.notNull((FeedEntryOPDS) b
        .getSerializable(CatalogBookDialog.ACQUISITION_ENTRY_ID));

    CatalogBookDialog.LOG.debug("showing dialog for id: {}", e.getBookID());
    this.entry = e;

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.booksObservableAddObserver(this);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);
    final Resources rr = NullCheck.notNull(this.getResources());
    final FeedEntryOPDS e = NullCheck.notNull(this.entry);

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.book_dialog,
        container,
        false));

    /**
     * Show the book status if status debugging is enabled.
     */

    final TextView in_debug_status =
      NullCheck.notNull((TextView) layout
        .findViewById(R.id.book_debug_status));
    if (rr.getBoolean(R.bool.debug_catalog_cell_view_states)) {
      in_debug_status.setVisibility(View.VISIBLE);
    } else {
      in_debug_status.setVisibility(View.GONE);
    }
    this.debug_status = in_debug_status;

    final ViewGroup header =
      NullCheck.notNull((ViewGroup) layout.findViewById(R.id.book_header));
    final ViewGroup header_left =
      NullCheck.notNull((ViewGroup) header
        .findViewById(R.id.book_header_left));
    final TextView header_title =
      NullCheck.notNull((TextView) header
        .findViewById(R.id.book_header_title));
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

    final ViewGroup bdf =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.book_downloading_failed));
    this.book_downloading_failed_text =
      NullCheck.notNull((TextView) bdf
        .findViewById(R.id.book_downloading_failed_text));
    this.book_downloading_failed_dismiss =
      NullCheck.notNull((Button) bdf
        .findViewById(R.id.book_downloading_failed_dismiss));
    this.book_downloading_failed = bdf;

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

    final OPDSAcquisitionFeedEntry eo = e.getFeedEntry();
    CatalogBookDetail.configureSummaryPublisher(eo, summary_publisher);
    CatalogBookDetail.configureSummaryWebView(eo, summary_text);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    final BookID book_id = e.getBookID();
    final OptionType<BookStatusType> status_opt =
      books.booksStatusGet(book_id);
    if (status_opt.isSome()) {
      final BookStatusType status = ((Some<BookStatusType>) status_opt).get();
      status.matchBookStatus(this);
    } else {
      this.onStatusNone(books, e);
    }

    hold_notification.setVisibility(View.GONE);
    header_title.setText(eo.getTitle());

    CatalogBookDetail.configureViewTextAuthor(eo, header_authors);
    CatalogBookDetail.configureViewTextMeta(rr, eo, header_meta);

    related_layout.setVisibility(View.GONE);

    final BookCoverProviderType cover_provider = app.getCoverProvider();
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

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.booksObservableDeleteObserver(this);
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

  private void onStatus(
    final FeedEntryOPDS e,
    final OptionType<BookStatusType> status_opt)
  {
    final BooksType books = Simplified.getCatalogAppServices().getBooks();

    if (status_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) status_opt;
      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          some.get().matchBookStatus(CatalogBookDialog.this);
        }
      });
    } else {
      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          CatalogBookDialog.this.onStatusNone(books, e);
        }
      });
    }
  }

  private void onStatusNone(
    final BooksType books,
    final FeedEntryOPDS e)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bd.setVisibility(View.GONE);
    bb.setVisibility(View.VISIBLE);
    bdf.setVisibility(View.GONE);
    bb.removeAllViews();

    CatalogAcquisitionButtons.addButtons(
      NullCheck.notNull(this.getActivity()),
      bb,
      books,
      e);
  }

  private void setStatus(
    final String name)
  {
    final Resources rr = NullCheck.notNull(this.getResources());
    if (rr.getBoolean(R.bool.debug_catalog_cell_view_states)) {
      NullCheck.notNull(this.debug_status).setText(NullCheck.notNull(name));
    }
  }

  @Override public void update(
    final @Nullable Observable observable,
    final @Nullable Object data)
  {
    NullCheck.notNull(observable);

    CatalogBookDialog.LOG.debug("update: {} {}", observable, data);

    final BooksType books = Simplified.getCatalogAppServices().getBooks();
    final BookID update_id = NullCheck.notNull((BookID) data);
    final FeedEntryOPDS e = this.entry;

    if (e != null) {
      final BookID current_id = e.getBookID();
      if (current_id.equals(update_id)) {
        final OptionType<BookStatusType> status_opt =
          books.booksStatusGet(current_id);
        this.onStatus(e, status_opt);
      }
    }
  }
}
