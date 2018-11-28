package org.nypl.simplified.books.core;

import android.content.Context;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default implementation of the {@link BooksType} interface.
 */

public final class BooksController implements BooksType {

  private static final Logger LOG = LoggerFactory.getLogger(BooksController.class);

  private final Context context;
  private BooksStatusCacheType books_status;
  private final DownloaderType downloader;
  private final ConcurrentHashMap<BookID, DownloadType> downloads;
  private final ListeningExecutorService exec;
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
  private Map<Integer, FluentFuture<?>> tasks;
  private final URI loans_uri;

  private BooksController(
    final Context in_context,
    final ListeningExecutorService in_exec,
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

    this.context = NullCheck.notNull(in_context);
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

    this.tasks = new ConcurrentHashMap<>(32);
    this.downloads = new ConcurrentHashMap<>(32);
    this.books_status = BooksStatusCache.newStatusCache();
    this.task_id = new AtomicInteger(0);
    this.feed_parser = this.feed_loader.getOPDSFeedParser();
    this.syncing = new AtomicBoolean(false);
    this.loans_uri = NullCheck.notNull(in_loans_uri);
  }

  /**
   * Construct a new books controller.
   *
   * @param in_context           An Android Context value (typically the application context)
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
    final Context in_context,
    final ListeningExecutorService in_exec,
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
      in_context,
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
            LOG.error("listener raised exception: ", x);
          }
          return Unit.unit();
        }

        @Override
        public Unit some(final Some<AccountCredentials> s) {
          try {
            listener.onAccountIsLoggedIn(s.get());
          } catch (final Throwable x) {
            LOG.error("listener raised exception: ", x);
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
      final OptionType<AccountCredentials> credentials_opt =
        this.accounts_database.accountGetCredentials();
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
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public FluentFuture<Unit> accountLoadBooks(
    final AccountDataLoadListenerType listener,
    final boolean needs_auch) {
    NullCheck.notNull(listener);
    return this.submitTask(
      new BooksControllerDataLoadTask(
        this.book_database,
        this.books_status,
        this.accounts_database,
        listener,
        needs_auch));
  }

  @Override
  public FluentFuture<Unit> accountLogin(
    final AccountCredentials account,
    final AccountLoginListenerType listener) {
    NullCheck.notNull(account);
    NullCheck.notNull(listener);

    LOG.debug("accountLogin");

    final DeviceActivationListenerType device_listener =
      new DeviceActivationListenerType() {
        @Override
        public void onDeviceActivationFailure(final String message) {
          LOG.error("onDeviceActivationFailure: {}", message);
        }

        @Override
        public void onDeviceActivationSuccess() {
          LOG.debug("onDeviceActivationSuccess");
        }
      };

    final FluentFuture<Unit> login_task =
      this.submitTask(
        new BooksControllerLoginTask(
          this,
          this.book_database,
          this.accounts_database,
          this.http,
          this.config,
          account,
          listener,
          device_listener));

    final Function<? super Unit, Unit> func0 = ignored -> {
      try {
        return new BooksControllerSyncTask(
          this,
          this.book_database,
          this.accounts_database,
          this.http,
          this.feed_parser,
          listener,
          this.syncing,
          this.loans_uri,
          this.adobe_drm,
          device_listener,
          true)
          .call();
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    };

    return login_task.transform(func0, this.exec);
  }

  @Override
  public FluentFuture<Unit> accountLogout(
    final AccountCredentials account,
    final AccountLogoutListenerType listener,
    final AccountSyncListenerType sync_listener,
    final DeviceActivationListenerType device_listener) {

    NullCheck.notNull(account, "account");
    NullCheck.notNull(listener, "listener");
    NullCheck.notNull(sync_listener, "sync_listener");
    NullCheck.notNull(device_listener, "device_listener");

    LOG.debug("accountLogout");

    final FluentFuture<Unit> future0 =
      this.accountSync(sync_listener, device_listener, true);

    final Function<? super Unit, Unit> logout = input -> {
      this.stopAllTasks();
      this.books_status.booksStatusClearAll();
      try {
        return new BooksControllerLogoutTask(
          this.book_database,
          this.accounts_database,
          this.adobe_drm,
          listener,
          this.config,
          this.http,
          account)
          .call();
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    };

    return future0.transform(logout, this.exec);
  }

  @Override
  public FluentFuture<Unit> accountSync(
    final AccountSyncListenerType listener,
    final DeviceActivationListenerType device_listener,
    final boolean needs_authentication) {

    NullCheck.notNull(listener, "listener");
    NullCheck.notNull(device_listener, "device_listener");

    LOG.debug("accountSync");

    return this.submitTask(
      new BooksControllerSyncTask(
        this,
        this.book_database,
        this.accounts_database,
        this.http,
        this.feed_parser,
        listener,
        this.syncing,
        this.loans_uri,
        this.adobe_drm,
        device_listener,
        needs_authentication));
  }

  /**
   * @param in_book_id book id to be fulfilled
   */
  @Override
  public void accountActivateDeviceAndFulFillBook(
    final BookID in_book_id,
    final OptionType<DRMLicensor> licensor,
    final DeviceActivationListenerType listener) {

    final OptionType<AccountCredentials> credentials_opt =
      this.accounts_database.accountGetCredentials();

    if (credentials_opt.isSome()) {
      final Some<AccountCredentials> credentials_some =
        (Some<AccountCredentials>) credentials_opt;

      final BooksControllerDeviceActivationTask activation_task =
        new BooksControllerDeviceActivationTask(
          this.adobe_drm,
          credentials_some.get(),
          this.accounts_database,
          listener);

      final BooksControllerFulFillTask fulfill_task =
        new BooksControllerFulFillTask(
          this,
          this.accounts_database,
          this.http,
          this.feed_parser,
          this.syncing,
          this.loans_uri,
          in_book_id);

      this.submitTask(activation_task).transform(input -> fulfill_task.call(), this.exec);
    }
  }


