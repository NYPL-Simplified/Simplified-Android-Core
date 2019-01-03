package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.books.accounts.AccountAuthenticatedHTTP;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.book_database.Book;
import org.nypl.simplified.books.book_database.BookDatabaseEntryType;
import org.nypl.simplified.books.book_database.BookDatabaseException;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.book_registry.BookStatusRequestingRevoke;
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.books.exceptions.BookRevokeExceptionNoCredentials;
import org.nypl.simplified.books.exceptions.BookRevokeExceptionNoURI;
import org.nypl.simplified.books.feeds.FeedEntryCorrupt;
import org.nypl.simplified.books.feeds.FeedEntryMatcherType;
import org.nypl.simplified.books.feeds.FeedEntryOPDS;
import org.nypl.simplified.books.feeds.FeedEntryType;
import org.nypl.simplified.books.feeds.FeedLoaderAuthenticationListenerType;
import org.nypl.simplified.books.feeds.FeedLoaderListenerType;
import org.nypl.simplified.books.feeds.FeedLoaderType;
import org.nypl.simplified.books.feeds.FeedMatcherType;
import org.nypl.simplified.books.feeds.FeedType;
import org.nypl.simplified.books.feeds.FeedWithGroups;
import org.nypl.simplified.books.feeds.FeedWithoutGroups;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

final class BookRevokeTask implements Callable<Unit> {

  private static final Logger LOG = LoggerFactory.getLogger(BookRevokeTask.class);

  private final AccountType account;
  private final BookRegistryType book_registry;
  private final BookID book_id;
  private final FeedLoaderType feed_loader;

  BookRevokeTask(
      final BookRegistryType book_registry,
      final FeedLoaderType feed_loader,
      final AccountType account,
      final BookID book_id) {

    this.book_registry =
        NullCheck.notNull(book_registry, "book_registry");
    this.feed_loader =
        NullCheck.notNull(feed_loader, "feed_loader");
    this.account =
        NullCheck.notNull(account, "account");
    this.book_id =
        NullCheck.notNull(book_id, "book_id");
  }

  @Override
  public Unit call() throws Exception {
    LOG.debug("[{}] revoke", this.book_id.brief());

    final BookDatabaseEntryType entry = this.account.bookDatabase().entry(this.book_id);
    final Book book = entry.book();

    try {
      this.book_registry.update(
          BookWithStatus.create(book, new BookStatusRequestingRevoke(this.book_id)));

      final OPDSAcquisitionFeedEntry opds_entry = book.entry();
      return revokeBasedOnAvailability(book, opds_entry.getAvailability());
    } catch (final Exception e) {
      this.revokeFailed(book, Option.some(e), e.getMessage());
      throw e;
    } finally {
      LOG.debug("[{}] revoke finished", this.book_id.brief());
    }
  }

  private Unit revokeBasedOnAvailability(
      final Book book,
      final OPDSAvailabilityType availability) throws Exception {

    return availability.matchAvailability(new OPDSAvailabilityMatcherType<Unit, Exception>() {
      @Override
      public Unit onHeldReady(final OPDSAvailabilityHeldReady availability) throws Exception {
        throw new UnimplementedCodeException();
      }

      @Override
      public Unit onHeld(final OPDSAvailabilityHeld availability) throws Exception {
        throw new UnimplementedCodeException();
      }

      @Override
      public Unit onHoldable(final OPDSAvailabilityHoldable availability) throws Exception {
        throw new UnimplementedCodeException();
      }

      @Override
      public Unit onLoaned(final OPDSAvailabilityLoaned availability) throws Exception {
        throw new UnimplementedCodeException();
      }

      @Override
      public Unit onLoanable(final OPDSAvailabilityLoanable availability) throws Exception {
        throw new UnimplementedCodeException();
      }

      @Override
      public Unit onOpenAccess(final OPDSAvailabilityOpenAccess availability) throws Exception {
        return revokeOpenAccess(book, availability.getRevoke());
      }

      @Override
      public Unit onRevoked(final OPDSAvailabilityRevoked availability) throws Exception {
        throw new UnimplementedCodeException();
      }
    });
  }

