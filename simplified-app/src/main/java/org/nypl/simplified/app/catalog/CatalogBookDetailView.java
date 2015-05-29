package org.nypl.simplified.app.catalog;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
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
import org.nypl.simplified.opds.core.OPDSCategory;
import org.slf4j.Logger;

import android.app.Activity;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
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

@SuppressWarnings("synthetic-access") public final class CatalogBookDetailView implements
  Observer,
  BookStatusMatcherType<Unit, UnreachableCodeException>,
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>,
  BookStatusDownloadingMatcherType<Unit, UnreachableCodeException>
{
  private static final URI    GENRES_URI;
  private static final String GENRES_URI_TEXT;
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookDetailView.class);
  }

  static {
    GENRES_URI =
      NullCheck.notNull(URI
        .create("http://librarysimplified.org/terms/genres/Simplified/"));
    GENRES_URI_TEXT =
      NullCheck.notNull(CatalogBookDetailView.GENRES_URI.toString());
  }

  private static void configureSummaryPublisher(
    final OPDSAcquisitionFeedEntry e,
    final TextView summary_publisher)
  {
    final OptionType<String> pub = e.getPublisher();
    if (pub.isSome()) {
      final Some<String> some = (Some<String>) pub;
      summary_publisher.setText(some.get());
    }
  }

  private static void configureSummaryWebView(
    final OPDSAcquisitionFeedEntry e,
    final WebView summary_text)
  {
    final StringBuilder text = new StringBuilder();
    text.append("<html>");
    text.append("<head>");
    text.append("<style>body {");
    text.append("padding: 0;");
    text.append("padding-right: 2em;");
    text.append("margin: 0;");
    text.append("}</style>");
    text.append("</head>");
    text.append("<body>");
    text.append(e.getSummary());
    text.append("</body>");
    text.append("</html>");

    final WebSettings summary_text_settings = summary_text.getSettings();
    summary_text_settings.setAllowContentAccess(false);
    summary_text_settings.setAllowFileAccess(false);
    summary_text_settings.setAllowFileAccessFromFileURLs(false);
    summary_text_settings.setAllowUniversalAccessFromFileURLs(false);
    summary_text_settings.setBlockNetworkLoads(true);
    summary_text_settings.setBlockNetworkImage(true);
    summary_text_settings.setDefaultTextEncodingName("UTF-8");
    summary_text_settings.setDefaultFixedFontSize(12);
    summary_text_settings.setDefaultFontSize(12);
    summary_text.loadDataWithBaseURL(
      null,
      text.toString(),
      "text/html",
      "UTF-8",
      null);
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

  private static void configureViewTextAuthor(
    final OPDSAcquisitionFeedEntry e,
    final TextView authors)
  {
    final StringBuilder buffer = new StringBuilder();
    final List<String> as = e.getAuthors();
    for (int index = 0; index < as.size(); ++index) {
      final String a = NullCheck.notNull(as.get(index));
      if (index > 0) {
        buffer.append("\n");
      }
      buffer.append(a);
    }
    authors.setText(NullCheck.notNull(buffer.toString()));
  }

  private static void configureViewTextMeta(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final TextView meta)
  {
    final StringBuilder buffer = new StringBuilder();
    CatalogBookDetailView.createViewTextPublicationDate(rr, e, buffer);
    CatalogBookDetailView.createViewTextPublisher(rr, e, buffer);
    CatalogBookDetailView.createViewTextCategories(rr, e, buffer);
    meta.setText(NullCheck.notNull(buffer.toString()));
  }

  private static void createViewTextCategories(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final StringBuilder buffer)
  {
    final List<OPDSCategory> cats = e.getCategories();

    boolean has_genres = false;
    for (int index = 0; index < cats.size(); ++index) {
      final OPDSCategory c = NullCheck.notNull(cats.get(index));
      if (CatalogBookDetailView.GENRES_URI_TEXT.equals(c.getScheme())) {
        has_genres = true;
      }
    }

    if (has_genres) {
      if (buffer.length() > 0) {
        buffer.append("\n");
      }

      buffer.append(NullCheck.notNull(rr
        .getString(R.string.catalog_categories)));
      buffer.append(": ");

      for (int index = 0; index < cats.size(); ++index) {
        final OPDSCategory c = NullCheck.notNull(cats.get(index));
        if (CatalogBookDetailView.GENRES_URI_TEXT.equals(c.getScheme())) {
          buffer.append(c.getTerm());
          if ((index + 1) <= cats.size()) {
            buffer.append(", ");
          }
        }
      }
    }
  }

  private static String createViewTextPublicationDate(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final StringBuilder buffer)
  {
    if (buffer.length() > 0) {
      buffer.append("\n");
    }

    final OptionType<Calendar> p_opt = e.getPublished();
    if (p_opt.isSome()) {
      final Some<Calendar> some = (Some<Calendar>) p_opt;
      final Calendar p = some.get();
      final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
      buffer.append(NullCheck.notNull(rr
        .getString(R.string.catalog_publication_date)));
      buffer.append(": ");
      buffer.append(fmt.format(p.getTime()));
      return NullCheck.notNull(buffer.toString());
    }

    return "";
  }

  private static void createViewTextPublisher(
    final Resources rr,
    final OPDSAcquisitionFeedEntry e,
    final StringBuilder buffer)
  {
    final OptionType<String> pub = e.getPublisher();
    if (pub.isSome()) {
      final Some<String> some = (Some<String>) pub;

      if (buffer.length() > 0) {
        buffer.append("\n");
      }

      buffer.append(NullCheck.notNull(rr
        .getString(R.string.catalog_publisher)));
      buffer.append(": ");
      buffer.append(some.get());
    }
  }

  private final Activity      activity;
  private final ViewGroup     book_buttons;
  private final ViewGroup     book_downloading;
  private final Button        book_downloading_cancel;
  private final ViewGroup     book_downloading_failed;
  private final Button        book_downloading_failed_dismiss;
  private final TextView      book_downloading_failed_text;
  private final TextView      book_downloading_percent_text;
  private final ProgressBar   book_downloading_progress;
  private final BooksType     books;
  private final FeedEntryOPDS entry;
  private final ScrollView    scroll_view;

  public CatalogBookDetailView(
    final Activity in_activity,
    final LayoutInflater in_inflater,
    final FeedEntryOPDS in_entry)
  {
    NullCheck.notNull(in_inflater);
    NullCheck.notNull(in_activity);
    NullCheck.notNull(in_entry);

    this.activity = NullCheck.notNull(in_activity);
    this.entry = NullCheck.notNull(in_entry);

    final ScrollView sv = new ScrollView(in_activity);
    this.scroll_view = sv;

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

    final View layout = in_inflater.inflate(R.layout.book_dialog, sv, false);
    sv.addView(layout);

    final SimplifiedCatalogAppServicesType cs =
      Simplified.getCatalogAppServices();
    this.books = cs.getBooks();
    final BookCoverProviderType cover_provider = cs.getCoverProvider();
    final Resources rr = NullCheck.notNull(in_activity.getResources());

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

    final OPDSAcquisitionFeedEntry eo = in_entry.getFeedEntry();
    CatalogBookDetailView.configureSummaryPublisher(eo, summary_publisher);

    final BookID book_id = in_entry.getBookID();
    final OptionType<BookStatusType> status_opt =
      this.books.booksStatusGet(book_id);
    this.onStatus(in_entry, status_opt);

    CatalogBookDetailView.configureSummaryWebView(eo, summary_text);
    CatalogBookDetailView.configureSummaryWebViewHeight(summary_text);

    hold_notification.setVisibility(View.GONE);
    header_title.setText(eo.getTitle());

    CatalogBookDetailView.configureViewTextAuthor(eo, header_authors);
    CatalogBookDetailView.configureViewTextMeta(rr, eo, header_meta);

    related_layout.setVisibility(View.GONE);

    cover_provider.loadCoverInto(
      in_entry,
      header_cover,
      cover_width,
      cover_height);

  }

  public ScrollView getScrollView()
  {
    return this.scroll_view;
  }

  @Override public Unit onBookStatusDownloadCancelled(
    final BookStatusDownloadCancelled c)
  {
    final BookID id = c.getID();
    this.books.bookDownloadAcknowledge(id);
    final OptionType<BookStatusType> status_opt =
      this.books.booksStatusGet(id);
    this.onStatus(this.entry, status_opt);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloaded(
    final BookStatusDownloaded d)
  {
    this.book_buttons.removeAllViews();
    this.book_buttons.addView(new CatalogBookDeleteButton(this.activity, d
      .getID()));
    this.book_buttons.addView(new CatalogBookReadButton(this.activity, d
      .getID()));
    this.book_buttons.setVisibility(View.VISIBLE);
    this.book_downloading.setVisibility(View.GONE);
    this.book_downloading_failed.setVisibility(View.GONE);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadFailed(
    final BookStatusDownloadFailed f)
  {
    this.book_buttons.setVisibility(View.GONE);
    this.book_downloading.setVisibility(View.GONE);
    this.book_downloading_failed.setVisibility(View.VISIBLE);

    final DownloadSnapshot snap = f.getDownloadSnapshot();
    final OptionType<Throwable> e_opt = snap.getError();
    if (e_opt.isSome()) {
      final Throwable e = ((Some<Throwable>) e_opt).get();
      this.book_downloading_failed_text.setText(e.getMessage());
    } else {
      this.book_downloading_failed_text.setText("");
    }

    final Button button =
      NullCheck.notNull(this.book_downloading_failed_dismiss);
    button.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        CatalogBookDetailView.this.books.bookDownloadAcknowledge(f.getID());
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
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadInProgress(
    final BookStatusDownloadInProgress d)
  {
    this.book_buttons.setVisibility(View.GONE);
    this.book_downloading.setVisibility(View.VISIBLE);
    this.book_downloading_failed.setVisibility(View.GONE);

    final DownloadSnapshot snap = d.getDownloadSnapshot();
    CatalogDownloadProgressBar.setProgressBar(
      snap,
      NullCheck.notNull(this.book_downloading_percent_text),
      NullCheck.notNull(this.book_downloading_progress));

    final Button dc = NullCheck.notNull(this.book_downloading_cancel);
    dc.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        CatalogBookDetailView.this.books.bookDownloadCancel(d.getID());
      }
    });

    return Unit.unit();
  }

  @Override public Unit onBookStatusLoaned(
    final BookStatusLoaned o)
  {
    this.book_buttons.removeAllViews();
    this.book_buttons.setVisibility(View.VISIBLE);
    this.book_downloading.setVisibility(View.GONE);
    this.book_downloading_failed.setVisibility(View.GONE);

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.book_buttons,
      NullCheck.notNull(this.books),
      NullCheck.notNull(this.entry));
    return Unit.unit();
  }

  @Override public Unit onBookStatusLoanedType(
    final BookStatusLoanedType o)
  {
    return o.matchBookLoanedStatus(this);
  }

  private void onBookStatusNone(
    final FeedEntryOPDS e)
  {
    this.book_buttons.removeAllViews();
    this.book_buttons.setVisibility(View.VISIBLE);
    this.book_downloading.setVisibility(View.GONE);
    this.book_downloading_failed.setVisibility(View.GONE);

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.book_buttons,
      this.books,
      e);
  }

  @Override public Unit onBookStatusRequestingDownload(
    final BookStatusRequestingDownload d)
  {
    this.book_buttons.setVisibility(View.GONE);
    this.book_buttons.removeAllViews();
    this.book_downloading.setVisibility(View.GONE);
    this.book_downloading_failed.setVisibility(View.GONE);
    return Unit.unit();
  }

  @Override public Unit onBookStatusRequestingLoan(
    final BookStatusRequestingLoan s)
  {
    this.book_buttons.setVisibility(View.GONE);
    this.book_buttons.removeAllViews();
    this.book_downloading.setVisibility(View.GONE);
    this.book_downloading_failed.setVisibility(View.GONE);
    return Unit.unit();
  }

  private void onStatus(
    final FeedEntryOPDS e,
    final OptionType<BookStatusType> status_opt)
  {
    if (status_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) status_opt;
      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          some.get().matchBookStatus(CatalogBookDetailView.this);
        }
      });
    } else {
      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          CatalogBookDetailView.this.onBookStatusNone(e);
        }
      });
    }
  }

  @Override public void update(
    final @Nullable Observable observable,
    final @Nullable Object data)
  {
    NullCheck.notNull(observable);

    CatalogBookDetailView.LOG.debug("update: {} {}", observable, data);

    final BookID update_id = NullCheck.notNull((BookID) data);
    final BookID current_id = this.entry.getBookID();
    if (current_id.equals(update_id)) {
      final OptionType<BookStatusType> status_opt =
        this.books.booksStatusGet(current_id);
      this.onStatus(this.entry, status_opt);
    }
  }
}
