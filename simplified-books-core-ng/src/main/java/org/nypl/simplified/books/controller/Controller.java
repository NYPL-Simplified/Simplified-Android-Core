package org.nypl.simplified.books.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;

import org.joda.time.LocalDate;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountEvent;
import org.nypl.simplified.books.accounts.AccountEventCreation;
import org.nypl.simplified.books.accounts.AccountEventDeletion;
import org.nypl.simplified.books.accounts.AccountEventLogin;
import org.nypl.simplified.books.accounts.AccountEventLogout;
import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountProviderCollection;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException;
import org.nypl.simplified.books.analytics.AnalyticsLogger;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.books.bundled_content.BundledContentResolverType;
import org.nypl.simplified.books.feeds.FeedEntryOPDS;
import org.nypl.simplified.books.feeds.FeedLoaderType;
import org.nypl.simplified.books.feeds.FeedWithoutGroups;
import org.nypl.simplified.books.idle_timer.ProfileIdleTimer;
import org.nypl.simplified.books.idle_timer.ProfileIdleTimerType;
import org.nypl.simplified.books.profiles.ProfileAccountSelectEvent;
import org.nypl.simplified.books.profiles.ProfileCreationEvent;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileID;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.books.profiles.ProfileNonexistentAccountProviderException;
import org.nypl.simplified.books.profiles.ProfilePreferences;
import org.nypl.simplified.books.profiles.ProfileReadableType;
import org.nypl.simplified.books.profiles.ProfileSelected;
import org.nypl.simplified.books.profiles.ProfileType;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType.AnonymousProfileEnabled;
import org.nypl.simplified.books.reader.ReaderBookLocation;
import org.nypl.simplified.downloader.core.DownloadType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.observable.Observable;
import org.nypl.simplified.observable.ObservableReadableType;
import org.nypl.simplified.observable.ObservableSubscriptionType;
import org.nypl.simplified.observable.ObservableType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * The default controller implementation.
 */

public final class Controller implements BooksControllerType, ProfilesControllerType, AnalyticsControllerType {

  private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

  private final ListeningExecutorService task_executor;
  private final ProfilesDatabaseType profiles;
  private final AnalyticsLogger analytics_logger;
  private final BookRegistryType book_registry;
  private final ObservableType<ProfileEvent> profile_events;
  private final BundledContentResolverType bundled_content;
  private final FunctionType<Unit, AccountProviderCollection> account_providers;
  private final HTTPType http;
  private final ObservableType<AccountEvent> account_events;
  private final ConcurrentHashMap<BookID, DownloadType> downloads;
  private final OPDSFeedParserType feed_parser;
  private final FeedLoaderType feed_loader;
  private final DownloaderType downloader;
  private final ObservableSubscriptionType<ProfileEvent> profile_event_subscription;
  private final ExecutorService timer_executor;
  private final ProfileIdleTimerType timer;

  private Controller(
    final ExecutorService in_task_executor,
    final HTTPType in_http,
    final OPDSFeedParserType in_feed_parser,
    final FeedLoaderType in_feed_loader,
    final DownloaderType in_downloader,
    final ProfilesDatabaseType in_profiles,
    final AnalyticsLogger in_analytics_logger,
    final BookRegistryType in_book_registry,
    final BundledContentResolverType in_bundled_content,
    final FunctionType<Unit, AccountProviderCollection> in_account_providers,
    final ExecutorService in_timer_executor) {

    this.task_executor =
      MoreExecutors.listeningDecorator(NullCheck.notNull(in_task_executor, "Executor"));
    this.http =
      NullCheck.notNull(in_http, "HTTP");
    this.feed_parser =
      NullCheck.notNull(in_feed_parser, "Feed parser");
    this.feed_loader =
      NullCheck.notNull(in_feed_loader, "Feed loader");
    this.downloader =
      NullCheck.notNull(in_downloader, "Downloader");
    this.profiles =
      NullCheck.notNull(in_profiles, "Profiles");
    this.analytics_logger =
      NullCheck.notNull(in_analytics_logger, "Analytics");
    this.book_registry =
      NullCheck.notNull(in_book_registry, "Book Registry");
    this.bundled_content =
      NullCheck.notNull(in_bundled_content, "Bundled content");
    this.account_providers =
      NullCheck.notNull(in_account_providers, "Account providers");
    this.timer_executor =
      NullCheck.notNull(in_timer_executor, "Timer executor");

    this.downloads = new ConcurrentHashMap<>(32);
    this.profile_events = Observable.create();
    this.account_events = Observable.create();
    this.timer = ProfileIdleTimer.create(this.timer_executor, this.profile_events);
    this.profile_event_subscription = this.profile_events.subscribe(this::onProfileEvent);
  }

