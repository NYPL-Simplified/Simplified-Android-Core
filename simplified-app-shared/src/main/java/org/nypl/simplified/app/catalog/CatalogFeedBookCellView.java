package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
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
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import com.squareup.picasso.Callback;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.ThemeMatcher;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.BookAcquisitionSelection;
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
import org.nypl.simplified.books.core.BookStatusRevoked;
import org.nypl.simplified.books.core.BookStatusType;
import org.nypl.simplified.books.core.BooksStatusCacheType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryCorrupt;
import org.nypl.simplified.books.core.FeedEntryMatcherType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.FeedEntryType;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.books.covers.BookCoverProviderType;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionPath;
import org.slf4j.Logger;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

/**
 * A single cell in feed (list or grid).
 */

public final class CatalogFeedBookCellView extends FrameLayout implements
  Observer,
  FeedEntryMatcherType<Unit, UnreachableCodeException>,
  BookStatusMatcherType<Unit, UnreachableCodeException>,
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>,
  BookStatusDownloadingMatcherType<Unit, UnreachableCodeException>
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogFeedBookCellView.class);
  }

  private final Activity                         activity;
  private final BooksType                        books;
  private final TextView                         cell_authors;
  private final ViewGroup                        cell_book;
  private final ViewGroup                        cell_buttons;
  private final ViewGroup                        cell_corrupt;
  private final TextView                         cell_corrupt_text;
  private final ImageView                        cell_cover_image;
  private final ViewGroup                        cell_cover_layout;
  private final ProgressBar                      cell_cover_progress;
  private final TextView                         cell_debug;
  private final ViewGroup                        cell_downloading;
  private final TextView                         cell_downloading_authors;
  private final Button                           cell_downloading_cancel;
  private final ViewGroup                        cell_downloading_failed;
  private final Button
                                                 cell_downloading_failed_dismiss;
  private final Button                           cell_downloading_failed_retry;
  private final TextView                         cell_downloading_failed_title;
  private final TextView                         cell_downloading_percent_text;
  private final ProgressBar                      cell_downloading_progress;
  private final TextView                         cell_downloading_title;
  private final ViewGroup                        cell_text_layout;
  private final TextView                         cell_title;
  private final BookCoverProviderType            cover_provider;
  private final boolean                          debug_cell_state;
  private final AtomicReference<FeedEntryOPDS>   entry;
  private final TextView                         cell_downloading_label;
  private final TextView                         cell_downloading_failed_label;
  private       CatalogBookSelectionListenerType book_selection_listener;

  /**
   * Construct a cell view.
   *
   * @param in_activity       The host activity
   * @param in_cover_provider A cover provider
   * @param in_books          The books database
   */

  public CatalogFeedBookCellView(
    final Activity in_activity,
    final Account in_account,
    final BookCoverProviderType in_cover_provider,
    final BooksType in_books)
  {
    super(in_activity.getApplicationContext(), null);

    this.activity = NullCheck.notNull(in_activity);
    this.cover_provider = NullCheck.notNull(in_cover_provider);
    this.books = NullCheck.notNull(in_books);

    this.book_selection_listener = new CatalogBookSelectionListenerType()
    {
      @Override public void onSelectBook(
        final CatalogFeedBookCellView v,
        final FeedEntryOPDS e)
      {
        CatalogFeedBookCellView.LOG.debug("doing nothing for {}", e);
      }
    };

    final Context context =
      NullCheck.notNull(in_activity.getApplicationContext());
    final Resources rr = NullCheck.notNull(context.getResources());

    final LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    inflater.inflate(R.layout.catalog_book_cell, this, true);

    /**
     * Receive book status updates.
     */

    final BooksStatusCacheType status_cache = this.books.bookGetStatusCache();
    status_cache.booksObservableAddObserver(this);

    this.cell_downloading = NullCheck.notNull(this.findViewById(R.id.cell_downloading));
    final int resID = ThemeMatcher.Companion.color(in_account.getMainColor());
    final int mainColor = ContextCompat.getColor(this.getContext(), resID);
    this.cell_downloading.setBackgroundColor(mainColor);

    this.cell_downloading_progress = NullCheck.notNull(
      (ProgressBar) this.cell_downloading.findViewById(
        R.id.cell_downloading_progress));
    this.cell_downloading_percent_text = NullCheck.notNull(
      (TextView) this.cell_downloading.findViewById(
        R.id.cell_downloading_percent_text));
    this.cell_downloading_label = NullCheck.notNull(
      (TextView) this.cell_downloading.findViewById(
        R.id.cell_downloading_label));
    this.cell_downloading_title = NullCheck.notNull(
      (TextView) this.cell_downloading.findViewById(
        R.id.cell_downloading_title));
    this.cell_downloading_authors = NullCheck.notNull(
      (TextView) this.cell_downloading.findViewById(
        R.id.cell_downloading_authors));
    this.cell_downloading_cancel = NullCheck.notNull(
      (Button) this.cell_downloading.findViewById(
        R.id.cell_downloading_cancel));

    this.cell_downloading_failed = NullCheck.notNull(
      (ViewGroup) this.findViewById(R.id.cell_downloading_failed));
    this.cell_downloading_failed_title = NullCheck.notNull(
      (TextView) this.cell_downloading_failed.findViewById(
        R.id.cell_downloading_failed_title));
    this.cell_downloading_failed_label = NullCheck.notNull(
      (TextView) this.cell_downloading_failed.findViewById(
        R.id.cell_downloading_failed_static_text));
    this.cell_downloading_failed_dismiss = NullCheck.notNull(
      (Button) this.cell_downloading_failed.findViewById(
        R.id.cell_downloading_failed_dismiss));
    this.cell_downloading_failed_retry = NullCheck.notNull(
      (Button) this.cell_downloading_failed.findViewById(
        R.id.cell_downloading_failed_retry));

    this.cell_downloading_cancel.setBackgroundResource(R.drawable.simplified_button);
    this.cell_downloading_cancel.setTextColor(mainColor);

    this.cell_downloading_failed_dismiss.setBackgroundResource(R.drawable.simplified_button);
    this.cell_downloading_failed_dismiss.setTextColor(mainColor);

    this.cell_downloading_failed_retry.setBackgroundResource(R.drawable.simplified_button);
    this.cell_downloading_failed_retry.setTextColor(mainColor);

    this.cell_corrupt =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.cell_corrupt));
    this.cell_corrupt_text = NullCheck.notNull(
      (TextView) this.cell_corrupt.findViewById(R.id.cell_corrupt_text));

    this.cell_book =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.cell_book));

    this.cell_debug =
      NullCheck.notNull((TextView) this.findViewById(R.id.cell_debug));
    this.debug_cell_state =
      rr.getBoolean(R.bool.debug_catalog_cell_view_states);
    if (this.debug_cell_state == false) {
      this.cell_debug.setVisibility(View.GONE);
    }

    this.cell_text_layout = NullCheck.notNull(
      (ViewGroup) this.cell_book.findViewById(R.id.cell_text_layout));
    this.cell_title = NullCheck.notNull(
      (TextView) this.cell_text_layout.findViewById(R.id.cell_title));
    this.cell_authors = NullCheck.notNull(
      (TextView) this.cell_text_layout.findViewById(R.id.cell_authors));
    this.cell_buttons = NullCheck.notNull(
      (ViewGroup) this.cell_text_layout.findViewById(R.id.cell_buttons));

    this.cell_cover_layout =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.cell_cover_layout));
    this.cell_cover_image = NullCheck.notNull(
      (ImageView) this.cell_cover_layout.findViewById(R.id.cell_cover_image));
    this.cell_cover_progress = NullCheck.notNull(
      (ProgressBar) this.cell_cover_layout.findViewById(
        R.id.cell_cover_loading));

    this.cell_cover_progress.getIndeterminateDrawable()
      .setColorFilter(mainColor, android.graphics.PorterDuff.Mode.SRC_IN);

    /*
     * The height of the row is known, so assume a roughly 4:3 aspect ratio
     * for cover images and calculate the width of the cover layout in pixels.
     */

    final int cover_height = this.cell_cover_layout.getLayoutParams().height;
    final int cover_width = (int) (((double) cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams ccl_p =
      new LinearLayout.LayoutParams(cover_width, cover_height);
    this.cell_cover_layout.setLayoutParams(ccl_p);

    this.entry = new AtomicReference<FeedEntryOPDS>();

    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
  }

  private void loadImageAndSetVisibility(
    final FeedEntryOPDS in_e)
  {
    final int in_image_height = this.cell_cover_layout.getLayoutParams().height;

    final ImageView coverImage = this.cell_cover_image;
    final ProgressBar coverProgress = this.cell_cover_progress;

    coverImage.setVisibility(View.INVISIBLE);
    coverProgress.setVisibility(View.VISIBLE);

    final FutureCallback<kotlin.Unit> callback = new FutureCallback<kotlin.Unit>()
    {
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
          CatalogFeedBookCellView.LOG.error("unable to load image");
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

  @Override public Unit onBookStatusDownloaded(
    final BookStatusDownloaded d)
  {
    final BookID book_id = d.getID();
    CatalogFeedBookCellView.LOG.debug("{}: downloaded", book_id);

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("downloaded");

    final FeedEntryOPDS e = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(e);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

      this.cell_buttons.addView(
        new CatalogBookReadButton(
          this.activity, book_id, this.entry.get(), this.books), 0);

    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadFailed(
    final BookStatusDownloadFailed f)
  {
    CatalogFeedBookCellView.LOG.debug("{}: download failed", f.getID());

    /*
     * Unset the content description so that the screen reader reads the error message.
     */

    this.setContentDescription(null);

    if (CatalogBookUnauthorized.isUnAuthorized(f))
    {
      CatalogFeedBookCellView.this.books.accountRemoveCredentials();
    }

    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.VISIBLE);
    this.setDebugCellText("download-failed");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    final OPDSAcquisitionFeedEntry oe = fe.getFeedEntry();

    final Resources rr = NullCheck.notNull(this.activity.getResources());
    this.cell_downloading_failed_label.setText(
      CatalogBookErrorStrings.getFailureString(rr, f));

    this.cell_downloading_failed_title.setText(oe.getTitle());
    this.cell_downloading_failed_dismiss.setOnClickListener(
      v -> this.books.bookDownloadAcknowledge(f.getID()));

    /*
     * Manually construct an acquisition controller for the retry button.
     */

    final OptionType<OPDSAcquisitionPath> a_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(oe.getAcquisitionPaths());

    /*
     * Theoretically, if the book has ever been downloaded, then the
     * acquisition list must have contained one usable acquisition relation...
     */

    if (a_opt.isNone()) {
      throw new UnreachableCodeException();
    }

    final OPDSAcquisitionPath a = ((Some<OPDSAcquisitionPath>) a_opt).get();
    final CatalogAcquisitionButtonController retry_ctl =
      new CatalogAcquisitionButtonController(
        this.activity, this.books, fe.getBookID(), a, fe);

    this.cell_downloading_failed_retry.setVisibility(View.VISIBLE);
    this.cell_downloading_failed_retry.setEnabled(true);
    this.cell_downloading_failed_retry.setOnClickListener(retry_ctl);

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
    CatalogFeedBookCellView.LOG.debug("{}: downloading", d.getID());

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
    this.cell_downloading_authors.setText(oe.getAuthorsCommaSeparated());

    CatalogDownloadProgressBar.setProgressBar(
      d.getCurrentTotalBytes(),
      d.getExpectedTotalBytes(),
      this.cell_downloading_percent_text,
      this.cell_downloading_progress);

    this.cell_downloading_cancel.setVisibility(View.VISIBLE);
    this.cell_downloading_cancel.setEnabled(true);
    this.cell_downloading_cancel.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          CatalogFeedBookCellView.this.books.bookDownloadCancel(book_id);
        }
      });

    return Unit.unit();
  }

  @Override public Unit onBookStatusHeld(
    final BookStatusHeld s)
  {
    CatalogFeedBookCellView.LOG.debug("{}: held", s.getID());

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("held");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(fe);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

    if (s.isRevocable()) {
      final CatalogBookRevokeButton revoke = new CatalogBookRevokeButton(
        this.activity, s.getID(), CatalogBookRevokeType.REVOKE_HOLD, this.books);
      this.cell_buttons.addView(revoke, 0);
    }

    return Unit.unit();
  }

  @Override public Unit onBookStatusHeldReady(
    final BookStatusHeldReady s)
  {
    CatalogFeedBookCellView.LOG.debug("{}: reserved", s.getID());

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("reserved");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(fe);

    CatalogAcquisitionButtons.Companion.addButtons(
      this.activity, this.cell_buttons, this.books, fe);

    if (s.isRevocable()) {
      final CatalogBookRevokeButton revoke = new CatalogBookRevokeButton(
        this.activity, s.getID(), CatalogBookRevokeType.REVOKE_HOLD, this.books);
      this.cell_buttons.addView(revoke, 0);
    }

    return Unit.unit();
  }

  @Override public Unit onBookStatusHoldable(
    final BookStatusHoldable s)
  {
    CatalogFeedBookCellView.LOG.debug("{}: holdable", s.getID());

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("holdable");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(fe);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

    CatalogAcquisitionButtons.Companion.addButtons(
      this.activity, this.cell_buttons, this.books, fe);
    return Unit.unit();
  }

  @Override public Unit onBookStatusLoanable(
    final BookStatusLoanable s)
  {
    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.onBookStatusNone(fe, s.getID());
    return Unit.unit();
  }

  @Override public Unit onBookStatusRevokeFailed(
    final BookStatusRevokeFailed s)
  {
    CatalogFeedBookCellView.LOG.debug("{}: revoke failed", s.getID());

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
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          CatalogFeedBookCellView.this.books.bookGetLatestStatusFromDisk(s.getID());
        }
      });

    this.cell_downloading_failed_retry.setVisibility(View.GONE);
    this.cell_downloading_failed_retry.setEnabled(false);
    return Unit.unit();
  }

  @Override public Unit onBookStatusRevoked(final BookStatusRevoked o)
  {
    CatalogFeedBookCellView.LOG.debug("{}: revoked", o.getID());

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("revoked");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(fe);

    return Unit.unit();
  }

  @Override public Unit onBookStatusLoaned(
    final BookStatusLoaned o)
  {
    CatalogFeedBookCellView.LOG.debug("{}: loaned", o.getID());

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("loaned");

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(fe);

    CatalogAcquisitionButtons.Companion.addButtons(
      this.activity, this.cell_buttons, this.books, fe);

    return Unit.unit();
  }

  @Override public Unit onBookStatusLoanedType(
    final BookStatusLoanedType o)
  {
    return o.matchBookLoanedStatus(this);
  }

  private void onBookStatusNone(
    final FeedEntryOPDS in_entry,
    final BookID id)
  {
    CatalogFeedBookCellView.LOG.debug("{}: none", id);

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("none");

    this.loadImageAndSetVisibility(in_entry);

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();

    CatalogAcquisitionButtons.Companion.addButtons(
      this.activity, this.cell_buttons, this.books, in_entry);
  }

  @Override public Unit onBookStatusRequestingDownload(
    final BookStatusRequestingDownload d)
  {
    CatalogFeedBookCellView.LOG.debug("{}: requesting download", d.getID());

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

  @Override public Unit onBookStatusRequestingLoan(
    final BookStatusRequestingLoan s)
  {
    CatalogFeedBookCellView.LOG.debug("{}: requesting loan", s.getID());

    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("requesting-loan");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    final OPDSAcquisitionFeedEntry oe = fe.getFeedEntry();

    this.cell_downloading_label.setText(R.string.catalog_requesting_loan);
    this.cell_downloading_title.setText(oe.getTitle());
    this.cell_downloading_authors.setText(oe.getAuthorsCommaSeparated());

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
  public Unit onBookStatusRequestingRevoke(final BookStatusRequestingRevoke s)
  {
    CatalogFeedBookCellView.LOG.debug("{}: requesting revoke", s.getID());

    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.VISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("requesting-revoke");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    final OPDSAcquisitionFeedEntry oe = fe.getFeedEntry();

    this.cell_downloading_label.setText(R.string.catalog_requesting_revoke);
    this.cell_downloading_title.setText(oe.getTitle());
    this.cell_downloading_authors.setText(oe.getAuthorsCommaSeparated());

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

  @Override public Unit onFeedEntryCorrupt(
    final FeedEntryCorrupt e)
  {
    CatalogFeedBookCellView.LOG.debug(
      "{}: feed entry corrupt: ", e.getBookID(), e.getError());

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

  @Override public Unit onFeedEntryOPDS(
    final FeedEntryOPDS feed_e)
  {
    final OPDSAcquisitionFeedEntry oe = feed_e.getFeedEntry();
    this.cell_title.setText(oe.getTitle());
    this.cell_authors.setText(oe.getAuthorsCommaSeparated());

    this.setContentDescription(
      CatalogBookFormats.contentDescriptionOfEntry(this.getResources(), feed_e));

    final CatalogBookSelectionListenerType book_listener =
      this.book_selection_listener;
    this.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          book_listener.onSelectBook(CatalogFeedBookCellView.this, feed_e);
        }
      });

    this.entry.set(feed_e);

    final BookID book_id = feed_e.getBookID();
    final BooksStatusCacheType status_cache = this.books.bookGetStatusCache();
    final OptionType<BookStatusType> stat =
      status_cache.booksStatusGet(book_id);

    this.onStatus(feed_e, book_id, stat);
    return Unit.unit();
  }

  private void onStatus(
    final FeedEntryOPDS in_entry,
    final BookID id,
    final OptionType<BookStatusType> status_opt)
  {
    if (status_opt.isSome()) {
      final Some<BookStatusType> some = (Some<BookStatusType>) status_opt;
      UIThread.runOnUIThread(
        new Runnable()
        {
          @Override public void run()
          {
            some.get().matchBookStatus(CatalogFeedBookCellView.this);
          }
        });
    } else {
      UIThread.runOnUIThread(
        new Runnable()
        {
          @Override public void run()
          {
            CatalogFeedBookCellView.this.onBookStatusNone(in_entry, id);
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
    Assertions.checkPrecondition(
      data instanceof BookID, "%s instanceof %s", data, BookID.class);

    CatalogFeedBookCellView.LOG.debug("update: {}", data);

    final BookID update_id = (BookID) data;
    final FeedEntryOPDS in_entry = this.entry.get();

    if (in_entry != null) {
      final BookID current_id = in_entry.getBookID();
      if (current_id.equals(update_id)) {
        UIThread.runOnUIThread(
          new Runnable()
          {
            @Override public void run()
            {
              CatalogFeedBookCellView.this.viewConfigure(
                in_entry, CatalogFeedBookCellView.this.book_selection_listener);
            }
          });
      }
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
    final FeedEntryType in_e,
    final CatalogBookSelectionListenerType in_listener)
  {
    NullCheck.notNull(in_e);
    NullCheck.notNull(in_listener);

    UIThread.checkIsUIThread();

    this.book_selection_listener = NullCheck.notNull(in_listener);
    in_e.matchFeedEntry(this);
  }
}
