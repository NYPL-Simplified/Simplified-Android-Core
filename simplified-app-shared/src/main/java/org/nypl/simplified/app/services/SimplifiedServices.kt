package org.nypl.simplified.app.services

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Environment
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jnull.NullCheck
import com.squareup.picasso.Picasso
import org.joda.time.LocalDateTime
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountBundledCredentialsType
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore
import org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty
import org.nypl.simplified.accounts.database.AccountsDatabases
import org.nypl.simplified.accounts.json.AccountBundledCredentialsJSON
import org.nypl.simplified.accounts.json.AccountProvidersJSON
import org.nypl.simplified.accounts.registry.AccountProviderRegistry
import org.nypl.simplified.analytics.api.Analytics
import org.nypl.simplified.analytics.api.AnalyticsConfiguration
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.app.AdobeDRMServices
import org.nypl.simplified.app.Bugsnag
import org.nypl.simplified.app.BundledContentResolver
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.SimplifiedNetworkConnectivity
import org.nypl.simplified.app.catalog.CatalogBookBorrowStrings
import org.nypl.simplified.app.catalog.CatalogBookRevokeStrings
import org.nypl.simplified.app.catalog.CatalogCoverBadgeImages
import org.nypl.simplified.app.helpstack.Helpstack
import org.nypl.simplified.app.helpstack.HelpstackType
import org.nypl.simplified.app.images.ImageAccountIconRequestHandler
import org.nypl.simplified.app.login.LoginStringResources
import org.nypl.simplified.app.login.LogoutStringResources
import org.nypl.simplified.app.profiles.ProfileAccountCreationStringResources
import org.nypl.simplified.app.profiles.ProfileAccountDeletionStringResources
import org.nypl.simplified.app.reader.ReaderHTTPMimeMap
import org.nypl.simplified.app.reader.ReaderHTTPServerAAsync
import org.nypl.simplified.app.reader.ReaderHTTPServerType
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoader
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverGenerator
import org.nypl.simplified.books.covers.BookCoverProvider
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCalls
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkService
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.branding.BrandingThemeOverrideServiceType
import org.nypl.simplified.bugsnag.IfBugsnag
import org.nypl.simplified.documents.clock.Clock
import org.nypl.simplified.documents.clock.ClockType
import org.nypl.simplified.documents.store.DocumentStore
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.ProfilesDatabases
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceProviderType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceUsableType
import org.nypl.simplified.tenprint.TenPrintGenerator
import org.nypl.simplified.theme.ThemeControl
import org.nypl.simplified.theme.ThemeValue
import org.nypl.simplified.threads.NamedThreadPools
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ServerSocket
import java.util.ServiceLoader
import java.util.concurrent.ExecutorService

