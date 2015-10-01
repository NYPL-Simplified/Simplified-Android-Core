package org.nypl.simplified.app.catalog;

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
import org.nypl.simplified.app.BookCoverProviderType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot;
import org.nypl.simplified.books.core.BookDatabaseReadableType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookStatusDownloadFailed;
import org.nypl.simplified.books.core.BookStatusDownloadInProgress;
import org.nypl.simplified.books.core.BookStatusDownloaded;
import org.nypl.simplified.books.core.BookStatusDownloadingMatcherType;
import org.nypl.simplified.books.core.BookStatusDownloadingType;
import org.nypl.simplified.books.core.BookStatusHeld;
import org.nypl.simplified.books.core.BookStatusHeldReady;
import org.nypl.simplified.books.core.BookStatusHoldable;
import org.nypl.simplified.books.core.BookStatusLoanable;
import org.nypl.simplified.books.core.BookStatusLoaned;
import org.nypl.simplified.books.core.BookStatusLoanedMatcherType;
import org.nypl.simplified.books.core.BookStatusLoanedType;
import org.nypl.simplified.books.core.BookStatusMatcherType;
import org.nypl.simplified.books.core.BookStatusRequestingDownload;
import org.nypl.simplified.books.core.BookStatusRequestingLoan;
import org.nypl.simplified.books.core.BookStatusRequestingRevoke;
import org.nypl.simplified.books.core.BookStatusRevokeFailed;
import org.nypl.simplified.books.core.BookStatusType;
import org.nypl.simplified.books.core.BooksStatusCacheType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryCorrupt;
import org.nypl.simplified.books.core.FeedEntryMatcherType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.FeedEntryType;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.nypl.simplified.opds.core.OPDSCategory;
import org.slf4j.Logger;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A book detail view.
 */

