package org.nypl.simplified.app.catalog;

import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.io7m.jfunctional.None;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.app.ApplicationColorScheme;
import org.nypl.simplified.app.NetworkConnectivityType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryReadableType;
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed;
import org.nypl.simplified.books.book_registry.BookStatusDownloadInProgress;
import org.nypl.simplified.books.book_registry.BookStatusDownloaded;
import org.nypl.simplified.books.book_registry.BookStatusDownloadingMatcherType;
import org.nypl.simplified.books.book_registry.BookStatusDownloadingType;
import org.nypl.simplified.books.book_registry.BookStatusHeld;
import org.nypl.simplified.books.book_registry.BookStatusHeldReady;
import org.nypl.simplified.books.book_registry.BookStatusHoldable;
import org.nypl.simplified.books.book_registry.BookStatusLoanable;
import org.nypl.simplified.books.book_registry.BookStatusLoaned;
import org.nypl.simplified.books.book_registry.BookStatusLoanedMatcherType;
import org.nypl.simplified.books.book_registry.BookStatusLoanedType;
import org.nypl.simplified.books.book_registry.BookStatusMatcherType;
import org.nypl.simplified.books.book_registry.BookStatusRequestingDownload;
import org.nypl.simplified.books.book_registry.BookStatusRequestingLoan;
import org.nypl.simplified.books.book_registry.BookStatusRequestingRevoke;
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed;
import org.nypl.simplified.books.book_registry.BookStatusRevoked;
import org.nypl.simplified.books.book_registry.BookStatusType;
import org.nypl.simplified.books.controller.BooksControllerType;
import org.nypl.simplified.books.controller.ProfilesControllerType;
import org.nypl.simplified.books.core.BookAcquisitionSelection;
import org.nypl.simplified.books.covers.BookCoverProviderType;
import org.nypl.simplified.books.document_store.DocumentStoreType;
import org.nypl.simplified.books.feeds.FeedEntry;
import org.nypl.simplified.books.feeds.FeedEntryOPDS;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

/**
 * A single cell in feed (list or grid).
 */

