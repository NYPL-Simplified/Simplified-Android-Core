package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthOAuth;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.DRMLicensor;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

final class BooksControllerSyncTask implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BooksControllerSyncTask.class);

  private final OPDSFeedParserType feed_parser;
  private final HTTPType http;
  private final AccountSyncListenerType listener;
  private final AtomicBoolean running;
  private final BooksControllerType books_controller;
  private final BookDatabaseType books_database;
  private final AccountsDatabaseType accounts_database;
  private final URI loans_uri;
  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final DeviceActivationListenerType device_activation_listener;

  BooksControllerSyncTask(
    final BooksControllerType in_books,
    final BookDatabaseType in_books_database,
    final AccountsDatabaseType in_accounts_database,
    final HTTPType in_http,
    final OPDSFeedParserType in_feed_parser,
    final AccountSyncListenerType in_listener,
    final AtomicBoolean in_running,
    final URI in_loans_uri,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final DeviceActivationListenerType in_device_activation_listener) {
    this.books_controller = NullCheck.notNull(in_books);
    this.books_database = NullCheck.notNull(in_books_database);
    this.accounts_database = NullCheck.notNull(in_accounts_database);
    this.http = NullCheck.notNull(in_http);
    this.feed_parser = NullCheck.notNull(in_feed_parser);
    this.listener = NullCheck.notNull(in_listener);
    this.running = NullCheck.notNull(in_running);
    this.loans_uri = NullCheck.notNull(in_loans_uri);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.device_activation_listener = NullCheck.notNull(in_device_activation_listener);
  }

  @Override
  public void run() {
    if (this.running.compareAndSet(false, true)) {
      try {
        LOG.debug("running");
        this.sync();
        this.listener.onAccountSyncSuccess();
      } catch (final Throwable x) {
        this.listener.onAccountSyncFailure(
          Option.some(x), NullCheck.notNull(x.getMessage()));
      } finally {
        this.running.set(false);
        LOG.debug("completed");
      }
    } else {
      LOG.debug("sync already in progress, exiting");
    }
  }

  private void sync()
    throws Exception {
//    final URI loans_uri = this.config.getCurrentRootFeedURI().resolve("loans/");

    final OptionType<AccountCredentials> credentials_opt =
      this.accounts_database.accountGetCredentials();
    if (credentials_opt.isSome()) {

      final AccountCredentials credentials =
        ((Some<AccountCredentials>) credentials_opt).get();
      final AccountBarcode barcode = credentials.getBarcode();
      final AccountPIN pin = credentials.getPin();
      final AccountSyncListenerType in_listener = this.listener;
      HTTPAuthType auth =
        new HTTPAuthBasic(barcode.toString(), pin.toString());

      if (credentials.getAuthToken().isSome()) {
        final AccountAuthToken token = ((Some<AccountAuthToken>) credentials.getAuthToken()).get();
        if (token != null) {
          auth = new HTTPAuthOAuth(token.toString());
        }
      }

      final HTTPResultType<InputStream> r =
        this.http.get(Option.some(auth), this.loans_uri, 0L);

      r.matchResult(
        new HTTPResultMatcherType<InputStream, Unit, Exception>() {
          @Override
          public Unit onHTTPError(
            final HTTPResultError<InputStream> e)
            throws Exception {
            final String m = NullCheck.notNull(
              String.format("%s: %d: %s", BooksControllerSyncTask.this.loans_uri, e.getStatus(), e.getMessage()));

            switch (e.getStatus()) {
              case HttpURLConnection.HTTP_UNAUTHORIZED: {
                in_listener.onAccountSyncAuthenticationFailure("Invalid PIN");
                BooksControllerSyncTask.this.accounts_database.accountRemoveCredentials();
                return Unit.unit();
              }
              default: {
                throw new IOException(m);
              }
            }
          }

          @Override
          public Unit onHTTPException(
            final HTTPResultException<InputStream> e)
            throws Exception {
            throw e.getError();
          }

          @Override
          public Unit onHTTPOK(
            final HTTPResultOKType<InputStream> e)
            throws Exception {
            try {
              BooksControllerSyncTask.this.syncFeedEntries(e);
              return Unit.unit();
            } finally {
              e.close();
            }
          }
        });
    }
  }

  private void syncFeedEntries(
    final HTTPResultOKType<InputStream> r_feed)
    throws Exception {
    final BooksStatusCacheType books_status =
      this.books_controller.bookGetStatusCache();

    final OPDSAcquisitionFeed feed =
      this.feed_parser.parse(this.loans_uri, r_feed.getValue());

    if (feed.getLicensor().isSome()) {
      final DRMLicensor licensor = ((Some<DRMLicensor>) feed.getLicensor()).get();

      final OptionType<AccountCredentials> credentials_opt =
        this.accounts_database.accountGetCredentials();

      if (credentials_opt.isSome()) {
        final AccountCredentials credentials = ((Some<AccountCredentials>) credentials_opt).get();

        credentials.setDrmLicensor(feed.getLicensor());
        credentials.setAdobeToken(Option.some(new AccountAdobeToken(licensor.getClientToken())));
        credentials.setAdobeVendor(Option.some(new AdobeVendorID(licensor.getVendor())));

        try {
          this.accounts_database.accountSetCredentials(credentials);
        } catch (final IOException e) {
          LOG.error("could not save credentials: ", e);
        }

        final BooksControllerDeviceActivationTask activation_task =
          new BooksControllerDeviceActivationTask(
            this.adobe_drm,
            credentials,
            this.accounts_database,
            this.books_database,
            this.device_activation_listener);

        activation_task.run();
      }
    }

    /*
     * Obtain the set of books that are on disk already. If any
     * of these books are not in the received feed, then they have
     * expired and should be deleted.
     */

    final Set<BookID> existing = this.books_database.databaseGetBooks();

    /*
     * Handle each book in the received feed.
     */

    final Set<BookID> received = new HashSet<BookID>(64);
    final List<OPDSAcquisitionFeedEntry> entries = feed.getFeedEntries();
    for (final OPDSAcquisitionFeedEntry e : entries) {
      final OPDSAcquisitionFeedEntry e_nn = NullCheck.notNull(e);
      final BookID book_id = BookID.newIDFromEntry(e_nn);

      try {
        received.add(book_id);
        final BookDatabaseEntryType db_e = this.books_database.databaseCreateEntry(book_id, e_nn);
        db_e.entryUpdateAll(e_nn, books_status, this.http);

        this.listener.onAccountSyncBook(book_id);
      } catch (final Throwable x) {
        LOG.error("[{}]: unable to save entry: {}: ", book_id.getShortID(), x);
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
        if (!received.contains(existing_id)) {
          final BookDatabaseEntryType e =
            this.books_database.databaseOpenExistingEntry(existing_id);

          final OPDSAvailabilityType a = e.entryGetFeedData().getAvailability();
          if (a instanceof OPDSAvailabilityRevoked) {
            revoking.add(existing_id);
          }

          e.entryDestroy();
          books_status.booksStatusClearFor(existing_id);
          this.listener.onAccountSyncBookDeleted(existing_id);
        }
      } catch (final Throwable x) {
        LOG.error("[{}]: unable to delete entry: ", existing_id.getShortID(), x);
      }
    }

    /*
     * Try to finish the revocation of any books that require it.
     */

    for (final BookID existing_id : revoking) {
      this.books_controller.bookRevoke(existing_id);
    }
  }
}