public final class CatalogBookDetailView implements Observer,
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
    GENRES_URI = NullCheck.notNull(
      URI.create("http://librarysimplified.org/terms/genres/Simplified/"));
    GENRES_URI_TEXT =
      NullCheck.notNull(CatalogBookDetailView.GENRES_URI.toString());
  }

  private final Activity                       activity;
  private final ViewGroup                      book_download;
  private final LinearLayout                   book_download_buttons;
  private final TextView                       book_download_text;
  private final ViewGroup                      book_downloading;
  private final Button                         book_downloading_cancel;
  private final ViewGroup                      book_downloading_failed;
  private final Button                         book_downloading_failed_dismiss;
  private final Button                         book_downloading_failed_retry;
  private final TextView                       book_downloading_percent_text;
  private final ProgressBar                    book_downloading_progress;
  private final BooksType                      books;
  private final AtomicReference<FeedEntryOPDS> entry;
  private final ScrollView                     scroll_view;
  private final TextView                       book_downloading_label;
  private final TextView                       book_downloading_failed_text;
  private final TextView                       book_debug_status;

  /**
   * Construct a detail view.
   *
   * @param in_activity The host activity
   * @param in_inflater A layout inflater
   * @param in_entry    The book
   */

  public CatalogBookDetailView(
    final Activity in_activity,
    final LayoutInflater in_inflater,
    final FeedEntryOPDS in_entry)
  {
    NullCheck.notNull(in_inflater);
    NullCheck.notNull(in_activity);
    NullCheck.notNull(in_entry);

    this.activity = NullCheck.notNull(in_activity);
    this.entry =
      new AtomicReference<FeedEntryOPDS>(NullCheck.notNull(in_entry));

    final ScrollView sv = new ScrollView(in_activity);
    this.scroll_view = sv;

    final LayoutParams p =
      new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    sv.setLayoutParams(p);
    sv.addOnLayoutChangeListener(
      new OnLayoutChangeListener()
      {
        @Override public void onLayoutChange(
          final @Nullable View v,
          final int left,
          final int top,
          final int right,
          final int bottom,
          final int old_left,
          final int old_top,
          final int old_right,
          final int old_bottom)
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

    final TextView in_debug_status = NullCheck.notNull(
      (TextView) layout.findViewById(R.id.book_debug_status));

    if (rr.getBoolean(R.bool.debug_catalog_cell_view_states)) {
      in_debug_status.setVisibility(View.VISIBLE);
    } else {
      in_debug_status.setVisibility(View.GONE);
    }
    this.book_debug_status = in_debug_status;

    final ViewGroup header =
      NullCheck.notNull((ViewGroup) layout.findViewById(R.id.book_header));
    final ViewGroup header_left = NullCheck.notNull(
      (ViewGroup) header.findViewById(R.id.book_header_left));
    final TextView header_title = NullCheck.notNull(
      (TextView) header.findViewById(R.id.book_header_title));
    final ImageView header_cover = NullCheck.notNull(
      (ImageView) header.findViewById(R.id.book_header_cover));
    final TextView header_authors = NullCheck.notNull(
      (TextView) header.findViewById(R.id.book_header_authors));
    final TextView header_meta =
      NullCheck.notNull((TextView) header.findViewById(R.id.book_header_meta));

    final ViewGroup bdd = NullCheck.notNull(
      (ViewGroup) layout.findViewById(R.id.book_dialog_downloading));
    this.book_downloading = bdd;

    this.book_downloading_label = NullCheck.notNull(
      (TextView) bdd.findViewById(R.id.book_dialog_downloading_label));
    this.book_downloading_percent_text = NullCheck.notNull(
      (TextView) bdd.findViewById(R.id.book_dialog_downloading_percent_text));
    this.book_downloading_progress = NullCheck.notNull(
      (ProgressBar) bdd.findViewById(R.id.book_dialog_downloading_progress));
    this.book_downloading_cancel = NullCheck.notNull(
      (Button) bdd.findViewById(R.id.book_dialog_downloading_cancel));

    final ViewGroup bdf = NullCheck.notNull(
      (ViewGroup) layout.findViewById(R.id.book_dialog_downloading_failed));
    this.book_downloading_failed_text = NullCheck.notNull(
      (TextView) bdf.findViewById(R.id.book_dialog_downloading_failed_text));
    this.book_downloading_failed_dismiss = NullCheck.notNull(
      (Button) bdf.findViewById(R.id.book_dialog_downloading_failed_dismiss));
    this.book_downloading_failed_retry = NullCheck.notNull(
      (Button) bdf.findViewById(R.id.book_dialog_downloading_failed_retry));
    this.book_downloading_failed = bdf;

    final ViewGroup bd = NullCheck.notNull(
      (ViewGroup) layout.findViewById(R.id.book_dialog_download));
    this.book_download = bd;
    this.book_download_buttons = NullCheck.notNull(
      (LinearLayout) bd.findViewById(R.id.book_dialog_download_buttons));
    this.book_download_text = NullCheck.notNull(
      (TextView) bd.findViewById(R.id.book_dialog_download_text));

    final ViewGroup summary = NullCheck.notNull(
      (ViewGroup) layout.findViewById(R.id.book_summary_layout));
    final TextView summary_publisher = NullCheck.notNull(
      (TextView) summary.findViewById(R.id.book_summary_publisher));
    final WebView summary_text = NullCheck.notNull(
      (WebView) summary.findViewById(R.id.book_summary_text));

    final ViewGroup related_layout = NullCheck.notNull(
      (ViewGroup) layout.findViewById(R.id.book_related_layout));

    /**
     * Assuming a roughly fixed height for cover images, assume a 4:3 aspect
     * ratio and set the width of the cover layout.
     */

    final int cover_height = header_cover.getLayoutParams().height;
    final int cover_width = (int) (((double) cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams cp =
      new LinearLayout.LayoutParams(cover_width, LayoutParams.WRAP_CONTENT);
    header_left.setLayoutParams(cp);

    /**
     * Configure detail texts.
     */

    final OPDSAcquisitionFeedEntry eo = in_entry.getFeedEntry();
    CatalogBookDetailView.configureSummaryPublisher(eo, summary_publisher);

    final BookID book_id = in_entry.getBookID();
    final BooksStatusCacheType status_cache = this.books.bookGetStatusCache();
    final OptionType<BookStatusType> status_opt =
      status_cache.booksStatusGet(book_id);
    this.onStatus(in_entry, status_opt);

    CatalogBookDetailView.configureSummaryWebView(eo, summary_text);
    CatalogBookDetailView.configureSummaryWebViewHeight(summary_text);

    header_title.setText(eo.getTitle());

    CatalogBookDetailView.configureViewTextAuthor(eo, header_authors);
    CatalogBookDetailView.configureViewTextMeta(rr, eo, header_meta);

    related_layout.setVisibility(View.GONE);

    cover_provider.loadCoverInto(
      in_entry, header_cover, cover_width, cover_height);
  }

  private static void configureButtonsHeight(
    final Resources rr,
    final LinearLayout layout)
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final int dp35 = (int) rr.getDimension(R.dimen.button_standard_height);
    final int dp8 = (int) app.screenDPToPixels(8);
    final int button_count = layout.getChildCount();
    for (int index = 0; index < button_count; ++index) {
      final View v = layout.getChildAt(index);

      Assertions.checkPrecondition(
        v instanceof CatalogBookButtonType,
        "view %s is an instance of CatalogBookButtonType",
        v);

      final android.widget.LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp35);
      lp.leftMargin = dp8;

      v.setLayoutParams(lp);
    }
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
      null, text.toString(), "text/html", "UTF-8", null);
  }

  /**
   * Configure the given web view to match the height of the rendered content.
   */

  private static void configureSummaryWebViewHeight(
    final WebView summary_text)
  {
    final LinearLayout.LayoutParams q = new LinearLayout.LayoutParams(
      LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
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

      buffer.append(
        NullCheck.notNull(
          rr.getString(R.string.catalog_categories)));
      buffer.append(": ");

      for (int index = 0; index < cats.size(); ++index) {
        final OPDSCategory c = NullCheck.notNull(cats.get(index));
        if (CatalogBookDetailView.GENRES_URI_TEXT.equals(c.getScheme())) {
          buffer.append(c.getEffectiveLabel());
          if ((index + 1) < cats.size()) {
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
      buffer.append(
        NullCheck.notNull(
          rr.getString(R.string.catalog_publication_date)));
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

      buffer.append(
        NullCheck.notNull(
          rr.getString(R.string.catalog_publisher)));
      buffer.append(": ");
      buffer.append(some.get());
    }
  }

  /**
   * @return The scrolling view containing the book details
   */

  public ScrollView getScrollView()
  {
    return this.scroll_view;
  }

  @Override public Unit onBookStatusDownloaded(
    final BookStatusDownloaded d)
  {
    this.book_debug_status.setText("downloaded");
    this.book_download_buttons.removeAllViews();

    final Resources rr = NullCheck.notNull(this.activity.getResources());
    final String text =
      CatalogBookAvailabilityStrings.getAvailabilityString(rr, d);
    this.book_download_text.setText(text);

    if (d.isReturnable()) {
      final CatalogBookRevokeButton revoke = new CatalogBookRevokeButton(
        this.activity, d.getID(), CatalogBookRevokeType.REVOKE_LOAN);
      this.book_download_buttons.addView(revoke, 0);
    }

    this.book_download_buttons.addView(
      new CatalogBookDeleteButton(
        this.activity, d.getID()));

    this.book_download_buttons.addView(
      new CatalogBookReadButton(
        this.activity, d.getID()));

    this.book_download_buttons.setVisibility(View.VISIBLE);

    CatalogBookDetailView.configureButtonsHeight(
      this.activity.getResources(), this.book_download_buttons);

    this.book_download.setVisibility(View.VISIBLE);
    this.book_downloading.setVisibility(View.INVISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadFailed(
    final BookStatusDownloadFailed f)
  {
    this.book_debug_status.setText("download failed");

    this.book_download.setVisibility(View.INVISIBLE);
    this.book_downloading.setVisibility(View.INVISIBLE);
    this.book_downloading_failed.setVisibility(View.VISIBLE);

    final Resources rr = NullCheck.notNull(this.activity.getResources());

    final TextView failed =
      NullCheck.notNull(this.book_downloading_failed_text);
    failed.setText(CatalogBookErrorStrings.getFailureString(rr, f));

    final Button dismiss =
      NullCheck.notNull(this.book_downloading_failed_dismiss);
    final Button retry = NullCheck.notNull(this.book_downloading_failed_retry);

    dismiss.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          CatalogBookDetailView.this.books.bookDownloadAcknowledge(f.getID());
        }
      });

    /**
     * Manually construct an acquisition controller for the retry button.
     */

    final FeedEntryOPDS current_entry = this.entry.get();
    final OPDSAcquisitionFeedEntry eo = current_entry.getFeedEntry();
    final OptionType<OPDSAcquisition> a_opt =
      CatalogAcquisitionButtons.getPreferredAcquisition(
        f.getID(), eo.getAcquisitions());

    /**
     * Theoretically, if the book has ever been downloaded, then the
     * acquisition list must have contained one usable acquisition relation...
     */

    if (a_opt.isNone()) {
      throw new UnreachableCodeException();
    }

    final OPDSAcquisition a = ((Some<OPDSAcquisition>) a_opt).get();
    final CatalogAcquisitionButtonController retry_ctl =
      new CatalogAcquisitionButtonController(
        this.activity, this.books, current_entry.getBookID(), a, current_entry);

    retry.setEnabled(true);
    retry.setVisibility(View.VISIBLE);
    retry.setOnClickListener(retry_ctl);
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloading(
    final BookStatusDownloadingType o)
  {
    return o.matchBookDownloadingStatus(this);
  }

  @Override public Unit onBookStatusDownloadInProgress(
    final BookStatusDownloadInProgress d)
  {
    this.book_debug_status.setText("download in progress");

    this.book_download.setVisibility(View.INVISIBLE);
    this.book_downloading.setVisibility(View.VISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);

    this.book_downloading_label.setText(R.string.catalog_downloading);
    CatalogDownloadProgressBar.setProgressBar(
      d.getCurrentTotalBytes(),
      d.getExpectedTotalBytes(),
      NullCheck.notNull(this.book_downloading_percent_text),
      NullCheck.notNull(this.book_downloading_progress));

    final Button dc = NullCheck.notNull(this.book_downloading_cancel);
    dc.setVisibility(View.VISIBLE);
    dc.setEnabled(true);
    dc.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          CatalogBookDetailView.this.books.bookDownloadCancel(d.getID());
        }
      });

    return Unit.unit();
  }

  @Override public Unit onBookStatusHeld(
    final BookStatusHeld s)
  {
    this.book_debug_status.setText("held");

    this.book_download_buttons.removeAllViews();
    this.book_download_buttons.setVisibility(View.VISIBLE);
    this.book_download.setVisibility(View.VISIBLE);
    this.book_downloading.setVisibility(View.INVISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);

    final Resources rr = NullCheck.notNull(this.activity.getResources());

    if (s.isRevocable()) {
      final CatalogBookRevokeButton revoke = new CatalogBookRevokeButton(
        this.activity, s.getID(), CatalogBookRevokeType.REVOKE_HOLD);
      this.book_download_buttons.addView(revoke, 0);
    }

    CatalogBookDetailView.configureButtonsHeight(
      rr, this.book_download_buttons);

    final String text =
      CatalogBookAvailabilityStrings.getAvailabilityString(rr, s);
    this.book_download_text.setText(text);

    return Unit.unit();
  }

  @Override public Unit onBookStatusHeldReady(
    final BookStatusHeldReady s)
  {
    this.book_debug_status.setText("held-ready");

    this.book_download_buttons.removeAllViews();
    this.book_download_buttons.setVisibility(View.VISIBLE);
    this.book_download.setVisibility(View.VISIBLE);
    this.book_downloading.setVisibility(View.INVISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);

    final Resources rr = NullCheck.notNull(this.activity.getResources());
    final String text =
      CatalogBookAvailabilityStrings.getAvailabilityString(rr, s);
    this.book_download_text.setText(text);

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.book_download_buttons,
      NullCheck.notNull(this.books),
      NullCheck.notNull(this.entry.get()));

    if (s.isRevocable()) {
      final CatalogBookRevokeButton revoke = new CatalogBookRevokeButton(
        this.activity, s.getID(), CatalogBookRevokeType.REVOKE_HOLD);
      this.book_download_buttons.addView(revoke, 0);
    }

    CatalogBookDetailView.configureButtonsHeight(
      rr, this.book_download_buttons);
    return Unit.unit();
  }

  @Override public Unit onBookStatusHoldable(
    final BookStatusHoldable s)
  {
    this.book_debug_status.setText("holdable");

    this.book_download_buttons.removeAllViews();
    this.book_download_buttons.setVisibility(View.VISIBLE);
    this.book_download.setVisibility(View.VISIBLE);
    this.book_downloading.setVisibility(View.INVISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);

    final Resources rr = NullCheck.notNull(this.activity.getResources());
    final String text =
      CatalogBookAvailabilityStrings.getAvailabilityString(rr, s);
    this.book_download_text.setText(text);

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.book_download_buttons,
      NullCheck.notNull(this.books),
      NullCheck.notNull(this.entry.get()));

    CatalogBookDetailView.configureButtonsHeight(
      rr, this.book_download_buttons);
    return Unit.unit();
  }

  @Override public Unit onBookStatusLoanable(
    final BookStatusLoanable s)
  {
    this.onBookStatusNone(this.entry.get());
    this.book_debug_status.setText("loanable");
    return Unit.unit();
  }

  @Override public Unit onBookStatusRevokeFailed(final BookStatusRevokeFailed s)
  {
    this.book_debug_status.setText("revoke failed");

    this.book_download.setVisibility(View.INVISIBLE);
    this.book_downloading.setVisibility(View.INVISIBLE);
    this.book_downloading_failed.setVisibility(View.VISIBLE);

    final TextView failed =
      NullCheck.notNull(this.book_downloading_failed_text);
    failed.setText(R.string.catalog_revoke_failed);

    final Button dismiss =
      NullCheck.notNull(this.book_downloading_failed_dismiss);
    final Button retry = NullCheck.notNull(this.book_downloading_failed_retry);

    dismiss.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          CatalogBookDetailView.this.books.bookGetLatestStatusFromDisk(s.getID());
        }
      });

    retry.setEnabled(false);
    retry.setVisibility(View.GONE);
    return Unit.unit();
  }

  @Override public Unit onBookStatusLoaned(
    final BookStatusLoaned o)
  {
    this.book_debug_status.setText("loaned");

    this.book_download_buttons.removeAllViews();
    this.book_download_buttons.setVisibility(View.VISIBLE);
    this.book_download.setVisibility(View.VISIBLE);
    this.book_downloading.setVisibility(View.INVISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);

    final Resources rr = NullCheck.notNull(this.activity.getResources());
    final String text =
      CatalogBookAvailabilityStrings.getAvailabilityString(rr, o);
    this.book_download_text.setText(text);

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.book_download_buttons,
      NullCheck.notNull(this.books),
      NullCheck.notNull(this.entry.get()));

    if (o.isReturnable()) {
      final CatalogBookRevokeButton revoke = new CatalogBookRevokeButton(
        this.activity, o.getID(), CatalogBookRevokeType.REVOKE_LOAN);
      this.book_download_buttons.addView(revoke, 0);
    }

    CatalogBookDetailView.configureButtonsHeight(
      rr, this.book_download_buttons);
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
    this.book_debug_status.setText("none");

    this.book_download_buttons.removeAllViews();
    this.book_download_buttons.setVisibility(View.VISIBLE);
    this.book_download.setVisibility(View.VISIBLE);
    this.book_downloading.setVisibility(View.INVISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);

    final Resources rr = NullCheck.notNull(this.activity.getResources());
    final OPDSAcquisitionFeedEntry eo = e.getFeedEntry();
    final OPDSAvailabilityType avail = eo.getAvailability();
    final String text =
      CatalogBookAvailabilityStrings.getOPDSAvailabilityString(rr, avail);
    this.book_download_text.setText(text);

    CatalogAcquisitionButtons.addButtons(
      this.activity, this.book_download_buttons, this.books, e);

    CatalogBookDetailView.configureButtonsHeight(
      rr, this.book_download_buttons);
  }

  @Override public Unit onBookStatusRequestingDownload(
    final BookStatusRequestingDownload d)
  {
    this.book_debug_status.setText("requesting download");

    this.book_download.setVisibility(View.INVISIBLE);
    this.book_downloading.setVisibility(View.VISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);

    this.book_downloading_label.setText(R.string.catalog_downloading);

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      NullCheck.notNull(this.book_downloading_percent_text),
      NullCheck.notNull(this.book_downloading_progress));

    final Button dc = NullCheck.notNull(this.book_downloading_cancel);
    dc.setEnabled(false);
    dc.setVisibility(View.INVISIBLE);
    dc.setOnClickListener(null);
    return Unit.unit();
  }

  @Override public Unit onBookStatusRequestingLoan(
    final BookStatusRequestingLoan s)
  {
    this.book_debug_status.setText("requesting loan");

    this.book_download.setVisibility(View.INVISIBLE);
    this.book_downloading.setVisibility(View.VISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);

    this.book_downloading_label.setText(R.string.catalog_requesting_loan);

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      NullCheck.notNull(this.book_downloading_percent_text),
      NullCheck.notNull(this.book_downloading_progress));

    final Button dc = NullCheck.notNull(this.book_downloading_cancel);
    dc.setEnabled(false);
    dc.setVisibility(View.INVISIBLE);
    dc.setOnClickListener(null);
    return Unit.unit();
  }

  @Override public Unit onBookStatusRequestingRevoke(
    final BookStatusRequestingRevoke s)
  {
    this.book_debug_status.setText("requesting revoke");

    this.book_download.setVisibility(View.INVISIBLE);
    this.book_downloading.setVisibility(View.VISIBLE);
    this.book_downloading_failed.setVisibility(View.INVISIBLE);

    this.book_downloading_label.setText(R.string.catalog_requesting_loan);

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      NullCheck.notNull(this.book_downloading_percent_text),
      NullCheck.notNull(this.book_downloading_progress));

    final Button dc = NullCheck.notNull(this.book_downloading_cancel);
    dc.setEnabled(false);
    dc.setVisibility(View.INVISIBLE);
    dc.setOnClickListener(null);
    return Unit.unit();
  }

  private void onStatus(
    final FeedEntryOPDS e,
    final OptionType<BookStatusType> status_opt)
  {
    if (status_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) status_opt;
      UIThread.runOnUIThread(
        new Runnable()
        {
          @Override public void run()
          {
            some.get().matchBookStatus(CatalogBookDetailView.this);
          }
        });
    } else {
      UIThread.runOnUIThread(
        new Runnable()
        {
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
    final FeedEntryOPDS current_entry = this.entry.get();
    final BookID current_id = current_entry.getBookID();

    /**
     * If the book for this view has been updated in some way, first check
     * the book database and replace the current feed entry with any snapshot
     * that exists. Then, check to see if a revocation feed entry has been
     * posted and use that.
     */

    if (current_id.equals(update_id)) {
      final BooksStatusCacheType status_cache = this.books.bookGetStatusCache();
      final OptionType<BookStatusType> status_opt =
        status_cache.booksStatusGet(current_id);

      CatalogBookDetailView.LOG.debug("received status update {}", status_opt);

      this.updateFetchDatabaseSnapshot(
        current_id, this.books.bookGetDatabase());
      this.updateCheckForRevocationEntry(current_id, status_cache);
      this.onStatus(this.entry.get(), status_opt);
    }
  }

  private void updateFetchDatabaseSnapshot(
    final BookID current_id,
    final BookDatabaseReadableType db)
  {
    final OptionType<BookDatabaseEntrySnapshot> snap_opt =
      db.databaseGetEntrySnapshot(current_id);

    CatalogBookDetailView.LOG.debug(
      "database snapshot is {}", snap_opt);

    if (snap_opt.isSome()) {
      final Some<BookDatabaseEntrySnapshot> some =
        (Some<BookDatabaseEntrySnapshot>) snap_opt;
      final BookDatabaseEntrySnapshot snap = some.get();
      this.entry.set(
        (FeedEntryOPDS) FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(
          snap.getEntry()));
    }
  }

  private void updateCheckForRevocationEntry(
    final BookID current_id,
    final BooksStatusCacheType status_cache)
  {
    final OptionType<FeedEntryType> revoke_opt =
      status_cache.booksRevocationFeedEntryGet(current_id);

    CatalogBookDetailView.LOG.debug(
      "revocation entry update is {}", revoke_opt);

    if (revoke_opt.isSome()) {
      final Some<FeedEntryType> some = (Some<FeedEntryType>) revoke_opt;
      final FeedEntryType update = some.get();
      update.matchFeedEntry(
        new FeedEntryMatcherType<Unit, UnreachableCodeException>()
        {
          @Override public Unit onFeedEntryOPDS(final FeedEntryOPDS e)
          {
            CatalogBookDetailView.this.entry.set(e);
            return Unit.unit();
          }

          @Override public Unit onFeedEntryCorrupt(final FeedEntryCorrupt e)
          {
            CatalogBookDetailView.LOG.debug(
              "cannot render an entry of type {}", e);
            return Unit.unit();
          }
        });
    }
  }
}