public final class CatalogFeedBookCellView extends FrameLayout implements
  FeedEntryMatcherType<Unit, UnreachableCodeException>,
  BookStatusMatcherType<Unit, UnreachableCodeException>,
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>,
  BookStatusDownloadingMatcherType<Unit, UnreachableCodeException> {

  private static final Logger LOG = LoggerFactory.getLogger(CatalogFeedBookCellView.class);

  private final AppCompatActivity activity;
  private final TextView cell_authors;
  private final ViewGroup cell_book;
  private final ViewGroup cell_buttons;
  private final ViewGroup cell_corrupt;
  private final TextView cell_corrupt_text;
  private final ImageView cell_cover_image;
  private final ViewGroup cell_cover_layout;
  private final ProgressBar cell_cover_progress;
  private final TextView cell_debug;
  private final ViewGroup cell_downloading;
  private final TextView cell_downloading_authors;
  private final Button cell_downloading_cancel;
  private final ViewGroup cell_downloading_failed;
  private final Button cell_downloading_failed_dismiss;
  private final Button cell_downloading_failed_retry;
  private final TextView cell_downloading_failed_title;
  private final TextView cell_downloading_percent_text;
  private final ProgressBar cell_downloading_progress;
  private final TextView cell_downloading_title;
  private final ViewGroup cell_text_layout;
  private final TextView cell_title;
  private final BookCoverProviderType cover_provider;
  private final boolean debug_cell_state;
  private final AtomicReference<FeedEntryOPDS> entry;
  private final TextView cell_downloading_label;
  private final TextView cell_downloading_failed_label;
  private final BookRegistryReadableType books_registry;
  private final BooksControllerType books_controller;
  private final ProfilesControllerType profiles_controller;
  private final NetworkConnectivityType network_connectivity;
  private final ListeningExecutorService executor;
  private final DocumentStoreType documents;
  private final ApplicationColorScheme color_scheme;
  private CatalogBookSelectionListenerType book_selection_listener;

  /**
   * Construct a cell view.
   */

  public CatalogFeedBookCellView(
    final AppCompatActivity in_activity,
    final BookCoverProviderType in_cover_provider,
    final BooksControllerType in_books_controller,
    final DocumentStoreType in_documents,
    final ProfilesControllerType in_profiles_controller,
    final BookRegistryReadableType in_books_registry,
    final NetworkConnectivityType in_network_connectivity,
    final ListeningExecutorService in_executor,
    final ApplicationColorScheme in_color_scheme) {

    super(in_activity);

    this.activity =
      NullCheck.notNull(in_activity, "Activity");
    this.cover_provider =
      NullCheck.notNull(in_cover_provider, "Cover provider");
    this.books_registry =
      NullCheck.notNull(in_books_registry, "Book registry");
    this.books_controller =
      NullCheck.notNull(in_books_controller, "Books controller");
    this.profiles_controller =
      NullCheck.notNull(in_profiles_controller, "Profiles controller");
    this.network_connectivity =
      NullCheck.notNull(in_network_connectivity, "Network connectivity");
    this.executor =
      NullCheck.notNull(in_executor, "Executor");
    this.documents =
      NullCheck.notNull(in_documents, "Documents");
    this.color_scheme =
      NullCheck.notNull(in_color_scheme, "Color scheme");

    this.book_selection_listener = (v, e) -> LOG.debug("doing nothing for {}", e);

    final Resources resources =
      NullCheck.notNull(in_activity.getResources());

    final LayoutInflater inflater = in_activity.getLayoutInflater();
    inflater.inflate(R.layout.catalog_book_cell, this, true);

    /*
     * Receive book status updates.
     */

    this.cell_downloading =
      NullCheck.notNull(this.findViewById(R.id.cell_downloading));

    this.cell_downloading_progress =
      NullCheck.notNull(this.cell_downloading.findViewById(R.id.cell_downloading_progress));
    this.cell_downloading_percent_text =
      NullCheck.notNull(this.cell_downloading.findViewById(R.id.cell_downloading_percent_text));
    this.cell_downloading_label =
      NullCheck.notNull(this.cell_downloading.findViewById(R.id.cell_downloading_label));
    this.cell_downloading_title =
      NullCheck.notNull(this.cell_downloading.findViewById(R.id.cell_downloading_title));
    this.cell_downloading_authors =
      NullCheck.notNull(this.cell_downloading.findViewById(R.id.cell_downloading_authors));
    this.cell_downloading_cancel =
      NullCheck.notNull(this.cell_downloading.findViewById(R.id.cell_downloading_cancel));

    this.cell_downloading_failed =
      NullCheck.notNull(this.findViewById(R.id.cell_downloading_failed));
    this.cell_downloading_failed_title =
      NullCheck.notNull(this.cell_downloading_failed.findViewById(R.id.cell_downloading_failed_title));
    this.cell_downloading_failed_label =
      NullCheck.notNull(this.cell_downloading_failed.findViewById(R.id.cell_downloading_failed_static_text));
    this.cell_downloading_failed_dismiss =
      NullCheck.notNull(this.cell_downloading_failed.findViewById(R.id.cell_downloading_failed_dismiss));
    this.cell_downloading_failed_retry =
      NullCheck.notNull(this.cell_downloading_failed.findViewById(R.id.cell_downloading_failed_retry));

    this.cell_corrupt =
      NullCheck.notNull(this.findViewById(R.id.cell_corrupt));
    this.cell_corrupt_text =
      NullCheck.notNull(this.cell_corrupt.findViewById(R.id.cell_corrupt_text));

    this.cell_book =
      NullCheck.notNull(this.findViewById(R.id.cell_book));

    this.cell_debug =
      NullCheck.notNull(this.findViewById(R.id.cell_debug));
    this.debug_cell_state =
      resources.getBoolean(R.bool.debug_catalog_cell_view_states);
    if (this.debug_cell_state == false) {
      this.cell_debug.setVisibility(View.GONE);
    }

    this.cell_text_layout = NullCheck.notNull(
      this.cell_book.findViewById(R.id.cell_text_layout));
    this.cell_title = NullCheck.notNull(
      this.cell_text_layout.findViewById(R.id.cell_title));
    this.cell_authors = NullCheck.notNull(
      this.cell_text_layout.findViewById(R.id.cell_authors));
    this.cell_buttons = NullCheck.notNull(
      this.cell_text_layout.findViewById(R.id.cell_buttons));

    this.cell_cover_layout =
      NullCheck.notNull(this.findViewById(R.id.cell_cover_layout));
    this.cell_cover_image =
      NullCheck.notNull(this.cell_cover_layout.findViewById(R.id.cell_cover_image));
    this.cell_cover_progress =
      NullCheck.notNull(this.cell_cover_layout.findViewById(R.id.cell_cover_loading));

    /*
     * The height of the row is known, so assume a roughly 4:3 aspect ratio
     * for cover images and calculate the width of the cover layout in pixels.
     */

    final int cover_height = this.cell_cover_layout.getLayoutParams().height;
    final int cover_width = (int) (((double) cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams ccl_p =
      new LinearLayout.LayoutParams(cover_width, cover_height);
    this.cell_cover_layout.setLayoutParams(ccl_p);

    this.entry = new AtomicReference<>();

    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
  }

  private static String makeAuthorText(final OPDSAcquisitionFeedEntry in_e) {
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
    return NullCheck.notNull(sb.toString());
  }

  private void loadImageAndSetVisibility(final FeedEntryOPDS in_e) {
    final int in_image_height = this.cell_cover_layout.getLayoutParams().height;

    final ImageView coverImage = this.cell_cover_image;
    final ProgressBar coverProgress = this.cell_cover_progress;

    coverImage.setVisibility(View.INVISIBLE);
    coverProgress.setVisibility(View.VISIBLE);

    final FutureCallback<kotlin.Unit> callback = new FutureCallback<kotlin.Unit>() {
      @Override
      public void onSuccess(kotlin.Unit result) {
        UIThread.runOnUIThread(() -> {
          coverImage.setVisibility(View.VISIBLE);
          coverProgress.setVisibility(View.INVISIBLE);
        });
      }

      @Override
      public void onFailure(Throwable t) {
        UIThread.runOnUIThread(() -> {
          LOG.error("unable to load image");
          coverImage.setVisibility(View.INVISIBLE);
          coverProgress.setVisibility(View.INVISIBLE);
        });
      }
    };

    FluentFuture<kotlin.Unit> future =
      this.cover_provider.loadThumbnailInto(
        in_e,
        this.cell_cover_image,
        (int) ((double) in_image_height * 0.75),
        in_image_height);

    Futures.addCallback(future, callback, directExecutor());
  }

  @Override
  public Unit onBookStatusDownloaded(final BookStatusDownloaded d) {

    final BookID book_id = d.getID();
    LOG.debug("{}: downloaded", book_id);

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("downloaded");

    final FeedEntryOPDS feed_entry =
      NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(feed_entry);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();
    this.cell_buttons.addView(
      new CatalogBookReadButton(
        this.activity,
        this.account(feed_entry.getBookID()),
        book_id,
        feed_entry,
        this.color_scheme),
      0);

    return Unit.unit();
  }

  private AccountType account(final BookID book_id) {
    try {
      return this.profiles_controller.profileAccountForBook(book_id);
    } catch (final ProfileNoneCurrentException | AccountsDatabaseNonexistentException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Unit onBookStatusDownloadFailed(final BookStatusDownloadFailed f) {
    LOG.debug("{}: download failed", f.getID());

    /*
     * If the download failed because there is no wifi, then mark the book as being
     * loaned. It can be downloaded later.
     */

    if (!this.network_connectivity.isWifiAvailable()) {
      this.onBookStatusLoaned(new BookStatusLoaned(f.getID(), None.none(), false));
      return Unit.unit();
    }

    /*
     * Unset the content description so that the screen reader reads the error message.
     */

    this.setContentDescription(null);

    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.VISIBLE);
    this.setDebugCellText("download-failed");

    final FeedEntryOPDS feed_entry = NullCheck.notNull(this.entry.get());
    final OPDSAcquisitionFeedEntry oe = feed_entry.getFeedEntry();

    final Resources rr = NullCheck.notNull(this.activity.getResources());
    this.cell_downloading_failed_label.setText(
      CatalogBookErrorStrings.getFailureString(rr, f));

    this.cell_downloading_failed_title.setText(oe.getTitle());
    this.cell_downloading_failed_dismiss.setOnClickListener(
      view -> this.books_controller.bookBorrowFailedDismiss(
        this.account(feed_entry.getBookID()), f.getID()));

    /*
     * Manually construct an acquisition controller for the retry button.
     */

    final OptionType<OPDSAcquisition> a_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(oe.getAcquisitions());

    /*
     * Theoretically, if the book has ever been downloaded, then the
     * acquisition list must have contained one usable acquisition relation...
     */

    if (a_opt.isNone()) {
      throw new UnreachableCodeException();
    }

    final OPDSAcquisition acquisition = ((Some<OPDSAcquisition>) a_opt).get();
    final CatalogAcquisitionButtonController retry_ctl =
      new CatalogAcquisitionButtonController(
        this.activity,
        this.profiles_controller,
        this.executor,
        this.documents,
        this.books_controller,
        this.books_registry,
        this.network_connectivity,
        feed_entry.getBookID(),
        acquisition,
        feed_entry);

    this.cell_downloading_failed_retry.setVisibility(View.VISIBLE);
    this.cell_downloading_failed_retry.setEnabled(true);
    this.cell_downloading_failed_retry.setOnClickListener(retry_ctl);

    return Unit.unit();
  }

  @Override
  public Unit onBookStatusDownloading(final BookStatusDownloadingType o) {
    return o.matchBookDownloadingStatus(this);
  }

  @Override
  public Unit onBookStatusDownloadInProgress(final BookStatusDownloadInProgress d) {
    LOG.debug("{}: downloading", d.getID());

    LOG.debug("{}: downloading", d.getID());
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("download-in-progress");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    final BookID book_id = d.getID();
    final OPDSAcquisitionFeedEntry oe = fe.getFeedEntry();
    this.cell_downloading_label.setText(R.string.catalog_downloading);
    this.cell_downloading_title.setText(oe.getTitle());
    this.cell_downloading_authors.setText(
      CatalogFeedBookCellView.makeAuthorText(oe));

    CatalogDownloadProgressBar.setProgressBar(
      d.getCurrentTotalBytes(),
      d.getExpectedTotalBytes(),
      this.cell_downloading_percent_text,
      this.cell_downloading_progress);

    this.cell_downloading_cancel.setVisibility(View.VISIBLE);
    this.cell_downloading_cancel.setEnabled(true);
    this.cell_downloading_cancel.setOnClickListener(
      view -> this.books_controller.bookDownloadCancel(this.account(fe.getBookID()), d.getID()));

    return Unit.unit();
  }

  @Override
  public Unit onBookStatusHeld(final BookStatusHeld s) {
    LOG.debug("{}: held", s.getID());

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("held");

    final FeedEntryOPDS feed_entry = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(feed_entry);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

    if (s.isRevocable()) {
      final CatalogBookRevokeButton revoke =
        new CatalogBookRevokeButton(
          this.activity,
          this.books_controller,
          this.account(feed_entry.getBookID()),
          s.getID(),
          CatalogBookRevokeType.REVOKE_HOLD);
      this.cell_buttons.addView(revoke, 0);
    }

    return Unit.unit();
  }

  @Override
  public Unit onBookStatusHeldReady(final BookStatusHeldReady s) {

    LOG.debug("{}: reserved", s.getID());
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("reserved");

    final FeedEntryOPDS feed_entry = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(feed_entry);

    CatalogAcquisitionButtons.Companion.addButtons(
      this.activity,
      this.account(feed_entry.getBookID()),
      this.cell_buttons,
      this.books_controller,
      this.profiles_controller,
      this.books_registry,
      feed_entry);

    if (s.isRevocable()) {
      final CatalogBookRevokeButton revoke =
        new CatalogBookRevokeButton(
          this.activity,
          this.books_controller,
          this.account(feed_entry.getBookID()),
          s.getID(),
          CatalogBookRevokeType.REVOKE_HOLD);
      this.cell_buttons.addView(revoke, 0);
    }

    return Unit.unit();
  }

  @Override
  public Unit onBookStatusHoldable(final BookStatusHoldable s) {
    LOG.debug("{}: holdable", s.getID());

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("holdable");

    final FeedEntryOPDS feed_entry = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(feed_entry);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

    CatalogAcquisitionButtons.Companion.addButtons(
      this.activity,
      this.account(feed_entry.getBookID()),
      this.cell_buttons,
      this.books_controller,
      this.profiles_controller,
      this.books_registry,
      feed_entry);

    return Unit.unit();
  }

  @Override
  public Unit onBookStatusLoanable(final BookStatusLoanable s) {
    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.onBookStatusNone(fe, s.getID());
    return Unit.unit();
  }

  @Override
  public Unit onBookStatusRevokeFailed(final BookStatusRevokeFailed s) {
    LOG.debug("{}: revoke failed", s.getID());

    /*
     * Unset the content description so that the screen reader reads the error message.
     */

    this.setContentDescription(null);

    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.VISIBLE);
    this.setDebugCellText("revoke-failed");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    final OPDSAcquisitionFeedEntry oe = fe.getFeedEntry();

    this.cell_downloading_failed_label.setText(R.string.catalog_revoke_failed);
    this.cell_downloading_failed_title.setText(oe.getTitle());
    this.cell_downloading_failed_dismiss.setOnClickListener(
      view -> books_controller.bookRevokeFailedDismiss(account(s.getID()), s.getID()));

    this.cell_downloading_failed_retry.setVisibility(View.GONE);
    this.cell_downloading_failed_retry.setEnabled(false);
    return Unit.unit();
  }

  @Override
  public Unit onBookStatusRevoked(final BookStatusRevoked o) {
    LOG.debug("{}: revoked", o.getID());
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("revoked");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(fe);
    return Unit.unit();
  }

  @Override
  public Unit onBookStatusLoaned(final BookStatusLoaned o) {
    LOG.debug("{}: loaned", o.getID());
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("loaned");

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

    final FeedEntryOPDS feed_entry = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(feed_entry);

    CatalogAcquisitionButtons.Companion.addButtons(
      this.activity,
      this.account(feed_entry.getBookID()),
      this.cell_buttons,
      this.books_controller,
      this.profiles_controller,
      this.books_registry,
      feed_entry);

    return Unit.unit();
  }

  @Override
  public Unit onBookStatusLoanedType(final BookStatusLoanedType o) {
    return o.matchBookLoanedStatus(this);
  }

  private void onBookStatusNone(
    final FeedEntryOPDS in_entry,
    final BookID id) {
    LOG.debug("{}: none", id);
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("none");

    this.loadImageAndSetVisibility(in_entry);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

    CatalogAcquisitionButtons.Companion.addButtons(
      this.activity,
      this.account(id),
      this.cell_buttons,
      this.books_controller,
      this.profiles_controller,
      this.books_registry,
      in_entry);
  }

  @Override
  public Unit onBookStatusRequestingDownload(final BookStatusRequestingDownload d) {
    LOG.debug("{}: requesting download", d.getID());
    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("requesting-download");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(fe);

    this.cell_downloading_label.setText(R.string.catalog_downloading);
    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();
    return Unit.unit();
  }

  @Override
  public Unit onBookStatusRequestingLoan(final BookStatusRequestingLoan s) {
    LOG.debug("{}: requesting loan", s.getID());
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("requesting-loan");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    final OPDSAcquisitionFeedEntry oe = fe.getFeedEntry();

    this.cell_downloading_label.setText(R.string.catalog_requesting_loan);
    this.cell_downloading_title.setText(oe.getTitle());
    this.cell_downloading_authors.setText(
      CatalogFeedBookCellView.makeAuthorText(oe));

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      this.cell_downloading_percent_text,
      this.cell_downloading_progress);

    this.cell_downloading_cancel.setVisibility(View.INVISIBLE);
    this.cell_downloading_cancel.setEnabled(false);
    this.cell_downloading_cancel.setOnClickListener(null);
    return Unit.unit();
  }

  @Override
  public Unit onBookStatusRequestingRevoke(final BookStatusRequestingRevoke s) {
    LOG.debug("{}: requesting revoke", s.getID());
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("requesting-revoke");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    final OPDSAcquisitionFeedEntry oe = fe.getFeedEntry();

    this.cell_downloading_label.setText(R.string.catalog_requesting_revoke);
    this.cell_downloading_title.setText(oe.getTitle());
    this.cell_downloading_authors.setText(
      CatalogFeedBookCellView.makeAuthorText(oe));

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      this.cell_downloading_percent_text,
      this.cell_downloading_progress);

    this.cell_downloading_cancel.setVisibility(View.INVISIBLE);
    this.cell_downloading_cancel.setEnabled(false);
    this.cell_downloading_cancel.setOnClickListener(null);
    return Unit.unit();
  }

  @Override
  public Unit onFeedEntryCorrupt(final FeedEntryCorrupt e) {
    LOG.debug("{}: feed entry corrupt: ", e.getBookID(), e.getError());

    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.VISIBLE);
    this.setDebugCellText("entry-corrupt");

    final Resources rr = NullCheck.notNull(this.getResources());
    final String text = String.format(
      "%s (%s)", rr.getString(R.string.catalog_meta_corrupt), e.getBookID());
    this.cell_corrupt_text.setText(text);
    return Unit.unit();
  }

  @Override
  public Unit onFeedEntryOPDS(final FeedEntryOPDS feed_e) {
    final OPDSAcquisitionFeedEntry oe = feed_e.getFeedEntry();
    this.cell_title.setText(oe.getTitle());
    this.cell_authors.setText(CatalogFeedBookCellView.makeAuthorText(oe));

    this.setContentDescription(
      CatalogBookFormats.contentDescriptionOfEntry(this.getResources(), feed_e));

    final CatalogBookSelectionListenerType book_listener =
      this.book_selection_listener;
    this.setOnClickListener(v -> book_listener.onSelectBook(this, feed_e));

    this.entry.set(feed_e);

    final BookID book_id = feed_e.getBookID();
    this.onStatus(feed_e, book_id, books_registry.bookStatus(book_id));
    return Unit.unit();
  }

  private void onStatus(
    final FeedEntryOPDS in_entry,
    final BookID id,
    final OptionType<BookStatusType> status_opt) {
    if (status_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) status_opt;
      UIThread.runOnUIThread(() -> some.get().matchBookStatus(CatalogFeedBookCellView.this));
    } else {
      UIThread.runOnUIThread(() -> CatalogFeedBookCellView.this.onBookStatusNone(in_entry, id));
    }
  }

  private void setDebugCellText(
    final String text) {
    if (this.debug_cell_state) {
      this.cell_debug.setText(text);
    }
  }

  /**
   * Configure the overall status of the cell. The cell displays a number of
   * different layouts depending on whether the current book is loaned, fully
   * downloaded, currently downloading, not loaned, etc.
   *
   * @param in_e        The new feed entry
   * @param in_listener A selection listener
   */

  public void viewConfigure(
    final FeedEntry in_e,
    final CatalogBookSelectionListenerType in_listener) {
    NullCheck.notNull(in_e);
    NullCheck.notNull(in_listener);

    UIThread.checkIsUIThread();

    this.book_selection_listener = NullCheck.notNull(in_listener);
    in_e.matchFeedEntry(this);
  }
}
