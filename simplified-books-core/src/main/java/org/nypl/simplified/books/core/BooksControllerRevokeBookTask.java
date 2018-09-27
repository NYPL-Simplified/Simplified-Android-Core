package org.nypl.simplified.books.core;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionPartialVisitorType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NonNull;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.drm.core.AdobeAdeptLoanReturnListenerType;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthOAuth;
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

import static org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.*;
import static org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.*;

final class BooksControllerRevokeBookTask
  implements Runnable, OPDSAvailabilityMatcherType<Unit, IOException> {

  private static final Logger LOG = LoggerFactory.getLogger(BooksControllerRevokeBookTask.class);

  private final BookID book_id;
  private final BookDatabaseType books_database;
  private final BooksStatusCacheType books_status;
  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final FeedLoaderType feed_loader;
  private final AccountsDatabaseReadableType accounts_database;
  private BookDatabaseEntryType database_entry;
  private BookDatabaseEntryFormatHandleEPUB database_epub_entry;

  BooksControllerRevokeBookTask(
    final BookDatabaseType in_books_database,
    final AccountsDatabaseReadableType in_accounts_database,
    final BooksStatusCacheType in_books_status,
    final FeedLoaderType in_feed_loader,
    final BookID in_book_id,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm) {
    this.book_id = NullCheck.notNull(in_book_id);
    this.books_database = NullCheck.notNull(in_books_database);
    this.accounts_database = NullCheck.notNull(in_accounts_database);
    this.books_status = NullCheck.notNull(in_books_status);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.feed_loader = NullCheck.notNull(in_feed_loader);
  }

  @Override
  public void run() {
    try {
      LOG.debug("[{}]: revoking", this.book_id.getShortID());

      this.database_entry = this.books_database.databaseOpenExistingEntry(this.book_id);

      OptionType<BookDatabaseEntryFormatHandleEPUB> format_opt =
        this.database_entry.entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB.class);

      if (format_opt.isNone()) {
        throw new UnimplementedCodeException();
      }

      this.database_epub_entry = ((Some<BookDatabaseEntryFormatHandleEPUB>) format_opt).get();

      final BookDatabaseEntrySnapshot snap = this.database_entry.entryGetSnapshot();
      final OPDSAvailabilityType avail = snap.getEntry().getAvailability();
      LOG.debug("[{}]: availability is {}", this.book_id.getShortID(), avail);
      avail.matchAvailability(this);
    } catch (final Throwable e) {
      LOG.error("[{}]: could not revoke book: ", this.book_id.getShortID(), e);
    }
  }

  @Override
  public Unit onHeldReady(final OPDSAvailabilityHeldReady a) {
    a.getRevoke().mapPartial_(revoke_uri -> this.revokeUsingURI(revoke_uri, RevokeType.HOLD));
    return Unit.unit();
  }

  private void revokeUsingURI(
    final URI u,
    final RevokeType type) {

    LOG.debug("[{}]: revoking URI {} of type {}", this.book_id.getShortID(), u, type);

    /*
     * Hitting a revoke link yields a single OPDS entry indicating
     * the current state of the book. It should be equivalent to the
     * entry seen by an unauthenticated user browsing the catalog right now.
     */

    final HTTPAuthType auth = this.getHTTPAuth();
    final FeedLoaderListenerType listener = new FeedLoaderListenerType() {
      @Override
      public void onFeedLoadSuccess(
        final URI u,
        final FeedType f) {
        try {
          BooksControllerRevokeBookTask.this.revokeFeedReceived(f);
        } catch (final Throwable e) {
          BooksControllerRevokeBookTask.this.revokeFailed(Option.some(e), e.getMessage());
        }
      }

      @Override
      public void onFeedRequiresAuthentication(
        final URI u,
        final int attempts,
        final FeedLoaderAuthenticationListenerType listener) {

        /*
         * If the saved authentication details are wrong, give up.
         */

        listener.onAuthenticationNotProvided();

      }

      @Override
      public void onFeedLoadFailure(
        final URI u,
        final Throwable x) {
        BooksControllerRevokeBookTask.this.revokeFailed(Option.some(x), x.getMessage());
      }
    };

    this.feed_loader.fromURIRefreshing(u, Option.some(auth), "PUT", listener);
  }

  private void revokeFeedReceived(final FeedType f)
    throws IOException {
    LOG.debug("[{}]: received a feed of type {}", this.book_id.getShortID(), f.getClass());

    f.matchFeed(
      new FeedMatcherType<Unit, IOException>() {
        /**
         * The server should never return a feed with groups.
         */

        @Override
        public Unit onFeedWithGroups(final FeedWithGroups f)
          throws IOException {
          throw new IOException("Received a feed with groups!");
        }

        @Override
        public Unit onFeedWithoutGroups(final FeedWithoutGroups f)
          throws IOException {
          /*
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
    throws IOException {
    LOG.debug("[{}]: received a feed entry of type {}", this.book_id.getShortID(), e.getClass());

    e.matchFeedEntry(
      new FeedEntryMatcherType<Unit, IOException>() {
        @Override
        public Unit onFeedEntryOPDS(final FeedEntryOPDS e)
          throws IOException {
          BooksControllerRevokeBookTask.this.revokeFeedEntryReceivedOPDS(e);
          return Unit.unit();
        }

        @Override
        public Unit onFeedEntryCorrupt(final FeedEntryCorrupt e) {
          /*
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
    throws IOException {
    LOG.debug("[{}]: publishing revocation status", this.book_id.getShortID());

    this.books_status.booksRevocationFeedEntryUpdate(e);
    this.books_status.booksStatusClearFor(this.book_id);
    this.database_entry.entryDestroy();
  }

  /**
   * Revocation failed.
   */

  private void revokeFailed(
    final OptionType<Throwable> error,
    final String message) {
    LOG.error("[{}]: revocation failed: ", this.book_id.getShortID(), message);

    if (error.isSome()) {
      final Throwable ex = ((Some<Throwable>) error).get();
      LOG.error("[{}]: revocation failed, exception: ", this.book_id.getShortID(), ex);
    }

    LOG.debug("[{}] publishing failure status", this.book_id.getShortID());

    final BookStatusRevokeFailed status =
      new BookStatusRevokeFailed(this.book_id, error);
    this.books_status.booksStatusUpdate(status);
  }

  @NonNull
  private HTTPAuthType getHTTPAuth() {
    final AccountCredentials credentials = this.getAccountCredentials();
    final AccountBarcode barcode = credentials.getBarcode();
    final AccountPIN pin = credentials.getPin();

    HTTPAuthType auth =
      new HTTPAuthBasic(barcode.toString(), pin.toString());

    if (credentials.getAuthToken().isSome()) {
      final AccountAuthToken token = ((Some<AccountAuthToken>) credentials.getAuthToken()).get();
      if (token != null) {
        auth = new HTTPAuthOAuth(token.toString());
      }
    }

    return auth;
  }

  private AccountCredentials getAccountCredentials() {
    final OptionType<AccountCredentials> credentials_opt =
      this.accounts_database.accountGetCredentials();
    if (credentials_opt.isNone()) {
      throw new IllegalStateException("Not logged in!");
    }

    return ((Some<AccountCredentials>) credentials_opt).get();
  }

  @Override
  public Unit onHeld(final OPDSAvailabilityHeld a) {
    a.getRevoke().mapPartial_(revoke_uri -> this.revokeUsingURI(revoke_uri, RevokeType.HOLD));
    return Unit.unit();
  }

  @Override
  public Unit onHoldable(final OPDSAvailabilityHoldable a) {
    this.notRevocable(a);
    return Unit.unit();
  }

  private void notRevocable(
    final OPDSAvailabilityType a) {
    final OptionType<Throwable> none = Option.none();
    this.revokeFailed(none, String.format("Status is %s, nothing to revoke!", a));
  }

  @Override
  public Unit onLoaned(final OPDSAvailabilityLoaned a)
    throws IOException {
    a.getRevoke().acceptPartial(
      new OptionPartialVisitorType<URI, Unit, IOException>() {
        @Override
        public Unit none(final None<URI> n) {
          BooksControllerRevokeBookTask.this.notRevocable(a);
          return Unit.unit();
        }

        @Override
        public Unit some(final Some<URI> s)
          throws IOException {
          BooksControllerRevokeBookTask.this.revokeLoanedWithDRM(s.get());
          return Unit.unit();
        }
      });
    return Unit.unit();
  }

  private void revokeLoanedWithDRM(final URI revoke_uri) throws IOException {
    if (this.adobe_drm.isSome()) {
      final AdobeAdeptExecutorType adobe = ((Some<AdobeAdeptExecutorType>) this.adobe_drm).get();
      final BookDatabaseEntrySnapshot snap = this.database_entry.entryGetSnapshot();
      final OptionType<BookDatabaseEntryFormatSnapshotEPUB> epub_snap_opt =
        snap.findFormat(BookDatabaseEntryFormatSnapshotEPUB.class);

      /*
       * If the Adobe loan information is gone, it's assumed that it is a non-drm
       * book from a library that still needs to be "returned"
       */

      final OptionType<AdobeAdeptLoan> loan_opt;
      if (epub_snap_opt.isSome()) {
        final BookDatabaseEntryFormatSnapshotEPUB epub_snap =
          ((Some<BookDatabaseEntryFormatSnapshotEPUB>) epub_snap_opt).get();
        loan_opt = epub_snap.getAdobeRights();
      } else {
        loan_opt = Option.none();
      }

      if (loan_opt.isNone()) {
        returnBookWithoutDRM(snap, revoke_uri);
        return;
      }

      /*
       * If it turns out that the loan is not actually returnable, well, there's
       * nothing we can do about that. This is a bug in the program.
       */

      final AdobeAdeptLoan loan = ((Some<AdobeAdeptLoan>) loan_opt).get();
      if (loan.isReturnable()) {

        /*
         * Execute a task using the Adobe DRM library, and wait for it to
         * finish. The reason for the waiting, as opposed to calling further
         * methods from inside the listener callbacks is to avoid any chance
         * of the methods in question propagating an unchecked exception back
         * to the native code. This will obviously crash the whole process,
         * rather than just failing the revocation.
         */

        final CountDownLatch latch = new CountDownLatch(1);
        final AdobeLoanReturnResult listener = new AdobeLoanReturnResult(latch);
        adobe.execute(connector -> {
          final AdobeUserID user =
            ((Some<AdobeUserID>) this.getAccountCredentials().getAdobeUserID()).get();
          connector.loanReturn(listener, loan.getID(), user);
        });

        /*
         * Wait for the Adobe task to finish. Give up if it appears to be
         * hanging.
         */

        try {
          latch.await(3, TimeUnit.MINUTES);
        } catch (final InterruptedException x) {
          throw new IOException("Timed out waiting for Adobe revocation!", x);
        }

        /*
         * If Adobe couldn't revoke the book, then the book isn't revoked.
         * The user can try again later.
         */

        final OptionType<Throwable> error_opt = listener.getError();
        if (error_opt.isSome()) {
          this.revokeFailed(error_opt, null);
          return;
        }

        /*
         * Save the "revoked" state of the book.
         */

        final OPDSAcquisitionFeedEntryBuilderType b =
          OPDSAcquisitionFeedEntry.newBuilderFrom(snap.getEntry());
        b.setAvailability(OPDSAvailabilityRevoked.get(revoke_uri));
        final OPDSAcquisitionFeedEntry ee = b.build();

        this.database_entry.entrySetFeedData(ee);
      }

      /*
       * Everything went well... Finish the revocation by telling
       * the server about it.
       */

      this.revokeUsingURI(revoke_uri, RevokeType.LOAN);
    } else {

      /*
       * DRM is apparently unsupported. It's unclear how the user
       * could have gotten this far without DRM support, as they'd not have
       * been able to fulfill a non open-access book.
       */

      final OptionType<Throwable> no_exception = Option.none();
      this.revokeFailed(no_exception, "DRM is not supported!");
    }
  }

  private void returnBookWithoutDRM(
    final BookDatabaseEntrySnapshot snapshot,
    final URI revoke_uri)
    throws IOException {

    /*
     * Save the "revoked" state of the book.
     * Finish the revocation by telling the server about it.
     */

    final OPDSAcquisitionFeedEntryBuilderType b =
      OPDSAcquisitionFeedEntry.newBuilderFrom(snapshot.getEntry());
    b.setAvailability(OPDSAvailabilityRevoked.get(revoke_uri));
    final OPDSAcquisitionFeedEntry ee = b.build();

    this.database_entry.entrySetFeedData(ee);
    this.revokeUsingURI(revoke_uri, RevokeType.LOAN);
  }

  @Override
  public Unit onLoanable(final OPDSAvailabilityLoanable a) {
    this.notRevocable(a);
    return Unit.unit();
  }

  @Override
  public Unit onOpenAccess(final OPDSAvailabilityOpenAccess a) {
    a.getRevoke().mapPartial_(revoke_uri -> this.revokeUsingURI(revoke_uri, RevokeType.LOAN));
    return Unit.unit();
  }

  @Override
  public Unit onRevoked(final OPDSAvailabilityRevoked a) {
    this.revokeUsingURI(a.getRevoke(), RevokeType.LOAN);
    return Unit.unit();
  }

  private enum RevokeType {
    LOAN, HOLD
  }

  private static final class AdobeLoanReturnResult implements AdobeAdeptLoanReturnListenerType {

    private final CountDownLatch latch;
    private OptionType<Throwable> error;

    AdobeLoanReturnResult(final CountDownLatch in_latch) {
      this.latch = NullCheck.notNull(in_latch);
      this.error = Option.some(new BookRevokeExceptionNotReady());
    }

    public OptionType<Throwable> getError() {
      return this.error;
    }

    @Override
    public void onLoanReturnSuccess() {
      try {
        LOG.debug("onLoanReturnSuccess");
        this.error = Option.none();
      } finally {
        this.latch.countDown();
      }
    }

    @Override
    public void onLoanReturnFailure(final String in_error) {
      try {
        LOG.debug("onLoanReturnFailure: {}", in_error);

        if (in_error.startsWith("E_ACT_NOT_READY")) {
          this.error = Option.some(new AccountNotReadyException(in_error));
        }

        // Known issue of 404 URL for OneClick/RBdigital books
        else if (in_error.startsWith("E_STREAM_ERROR")) {
          LOG.debug("E_STREAM_ERROR: Ignore and continue with return.");
          this.error = Option.none();
        } else {
          this.error = Option.some(new BookRevokeExceptionDRMWorkflowError(in_error));
        }

      } finally {
        this.latch.countDown();
      }
    }
  }
}
