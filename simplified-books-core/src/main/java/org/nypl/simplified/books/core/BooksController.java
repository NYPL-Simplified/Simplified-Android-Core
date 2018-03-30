package org.nypl.simplified.books.core;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.simplified.downloader.core.DownloadType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.DRMLicensor;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default implementation of the {@link BooksType} interface.
 */

public final class BooksController implements BooksType {
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BooksController.class));
  }

  private BooksStatusCacheType books_status;
  private final DownloaderType downloader;
  private final ConcurrentHashMap<BookID, DownloadType> downloads;
  private final ExecutorService exec;
  private final FeedLoaderType feed_loader;
  private final OPDSFeedParserType feed_parser;
  private final HTTPType http;
  private final AtomicInteger task_id;
  private final OptionType<AdobeAdeptExecutorType> adobe_drm;
  private final DocumentStoreType docs;
  private final BooksControllerConfigurationType config;
  private final BookDatabaseType book_database;
  private final AtomicBoolean syncing;
  private final AccountsDatabaseType accounts_database;
  private Map<Integer, Future<?>> tasks;
  private final URI loans_uri;

  private BooksController(
    final ExecutorService in_exec,
    final FeedLoaderType in_feeds,
    final HTTPType in_http,
    final DownloaderType in_downloader,
    final OPDSJSONSerializerType in_json_serializer,
    final OPDSJSONParserType in_json_parser,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final DocumentStoreType in_docs,
    final BookDatabaseType in_book_database,
    final AccountsDatabaseType in_accounts_database,
    final BooksControllerConfigurationType in_config,
    final URI in_loans_uri) {
    this.exec = NullCheck.notNull(in_exec);
    this.feed_loader = NullCheck.notNull(in_feeds);
    this.http = NullCheck.notNull(in_http);
    this.downloader = NullCheck.notNull(in_downloader);
    NullCheck.notNull(in_json_serializer);
    NullCheck.notNull(in_json_parser);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);
    this.docs = NullCheck.notNull(in_docs);
    this.config = NullCheck.notNull(in_config);
    this.book_database = NullCheck.notNull(in_book_database);
    this.accounts_database = NullCheck.notNull(in_accounts_database);

    this.tasks = new ConcurrentHashMap<Integer, Future<?>>(32);
    this.downloads = new ConcurrentHashMap<BookID, DownloadType>(32);
    this.books_status = BooksStatusCache.newStatusCache();
    this.task_id = new AtomicInteger(0);
    this.feed_parser = this.feed_loader.getOPDSFeedParser();
    this.syncing = new AtomicBoolean(false);
    this.loans_uri = NullCheck.notNull(in_loans_uri);
  }

  /**
   * Construct a new books controller.
   *
   * @param in_exec              An executor
   * @param in_feeds             An asynchronous feed loader
   * @param in_http              An HTTP interface
   * @param in_downloader        A downloader
   * @param in_json_serializer   A JSON serializer
   * @param in_json_parser       A JSON parser
   * @param in_adobe_drm         An Adobe DRM interface, if supported
   * @param in_docs              A document store
   * @param in_book_database     A book database
   * @param in_accounts_database The accounts database
   * @param in_config            Mutable configuration data
   * @param in_loans_url         loans url
   * @return A new books controller
   */

  public static BooksType newBooks(
    final ExecutorService in_exec,
    final FeedLoaderType in_feeds,
    final HTTPType in_http,
    final DownloaderType in_downloader,
    final OPDSJSONSerializerType in_json_serializer,
    final OPDSJSONParserType in_json_parser,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
    final DocumentStoreType in_docs,
    final BookDatabaseType in_book_database,
    final AccountsDatabaseType in_accounts_database,
    final BooksControllerConfigurationType in_config,
    final URI in_loans_url) {
    return new BooksController(
      in_exec,
      in_feeds,
      in_http,
      in_downloader,
      in_json_serializer,
      in_json_parser,
      in_adobe_drm,
      in_docs,
      in_book_database,
      in_accounts_database,
      in_config,
      in_loans_url);
  }

  @Override
  public void accountGetCachedLoginDetails(
    final AccountGetCachedCredentialsListenerType listener) {
    final OptionType<AccountCredentials> p =
      this.accounts_database.accountGetCredentials();

    p.accept(
      new OptionVisitorType<AccountCredentials, Unit>() {
        @Override
        public Unit none(final None<AccountCredentials> n) {
          try {
            listener.onAccountIsNotLoggedIn();
          } catch (final Throwable x) {
            BooksController.LOG.error("listener raised exception: ", x);
          }
          return Unit.unit();
        }

        @Override
        public Unit some(final Some<AccountCredentials> s) {
          try {
            listener.onAccountIsLoggedIn(s.get());
          } catch (final Throwable x) {
            BooksController.LOG.error("listener raised exception: ", x);
          }
          return Unit.unit();
        }
      });
  }

  @Override
  public boolean accountIsLoggedIn() {
    return this.accounts_database.accountGetCredentials().isSome();
  }

  @Override
  public boolean accountIsDeviceActive() {
    if (this.adobe_drm.isSome()) {
      final OptionType<AccountCredentials> credentials_opt = this.accounts_database.accountGetCredentials();
      if (credentials_opt.isSome()) {
        final Some<AccountCredentials> credentials_some = (Some<AccountCredentials>) credentials_opt;

        final OptionType<AdobeUserID> adobe_user_id = credentials_some.get().getAdobeUserID();

        return adobe_user_id.isSome();

      }
    }
    return false;
  }

  @Override
  public void accountRemoveCredentials() {
    try {
      this.accounts_database.accountRemoveCredentials();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void accountLoadBooks(
    final AccountDataLoadListenerType listener,
    final boolean needs_auch) {
    NullCheck.notNull(listener);
    this.submitRunnable(
      new BooksControllerDataLoadTask(
        this.book_database,
        this.books_status,
        this.accounts_database,
        listener,
        needs_auch));
  }

  @Override
  public void accountLogin(
    final AccountCredentials account,
    final AccountLoginListenerType listener) {
    NullCheck.notNull(account);
    NullCheck.notNull(listener);

    final DeviceActivationListenerType device_listener = new DeviceActivationListenerType() {
      @Override
      public void onDeviceActivationFailure(final String message) {
        // do nothing
      }

      @Override
      public void onDeviceActivationSuccess() {
        // do nothing
      }
    };

    this.submitRunnable(
      new BooksControllerLoginTask(
        this,
        this.book_database,
        this.accounts_database,
        this.http,
        this.config,
        this.adobe_drm,
        this.feed_parser,
        account,
        listener,
        this.syncing,
        device_listener));
  }

  @Override
  public void accountLogout(
    final AccountCredentials account,
    final AccountLogoutListenerType listener,
    final AccountSyncListenerType sync_listener,
    final DeviceActivationListenerType device_listener) {
    NullCheck.notNull(listener);

    synchronized (this) {
      this.accountSync(sync_listener, device_listener);
      this.stopAllTasks();
      this.books_status.booksStatusClearAll();
      this.submitRunnable(
        new BooksControllerLogoutTask(
          this.book_database,
          this.accounts_database,
          this.adobe_drm,
          listener,
          this.config,
          this.http,
          account));
    }
  }

  @Override
  public void accountSync(
    final AccountSyncListenerType listener,
    final DeviceActivationListenerType device_listener) {
    NullCheck.notNull(listener);
    this.submitRunnable(
      new BooksControllerSyncTask(
        this,
        this.book_database,
        this.accounts_database,
        this.config,
        this.http,
        this.feed_parser,
        listener,
        this.syncing,
        this.loans_uri,
        this.adobe_drm,
        device_listener));
  }


  /**
   * @param in_book_id book id to be fulfilled
   */
  @Override
  public void accountActivateDeviceAndFulFillBook(final BookID in_book_id,
                                                  final OptionType<DRMLicensor> licensor,
                                                  final DeviceActivationListenerType listener) {

    final OptionType<AccountCredentials> credentials_opt = this.accounts_database.accountGetCredentials();
    if (credentials_opt.isSome()) {
      final Some<AccountCredentials> credentials_some = (Some<AccountCredentials>) credentials_opt;
      final BooksControllerDeviceActivationTask activation_task = new BooksControllerDeviceActivationTask(
        this.adobe_drm,
        credentials_some.get(),
        this.accounts_database,
        this.book_database,
        listener);
      this.submitRunnable(activation_task);


      //fulfill book which were already downloaded when device was active.
      this.submitRunnable(
        new BooksControllerFulFillTask(
          this,
          this.accounts_database,
          this.http,
          this.feed_parser,
          this.syncing,
          this.loans_uri,
          in_book_id));


    }

  }


  @Override
  public void accountActivateDevice(final DeviceActivationListenerType device_listener) {

    final OptionType<AccountCredentials> credentials_opt = this.accounts_database.accountGetCredentials();
    if (credentials_opt.isSome()) {
      final Some<AccountCredentials> credentials_some = (Some<AccountCredentials>) credentials_opt;
      final BooksControllerDeviceActivationTask activation_task = new BooksControllerDeviceActivationTask(
        this.adobe_drm,
        credentials_some.get(),
        this.accounts_database,
        this.book_database,
        device_listener);
      this.submitRunnable(activation_task);

    }

  }

  @Override
  public void fulfillExistingBooks() {

      //fulfill book which were already downloaded when device was active.
      this.submitRunnable(
        new BooksControllerFulFillTask(
          this,
          this.accounts_database,
          this.http,
          this.feed_parser,
          this.syncing,
          this.loans_uri));

  }

  @Override
  public void accountActivateDeviceAndFulfillBooks(
    final OptionType<DRMLicensor> licensor,
    final DeviceActivationListenerType device_listener) {
    final OptionType<AccountCredentials> credentials_opt = this.accounts_database.accountGetCredentials();
    if (credentials_opt.isSome()) {
      final Some<AccountCredentials> credentials_some = (Some<AccountCredentials>) credentials_opt;
      final BooksControllerDeviceActivationTask activation_task = new BooksControllerDeviceActivationTask(
        this.adobe_drm,
        credentials_some.get(),
        this.accounts_database,
        this.book_database,
        device_listener);
      this.submitRunnable(activation_task);

      //fulfill book which were already downloaded when device was active.
      this.fulfillExistingBooks();

    }
  }

  @Override
  public void accountDeActivateDevice() {
    final OptionType<AccountCredentials> credentials_opt = this.accounts_database.accountGetCredentials();
    if (credentials_opt.isSome()) {
      final Some<AccountCredentials> credentials_some = (Some<AccountCredentials>) credentials_opt;
      final BooksControllerDeviceDeActivationTask deactivation_task = new BooksControllerDeviceDeActivationTask(
        this.adobe_drm,
        credentials_some.get(),
        this.accounts_database,
        this.book_database
      );
      this.submitRunnable(deactivation_task);
    }
  }

  @Override
  public BooksStatusCacheType bookGetStatusCache() {
    return this.books_status;
  }

  @Override
  public void destroyBookStatusCache() {
    this.books_status = BooksStatusCache.newStatusCache();
  }

  @Override
  public BookDatabaseReadableType bookGetDatabase() {
    return this.book_database;
  }

  @Override public BookDatabaseType bookGetWritableDatabase() {
    return this.book_database;
  }

  @Override
  public void bookBorrow(
    final BookID id,
    final OPDSAcquisition acq,
    final OPDSAcquisitionFeedEntry eo,
    final boolean needs_auth) {
    NullCheck.notNull(id);
    NullCheck.notNull(acq);
    NullCheck.notNull(eo);

    BooksController.LOG.debug("borrow {}", id);

    this.books_status.booksStatusUpdate(new BookStatusRequestingLoan(id));
    this.submitRunnable(
      new BooksControllerBorrowTask(
        this.book_database,
        this.accounts_database,
        this.books_status,
        this.downloader,
        this.http,
        this.downloads,
        id,
        acq,
        eo,
        this.feed_loader,
        this.adobe_drm,
        needs_auth
        ));
  }

  @Override
  public void bookDeleteData(
    final BookID id,
    final boolean needs_auth) {
    NullCheck.notNull(id);

    BooksController.LOG.debug("delete: {}", id);
    this.submitRunnable(
      new BooksControllerDeleteBookDataTask(
        this.books_status, this.book_database, id, needs_auth));
  }

  @Override
  public void bookDownloadAcknowledge(
    final BookID id) {
    BooksController.LOG.debug("acknowledging download of book {}", id);

    final OptionType<BookStatusType> status_opt =
      this.books_status.booksStatusGet(id);
    if (status_opt.isSome()) {
      final Some<BookStatusType> status_some =
        (Some<BookStatusType>) status_opt;
      final BookStatusType status = status_some.get();
      BooksController.LOG.debug(
        "status of book {} is currently {}", id, status);

      if (status instanceof BookStatusDownloadFailed) {
        final OptionType<BookDatabaseEntrySnapshot> snap_opt =
          this.book_database.databaseGetEntrySnapshot(id);
        if (snap_opt.isSome()) {
          final Some<BookDatabaseEntrySnapshot> snap_some =
            (Some<BookDatabaseEntrySnapshot>) snap_opt;
          final BookDatabaseEntrySnapshot snap = snap_some.get();
          this.books_status.booksStatusUpdate(
            BookStatus.fromSnapshot(
              id, snap));
        } else {

          /**
           * A snapshot *must* exist for a book that has had a download
           * attempt.
           */

          throw new UnreachableCodeException();
        }
      }
    } else {
      BooksController.LOG.debug("status of book {} unavailable", id);
    }
  }

  @Override
  public void bookDownloadCancel(
    final BookID id) {
    BooksController.LOG.debug("download cancel {}", id);

    final DownloadType d = this.downloads.get(id);
    if (d != null) {
      BooksController.LOG.debug("cancelling download {}", d);
      d.cancel();
      this.downloads.remove(id);
    }
  }

  @Override
  public void booksGetFeed(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final FeedFacetPseudo.FacetType in_facet_active,
    final String in_facet_group,
    final FeedFacetPseudoTitleProviderType in_facet_titles,
    final OptionType<String> in_search,
    final BooksFeedSelection in_selection,
    final BookFeedListenerType in_listener) {
    NullCheck.notNull(in_uri);
    NullCheck.notNull(in_id);
    NullCheck.notNull(in_updated);
    NullCheck.notNull(in_title);
    NullCheck.notNull(in_facet_active);
    NullCheck.notNull(in_facet_group);
    NullCheck.notNull(in_facet_titles);
    NullCheck.notNull(in_search);
    NullCheck.notNull(in_selection);
    NullCheck.notNull(in_listener);

    this.submitRunnable(
      new BooksControllerFeedTask(
        this.book_database,
        in_uri,
        in_id,
        in_updated,
        in_title,
        in_facet_active,
        in_facet_group,
        in_facet_titles,
        in_search,
        in_selection,
        in_listener));
  }

  @Override
  public void bookReport(
    final FeedEntryOPDS feed_entry,
    final String report_type) {
    NullCheck.notNull(feed_entry);
    NullCheck.notNull(report_type);
    this.submitRunnable(
      new BooksControllerReportTask(
        report_type,
        feed_entry,
        this.http,
        this.accounts_database));
  }

  @Override
  public void bookRevoke(final BookID id) {
    NullCheck.notNull(id);

    this.books_status.booksStatusUpdate(new BookStatusRequestingRevoke(id));
    this.submitRunnable(
      new BooksControllerRevokeBookTask(
        this.book_database,
        this.accounts_database,
        this.books_status,
        this.feed_loader,
        id,
        this.adobe_drm));
  }

  @Override
  public void bookGetLatestStatusFromDisk(final BookID id) {
    NullCheck.notNull(id);
    this.submitRunnable(
      new BooksControllerGetLatestStatusTask(
        this.book_database, this.books_status, id));
  }

  @Override
  public BooksControllerConfigurationType booksGetConfiguration() {
    return this.config;
  }

  private synchronized void stopAllTasks() {
    final Map<Integer, Future<?>> t_old = this.tasks;
    this.tasks = new ConcurrentHashMap<Integer, Future<?>>(32);

    final Iterator<Future<?>> iter = t_old.values().iterator();
    while (iter.hasNext()) {
      try {
        final Future<?> f = iter.next();
        f.cancel(true);
        iter.remove();
      } catch (final Throwable x) {
        // Ignore
      }
    }
  }

  protected synchronized void submitRunnable(
    final Runnable r) {
    final int id = this.task_id.incrementAndGet();
    final Runnable rb = new Runnable() {
      @Override
      public void run() {
        try {
          r.run();
        } finally {
          BooksController.this.tasks.remove(id);
        }
      }
    };
    this.tasks.put(id, this.exec.submit(rb));
  }
}
