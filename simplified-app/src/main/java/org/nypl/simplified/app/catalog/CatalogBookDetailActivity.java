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

import com.google.common.collect.ImmutableList;
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
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookDetailActivity.class);
  }

  private static final String CATALOG_BOOK_DETAIL_FEED_ENTRY_ID;

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
    final ImmutableList<CatalogUpStackEntry> up_stack,
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
    final ImmutableList<CatalogUpStackEntry> up_stack,
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
  private @Nullable TextView                 book_downloading_percent_text;
  private @Nullable ProgressBar              book_downloading_progress;
  private @Nullable OPDSAcquisitionFeedEntry entry;

  private OPDSAcquisitionFeedEntry getFeedEntry()
  {
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = NullCheck.notNull(i.getExtras());
    return NullCheck
      .notNull((OPDSAcquisitionFeedEntry) a
        .getSerializable(CatalogBookDetailActivity.CATALOG_BOOK_DETAIL_FEED_ENTRY_ID));
  }

  @Override public Unit onBookStatusCancelled(
    final BookStatusCancelled c)
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
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

    final Resources rr = NullCheck.notNull(this.getResources());
    final Button b = new Button(this);
    b.setText(NullCheck.notNull(rr.getString(R.string.catalog_book_read)));
    b.setTextSize(12.0f);
    b.setOnClickListener(new CatalogBookRead(this, d.getID()));
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
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final ViewGroup bb = NullCheck.notNull(this.book_buttons);
    final ViewGroup bd = NullCheck.notNull(this.book_downloading);

    bb.setVisibility(View.VISIBLE);
    bd.setVisibility(View.GONE);

    CatalogAcquisitionButtons.configureAllAcquisitionButtonsForLayout(
      this,
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

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.entry = this.getFeedEntry();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.addObserver(this);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.deleteObserver(this);
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

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
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

    final CoverProviderType cover_provider = app.getCoverProvider();
    cover_provider.loadCoverInto(e, header_cover, cover_width, cover_height);

    final FrameLayout content_area = this.getContentFrame();
    content_area.removeAllViews();
    content_area.addView(sv);
    content_area.requestLayout();
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
        this,
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

    CatalogBookDetailActivity.LOG.debug("update: {} {}", observable, data);

    final BookStatusType status = NullCheck.notNull((BookStatusType) data);
    final OPDSAcquisitionFeedEntry e = this.entry;

    if (e != null) {
      final BookID id = BookID.newIDFromEntry(e);
      if (id.equals(status.getID())) {
        UIThread.runOnUIThread(new Runnable() {
          @Override public void run()
          {
            status.matchBookStatus(CatalogBookDetailActivity.this);
          }
        });
      }
    }
  }
}
