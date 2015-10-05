package org.nypl.simplified.books.core;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionPartialVisitorType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialProcedureType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NonNull;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.drm.core.AdobeAdeptConnectorType;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.drm.core.AdobeAdeptLoanReturnListenerType;
import org.nypl.drm.core.AdobeAdeptProcedureType;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryBuilderType;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class BooksControllerRevokeBookTask
  implements Runnable, OPDSAvailabilityMatcherType<Unit, IOException>
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerRevokeBookTask.class));
  }

  private final BookID                             book_id;
  private final BookDatabaseType                   books_database;
  private final BooksStatusCacheType               books_status;
  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final FeedLoaderType                     feed_loader;

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
  }

  @Override public void run()
  {
    try {
      BooksControllerRevokeBookTask.LOG.debug(
        "[{}]: revoking", this.book_id.getShortID());

      final BookDatabaseEntryReadableType e =
        this.books_database.databaseOpenEntryForReading(this.book_id);
      final BookDatabaseEntrySnapshot snap = e.entryGetSnapshot();
      final OPDSAvailabilityType avail = snap.getEntry().getAvailability();
      BooksControllerRevokeBookTask.LOG.debug(
        "[{}]: availability is {}", this.book_id.getShortID(), avail);
      avail.matchAvailability(this);
    } catch (final Throwable e) {
      BooksControllerRevokeBookTask.LOG.error(
        "[{}]: could not revoke book: ", this.book_id.getShortID(), e);
    }
  }

  @Override public Unit onHeldReady(final OPDSAvailabilityHeldReady a)
    throws IOException
  {
    a.getRevoke().mapPartial_(
      new PartialProcedureType<URI, IOException>()
      {
        @Override public void call(final URI revoke_uri)
          throws IOException
        {
          BooksControllerRevokeBookTask.this.revokeUsingURI(
            revoke_uri, RevokeType.HOLD);
        }
      });
    return Unit.unit();
  }

  private void revokeUsingURI(
    final URI u,
    final RevokeType type)
    throws IOException
  {
    BooksControllerRevokeBookTask.LOG.debug(
      "[{}]: revoking URI {} of type {}", this.book_id.getShortID(), u, type);

    /**
     * Hitting a revoke link yields a single OPDS entry indicating
     * the current state of the book. It should be equivalent to the
     * entry seen by an unauthenticated user browsing the catalog right now.
     */

    final HTTPAuthType auth = this.getHTTPAuth();
    final FeedLoaderListenerType listener = new FeedLoaderListenerType()
    {
      @Override public void onFeedLoadSuccess(
        final URI u,
        final FeedType f)
      {
        try {
          BooksControllerRevokeBookTask.this.revokeFeedReceived(f);
        } catch (final Throwable e) {
          BooksControllerRevokeBookTask.this.revokeFailed(
            Option.some(e), e.getMessage());
        }
      }

      @Override public void onFeedRequiresAuthentication(
        final URI u,
        final int attempts,
        final FeedLoaderAuthenticationListenerType listener)
      {
        /**
         * If the saved authentication details are wrong, give up.
         */

        if (attempts > 0) {
          listener.onAuthenticationNotProvided();
        }

        /**
         * Otherwise, try the saved authentication details!
         */

        try {
          final AccountCredentials creds =
            BooksControllerRevokeBookTask.this.books_database
              .databaseAccountCredentialsGet();
          listener.onAuthenticationProvided(creds);
        } catch (final IOException e) {
          listener.onAuthenticationError(
            Option.some((Throwable) e), e.getMessage());
        }
      }

      @Override public void onFeedLoadFailure(
        final URI u,
        final Throwable x)
      {
        BooksControllerRevokeBookTask.this.revokeFailed(
          Option.some(x), x.getMessage());
      }
    };

    this.feed_loader.fromURIRefreshing(
      u, Option.some(auth), listener);
  }

  private void revokeFeedReceived(final FeedType f)
    throws IOException
  {
    BooksControllerRevokeBookTask.LOG.debug(
      "[{}]: received a feed of type {}",
      this.book_id.getShortID(),
      f.getClass());

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
    BooksControllerRevokeBookTask.LOG.debug(
      "[{}]: received a feed entry of type {}",
      this.book_id.getShortID(),
      e.getClass());

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
    throws IOException
  {
    BooksControllerRevokeBookTask.LOG.debug(
      "[{}]: publishing revocation status", this.book_id.getShortID());

    this.books_status.booksRevocationFeedEntryUpdate(e);
    this.books_status.booksStatusClearFor(this.book_id);

    final BookDatabaseEntryType de =
      this.books_database.databaseOpenEntryForWriting(this.book_id);
    de.entryDestroy();
  }

  /**
   * Revocation failed.
   */

  private void revokeFailed(
    final OptionType<Throwable> error,
    final String message)
  {
    BooksControllerRevokeBookTask.LOG.error(
      "[{}]: revocation failed: ", this.book_id.getShortID(), message);

    if (error.isSome()) {
      final Throwable ex = ((Some<Throwable>) error).get();
      BooksControllerRevokeBookTask.LOG.error(
        "[{}]: revocation failed, exception: ", this.book_id.getShortID(), ex);
    }

    BooksControllerRevokeBookTask.LOG.debug(
      "[{}] publishing failure status", this.book_id.getShortID());

    final BookStatusRevokeFailed status =
      new BookStatusRevokeFailed(this.book_id, error);
    this.books_status.booksStatusUpdate(status);
  }

  @NonNull private HTTPAuthType getHTTPAuth()
    throws IOException
  {
    final AccountCredentials c =
      this.books_database.databaseAccountCredentialsGet();
    final AccountBarcode barcode = c.getUser();
    final AccountPIN pin = c.getPassword();
    return new HTTPAuthBasic(barcode.toString(), pin.toString());
  }

  @Override public Unit onHeld(final OPDSAvailabilityHeld a)
    throws IOException
  {
    a.getRevoke().mapPartial_(
      new PartialProcedureType<URI, IOException>()
      {
        @Override public void call(final URI revoke_uri)
          throws IOException
        {
          BooksControllerRevokeBookTask.this.revokeUsingURI(
            revoke_uri, RevokeType.HOLD);
        }
      });
    return Unit.unit();
  }

  @Override public Unit onHoldable(final OPDSAvailabilityHoldable a)
    throws IOException
  {
    this.notRevocable(a);
    return Unit.unit();
  }

  private void notRevocable(
    final OPDSAvailabilityType a)
  {
    final OptionType<Throwable> none = Option.none();
    this.revokeFailed(
      none, String.format("Status is %s, nothing to revoke!", a));
  }

  @Override public Unit onLoaned(final OPDSAvailabilityLoaned a)
    throws IOException
  {
    a.getRevoke().acceptPartial(
      new OptionPartialVisitorType<URI, Unit, IOException>()
      {
        @Override public Unit none(final None<URI> n)
          throws IOException
        {
          BooksControllerRevokeBookTask.this.notRevocable(a);
          return Unit.unit();
        }

        @Override public Unit some(final Some<URI> s)
          throws IOException
        {
          BooksControllerRevokeBookTask.this.revokeLoanedWithDRM(s.get());
          return Unit.unit();
        }
      });
    return Unit.unit();
  }

  private void revokeLoanedWithDRM(final URI revoke_uri)
    throws IOException
  {
    if (this.adobe_drm.isSome()) {
      final AdobeAdeptExecutorType adobe =
        ((Some<AdobeAdeptExecutorType>) this.adobe_drm).get();

      final BookDatabaseEntryReadableType er =
        this.books_database.databaseOpenEntryForReading(this.book_id);
      final BookDatabaseEntrySnapshot snap = er.entryGetSnapshot();

      /**
       * If the loan information is gone, well, there's nothing we can
       * do about that. This is a bug in the program.
       */

      final OptionType<AdobeAdeptLoan> loan_opt = snap.getAdobeRights();
      if (loan_opt.isNone()) {
        throw new UnreachableCodeException();
      }

      /**
       * If it turns out that the loan is not actually returnable, well, there's
       * nothing we can do about that. This is a bug in the program.
       */

      final AdobeAdeptLoan loan = ((Some<AdobeAdeptLoan>) loan_opt).get();
      if (loan.isReturnable() == false) {
        throw new UnreachableCodeException();
      }

      /**
       * Execute a task using the Adobe DRM library, and wait for it to
       * finish. The reason for the waiting, as opposed to calling further
       * methods from inside the listener callbacks is to avoid any chance
       * of the methods in question propagating an unchecked exception back
       * to the native code. This will obviously crash the whole process,
       * rather than just failing the revocation.
       */

      final CountDownLatch latch = new CountDownLatch(1);
      final AdobeLoanReturnResult listener = new AdobeLoanReturnResult(latch);
      adobe.execute(
        new AdobeAdeptProcedureType()
        {
          @Override public void executeWith(final AdobeAdeptConnectorType c)
          {
            c.loanReturn(listener, loan.getID());
          }
        });

      /**
       * Wait for the Adobe task to finish. Give up if it appears to be
       * hanging.
       */

      try {
        latch.await(3, TimeUnit.MINUTES);
      } catch (final InterruptedException x) {
        throw new IOException("Timed out waiting for Adobe revocation!", x);
      }

      /**
       * If Adobe couldn't revoke the book, then the book isn't revoked.
       * The user can try again later.
       */

      final OptionType<String> error_opt = listener.getError();
      if (error_opt.isSome()) {
        final String message = ((Some<String>) error_opt).get();
        final OptionType<Throwable> no_exception = Option.none();
        this.revokeFailed(no_exception, message);
        return;
      }

      /**
       * Save the "revoked" state of the book.
       */

      {
        final OPDSAcquisitionFeedEntryBuilderType b =
          OPDSAcquisitionFeedEntry.newBuilderFrom(snap.getEntry());
        b.setAvailability(OPDSAvailabilityRevoked.get(revoke_uri));
        final OPDSAcquisitionFeedEntry ee = b.build();
        final BookDatabaseEntryWritableType ew =
          this.books_database.databaseOpenEntryForWriting(this.book_id);
        ew.entrySetFeedData(ee);
      }

      /**
       * Everything went well... Finish the revocation by telling
       * the server about it.
       */

      this.revokeUsingURI(revoke_uri, RevokeType.LOAN);
    } else {

      /**
       * DRM is apparently unsupported. It's unclear how the user
       * could have gotten this far without DRM support, as they'd not have
       * been able to fulfill a non open-access book.
       */

      final OptionType<Throwable> no_exception = Option.none();
      this.revokeFailed(no_exception, "DRM is not supported!");
    }
  }

  @Override public Unit onLoanable(final OPDSAvailabilityLoanable a)
    throws IOException
  {
    this.notRevocable(a);
    return Unit.unit();
  }

  @Override public Unit onOpenAccess(final OPDSAvailabilityOpenAccess a)
    throws IOException
  {
    a.getRevoke().mapPartial_(
      new PartialProcedureType<URI, IOException>()
      {
        @Override public void call(final URI revoke_uri)
          throws IOException
        {
          BooksControllerRevokeBookTask.this.revokeUsingURI(
            revoke_uri, RevokeType.LOAN);
        }
      });
    return Unit.unit();
  }

  @Override public Unit onRevoked(final OPDSAvailabilityRevoked a)
    throws IOException
  {
    BooksControllerRevokeBookTask.this.revokeUsingURI(
      a.getRevoke(), RevokeType.LOAN);
    return Unit.unit();
  }

  private enum RevokeType
  {
    LOAN, HOLD
  }

  private static final class AdobeLoanReturnResult
    implements AdobeAdeptLoanReturnListenerType
  {
    private final CountDownLatch     latch;
    private       OptionType<String> error;

    public AdobeLoanReturnResult(final CountDownLatch in_latch)
    {
      this.latch = NullCheck.notNull(in_latch);
      this.error = Option.some("Revoke still in progress!");
    }

    public OptionType<String> getError()
    {
      return this.error;
    }

    @Override public void onLoanReturnSuccess()
    {
      try {
        BooksControllerRevokeBookTask.LOG.debug("onLoanReturnSuccess");
        this.error = Option.none();
      } finally {
        this.latch.countDown();
      }
    }

    @Override public void onLoanReturnFailure(final String in_error)
    {
      try {
        BooksControllerRevokeBookTask.LOG.debug(
          "onLoanReturnFailure: {}", in_error);
        this.error = Option.some(in_error);
      } finally {
        this.latch.countDown();
      }
    }
  }
}