class SimplifiedServices private constructor(
  override val accountProviderRegistry: org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType,
  override val analytics: AnalyticsType,
  override val adobeExecutor: AdobeAdeptExecutorType?,
  override val backgroundExecutor: ListeningScheduledExecutorService,
  override val booksController: BooksControllerType,
  override val bookCovers: BookCoverProviderType,
  override val bookRegistry: BookRegistryReadableType,
  private val brandingThemeOverride: OptionType<ThemeValue>,
  override val documentStore: DocumentStoreType,
  override val feedLoader: FeedLoaderType,
  override val imageLoader: Picasso,
  override val helpStack: HelpstackType?,
  override val http: HTTPType,
  override val networkConnectivity: NetworkConnectivityType,
  override val profilesController: ProfilesControllerType,
  private val profilesDatabase: ProfilesDatabaseType,
  override val readerBookmarkService: ReaderBookmarkServiceUsableType,
  override val readerEPUBLoader: ReaderReadiumEPUBLoaderType,
  override val readerHTTPServer: ReaderHTTPServerType,
  override val screenSize: ScreenSizeInformationType
) : SimplifiedServicesType {

  override val currentTheme: ThemeValue
    get() = this.findCurrentTheme()

  private fun findCurrentTheme(): ThemeValue {
    if (this.brandingThemeOverride.isSome) {
      return (this.brandingThemeOverride as Some<ThemeValue>).get()
    }

    val currentProfileOpt = this.profilesDatabase.currentProfile()
    if (currentProfileOpt.isSome) {
      val currentProfile = (currentProfileOpt as Some<ProfileType>).get()
      val accountCurrent = currentProfile.accountCurrent()
      val theme = ThemeControl.themesByName[accountCurrent.provider.mainColor]
      if (theme != null) {
        return theme
      }
    }

    return ThemeControl.themeFallback
  }


  companion object {

    private val logger = LoggerFactory.getLogger(SimplifiedServices::class.java)

    private data class Directories(
      val directoryBase: File,
      val directoryDownloads: File,
      val directoryDocuments: File,
      val directoryProfiles: File)

    fun create(
      context: Context,
      onProgress: (BootEvent) -> Unit): SimplifiedServicesType {

      fun publishEvent(message: String) {
        this.logger.debug("boot: {}", message)
        onProgress.invoke(BootEvent.BootInProgress(message))
        Thread.sleep(100)
      }

      val assets = context.assets
      val strings = SimplifiedServicesStrings(context.resources)

      publishEvent(strings.bootingDirectories)
      val directories = this.initializeDirectories(context)

      publishEvent(strings.bootingBugsnag)
      this.initBugsnag(context, Bugsnag.getApiToken(assets))

      publishEvent(strings.bootingAdobeDRM)
      val adobeDRM =
        AdobeDRMServices.newAdobeDRMOrNull(
          context, AdobeDRMServices.getPackageOverride(context.resources))

      publishEvent(strings.bootingScreenSize)
      val screenSize = ScreenSizeInformation(this.logger, context.resources)

      publishEvent(strings.bootingHTTP)
      val http = HTTP.newHTTP()

      publishEvent(strings.bootingDownloadService)
      val execDownloader =
        NamedThreadPools.namedThreadPool(4, "downloader", 19)
      val downloader =
        DownloaderHTTP.newDownloader(execDownloader, directories.directoryDownloads, http)

      publishEvent(strings.bootingBookRegistry)
      val bookRegistry = BookRegistry.create()

      publishEvent(strings.bootingBrandingServices)
      val brandingThemeOverride = this.loadOptionalBrandingThemeOverride()

      publishEvent(strings.bootingTenPrint)
      val tenPrint = TenPrintGenerator.newGenerator()

      publishEvent(strings.bootingCoverGenerator)
      val coverGenerator = BookCoverGenerator(tenPrint)

      publishEvent(strings.bootingCoverBadgeProvider)
      val coverBadges = CatalogCoverBadgeImages.create(
        context.resources,
        ThemeControl.resolveColorAttribute(context.theme, R.attr.colorPrimary),
        screenSize)

      publishEvent(strings.bootingCoverProvider)
      val execCovers =
        NamedThreadPools.namedThreadPool(2, "cover", 19)
      val coverProvider =
        BookCoverProvider.newCoverProvider(
          context,
          bookRegistry,
          coverGenerator,
          coverBadges,
          execCovers,
          false,
          false)

      publishEvent(strings.bootingLocalImageLoader)
      val localImageLoader =
        Picasso.Builder(context)
          .indicatorsEnabled(false)
          .loggingEnabled(false)
          .addRequestHandler(ImageAccountIconRequestHandler())
          .build()

      publishEvent(strings.bootingEPUBLoader)
      val execEPUB =
        NamedThreadPools.namedThreadPool(1, "epub", 19)
      val mime =
        ReaderHTTPMimeMap.newMap("application/octet-stream")
      val httpd =
        ReaderHTTPServerAAsync.newServer(assets, mime, this.fetchUnusedHTTPPort())
      val epubLoader =
        ReaderReadiumEPUBLoader.newLoader(context, execEPUB)
      val clock = Clock.get()

      publishEvent(strings.bootingDocumentStore)
      val documents = this.createDocumentStore(
        assets,
        clock,
        http,
        execDownloader,
        directories.directoryDocuments)

      publishEvent(strings.bootingAccountProviders)
      val defaultAccountProvider =
        this.loadDefaultAccountProvider(context)
      val accountProviders =
        AccountProviderRegistry.createFromServiceLoader(context, defaultAccountProvider)

      for (id in accountProviders.accountProviderDescriptions().keys) {
        this.logger.debug("loaded account provider: {}", id)
      }

      publishEvent(strings.bootingBundledCredentials)
      val bundledCredentials = try {
        this.createBundledCredentials(context.assets)
      } catch (e: FileNotFoundException) {
        this.logger.debug("could not initialize bundled credentials: ", e)
        AccountBundledCredentialsEmpty.getInstance()
      } catch (e: IOException) {
        this.logger.debug("could not initialize bundled credentials: ", e)
        throw IllegalStateException("could not initialize bundled credentials", e)
      }

      publishEvent(strings.bootingCredentialStore)
      val accountCredentialsStore = try {
        val privateDirectory = context.getFilesDir()
        val credentials = File(privateDirectory, "credentials.json")
        val credentialsTemp = File(privateDirectory, "credentials.json.tmp")

        this.logger.debug("credentials store path: {}", credentials)
        AccountAuthenticationCredentialsStore.open(credentials, credentialsTemp)
      } catch (e: Exception) {
        this.logger.debug("could not initialize credentials store: ", e)
        throw IllegalStateException("could not initialize credentials store", e)
      }
      this.logger.debug("credentials loaded: {}", accountCredentialsStore.size())

      val accountEvents =
        Observable.create<AccountEvent>()
      val profileEvents =
        Observable.create<ProfileEvent>()
      val readerBookmarkEvents =
        Observable.create<ReaderBookmarkEvent>()

      publishEvent(strings.bootingProfilesDatabase)
      val profilesDatabase = try {
        this.createProfileDatabase(
          context,
          context.resources,
          accountEvents,
          accountProviders,
          bundledCredentials,
          accountCredentialsStore,
          directories.directoryProfiles)
      } catch (e: ProfileDatabaseException) {
        throw IllegalStateException("Could not initialize profile database", e)
      }

      publishEvent(strings.bootingBundledContent)
      val bundledContentResolver = BundledContentResolver.create(context.assets)

      publishEvent(strings.bootingFeedLoader)
      val execCatalogFeeds =
        NamedThreadPools.namedThreadPool(1, "catalog-feed", 19)
      val feedParser =
        this.createFeedParser()
      val feedSearchParser =
        OPDSSearchParser.newParser()
      val feedTransport =
        FeedHTTPTransport.newTransport(http)
      val feedLoader =
        FeedLoader.create(
          execCatalogFeeds,
          feedParser,
          feedSearchParser,
          feedTransport,
          bookRegistry,
          bundledContentResolver)

      publishEvent(strings.bootingAnalytics)
      val analytics = Analytics.create(AnalyticsConfiguration(context, http))

      publishEvent(strings.bootingPatronProfileParsers)
      val patronProfileParsers =
        ServiceLoader.load(PatronUserProfileParsersType::class.java)
          .iterator()
          .next()

      publishEvent(strings.bootingBookController)
      val execBooks =
        NamedThreadPools.namedThreadPool(1, "books", 19)
      val execProfileTimer =
        NamedThreadPools.namedThreadPool(1, "profile-timer", 19)

      val bookController =
        Controller.create(
          accountEvents = accountEvents,
          accountLoginStringResources = LoginStringResources(context.resources),
          accountLogoutStringResources = LogoutStringResources(context.resources),
          accountProviders = accountProviders,
          adobeDrm = adobeDRM,
          analytics = analytics,
          bookBorrowStrings = CatalogBookBorrowStrings(context.resources),
          bookRegistry = bookRegistry,
          bundledContent = bundledContentResolver,
          cacheDirectory = context.cacheDir,
          downloader = downloader,
          exec = execBooks,
          feedLoader = feedLoader,
          feedParser = feedParser,
          http = http,
          patronUserProfileParsers = patronProfileParsers,
          profileAccountCreationStringResources = ProfileAccountCreationStringResources(context.resources),
          profileAccountDeletionStringResources = ProfileAccountDeletionStringResources(context.resources),
          profileEvents = profileEvents,
          profiles = profilesDatabase,
          readerBookmarkEvents = readerBookmarkEvents,
          revokeStrings = CatalogBookRevokeStrings(context.resources),
          timerExecutor = execProfileTimer)

      publishEvent(strings.bootingReaderBookmarkService)
      val readerBookmarksService =
        ReaderBookmarkService.createService(
          ReaderBookmarkServiceProviderType.Requirements(
            { runnable -> NamedThreadPools.namedThreadPoolFactory("reader-bookmarks", 19).newThread(runnable) },
            readerBookmarkEvents,
            ReaderBookmarkHTTPCalls(ObjectMapper(), http),
            bookController
          ))

      /*
       * Log out the current profile after ten minutes, warning one minute before this happens.
       */

      bookController.profileIdleTimer().setWarningIdleSecondsRemaining(60)
      bookController.profileIdleTimer().setMaximumIdleSeconds(10 * 60)

      publishEvent(strings.bootingNetworkConnectivity)
      val networkConnectivity = SimplifiedNetworkConnectivity(context)

      publishEvent(strings.bootingHelpstack)
      val helpstackOpt = Helpstack.get(context, context.assets)
      val helpstack =
        if (helpstackOpt is Some<HelpstackType>) {
          helpstackOpt.get()
        } else {
          null
        }

      try {
        val packageInfo =
          context.getPackageManager().getPackageInfo(context.getPackageName(), 0)

        analytics.publishEvent(
          AnalyticsEvent.ApplicationOpened(
            LocalDateTime.now(),
            null,
            packageInfo.packageName,
            packageInfo.versionName,
            packageInfo.versionCode
          ))
      } catch (e: PackageManager.NameNotFoundException) {
        this.logger.debug("could not get package info for analytics: ", e)
      }

      val execBackground =
        NamedThreadPools.namedThreadPool(1, "background", 19)

      this.logger.debug("boot completed")
      onProgress.invoke(BootEvent.BootCompleted(strings.bootCompleted))
      return SimplifiedServices(
        accountProviderRegistry = accountProviders,
        adobeExecutor = adobeDRM,
        analytics = analytics,
        backgroundExecutor = execBackground,
        brandingThemeOverride = brandingThemeOverride,
        bookCovers = coverProvider,
        bookRegistry = bookRegistry,
        booksController = bookController,
        documentStore = documents,
        feedLoader = feedLoader,
        helpStack = helpstack,
        http = http,
        imageLoader = localImageLoader,
        networkConnectivity = networkConnectivity,
        profilesController = bookController,
        profilesDatabase = profilesDatabase,
        readerBookmarkService = readerBookmarksService,
        readerEPUBLoader = epubLoader,
        readerHTTPServer = httpd,
        screenSize = screenSize
      )
    }

    private fun createFeedParser(): OPDSFeedParserType {
      return OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
        BookFormats.supportedBookMimeTypes()))
    }

    @Throws(ProfileDatabaseException::class)
    private fun createProfileDatabase(
      context: Context,
      resources: Resources,
      accountEvents: ObservableType<AccountEvent>,
      accountProviders: org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType,
      accountBundledCredentials: AccountBundledCredentialsType,
      accountCredentialsStore: AccountAuthenticationCredentialsStoreType,
      directory: File): ProfilesDatabaseType {

      /*
       * If profiles are enabled, then disable the anonymous profile.
       */

      val anonymous = !resources.getBoolean(R.bool.feature_profiles_enabled)

      if (anonymous) {
        this.logger.debug("opening profile database with anonymous profile")
        return ProfilesDatabases.openWithAnonymousProfileEnabled(
          context,
          accountEvents,
          accountProviders,
          accountBundledCredentials,
          accountCredentialsStore,
          AccountsDatabases,
          directory)
      }

      this.logger.debug("opening profile database without anonymous profile")
      return ProfilesDatabases.openWithAnonymousProfileDisabled(
        context,
        accountEvents,
        accountProviders,
        accountBundledCredentials,
        accountCredentialsStore,
        AccountsDatabases,
        directory)
    }

    @Throws(IOException::class)
    private fun loadDefaultAccountProvider(context: Context): AccountProviderType {
      return context.assets.open("account_provider_default.json").use { stream ->
        AccountProvidersJSON.deserializeOneFromStream(stream)
      }
    }

    @Throws(IOException::class)
    private fun createBundledCredentials(assets: AssetManager): AccountBundledCredentialsType {
      return assets.open("account_bundled_credentials.json").use { stream ->
        AccountBundledCredentialsJSON.deserializeFromStream(ObjectMapper(), stream)
      }
    }

    private fun loadOptionalBrandingThemeOverride(): OptionType<ThemeValue> {
      val iter =
        ServiceLoader.load(BrandingThemeOverrideServiceType::class.java)
        .iterator()

      if (iter.hasNext()) {
        val service = iter.next()
        return Option.some(service.overrideTheme())
      }

      return Option.none()
    }

    private fun initializeDirectories(context: Context): Directories {
      this.logger.debug("initializing directories")
      val directoryBase = this.determineDiskDataDirectory(context)
      val directoryDownloads = File(directoryBase, "downloads")
      val directoryDocuments = File(directoryBase, "documents")
      val directoryProfiles = File(directoryBase, "profiles")

      this.logger.debug("directory_base:      {}", directoryBase)
      this.logger.debug("directory_downloads: {}", directoryDownloads)
      this.logger.debug("directory_documents: {}", directoryDocuments)
      this.logger.debug("directory_profiles:  {}", directoryProfiles)

      /*
       * Make sure the required directories exist. There is no sane way to
       * recover if they cannot be created!
       */

      try {
        DirectoryUtilities.directoryCreate(directoryBase)
        DirectoryUtilities.directoryCreate(directoryDownloads)
        DirectoryUtilities.directoryCreate(directoryDocuments)
        DirectoryUtilities.directoryCreate(directoryProfiles)
      } catch (e: IOException) {
        this.logger.error("could not create directories: {}", e.message, e)
        throw IllegalStateException(e)
      }

      return Directories(
        directoryBase = directoryBase,
        directoryDownloads = directoryDownloads,
        directoryDocuments = directoryDocuments,
        directoryProfiles = directoryProfiles)
    }


    private class ScreenSizeInformation(
      private val logger: Logger,
      private val resources: Resources) : ScreenSizeInformationType {

      init {
        val dm = this.resources.displayMetrics
        val dp_height = dm.heightPixels.toFloat() / dm.density
        val dp_width = dm.widthPixels.toFloat() / dm.density
        this.logger.debug("screen ({} x {})", dp_width, dp_height)
        this.logger.debug("screen ({} x {})", dm.widthPixels, dm.heightPixels)
      }

      override fun screenDPToPixels(
        dp: Int): Double {
        val scale = this.resources.displayMetrics.density
        return (dp * scale).toDouble() + 0.5
      }

      override fun screenGetDPI(): Double {
        val metrics = this.resources.displayMetrics
        return metrics.densityDpi.toDouble()
      }

      override fun screenGetHeightPixels(): Int {
        val rr = NullCheck.notNull(this.resources)
        val dm = rr.displayMetrics
        return dm.heightPixels
      }

      override fun screenGetWidthPixels(): Int {
        val rr = NullCheck.notNull(this.resources)
        val dm = rr.displayMetrics
        return dm.widthPixels
      }
    }

    private fun initBugsnag(
      context: Context,
      apiTokenOpt: OptionType<String>) {
      if (apiTokenOpt.isSome) {
        val apiToken = (apiTokenOpt as Some<String>).get()
        this.logger.debug("IfBugsnag: init live interface")
        IfBugsnag.init(context, apiToken)
      } else {
        this.logger.debug("IfBugsnag: init no-op interface")
        IfBugsnag.init()
      }
    }

    private fun determineDiskDataDirectory(context: Context): File {

      /*
       * If external storage is mounted and is on a device that doesn't allow
       * the storage to be removed, use the external storage for data.
       */

      if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
        this.logger.debug("trying external storage")
        if (!Environment.isExternalStorageRemovable()) {
          val result = context.getExternalFilesDir(null)
          this.logger.debug("external storage is not removable, using it ({})", result)
          Preconditions.checkArgument(result!!.isDirectory, "Data directory {} is a directory", result)
          return result
        }
      }

      /*
       * Otherwise, use internal storage.
       */

      val result = context.filesDir
      this.logger.debug("no non-removable external storage, using internal storage ({})", result)
      Preconditions.checkArgument(result.isDirectory, "Data directory {} is a directory", result)
      return result
    }

    private fun fetchUnusedHTTPPort(): Int {
      // Fallback port
      var port: Int? = 8080
      try {
        val socket = ServerSocket(0)
        port = socket.localPort
        socket.close()
      } catch (e: IOException) {
        // Ignore
      }

      this.logger.debug("HTTP server will run on port {}", port)
      return port!!
    }

    /**
     * Create a document store and conditionally enable each of the documents based on the
     * presence of assets.
     */

    private fun createDocumentStore(
      assets: AssetManager,
      clock: ClockType,
      http: HTTPType,
      exec: ExecutorService,
      directory: File): DocumentStoreType {

      val documentsBuilder =
        DocumentStore.newBuilder(clock, http, exec, directory)

      try {
        val stream = assets.open("eula.html")
        documentsBuilder.enableEULA { x -> stream }
      } catch (e: IOException) {
        this.logger.debug("No EULA defined: ", e)
      }

      try {
        val stream = assets.open("software-licenses.html")
        documentsBuilder.enableLicenses { x -> stream }
      } catch (e: IOException) {
        this.logger.debug("No licenses defined: ", e)
      }

      return documentsBuilder.build()
    }
  }

}