package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NonNull;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;

final class BooksControllerRevokeBookTask
  implements Runnable, OPDSAvailabilityMatcherType<Unit, IOException>
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerRevokeBookTask.class));
  }

  private final BookID                                      book_id;
  private final BookDatabaseType                            books_database;
  private final BooksStatusCacheType                        books_status;
  private final OptionType<AdobeAdeptExecutorType>          adobe_drm;
  private final PartialFunctionType<URI, Unit, IOException> revoke_hold;
  private final PartialFunctionType<URI, Unit, IOException> revoke_loan;
  private final FeedLoaderType                              feed_loader;

  BooksControllerRevokeBookTask(
    final BookDatabaseType in_books_database,
    final BooksStatusCacheType in_books_status,
    final FeedLoaderType in_feed_loader,
    final BookID in_book_id,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm)
  {
    this.book_id = NullCheck.notNull(in_book_id);
    this.books_database = NullCheck.notNull(in_books_database);
    this.books_status = NullCheck.notNull(in_books_status);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.feed_loader = NullCheck.notNull(in_feed_loader);

    this.revoke_hold = new PartialFunctionType<URI, Unit, IOException>()
    {
      @Override public Unit call(final URI u)
        throws IOException
      {
        BooksControllerRevokeBookTask.this.revoke(u, RevokeType.HOLD);
        return Unit.unit();
      }
    };

    this.revoke_loan = new PartialFunctionType<URI, Unit, IOException>()
    {
      @Override public Unit call(final URI u)
        throws IOException
      {
        BooksControllerRevokeBookTask.this.revoke(u, RevokeType.LOAN);
        return Unit.unit();
      }
    };
  }

  @Override public void run()
  {
    try {
      final BookDatabaseEntryType e =
        this.books_database.getBookDatabaseEntry(this.book_id);
      final BookSnapshot snap = e.getSnapshot();
      final OPDSAvailabilityType avail = snap.getEntry().getAvailability();

      avail.matchAvailability(this);
    } catch (final Throwable e) {
      BooksControllerRevokeBookTask.LOG.error(
        "could not revoke book {}: ", this.book_id, e);
    }
  }

  @Override public Unit onHeldReady(final OPDSAvailabilityHeldReady a)
    throws IOException
  {
    a.getRevoke().mapPartial(this.revoke_hold);
    return Unit.unit();
  }

  private void revoke(
    final URI u,
    final RevokeType type)
    throws IOException
  {
    /**
     * Hitting a revoke link yields a single OPDS entry indicating
     * the current state of the book. It should be equivalent to the
     * entry seen by an unauthenticated user browsing the catalog right now.
     */

    final HTTPAuthType auth = this.getHTTPAuth();
    this.feed_loader.fromURIRefreshing(
      u, Option.some(auth), new FeedLoaderListenerType()
      {
        @Override public void onFeedLoadSuccess(
          final URI u,
          final FeedType f)
        {
          try {
            BooksControllerRevokeBookTask.this.revokeFeedReceived(f);
          } catch (final IOException e) {
            final OptionType<Throwable> es = Option.some((Throwable) e);
            BooksControllerRevokeBookTask.this.revokeFailed(es);
          }
        }

        @Override public void onFeedLoadFailure(
          final URI u,
          final Throwable x)
        {
          final OptionType<Throwable> error = Option.some(x);
          BooksControllerRevokeBookTask.this.revokeFailed(error);
        }
      });
  }

  private void revokeFeedReceived(final FeedType f)
    throws IOException
  {
    f.matchFeed(
      new FeedMatcherType<Unit, IOException>()
      {
        /**
         * The server should never return a feed with groups.
         */

        @Override public Unit onFeedWithGroups(final FeedWithGroups f)
          throws IOException
        {
          throw new IOException("Received a feed with groups!");
        }

        @Override public Unit onFeedWithoutGroups(final FeedWithoutGroups f)
          throws IOException
        {
          /**
           * The server should never return an empty feed.
           */

          if (f.size() == 0) {
            throw new IOException("Received empty feed");
          }
          BooksControllerRevokeBookTask.this.revokeFeedEntryReceived(f.get(0));
          return Unit.unit();
        }
      });
  }

  private void revokeFeedEntryReceived(final FeedEntryType e)
    throws IOException
  {
    e.matchFeedEntry(
      new FeedEntryMatcherType<Unit, IOException>()
      {
        @Override public Unit onFeedEntryOPDS(final FeedEntryOPDS e)
          throws IOException
        {
          BooksControllerRevokeBookTask.this.revokeFeedEntryReceivedOPDS(e);
          return Unit.unit();
        }

        @Override public Unit onFeedEntryCorrupt(final FeedEntryCorrupt e)
          throws IOException
        {
          /**
           * A corrupt feed entry can only be seen in local feed entries. They
           * are never seen as the result of parsing a remote feed; the feed
           * as a whole would be considered invalid.
           */

          throw new UnreachableCodeException();
        }
      });
  }

  /**
   * An entry was received regarding the current state of the book. Publish a
   * status value so that any views that are still looking at the book can
   * re-render themselves with the new information.
   */

  private void revokeFeedEntryReceivedOPDS(final FeedEntryOPDS e)
  {
    final OptionType<File> no_cover = Option.none();
    final OptionType<File> no_book = Option.none();
    final OptionType<AdobeAdeptLoan> no_adobe_loan = Option.none();

    final BookSnapshot snap =
      new BookSnapshot(no_cover, no_book, e.getFeedEntry(), no_adobe_loan);
    final BookStatusType status = BookStatus.fromSnapshot(this.book_id, snap);
    this.books_status.booksSnapshotUpdate(this.book_id, snap);
    this.books_status.booksStatusUpdate(status);
  }

  /**
   * Revoking failed.
   */

  private void revokeFailed(final OptionType<Throwable> error)
  {
    final BookStatusRevokeFailed status =
      new BookStatusRevokeFailed(this.book_id, error);
    this.books_status.booksStatusUpdate(status);
  }

  private void deleteBookEntry()
    throws IOException
  {
    final BookDatabaseEntryType e =
      this.books_database.getBookDatabaseEntry(this.book_id);
    e.destroy();
    this.books_status.booksStatusClearFor(this.book_id);
  }

  @NonNull private HTTPAuthType getHTTPAuth()
    throws IOException
  {
    final Pair<AccountBarcode, AccountPIN> pair =
      this.books_database.credentialsGet();
    final AccountBarcode barcode = pair.getLeft();
    final AccountPIN pin = pair.getRight();
    return new HTTPAuthBasic(barcode.toString(), pin.toString());
  }

  @Override public Unit onHeld(final OPDSAvailabilityHeld a)
    throws IOException
  {
    a.getRevoke().mapPartial(this.revoke_hold);
    return Unit.unit();
  }

  @Override public Unit onHoldable(final OPDSAvailabilityHoldable a)
    throws IOException
  {
    BooksControllerRevokeBookTask.LOG.debug(
      "book status is {}, no revocation possible", a);
    return Unit.unit();
  }

  @Override public Unit onLoaned(final OPDSAvailabilityLoaned a)
    throws IOException
  {
    a.getRevoke().mapPartial(this.revoke_loan);
    return Unit.unit();
  }

  @Override public Unit onLoanable(final OPDSAvailabilityLoanable a)
    throws IOException
  {
    BooksControllerRevokeBookTask.LOG.debug(
      "book status is {}, no revocation possible", a);
    return Unit.unit();
  }

  @Override public Unit onOpenAccess(final OPDSAvailabilityOpenAccess a)
    throws IOException
  {
    a.getRevoke().mapPartial(this.revoke_loan);
    return Unit.unit();
  }

  private enum RevokeType
  {
    LOAN, HOLD
  }
}