  @Override
  public void accountActivateDevice(final DeviceActivationListenerType device_listener) {

    final OptionType<AccountCredentials> credentials_opt =
      this.accounts_database.accountGetCredentials();

    if (credentials_opt.isSome()) {
      final Some<AccountCredentials> credentials_some =
        (Some<AccountCredentials>) credentials_opt;

      this.submitTask(
        new BooksControllerDeviceActivationTask(
          this.adobe_drm,
          credentials_some.get(),
          this.accounts_database,
          device_listener));
    }
  }

  @Override
  public void fulfillExistingBooks() {

    //fulfill book which were already downloaded when device was active.
    this.submitTask(
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
      final Some<AccountCredentials> credentials_some =
        (Some<AccountCredentials>) credentials_opt;

      this.submitTask(
        new BooksControllerDeviceActivationTask(
          this.adobe_drm,
          credentials_some.get(),
          this.accounts_database,
          device_listener));

      this.fulfillExistingBooks();
    }
  }

  @Override
  public void accountDeActivateDevice() {
    final OptionType<AccountCredentials> credentials_opt =
      this.accounts_database.accountGetCredentials();
    if (credentials_opt.isSome()) {
      final Some<AccountCredentials> credentials_some = (Some<AccountCredentials>) credentials_opt;
      this.submitTask(new BooksControllerDeviceDeActivationTask(
        this.adobe_drm,
        credentials_some.get(),
        this.accounts_database
      ));
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
  public BookDatabaseType bookGetDatabase() {
    return this.book_database;
  }

  @Override
  public BookDatabaseType bookGetWritableDatabase() {
    return this.book_database;
  }

  @Override
  public void bookBorrow(
    final BookID id,
    final OPDSAcquisitionFeedEntry entry,
    final boolean needs_auth) {

    NullCheck.notNull(id);
    NullCheck.notNull(entry);

    LOG.debug("borrow {}", id);

    this.books_status.booksStatusUpdate(new BookStatusRequestingLoan(id));
    this.submitTask(
      new BooksControllerBorrowTask(
        this.book_database,
        this.accounts_database,
        this.books_status,
        this.downloader,
        this.http,
        this.downloads,
        id,
        entry,
        this.feed_loader,
        this.adobe_drm,
        needs_auth));
  }

  @Override
  public void bookDeleteData(
    final BookID id,
    final boolean needs_auth) {
    NullCheck.notNull(id);

    LOG.debug("delete: {}", id);
    this.submitTask(
      new BooksControllerDeleteBookDataTask(
        this.books_status,
        this.book_database,
        id,
        needs_auth));
  }

  @Override
  public void bookDownloadAcknowledge(
    final BookID id) {
    LOG.debug("acknowledging download of book {}", id);

    final OptionType<BookStatusType> status_opt =
      this.books_status.booksStatusGet(id);
    if (status_opt.isSome()) {
      final Some<BookStatusType> status_some =
        (Some<BookStatusType>) status_opt;
      final BookStatusType status = status_some.get();
      LOG.debug(
        "status of book {} is currently {}", id, status);

      if (status instanceof BookStatusDownloadFailed) {
        final OptionType<BookDatabaseEntrySnapshot> snap_opt =
          this.book_database.databaseGetEntrySnapshot(id);
        if (snap_opt.isSome()) {
          final Some<BookDatabaseEntrySnapshot> snap_some =
            (Some<BookDatabaseEntrySnapshot>) snap_opt;
          final BookDatabaseEntrySnapshot snap = snap_some.get();
          this.books_status.booksStatusUpdate(
            BookStatus.Companion.fromSnapshot(
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
      LOG.debug("status of book {} unavailable", id);
    }
  }

  @Override
  public void bookDownloadCancel(
    final BookID id) {
    LOG.debug("download cancel {}", id);

    final DownloadType d = this.downloads.get(id);
    if (d != null) {
      LOG.debug("cancelling download {}", d);
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

    this.submitTask(
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
    this.submitTask(
      new BooksControllerReportTask(
        report_type,
        feed_entry,
        this.http,
        this.accounts_database));
  }

  @Override
  public FluentFuture<Unit> bookRevoke(
    final BookID id,
    final boolean needsAuthentication) {
    NullCheck.notNull(id);

    this.books_status.booksStatusUpdate(new BookStatusRequestingRevoke(id));
    return this.submitTask(
      new BooksControllerRevokeBookTask(
        this.context,
        this.book_database,
        this.accounts_database,
        this.books_status,
        this.feed_loader,
        id,
        this.adobe_drm,
        needsAuthentication));
  }

  @Override
  public void bookGetLatestStatusFromDisk(final BookID id) {
    NullCheck.notNull(id);
    this.submitTask(
      new BooksControllerGetLatestStatusTask(this.book_database, this.books_status, id));
  }

  @Override
  public BooksControllerConfigurationType booksGetConfiguration() {
    return this.config;
  }

  private synchronized void stopAllTasks() {
    final Map<Integer, FluentFuture<?>> t_old = this.tasks;
    this.tasks = new ConcurrentHashMap<>(32);

    final Iterator<FluentFuture<?>> iter = t_old.values().iterator();
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

  protected synchronized <T> FluentFuture<T> submitTask(final Callable<T> task) {
    final int id = this.task_id.incrementAndGet();
    final Callable<T> task_controller = () -> {
      try {
        return task.call();
      } finally {
        this.tasks.remove(id);
      }
    };

    final FluentFuture<T> future = FluentFuture.from(this.exec.submit(task_controller));
    this.tasks.put(id, future);
    return future;
  }

}