  private void onProfileEvent(final ProfileEvent e) {
    if (e instanceof ProfileSelected) {
      onProfileEventSelected((ProfileSelected) e);
      return;
    }
  }

  private void onProfileEventSelected(final ProfileSelected ev) {
    LOG.debug("onProfileEventSelected: {}", ev);

    LOG.debug("clearing the book registry");
    this.book_registry.clear();
    try {
      this.task_executor.execute(
        new ProfileDataLoadTask(this.profiles.currentProfileUnsafe(), this.book_registry));
    } catch (final ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Controller create(
    final ExecutorService in_exec,
    final HTTPType in_http,
    final OPDSFeedParserType in_feed_parser,
    final FeedLoaderType in_feed_loader,
    final DownloaderType in_downloader,
    final ProfilesDatabaseType in_profiles,
    final AnalyticsLogger in_analytics_logger,
    final BookRegistryType in_book_registry,
    final BundledContentResolverType in_bundled_content,
    final FunctionType<Unit, AccountProviderCollection> in_account_providers,
    final ExecutorService in_timer_executor) {

    return new Controller(
      in_exec,
      in_http,
      in_feed_parser,
      in_feed_loader,
      in_downloader,
      in_profiles,
      in_analytics_logger,
      in_book_registry,
      in_bundled_content,
      in_account_providers,
      in_timer_executor);
  }

  @Override
  public void logToAnalytics(String message) {
    if (analytics_logger != null) {
      analytics_logger.logToAnalytics(message);
    }
  }

  @Override
  public void attemptToPushAnalytics(String deviceId) {
    if (analytics_logger != null) {
      analytics_logger.attemptToPushAnalytics(deviceId);
    }
  }

  /**
   * Perform an unchecked (but safe) cast of the given map type. The cast is safe because
   * {@code V <: VB}.
   */

  @SuppressWarnings("unchecked")
  private static <K, VB, V extends VB> SortedMap<K, VB> castMap(final SortedMap<K, V> m) {
    return (SortedMap<K, VB>) m;
  }

  @Override
  public SortedMap<ProfileID, ProfileReadableType> profiles() {
    return castMap(this.profiles.profiles());
  }

  @Override
  public AnonymousProfileEnabled profileAnonymousEnabled() {
    return this.profiles.anonymousProfileEnabled();
  }

  @Override
  public ProfileReadableType profileCurrent() throws ProfileNoneCurrentException {
    return this.profiles.currentProfileUnsafe();
  }

  @Override
  public ObservableReadableType<ProfileEvent> profileEvents() {
    return this.profile_events;
  }

  @Override
  public FluentFuture<ProfileCreationEvent> profileCreate(
    final AccountProvider account_provider,
    final String display_name,
    final String gender,
    final LocalDate date) {

    NullCheck.notNull(account_provider, "Account provider");
    NullCheck.notNull(display_name, "Display name");
    NullCheck.notNull(gender, "Gender");
    NullCheck.notNull(date, "Date");

    return FluentFuture.from(this.task_executor.submit(new ProfileCreationTask(
      this.profiles, this.profile_events, account_provider, display_name, gender, date)));
  }

  @Override
  public FluentFuture<Unit> profileSelect(final ProfileID id) {
    NullCheck.notNull(id, "ID");
    return FluentFuture.from(this.task_executor.submit(
      new ProfileSelectionTask(this.profiles, this.profile_events, id)));
  }

  @Override
  public AccountType profileAccountCurrent() throws ProfileNoneCurrentException {
    final ProfileReadableType profile = this.profileCurrent();
    return profile.accountCurrent();
  }

  @Override
  public FluentFuture<AccountEventLogin> profileAccountCurrentLogin(
    final AccountAuthenticationCredentials credentials) {
    NullCheck.notNull(credentials, "Credentials");
    return FluentFuture.from(this.task_executor.submit(
      new ProfileAccountLoginTask(
        this,
        this.http,
        this.profiles,
        this.account_events,
        ProfileReadableType::accountCurrent,
        credentials)));
  }

  @Override
  public FluentFuture<AccountEventLogin> profileAccountLogin(
    final AccountID account,
    final AccountAuthenticationCredentials credentials) {
    NullCheck.notNull(account, "Account");
    NullCheck.notNull(credentials, "Credentials");
    return FluentFuture.from(
      this.task_executor.submit(new ProfileAccountLoginTask(
        this,
        this.http,
        this.profiles,
        this.account_events,
        p -> p.account(account),
        credentials)));
  }

  @Override
  public FluentFuture<AccountEventCreation> profileAccountCreate(final URI provider) {
    NullCheck.notNull(provider, "Provider");
    return FluentFuture.from(this.task_executor.submit(
      new ProfileAccountCreateTask(
        this.profiles,
        this.account_events,
        this.account_providers,
        provider)));
  }

  @Override
  public FluentFuture<AccountEventDeletion> profileAccountDeleteByProvider(final URI provider) {
    NullCheck.notNull(provider, "Provider");
    return FluentFuture.from(this.task_executor.submit(
      new ProfileAccountDeleteTask(
        this.profiles,
        this.account_events,
        this.profile_events,
        this.account_providers,
        provider)));
  }

  @Override
  public FluentFuture<ProfileAccountSelectEvent> profileAccountSelectByProvider(
    final URI provider) {
    NullCheck.notNull(provider, "Provider");
    return FluentFuture.from(this.task_executor.submit(
      new ProfileAccountSelectionTask(
        this.profiles,
        this.profile_events,
        this.account_providers,
        provider)));
  }

  @Override
  public AccountType profileAccountFindByProvider(
    final URI provider)
    throws ProfileNoneCurrentException, AccountsDatabaseNonexistentException {
    NullCheck.notNull(provider, "Provider");

    final ProfileReadableType profile = this.profileCurrent();
    final AccountType account = profile.accountsByProvider().get(provider);
    if (account == null) {
      throw new AccountsDatabaseNonexistentException("No account with provider: " + provider);
    }
    return account;
  }

  @Override
  public ObservableReadableType<AccountEvent> accountEvents() {
    return this.account_events;
  }

  @Override
  public ImmutableList<AccountProvider> profileCurrentlyUsedAccountProviders()
    throws ProfileNoneCurrentException, ProfileNonexistentAccountProviderException {

    final ArrayList<AccountProvider> accounts = new ArrayList<>();
    final AccountProviderCollection account_providers =
      this.account_providers.call(Unit.unit());
    final ProfileReadableType profile =
      this.profileCurrent();

    for (final AccountType account : profile.accounts().values()) {
      final AccountProvider provider = account.provider();
      if (account_providers.providers().containsKey(provider.id())) {
        final AccountProvider account_provider =
          account_providers.providers().get(provider.id());
        accounts.add(account_provider);
      }
    }

    return ImmutableList.sortedCopyOf(accounts);
  }

  @Override
  public FluentFuture<AccountEventLogout> profileAccountCurrentLogout() {
    return FluentFuture.from(this.task_executor.submit(
      new ProfileAccountLogoutTask(
        this.profiles,
        this.book_registry,
        this.account_events)));
  }

  @Override
  public FluentFuture<AccountEventLogout> profileAccountLogout(AccountID account) {
    throw new UnimplementedCodeException();
  }

  @Override
  public URI profileAccountCurrentCatalogRootURI()
    throws ProfileNoneCurrentException {

    final ProfileType profile = this.profiles.currentProfileUnsafe();
    final AccountType account = profile.accountCurrent();

    return profile.preferences().dateOfBirth().accept(new OptionVisitorType<LocalDate, URI>() {
      @Override
      public URI none(final None<LocalDate> none) {
        return account.provider().catalogURI();
      }

      @Override
      public URI some(final Some<LocalDate> some) {
        final LocalDate now = LocalDate.now();
        final LocalDate then = some.get();
        final int age = now.getYear() - then.getYear();
        return account.provider().catalogURIForAge(age);
      }
    });
  }

  @Override
  public FluentFuture<Unit> profileBookmarkSet(
    final BookID book_id,
    final ReaderBookLocation new_location)
    throws ProfileNoneCurrentException {

    NullCheck.notNull(book_id, "Book ID");
    NullCheck.notNull(new_location, "Location");

    final ProfileType profile = this.profiles.currentProfileUnsafe();
    return FluentFuture.from(this.task_executor.submit(
      new ProfileBookmarkSetTask(
        profile,
        this.profile_events,
        book_id,
        new_location)));
  }

  @Override
  public OptionType<ReaderBookLocation> profileBookmarkGet(final BookID book_id)
    throws ProfileNoneCurrentException {

    NullCheck.notNull(book_id, "Book ID");
    return Option.of(this.profiles.currentProfileUnsafe().preferences()
      .readerBookmarks()
      .bookmarks()
      .get(book_id));
  }

  @Override
  public FluentFuture<Unit> profilePreferencesUpdate(final ProfilePreferences preferences)
    throws ProfileNoneCurrentException {

    NullCheck.notNull(preferences, "Preferences");

    return FluentFuture.from(this.task_executor.submit(
      new ProfilePreferencesUpdateTask(
        this.profile_events,
        this.profiles.currentProfileUnsafe(),
        preferences)));
  }

  @Override
  public FluentFuture<FeedWithoutGroups> profileFeed(
    final ProfileFeedRequest request)
    throws ProfileNoneCurrentException {

    NullCheck.notNull(request, "Request");
    return FluentFuture.from(this.task_executor.submit(
      new ProfileFeedTask(this.book_registry, request)));
  }

  @Override
  public AccountType profileAccountForBook(final BookID id)
    throws ProfileNoneCurrentException, AccountsDatabaseNonexistentException {
    NullCheck.notNull(id, "Book ID");

    final OptionType<BookWithStatus> book_with_status =
      book_registry.book(id);

    if (book_with_status.isSome()) {
      final AccountID account_id =
        ((Some<BookWithStatus>) book_with_status).get().book().account();
      return profileCurrent().account(account_id);
    }

    return profileAccountCurrent();
  }

  @Override
  public ProfileIdleTimerType profileIdleTimer() {
    return this.timer;
  }

  @Override
  public void bookBorrow(
    final AccountType account,
    final BookID id,
    final OPDSAcquisition acquisition,
    final OPDSAcquisitionFeedEntry entry) {

    NullCheck.notNull(account, "Account");
    NullCheck.notNull(id, "Book ID");
    NullCheck.notNull(acquisition, "Acquisition");
    NullCheck.notNull(entry, "Entry");

    this.task_executor.submit(new BookBorrowTask(
      this.downloader,
      this.downloads,
      this.feed_loader,
      this.bundled_content,
      this.book_registry,
      id,
      account,
      acquisition,
      entry));
  }

  @Override
  public void bookBorrowFailedDismiss(
    final AccountType account,
    final BookID id) {

    NullCheck.notNull(account, "Account");
    NullCheck.notNull(id, "Book ID");

    this.task_executor.submit(new BookBorrowFailedDismissTask(
      this.downloader,
      this.downloads,
      account.bookDatabase(),
      this.book_registry,
      id));
  }

  @Override
  public void bookDownloadCancel(
    final AccountType account,
    final BookID id) {

    NullCheck.notNull(account, "Account");
    NullCheck.notNull(id, "Book ID");

    LOG.debug("[{}] download cancel", id.brief());
    final DownloadType d = this.downloads.get(id);
    if (d != null) {
      LOG.debug("[{}] cancelling download {}", d);
      d.cancel();
      this.downloads.remove(id);
    }
  }

  @Override
  public ListenableFuture<Unit> bookReport(
    final FeedEntryOPDS feed_entry,
    final String report_type) {
    throw new UnimplementedCodeException();
  }

  @Override
  public ListenableFuture<Unit> booksSync(final AccountType account) {

    NullCheck.notNull(account, "Account");

    return this.task_executor.submit(new BookSyncTask(
      this,
      account,
      this.book_registry,
      this.http,
      this.feed_parser));
  }

  @Override
  public ListenableFuture<Unit> bookRevoke(
    final AccountType account,
    final BookID book_id) {

    NullCheck.notNull(account, "Account");
    NullCheck.notNull(book_id, "Book ID");

    return this.task_executor.submit(new BookRevokeTask(
      this.book_registry,
      this.feed_loader,
      account,
      book_id));
  }

  @Override
  public ListenableFuture<Unit> bookDelete(
    final AccountType account,
    final BookID book_id) {

    NullCheck.notNull(account, "Account");
    NullCheck.notNull(book_id, "Book ID");

    return this.task_executor.submit(new BookDeleteTask(
      account,
      this.book_registry,
      book_id));
  }

  @Override
  public ListenableFuture<Unit> bookRevokeFailedDismiss(
    final AccountType account,
    final BookID book_id) {

    NullCheck.notNull(account, "Account");
    NullCheck.notNull(book_id, "Book ID");

    return this.task_executor.submit(new BookRevokeFailedDismissTask(
      account.bookDatabase(),
      this.book_registry,
      book_id));
  }
}
