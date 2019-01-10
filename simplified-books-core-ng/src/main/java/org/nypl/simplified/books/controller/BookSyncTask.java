package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.accounts.AccountAuthenticatedHTTP;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountProviderAuthenticationDescription;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.book_database.Book;
import org.nypl.simplified.books.book_database.BookDatabaseEntryType;
import org.nypl.simplified.books.book_database.BookDatabaseException;
import org.nypl.simplified.books.book_database.BookDatabaseType;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_database.BookIDs;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.book_registry.BookStatus;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

final class BookSyncTask implements Callable<Unit> {

  private static final Logger LOG = LoggerFactory.getLogger(BookSyncTask.class);

  private final BooksControllerType books_controller;
  private final AccountType account;
  private final BookRegistryType book_registry;
  private final HTTPType http;
  private final OPDSFeedParserType feed_parser;

  BookSyncTask(
      final BooksControllerType books_controller,
      final AccountType account,
      final BookRegistryType book_registry,
      final HTTPType http,
      final OPDSFeedParserType feed_parser) {

    this.books_controller =
        NullCheck.notNull(books_controller, "Books controller");
    this.account =
        NullCheck.notNull(account, "Account");
    this.book_registry =
        NullCheck.notNull(book_registry, "Book registry");
    this.http =
        NullCheck.notNull(http, "Http");
    this.feed_parser =
        NullCheck.notNull(feed_parser, "Feed parser");
  }

  @Override
  public Unit call() throws Exception {
    try {
      LOG.debug("syncing account {}", this.account.id().id());
      return execute();
    } finally {
      LOG.debug("finished syncing account {}", this.account.id().id());
    }
  }

  private Unit execute() throws Exception {
    final OptionType<AccountProviderAuthenticationDescription> provider_auth_opt =
        this.account.provider().authentication();

    if (!provider_auth_opt.isSome()) {
      LOG.debug("account does not support syncing");
      return Unit.unit();
    }

    final AccountProviderAuthenticationDescription provider_auth =
        ((Some<AccountProviderAuthenticationDescription>) provider_auth_opt).get();

    final OptionType<AccountAuthenticationCredentials> credentials_opt =
        this.account.credentials();

    if (credentials_opt.isNone()) {
      LOG.debug("no credentials, aborting!");
      return Unit.unit();
    }

    final AccountAuthenticationCredentials credentials =
        ((Some<AccountAuthenticationCredentials>) credentials_opt).get();
    final HTTPAuthType auth =
        AccountAuthenticatedHTTP.createAuthenticatedHTTP(credentials);

    final HTTPResultType<InputStream> result =
        this.http.get(Option.some(auth), provider_auth.loginURI(), 0L);

    return result.matchResult(
        new HTTPResultMatcherType<InputStream, Unit, Exception>() {
          @Override
          public Unit onHTTPError(final HTTPResultError<InputStream> e) throws Exception {
            return BookSyncTask.this.onHTTPError(e, provider_auth);
          }

          @Override
          public Unit onHTTPException(final HTTPResultException<InputStream> e) throws Exception {
            throw e.getError();
          }

          @Override
          public Unit onHTTPOK(final HTTPResultOKType<InputStream> e) throws Exception {
            return BookSyncTask.this.onHTTPOK(e, provider_auth);
          }
        });
  }

  private Unit onHTTPOK(
      final HTTPResultOKType<InputStream> result,
      final AccountProviderAuthenticationDescription provider_auth) throws IOException {
    try {
      parseFeed(result, provider_auth);
      return Unit.unit();
    } finally {
      result.close();
    }
  }

  private void parseFeed(
      final HTTPResultOKType<InputStream> result,
      final AccountProviderAuthenticationDescription provider_auth)
      throws OPDSParseException {

    final OPDSAcquisitionFeed feed =
        this.feed_parser.parse(provider_auth.loginURI(), result.getValue());

    /*
     * Obtain the set of books that are on disk already. If any
     * of these books are not in the received feed, then they have
     * expired and should be deleted.
     */

    final BookDatabaseType book_database = this.account.bookDatabase();
    final Set<BookID> existing = book_database.books();

    /*
     * Handle each book in the received feed.
     */

    final Set<BookID> received = new HashSet<>(64);
    final List<OPDSAcquisitionFeedEntry> entries = feed.getFeedEntries();
    for (final OPDSAcquisitionFeedEntry opds_entry : entries) {
      final BookID book_id = BookIDs.newFromOPDSEntry(opds_entry);
      received.add(book_id);
      LOG.debug("[{}] updating", book_id.brief());

      try {
        final BookDatabaseEntryType database_entry =
            book_database.createOrUpdate(book_id, opds_entry);
        final Book book = database_entry.getBook();
        this.book_registry.update(BookWithStatus.create(book, BookStatus.fromBook(book)));
      } catch (final BookDatabaseException e) {
        LOG.error("[{}] unable to update database entry: ", book_id.brief(), e);
      }
    }

    /*
     * Now delete any book that previously existed, but is not in the
     * received set. Queue any revoked books for completion and then
     * deletion.
     */

    final Set<BookID> revoking = new HashSet<BookID>(existing.size());
    for (final BookID existing_id : existing) {
      try {
        LOG.debug("[{}] checking for deletion", existing_id.brief());

        if (!received.contains(existing_id)) {
          final BookDatabaseEntryType db_entry = book_database.entry(existing_id);
          final OPDSAvailabilityType a = db_entry.getBook().getEntry().getAvailability();
          if (a instanceof OPDSAvailabilityRevoked) {
            revoking.add(existing_id);
          }

          LOG.debug("[{}] deleting", existing_id.brief());
          db_entry.delete();
          this.book_registry.clearFor(existing_id);
        } else {
          LOG.debug("[{}] keeping", existing_id.brief());
        }
      } catch (final Throwable x) {
        LOG.error("[{}]: unable to delete entry: ", existing_id.value(), x);
      }
    }

    /*
     * Finish the revocation of any books that need it.
     */

    for (final BookID revoke_id : revoking) {
      LOG.debug("[{}] revoking", revoke_id.brief());
      this.books_controller.bookRevoke(this.account, revoke_id);
    }
  }

  private Unit onHTTPError(
      final HTTPResultError<InputStream> result,
      final AccountProviderAuthenticationDescription provider_auth) throws Exception {

    final String message =
        String.format("%s: %d: %s", provider_auth.loginURI(), result.getStatus(), result.getMessage());

    switch (result.getStatus()) {
      case HttpURLConnection.HTTP_UNAUTHORIZED: {
        this.account.setCredentials(Option.none());
        return Unit.unit();
      }
      default: {
        throw new IOException(message);
      }
    }
  }
}
