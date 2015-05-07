package org.nypl.simplified.app.catalog;

import java.util.Observable;
import java.util.Observer;

import org.nypl.simplified.app.CoverProviderType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
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
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.stack.ImmutableStack;
import org.slf4j.Logger;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * An activity showing a full-screen book detail page.
 */

public final class CatalogBookDetailActivity extends CatalogActivity implements
  Observer,
  BookStatusMatcherType<Unit, UnreachableCodeException>,
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>,
  BookStatusDownloadingMatcherType<Unit, UnreachableCodeException>
{
  private static final String CATALOG_BOOK_DETAIL_FEED_ENTRY_ID;

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookDetailActivity.class);
  }

  static {
    CATALOG_BOOK_DETAIL_FEED_ENTRY_ID =
      "org.nypl.simplified.app.CatalogBookDetailActivity.feed_entry";
  }

  /**
   * Configure the given web view to match the height of the rendered content.
   */

  private static void configureSummaryWebViewHeight(
    final WebView summary_text)
  {
    final LinearLayout.LayoutParams q =
      new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT);
    summary_text.setLayoutParams(q);
  }

  public static void setActivityArguments(
    final Bundle b,
    final boolean drawer_open,
    final ImmutableStack<CatalogUpStackEntry> up_stack,
    final OPDSAcquisitionFeedEntry e)
  {
    NullCheck.notNull(b);
    SimplifiedActivity.setActivityArguments(b, drawer_open);
    CatalogActivity.setActivityArguments(b, up_stack);
    b.putSerializable(
      CatalogBookDetailActivity.CATALOG_BOOK_DETAIL_FEED_ENTRY_ID,
      NullCheck.notNull(e));
  }

  public static void startNewActivity(
    final Activity from,
    final ImmutableStack<CatalogUpStackEntry> up_stack,
    final OPDSAcquisitionFeedEntry e)
  {
    final Bundle b = new Bundle();
    CatalogBookDetailActivity.setActivityArguments(b, false, up_stack, e);
    final Intent i = new Intent(from, CatalogBookDetailActivity.class);
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    from.startActivity(i);
  }

  private @Nullable ViewGroup                book_buttons;
  private @Nullable ViewGroup                book_downloading;
  private @Nullable Button                   book_downloading_cancel;
  private @Nullable ViewGroup                book_downloading_failed;
  private @Nullable Button                   book_downloading_failed_dismiss;
  private @Nullable TextView                 book_downloading_failed_text;
  private @Nullable TextView                 book_downloading_percent_text;
  private @Nullable ProgressBar              book_downloading_progress;
  private @Nullable BooksType                books;
  private @Nullable TextView                 debug_status;
  private @Nullable OPDSAcquisitionFeedEntry entry;

  private OPDSAcquisitionFeedEntry getFeedEntry()
  {
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = NullCheck.notNull(i.getExtras());
    return NullCheck
      .notNull((OPDSAcquisitionFeedEntry) a
        .getSerializable(CatalogBookDetailActivity.CATALOG_BOOK_DETAIL_FEED_ENTRY_ID));
  }

  @Override public Unit onBookStatusDownloadCancelled(
    final BookStatusDownloadCancelled c)
  {
    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry);
    final BookID id = c.getID();

    final BooksType in_books = NullCheck.notNull(this.books);
    in_books.bookDownloadAcknowledge(id);

    final OptionType<BookStatusType> status_opt = in_books.booksStatusGet(id);
    this.onStatus(e, id, status_opt);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloaded(
    final BookStatusDownloaded d)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bb.setVisibility(View.VISIBLE);
    bd.setVisibility(View.GONE);
    bdf.setVisibility(View.GONE);

    bb.addView(new CatalogBookDeleteButton(this, d.getID()));
    bb.addView(new CatalogBookReadButton(this, d.getID()));
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadFailed(
    final BookStatusDownloadFailed f)
  {
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

    final BooksType b = NullCheck.notNull(this.books);
    final Button button =
      NullCheck.notNull(this.book_downloading_failed_dismiss);
    button.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        b.bookDownloadAcknowledge(f.getID());
      }
    });

    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloading(
    final BookStatusDownloadingType o)
    throws UnreachableCodeException
  {
    return o.matchBookDownloadingStatus(this);
  }

  @Override public Unit onBookStatusDownloadingPaused(
    final BookStatusDownloadingPaused p)
  {
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadInProgress(
    final BookStatusDownloadInProgress d)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.VISIBLE);
    bdf.setVisibility(View.GONE);

    final DownloadSnapshot snap = d.getDownloadSnapshot();
    CatalogAcquisitionDownloadProgressBar.setProgressBar(
      snap,
      NullCheck.notNull(this.book_downloading_percent_text),
      NullCheck.notNull(this.book_downloading_progress));

    final BooksType in_books = NullCheck.notNull(this.books);
    final Button dc = NullCheck.notNull(this.book_downloading_cancel);
    dc.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        in_books.bookDownloadCancel(d.getID());
      }
    });

    return Unit.unit();
  }

  @Override public Unit onBookStatusLoaned(
    final BookStatusLoaned o)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bd.setVisibility(View.GONE);
    bdf.setVisibility(View.GONE);

    CatalogAcquisitionButtons.addButtons(
      this,
      bb,
      NullCheck.notNull(this.books),
      o.getID(),
      NullCheck.notNull(this.entry));
    return Unit.unit();
  }

  @Override public Unit onBookStatusLoanedType(
    final BookStatusLoanedType o)
  {
    return o.matchBookLoanedStatus(this);
  }

  private void onBookStatusNone(
    final BookID book_id,
    final OPDSAcquisitionFeedEntry e)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    final BooksType in_books = NullCheck.notNull(this.books);

    bd.setVisibility(View.GONE);
    bdf.setVisibility(View.GONE);
    bb.setVisibility(View.VISIBLE);
    bb.removeAllViews();

    CatalogAcquisitionButtons.addButtons(this, bb, in_books, book_id, e);
  }

  @Override public Unit onBookStatusRequestingDownload(
    final BookStatusRequestingDownload d)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.GONE);
    bb.removeAllViews();
    bdf.setVisibility(View.GONE);
    return Unit.unit();
  }

  @Override public Unit onBookStatusRequestingLoan(
    final BookStatusRequestingLoan s)
  {
    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);
    final ViewGroup bdf = NullCheck.notNull(this.book_downloading_failed);

    bb.setVisibility(View.GONE);
    bd.setVisibility(View.GONE);
    bb.removeAllViews();
    bdf.setVisibility(View.GONE);
    return Unit.unit();
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.entry = this.getFeedEntry();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    this.books = app.getBooks();
    this.books.booksObservableAddObserver(this);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    final BooksType in_books = NullCheck.notNull(this.books);
    in_books.booksObservableDeleteObserver(this);
  }

  @Override protected void onResume()
  {
    super.onResume();

    final ScrollView sv = new ScrollView(this);
    final LayoutParams p =
      new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    sv.setLayoutParams(p);
    sv.addOnLayoutChangeListener(new OnLayoutChangeListener() {
      @Override public void onLayoutChange(
        final @Nullable View v,
        final int left,
        final int top,
        final int right,
        final int bottom,
        final int oldLeft,
        final int oldTop,
        final int oldRight,
        final int oldBottom)
      {
        sv.setScrollY(0);
      }
    });

    final LayoutInflater inflater =
      NullCheck.notNull(this.getLayoutInflater());

    final View layout = inflater.inflate(R.layout.book_dialog, sv, false);
    sv.addView(layout);

    final OPDSAcquisitionFeedEntry e = NullCheck.notNull(this.entry);
    final Resources rr = NullCheck.notNull(this.getResources());

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

    CatalogBookDetail.configureSummaryPublisher(e, summary_publisher);

    final BooksType in_books = NullCheck.notNull(this.books);
    final BookID book_id = BookID.newIDFromEntry(e);
    final OptionType<BookStatusType> status_opt =
      in_books.booksStatusGet(book_id);
    this.onStatus(e, book_id, status_opt);

    CatalogBookDetail.configureSummaryWebView(e, summary_text);
    CatalogBookDetailActivity.configureSummaryWebViewHeight(summary_text);

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

    final SimplifiedCatalogAppServicesType cs =
      Simplified.getCatalogAppServices();
    final CoverProviderType cover_provider = cs.getCoverProvider();
    cover_provider.loadCoverInto(e, header_cover, cover_width, cover_height);

    final FrameLayout content_area = this.getContentFrame();
    content_area.removeAllViews();
    content_area.addView(sv);
    content_area.requestLayout();
  }

  private void onStatus(
    final OPDSAcquisitionFeedEntry e,
    final BookID id,
    final OptionType<BookStatusType> status_opt)
  {
    if (status_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) status_opt;
      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          some.get().matchBookStatus(CatalogBookDetailActivity.this);
        }
      });
    } else {
      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          CatalogBookDetailActivity.this.onBookStatusNone(id, e);
        }
      });
    }
  }

  @Override public void update(
    final @Nullable Observable observable,
    final @Nullable Object data)
  {
    NullCheck.notNull(observable);

    CatalogBookDetailActivity.LOG.debug("update: {} {}", observable, data);

    final BookID update_id = NullCheck.notNull((BookID) data);
    final OPDSAcquisitionFeedEntry e = this.entry;

    if (e != null) {
      final BookID id = BookID.newIDFromEntry(e);
      if (id.equals(update_id)) {
        final BooksType in_books = NullCheck.notNull(this.books);
        final OptionType<BookStatusType> status_opt =
          in_books.booksStatusGet(id);
        this.onStatus(e, id, status_opt);
      }
    }
  }
}
