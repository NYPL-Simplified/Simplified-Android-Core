package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
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
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.squareup.picasso.Callback;
import org.nypl.simplified.app.BookCoverProviderType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookStatusDownloadFailed;
import org.nypl.simplified.books.core.BookStatusDownloadInProgress;
import org.nypl.simplified.books.core.BookStatusDownloaded;
import org.nypl.simplified.books.core.BookStatusDownloadingMatcherType;
import org.nypl.simplified.books.core.BookStatusDownloadingType;
import org.nypl.simplified.books.core.BookStatusHeld;
import org.nypl.simplified.books.core.BookStatusHoldable;
import org.nypl.simplified.books.core.BookStatusLoanable;
import org.nypl.simplified.books.core.BookStatusLoaned;
import org.nypl.simplified.books.core.BookStatusLoanedMatcherType;
import org.nypl.simplified.books.core.BookStatusLoanedType;
import org.nypl.simplified.books.core.BookStatusMatcherType;
import org.nypl.simplified.books.core.BookStatusRequestingDownload;
import org.nypl.simplified.books.core.BookStatusRequestingLoan;
import org.nypl.simplified.books.core.BookStatusType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryCorrupt;
import org.nypl.simplified.books.core.FeedEntryMatcherType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.FeedEntryType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A single cell in feed (list or grid).
 */

@SuppressWarnings("synthetic-access") public final class CatalogFeedBookCellView
  extends FrameLayout implements Observer,
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

    final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
      Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.catalog_book_cell, this, true);

    /**
     * Receive book status updates.
     */

    this.books.booksObservableAddObserver(this);

    this.cell_downloading =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.cell_downloading));
    this.cell_downloading_progress = NullCheck.notNull(
      (ProgressBar) this.cell_downloading.findViewById(
        R.id.cell_downloading_progress));
    this.cell_downloading_percent_text = NullCheck.notNull(
      (TextView) this.cell_downloading.findViewById(
        R.id.cell_downloading_percent_text));
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
    this.cell_downloading_failed_dismiss = NullCheck.notNull(
      (Button) this.cell_downloading_failed.findViewById(
        R.id.cell_downloading_failed_dismiss));
    this.cell_downloading_failed_retry = NullCheck.notNull(
      (Button) this.cell_downloading_failed.findViewById(
        R.id.cell_downloading_failed_retry));

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

    /**
     * The height of the row is known, so assume a roughly 4:3 aspect ratio
     * for cover images and calculate the width of the cover layout in pixels.
     */

    final int cover_height = this.cell_cover_layout.getLayoutParams().height;
    final int cover_width = (int) ((cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams ccl_p =
      new LinearLayout.LayoutParams(cover_width, cover_height);
    this.cell_cover_layout.setLayoutParams(ccl_p);

    this.entry = new AtomicReference<FeedEntryOPDS>();

    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
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
    return NullCheck.notNull(sb.toString());
  }

  private void loadImageAndSetVisibility(
    final FeedEntryOPDS in_e)
  {
    final int in_image_height = this.cell_cover_layout.getLayoutParams().height;

    final ImageView ci = this.cell_cover_image;
    final ProgressBar cp = this.cell_cover_progress;

    ci.setVisibility(View.INVISIBLE);
    cp.setVisibility(View.VISIBLE);

    final Callback callback = new Callback()
    {
      @Override public void onError()
      {
        CatalogFeedBookCellView.LOG.error("unable to load image");
        ci.setVisibility(View.INVISIBLE);
        cp.setVisibility(View.INVISIBLE);
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
      new CatalogBookDeleteButton(
        this.activity, book_id));
    this.cell_buttons.addView(
      new CatalogBookReadButton(
        this.activity, book_id));
    return Unit.unit();
  }

  @Override public Unit onBookStatusDownloadFailed(
    final BookStatusDownloadFailed f)
  {
    CatalogFeedBookCellView.LOG.debug("{}: download failed", f.getID());

    this.cell_book.setVisibility(View.INVISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.VISIBLE);
    this.setDebugCellText("download-failed");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    final OPDSAcquisitionFeedEntry oe = fe.getFeedEntry();

    this.cell_downloading_failed_title.setText(oe.getTitle());
    this.cell_downloading_failed_dismiss.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          CatalogFeedBookCellView.this.books.bookDownloadAcknowledge(
            f.getID());
        }
      });

    /**
     * Manually construct an acquisition controller for the retry button.
     */

    final OptionType<OPDSAcquisition> a_opt =
      CatalogAcquisitionButtons.getPreferredAcquisition(oe.getAcquisitions());

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
        this.activity, this.books, fe.getBookID(), a, fe);

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
    this.cell_downloading_title.setText(oe.getTitle());
    this.cell_downloading_authors.setText(
      CatalogFeedBookCellView.makeAuthorText(oe));

    CatalogDownloadProgressBar.setProgressBar(
      d.getCurrentTotalBytes(),
      d.getExpectedTotalBytes(),
      this.cell_downloading_percent_text,
      this.cell_downloading_progress);

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
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }

  @Override public Unit onBookStatusHoldable(
    final BookStatusHoldable s)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }

  @Override public Unit onBookStatusLoanable(
    final BookStatusLoanable s)
  {
    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.onBookStatusNone(fe, s.getID());
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

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(fe);

    CatalogAcquisitionButtons.addButtons(
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

    CatalogAcquisitionButtons.addButtons(
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

    this.cell_buttons.setVisibility(View.VISIBLE);
    this.cell_buttons.removeAllViews();
    return Unit.unit();
  }

  @Override public Unit onBookStatusRequestingLoan(
    final BookStatusRequestingLoan s)
  {
    CatalogFeedBookCellView.LOG.debug("{}: requesting loan", s.getID());

    this.cell_book.setVisibility(View.VISIBLE);
    this.cell_corrupt.setVisibility(View.INVISIBLE);
    this.cell_downloading.setVisibility(View.INVISIBLE);
    this.cell_downloading_failed.setVisibility(View.INVISIBLE);
    this.setDebugCellText("requesting-loan");

    final FeedEntryOPDS fe = NullCheck.notNull(this.entry.get());
    this.loadImageAndSetVisibility(fe);

    this.cell_buttons.setVisibility(View.INVISIBLE);
    this.cell_buttons.removeAllViews();
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
    this.cell_authors.setText(CatalogFeedBookCellView.makeAuthorText(oe));

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
    final OptionType<BookStatusType> stat = this.books.booksStatusGet(book_id);
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