  private Unit revokeOpenAccess(
      final Book book,
      final OptionType<URI> revoke_opt)
      throws Exception {

    if (revoke_opt.isNone()) {
      LOG.error("[{}] no revocation URI!", this.book_id.brief());
      throw new BookRevokeExceptionNoURI();
    }
    if (this.account.credentials().isNone()) {
      LOG.error("[{}] revocation requires credentials, but none are available", this.book_id.brief());
      throw new BookRevokeExceptionNoCredentials();
    }

    final AccountAuthenticationCredentials creds =
        ((Some<AccountAuthenticationCredentials>) this.account.credentials()).get();
    final URI revoke =
        ((Some<URI>) revoke_opt).get();
    final HTTPAuthType http_auth =
        AccountAuthenticatedHTTP.createAuthenticatedHTTP(creds);

    /*
     * Hitting a revoke link yields a single OPDS entry indicating
     * the current state of the book. It should be equivalent to the
     * entry seen by an unauthenticated user browsing the catalog right now.
     */

    final AtomicReference<Exception> exception_saved = new AtomicReference<>();
    final FeedLoaderListenerType listener = new FeedLoaderListenerType()
    {
      @Override public void onFeedLoadSuccess(
          final URI u,
          final FeedType f)
      {
        try {
          revokeFeedReceived(f);
        } catch (final Exception e) {
          exception_saved.set(e);
        }
      }

      @Override public void onFeedRequiresAuthentication(
          final URI u,
          final int attempts,
          final FeedLoaderAuthenticationListenerType listener)
      {
        /*
         * If the saved authentication details are wrong, give up.
         */

        listener.onAuthenticationNotProvided();
      }

      @Override public void onFeedLoadFailure(
          final URI u,
          final Throwable x)
      {
        exception_saved.set(new IOException(x));
      }
    };

    LOG.debug("[{}] contacting revoke URI {}", this.book_id.brief(), revoke);
    try {
      this.feed_loader.fromURIRefreshing(revoke, Option.some(http_auth), "PUT", listener).get();
    } catch (final ExecutionException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      throw e;
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    final Exception restored = exception_saved.get();
    if (restored != null) {
      throw restored;
    }
    return Unit.unit();
  }

  private Unit revokeFeedReceived(final FeedType feed) throws Exception {
    LOG.debug("[{}] received a feed of type {}", this.book_id.brief(), feed.getClass());

    return feed.matchFeed(
        new FeedMatcherType<Unit, Exception>()
        {
          /**
           * The server should never return a feed with groups.
           */

          @Override public Unit onFeedWithGroups(final FeedWithGroups f)
              throws Exception
          {
            throw new IOException("Received a feed with groups!");
          }

          @Override public Unit onFeedWithoutGroups(final FeedWithoutGroups f)
              throws Exception
          {
            /*
             * The server should never return an empty feed.
             */

            if (f.size() == 0) {
              throw new IOException("Received empty feed");
            }

            revokeFeedEntryReceived(f.get(0));
            return Unit.unit();
          }
        });
  }


  private Unit revokeFeedEntryReceived(final FeedEntryType entry) throws Exception {
    LOG.debug("[{}] received a feed entry of type {}",
        this.book_id.brief(),
        entry.getClass());

    return entry.matchFeedEntry(
        new FeedEntryMatcherType<Unit, Exception>()
        {
          @Override public Unit onFeedEntryOPDS(final FeedEntryOPDS e)
              throws Exception
          {
            return revokeFeedEntryReceivedOPDS(e);
          }

          @Override public Unit onFeedEntryCorrupt(final FeedEntryCorrupt e)
              throws Exception
          {
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

  private Unit revokeFeedEntryReceivedOPDS(final FeedEntryOPDS entry) throws BookDatabaseException {
    LOG.debug("[{}] deleting book and publishing revocation status", this.book_id.brief());

    final BookDatabaseEntryType database_entry = this.account.bookDatabase().entry(this.book_id);
    database_entry.delete();

    this.book_registry.clearFor(this.book_id);
    return Unit.unit();
  }

  /**
   * The revocation failed.
   */

  private void revokeFailed(
      final Book book,
      final OptionType<Throwable> exception,
      final String message) {
    LOG.error("[{}] revocation failed: ", this.book_id.brief(), message);

    if (exception.isSome()) {
      final Throwable ex = ((Some<Throwable>) exception).get();
      LOG.error("[{}] revocation failed, exception: ", this.book_id.brief(), ex);
    }

    LOG.debug("[{}] publishing failure status", this.book_id.brief());
    this.book_registry.update(
        BookWithStatus.create(book, new BookStatusRevokeFailed(this.book_id, exception)));
  }
}
