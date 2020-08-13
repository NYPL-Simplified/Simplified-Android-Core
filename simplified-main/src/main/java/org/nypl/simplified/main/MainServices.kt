package org.nypl.simplified.main

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Color
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.squareup.picasso.Picasso
import io.reactivex.subjects.PublishSubject
import org.joda.time.LocalDateTime
import org.librarysimplified.services.api.ServiceDirectory
import org.librarysimplified.services.api.ServiceDirectoryType
import org.librarysimplified.services.api.Services
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountBundledCredentialsType
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountProviderFallbackType
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore
import org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty
import org.nypl.simplified.accounts.database.AccountsDatabases
import org.nypl.simplified.accounts.json.AccountBundledCredentialsJSON
import org.nypl.simplified.accounts.registry.AccountProviderRegistry
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceResolutionStrings
import org.nypl.simplified.adobe.extensions.AdobeConfigurationServiceType
import org.nypl.simplified.adobe.extensions.AdobeDRMServices
import org.nypl.simplified.analytics.api.Analytics
import org.nypl.simplified.analytics.api.AnalyticsConfiguration
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.audio.AudioBookFeedbooksSecretServiceType
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.audio.AudioBookManifests
import org.nypl.simplified.books.audio.AudioBookOverdriveSecretServiceType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverBadgeLookupType
import org.nypl.simplified.books.covers.BookCoverGenerator
import org.nypl.simplified.books.covers.BookCoverGeneratorType
import org.nypl.simplified.books.covers.BookCoverProvider
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCalls
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkService
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.boot.api.BootFailureTesting
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.cardcreator.CardCreatorService
import org.nypl.simplified.cardcreator.CardCreatorServiceType
import org.nypl.simplified.clock.Clock
import org.nypl.simplified.clock.ClockType
import org.nypl.simplified.documents.store.DocumentStore
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.downloader.core.DownloaderHTTP
import org.nypl.simplified.downloader.core.DownloaderType
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.networkconnectivity.NetworkConnectivity
import org.nypl.simplified.networkconnectivity.api.NetworkConnectivityType
import org.nypl.simplified.notifications.NotificationsService
import org.nypl.simplified.notifications.NotificationsWrapper
import org.nypl.simplified.opds.auth_document.AuthenticationDocumentParsers
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.patron.PatronUserProfileParsers
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.ProfilesDatabases
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimer
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerConfigurationServiceType
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceProviderType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceUsableType
import org.nypl.simplified.tenprint.TenPrintGenerator
import org.nypl.simplified.tenprint.TenPrintGeneratorType
import org.nypl.simplified.threads.NamedThreadPools
import org.nypl.simplified.ui.branding.BrandingThemeOverrideServiceType
import org.nypl.simplified.ui.catalog.CatalogCoverBadgeImages
import org.nypl.simplified.ui.images.ImageAccountIconRequestHandler
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.profiles.ProfileModificationFragmentServiceType
import org.nypl.simplified.ui.screen.ScreenSizeInformation
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.theme.ThemeControl
import org.nypl.simplified.ui.theme.ThemeServiceType
import org.nypl.simplified.ui.theme.ThemeValue
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.viewer.epub.readium1.ReaderHTTPMimeMap
import org.nypl.simplified.viewer.epub.readium1.ReaderHTTPServerAAsync
import org.nypl.simplified.viewer.epub.readium1.ReaderHTTPServerType
import org.nypl.simplified.viewer.epub.readium1.ReaderReadiumEPUBLoader
import org.nypl.simplified.viewer.epub.readium1.ReaderReadiumEPUBLoaderType
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ServerSocket
import java.util.ServiceLoader
import java.util.concurrent.ExecutorService

internal object MainServices {

  private val logger = LoggerFactory.getLogger(MainServices::class.java)

  /**
   * The current on-disk data version. The entire directory tree the application uses
   * to store data is versioned in order to make it easier to migrate data to new versions
   * at a later date.
   *
   * It's important that this version number begins with a letter: Old version of the software
   * stored individual accounts in numbered directories, and we want to avoid any possibility
   * of migration code thinking that this directory is an old account just because the name
   * happens to parse as an integer.
   */

  const val CURRENT_DATA_VERSION = "v4.0"

  private data class Directories(
    val directoryStorageBaseVersioned: File,
    val directoryStorageDownloads: File,
    val directoryStorageDocuments: File,
    val directoryStorageProfiles: File
  )

  private fun initializeDirectories(context: Context): Directories {
    this.logger.debug("initializing directories")

    val directoryStorageBaseVersioned =
      File(context.filesDir, this.CURRENT_DATA_VERSION)
    val directoryStorageDownloads =
      File(directoryStorageBaseVersioned, "downloads")
    val directoryStorageDocuments =
      File(directoryStorageBaseVersioned, "documents")
    val directoryStorageProfiles =
      File(directoryStorageBaseVersioned, "profiles")

    this.logger.debug("directoryStorageBaseVersioned: {}", directoryStorageBaseVersioned)
    this.logger.debug("directoryStorageDownloads:     {}", directoryStorageDownloads)
    this.logger.debug("directoryStorageDocuments:     {}", directoryStorageDocuments)
    this.logger.debug("directoryStorageProfiles:      {}", directoryStorageProfiles)

    /*
     * Make sure the required directories exist. There is no sane way to
     * recover if they cannot be created!
     */

    val directories =
      listOf<File>(
        directoryStorageBaseVersioned,
        directoryStorageDownloads,
        directoryStorageDocuments,
        directoryStorageProfiles)

    var exception: Exception? = null
    for (directory in directories) {
      try {
        DirectoryUtilities.directoryCreate(directory)
      } catch (e: Exception) {
        if (exception == null) {
          exception = e
        } else {
          exception.addSuppressed(exception)
        }
      }
    }

    if (exception != null) {
      throw exception
    }

    return Directories(
      directoryStorageBaseVersioned = directoryStorageBaseVersioned,
      directoryStorageDownloads = directoryStorageDownloads,
      directoryStorageDocuments = directoryStorageDocuments,
      directoryStorageProfiles = directoryStorageProfiles)
  }

  private class ThemeService(
    private val brandingThemeOverride: OptionType<ThemeValue>
  ) : ThemeServiceType {
    override fun findCurrentTheme(): ThemeValue {
      if (this.brandingThemeOverride.isSome) {
        return (this.brandingThemeOverride as Some<ThemeValue>).get()
      }
      return ThemeControl.themeFallback
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

  private fun findAdobeConfiguration(
    resources: Resources
  ): AdobeConfigurationServiceType {
    return object : AdobeConfigurationServiceType {
      override val packageOverride: String?
        get() = this.run {
          val override = resources.getString(R.string.featureAdobeDRMPackageOverride).trim()
          if (override.isNotEmpty()) {
            return override
          } else {
            return null
          }
        }

      override val debugLogging: Boolean
        get() = resources.getBoolean(R.bool.featureAdobeDRMDebugLogging)

      override val dataDirectoryName: String
        get() = this@MainServices.CURRENT_DATA_VERSION
    }
  }

  private fun createDownloader(
    execDownloader: ListeningScheduledExecutorService,
    directories: Directories,
    http: HTTPType
  ): DownloaderType {
    return DownloaderHTTP.newDownloader(execDownloader, directories.directoryStorageDownloads, http)
  }

  private fun createLocalImageLoader(context: Context): ImageLoaderType {
    val localImageLoader =
      Picasso.Builder(context)
        .indicatorsEnabled(false)
        .loggingEnabled(true)
        .addRequestHandler(ImageAccountIconRequestHandler(context))
        .build()

    return object : ImageLoaderType {
      override val loader: Picasso
        get() = localImageLoader
    }
  }

  private fun createHTTPServer(assets: AssetManager): ReaderHTTPServerType {
    val mime = ReaderHTTPMimeMap.newMap("application/octet-stream")
    return ReaderHTTPServerAAsync.newServer(assets, mime, this.fetchUnusedHTTPPort())
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

  private fun createEPUBLoader(
    context: Context,
    adobeConfiguration: AdobeConfigurationServiceType
  ): ReaderReadiumEPUBLoaderType {
    val execEPUB =
      NamedThreadPools.namedThreadPool(1, "epub", 19)
    return ReaderReadiumEPUBLoader.newLoader(context, adobeConfiguration, execEPUB)
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
    directory: File
  ): DocumentStoreType {

    val documentsBuilder =
      DocumentStore.newBuilder(clock, http, exec, directory)

    try {
      val stream = assets.open("eula.html")
      documentsBuilder.enableEULA { stream }
    } catch (e: IOException) {
      this.logger.debug("No EULA defined: ", e)
    }

    try {
      val stream = assets.open("software-licenses.html")
      documentsBuilder.enableLicenses { stream }
    } catch (e: IOException) {
      this.logger.debug("No licenses defined: ", e)
    }

    return documentsBuilder.build()
  }

  private fun loadDefaultAccountProvider(): AccountProviderType {
    val providers =
      ServiceLoader.load(AccountProviderFallbackType::class.java)
        .map { provider -> provider.get() }
        .toList()

    if (providers.isEmpty()) {
      throw java.lang.IllegalStateException("No fallback account providers available!")
    }
    return providers.first()
  }

  private fun createAccountProviderRegistry(context: Context): AccountProviderRegistryType {
    val defaultAccountProvider =
      this.loadDefaultAccountProvider()
    val accountProviders =
      AccountProviderRegistry.createFromServiceLoader(context, defaultAccountProvider)
    for (id in accountProviders.accountProviderDescriptions().keys) {
      this.logger.debug("loaded account provider: {}", id)
    }
    return accountProviders
  }

  private fun createAccountAuthenticationCredentialsStore(
    directories: Directories
  ): AccountAuthenticationCredentialsStoreType {
    val accountCredentialsStore = try {
      val credentials =
        File(directories.directoryStorageBaseVersioned, "credentials.json")
      val credentialsTemp =
        File(directories.directoryStorageBaseVersioned, "credentials.json.tmp")

      this.logger.debug("credentials store path: {}", credentials)
      AccountAuthenticationCredentialsStore.open(credentials, credentialsTemp)
    } catch (e: Exception) {
      this.logger.debug("could not initialize credentials store: ", e)
      throw IllegalStateException("could not initialize credentials store", e)
    }
    this.logger.debug("credentials loaded: {}", accountCredentialsStore.size())
    return accountCredentialsStore
  }

  @Throws(IOException::class)
  private fun createBundledCredentials(assets: AssetManager): AccountBundledCredentialsType {
    return assets.open("account_bundled_credentials.json").use { stream ->
      AccountBundledCredentialsJSON.deserializeFromStream(ObjectMapper(), stream)
    }
  }

  private fun createAccountBundledCredentials(
    context: Context
  ): AccountBundledCredentialsType {
    return try {
      this.createBundledCredentials(context.assets)
    } catch (e: FileNotFoundException) {
      this.logger.debug("could not initialize bundled credentials: ", e)
      AccountBundledCredentialsEmpty.getInstance()
    } catch (e: IOException) {
      this.logger.debug("could not initialize bundled credentials: ", e)
      throw IllegalStateException("could not initialize bundled credentials", e)
    }
  }

  @Throws(ProfileDatabaseException::class)
  private fun createProfileDatabase(
    context: Context,
    resources: Resources,
    analytics: AnalyticsType,
    accountEvents: PublishSubject<AccountEvent>,
    accountProviders: AccountProviderRegistryType,
    accountBundledCredentials: AccountBundledCredentialsType,
    accountCredentialsStore: AccountAuthenticationCredentialsStoreType,
    directory: File
  ): ProfilesDatabaseType {

    /*
     * If profiles are enabled, then disable the anonymous profile.
     */

    val anonymous = !resources.getBoolean(R.bool.featureProfilesEnabled)
    if (anonymous) {
      this.logger.debug("opening profile database with anonymous profile")
      return ProfilesDatabases.openWithAnonymousProfileEnabled(
        context,
        analytics,
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
      analytics,
      accountEvents,
      accountProviders,
      accountBundledCredentials,
      accountCredentialsStore,
      AccountsDatabases,
      directory)
  }

  private fun createFeedLoader(
    context: Context,
    http: HTTPType,
    opdsFeedParser: OPDSFeedParserType,
    bookRegistry: BookRegistryType,
    bundledContent: BundledContentResolverType
  ): FeedLoaderType {
    val execCatalogFeeds =
      NamedThreadPools.namedThreadPool(1, "catalog-feed", 19)
    val feedSearchParser =
      OPDSSearchParser.newParser()
    val feedTransport =
      FeedHTTPTransport.newTransport(http)
    return FeedLoader.create(
      bookRegistry = bookRegistry,
      bundledContent = bundledContent,
      contentResolver = context.contentResolver,
      exec = execCatalogFeeds,
      parser = opdsFeedParser,
      searchParser = feedSearchParser,
      transport = feedTransport
    )
  }

  private fun createFeedParser(): OPDSFeedParserType {
    return OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
      BookFormats.supportedBookMimeTypes()))
  }

  private fun <T : Any> optionalFromServiceLoader(interfaceType: Class<T>): T? {
    return ServiceLoader.load(interfaceType)
      .toList()
      .firstOrNull()
  }

  private fun createProfileIdleTimer(
    profileEvents: PublishSubject<ProfileEvent>
  ): ProfileIdleTimerType {
    val execProfileTimer =
      NamedThreadPools.namedThreadPool(1, "profile-timer", 19)
    return ProfileIdleTimer.create(execProfileTimer, profileEvents)
  }

  private fun createReaderBookmarksService(
    http: HTTPType,
    bookController: ProfilesControllerType
  ): ReaderBookmarkServiceType {
    val threadFactory: (Runnable) -> Thread = { runnable ->
      NamedThreadPools.namedThreadPoolFactory("reader-bookmarks", 19).newThread(runnable)
    }

    return ReaderBookmarkService.createService(
      ReaderBookmarkServiceProviderType.Requirements(
        threads = threadFactory,
        events = PublishSubject.create(),
        httpCalls = ReaderBookmarkHTTPCalls(ObjectMapper(), http),
        profilesController = bookController
      ))
  }

  private fun createCoverProvider(
    context: Context,
    bookRegistry: BookRegistryReadableType,
    bundledContentResolver: BundledContentResolverType,
    coverGenerator: BookCoverGeneratorType,
    badgeLookup: BookCoverBadgeLookupType
  ): BookCoverProviderType {
    val execCovers =
      NamedThreadPools.namedThreadPool(2, "cover", 19)
    return BookCoverProvider.newCoverProvider(
      context = context,
      bookRegistry = bookRegistry,
      coverGenerator = coverGenerator,
      badgeLookup = badgeLookup,
      bundledContentResolver = bundledContentResolver,
      executor = execCovers,
      debugCacheIndicators = false,
      debugLogging = false)
  }

  private fun createBookCoverBadgeLookup(
    context: Context,
    screenSize: ScreenSizeInformationType
  ): BookCoverBadgeLookupType {
    return CatalogCoverBadgeImages.create(
      context.resources,
      { Color.RED },
      screenSize)
  }

  private fun createNotificationsService(
    context: Context,
    profileEvents: PublishSubject<ProfileEvent>,
    bookRegistry: BookRegistryReadableType
  ): NotificationsService {
    val notificationsThreads =
      NamedThreadPools.namedThreadPoolFactory("notifications", 19)

    return NotificationsService(
      context = context,
      threadFactory = notificationsThreads,
      profileEvents = profileEvents,
      bookRegistry = bookRegistry,
      notificationsWrapper = NotificationsWrapper(context),
      notificationResourcesType = MainNotificationResources(context))
  }

  private fun createCardCreatorService(context: Context): CardCreatorServiceType? {
    return try {
      context.assets.open("cardcreator.conf").use { stream ->
        CardCreatorService.create(stream)
      }
    } catch (e: FileNotFoundException) {
      this.logger.debug("card creator configuration not present: ", e)
      null
    } catch (e: IOException) {
      this.logger.debug("could not initialize card creator: ", e)
      throw IllegalStateException("could not initialize card creator", e)
    }
  }

  private fun publishApplicationStartupEvent(
    context: Context,
    analytics: AnalyticsType
  ) {
    try {
      val packageInfo =
        context.packageManager.getPackageInfo(context.packageName, 0)

      val event =
        AnalyticsEvent.ApplicationOpened(
          LocalDateTime.now(),
          null,
          packageInfo.packageName,
          packageInfo.versionName,
          packageInfo.versionCode)

      analytics.publishEvent(event)
    } catch (e: PackageManager.NameNotFoundException) {
      this.logger.debug("could not get package info for analytics: ", e)
    }
  }

  private fun findBuildConfiguration(): BuildConfigurationServiceType {
    val existing =
      this.optionalFromServiceLoader(BuildConfigurationServiceType::class.java)

    if (existing != null) {
      return existing
    }

    throw IllegalStateException("Missing build configuration service")
  }

  private fun findIdleTimerConfiguration(): ProfileIdleTimerConfigurationServiceType {
    val existing =
      this.optionalFromServiceLoader(ProfileIdleTimerConfigurationServiceType::class.java)

    if (existing != null) {
      return existing
    }

    this.logger.debug("returning fallback idle timer configuration service")
    return object : ProfileIdleTimerConfigurationServiceType {
      override val warningWhenSecondsRemaining: Int
        get() = 60
      override val logOutAfterSeconds: Int
        get() = 10 * 60
    }
  }

  fun setup(
    context: Context,
    onProgress: (BootEvent) -> Unit
  ): ServiceDirectoryType {

    fun publishEvent(message: String) {
      this.logger.debug("boot: {}", message)
      onProgress.invoke(BootEvent.BootInProgress(message))
    }

    BootFailureTesting.failBootProcessForTestingPurposesIfRequested(context)

    val services = ServiceDirectory.builder()
    val assets = context.assets
    val strings = MainServicesStrings(context.resources)

    fun <T : Any> addService(
      message: String,
      interfaceType: Class<T>,
      serviceConstructor: () -> T
    ): T {
      publishEvent(message)
      val service = serviceConstructor.invoke()
      services.addService(interfaceType, service)
      return service
    }

    fun <T : Any> addServiceOptionally(
      message: String,
      interfaceType: Class<T>,
      serviceConstructor: () -> T?
    ): T? {
      publishEvent(message)
      val service = serviceConstructor.invoke()
      if (service != null) {
        services.addService(interfaceType, service)
      }
      return service
    }

    addService(
      message = strings.bootingStrings("login"),
      interfaceType = AccountLoginStringResourcesType::class.java,
      serviceConstructor = { MainLoginStringResources(context.resources) })

    addService(
      message = strings.bootingStrings("logout"),
      interfaceType = AccountLogoutStringResourcesType::class.java,
      serviceConstructor = { MainLogoutStringResources(context.resources) })

    addService(
      message = strings.bootingStrings("resolution"),
      interfaceType = AccountProviderResolutionStringsType::class.java,
      serviceConstructor = {
        AccountProviderSourceResolutionStrings(context.resources)
      })

    addService(
      message = strings.bootingStrings("borrow"),
      interfaceType = BookBorrowStringResourcesType::class.java,
      serviceConstructor = { MainCatalogBookBorrowStrings(context.resources) })

    addService(
      message = strings.bootingStrings("account creation"),
      interfaceType = ProfileAccountCreationStringResourcesType::class.java,
      serviceConstructor = { MainProfileAccountCreationStringResources(context.resources) })

    addService(
      message = strings.bootingStrings("account deletion"),
      interfaceType = ProfileAccountDeletionStringResourcesType::class.java,
      serviceConstructor = { MainProfileAccountDeletionStringResources(context.resources) })

    addService(
      message = strings.bootingStrings("book revocation"),
      interfaceType = BookRevokeStringResourcesType::class.java,
      serviceConstructor = { MainCatalogBookRevokeStrings(context.resources) })

    val clock =
      addService(
        message = strings.bootingClock,
        interfaceType = ClockType::class.java,
        serviceConstructor = { Clock })

    publishEvent(strings.bootingDirectories)
    val directories = this.initializeDirectories(context)

    val adobeConfiguration = this.findAdobeConfiguration(context.resources)
    addServiceOptionally(
      message = strings.bootingAdobeDRM,
      interfaceType = AdobeAdeptExecutorType::class.java,
      serviceConstructor = { AdobeDRMServices.newAdobeDRMOrNull(context, adobeConfiguration) })

    val screenSize =
      addService(
        message = strings.bootingScreenSize,
        interfaceType = ScreenSizeInformationType::class.java,
        serviceConstructor = { ScreenSizeInformation(context.resources) })

    val http =
      addService(
        message = strings.bootingHTTP,
        interfaceType = HTTPType::class.java,
        serviceConstructor = { HTTP.newHTTP() })

    addService(
      message = strings.bootingUIThreadService,
      interfaceType = UIThreadServiceType::class.java,
      serviceConstructor = { MainUIThreadService() })

    val execDownloader =
      NamedThreadPools.namedThreadPool(1, "downloader", 19)

    addService(
      message = strings.bootingDownloadService,
      interfaceType = DownloaderType::class.java,
      serviceConstructor = { this.createDownloader(execDownloader, directories, http) })

    val bookRegistry =
      addService(
        message = strings.bootingBookRegistry,
        interfaceType = BookRegistryType::class.java,
        serviceConstructor = { BookRegistry.create() }
      )
    addService(
      message = strings.bootingBookRegistry,
      interfaceType = BookRegistryReadableType::class.java,
      serviceConstructor = { bookRegistry }
    )

    val tenPrint =
      addService(
        message = strings.bootingTenPrint,
        interfaceType = TenPrintGeneratorType::class.java,
        serviceConstructor = { TenPrintGenerator.newGenerator() })

    val coverGenerator =
      addService(
        message = strings.bootingCoverGenerator,
        interfaceType = BookCoverGeneratorType::class.java,
        serviceConstructor = { BookCoverGenerator(tenPrint) })

    addService(
      message = strings.bootingLocalImageLoader,
      interfaceType = ImageLoaderType::class.java,
      serviceConstructor = { this.createLocalImageLoader(context) })

    addService(
      message = strings.bootingHTTPServer,
      interfaceType = ReaderHTTPServerType::class.java,
      serviceConstructor = { this.createHTTPServer(assets) })

    addService(
      message = strings.bootingEPUBLoader,
      interfaceType = ReaderReadiumEPUBLoaderType::class.java,
      serviceConstructor = { this.createEPUBLoader(context, adobeConfiguration) })

    addService(
      message = strings.bootingBuildConfigurationService,
      interfaceType = BuildConfigurationServiceType::class.java,
      serviceConstructor = { this.findBuildConfiguration() })

    addService(
      message = strings.bootingDocumentStore,
      interfaceType = DocumentStoreType::class.java,
      serviceConstructor = {
        this.createDocumentStore(
          assets = assets,
          clock = clock,
          http = http,
          exec = execDownloader,
          directory = directories.directoryStorageDocuments)
      }
    )

    addServiceOptionally(
      message = strings.bootingProfileModificationFragmentService,
      interfaceType = ProfileModificationFragmentServiceType::class.java,
      serviceConstructor = {
        this.optionalFromServiceLoader(ProfileModificationFragmentServiceType::class.java)
      }
    )

    val accountProviderRegistry =
      addService(
        message = strings.bootingAccountProviders,
        interfaceType = AccountProviderRegistryType::class.java,
        serviceConstructor = { this.createAccountProviderRegistry(context) })

    val accountBundledCredentials =
      addService(
        message = strings.bootingBundledCredentials,
        interfaceType = AccountBundledCredentialsType::class.java,
        serviceConstructor = { this.createAccountBundledCredentials(context) })

    val accountCredentials =
      addService(
        message = strings.bootingCredentialStore,
        interfaceType = AccountAuthenticationCredentialsStoreType::class.java,
        serviceConstructor = { this.createAccountAuthenticationCredentialsStore(directories) })

    val analytics =
      addService(
        message = strings.bootingAnalytics,
        interfaceType = AnalyticsType::class.java,
        serviceConstructor = {
          Analytics.create(AnalyticsConfiguration(
            context = context,
            http = http))
        }
      )

    val accountEvents =
      PublishSubject.create<AccountEvent>()

    val profilesDatabase =
      addService(
        message = strings.bootingProfilesDatabase,
        interfaceType = ProfilesDatabaseType::class.java,
        serviceConstructor = {
          this.createProfileDatabase(
            context,
            context.resources,
            analytics,
            accountEvents,
            accountProviderRegistry,
            accountBundledCredentials,
            accountCredentials,
            directories.directoryStorageProfiles)
        }
      )

    val bundledContent =
      addService(
        message = strings.bootingBundledContent,
        interfaceType = BundledContentResolverType::class.java,
        serviceConstructor = { MainBundledContentResolver.create(context.assets) })

    val opdsFeedParser =
      addService(
        message = strings.bootingFeedParser,
        interfaceType = OPDSFeedParserType::class.java,
        serviceConstructor = {
          this.createFeedParser()
        }
      )

    addService(
      message = strings.bootingFeedLoader,
      interfaceType = FeedLoaderType::class.java,
      serviceConstructor = {
        this.createFeedLoader(
          bookRegistry = bookRegistry,
          bundledContent = bundledContent,
          context = context,
          http = http,
          opdsFeedParser = opdsFeedParser
        )
      }
    )

    addService(
      message = strings.bootingPatronProfileParsers,
      interfaceType = PatronUserProfileParsersType::class.java,
      serviceConstructor = { PatronUserProfileParsers() })

    addService(
      message = strings.bootingAuthenticationDocumentParsers,
      interfaceType = AuthenticationDocumentParsersType::class.java,
      serviceConstructor = { AuthenticationDocumentParsers() })

    val profileEvents = PublishSubject.create<ProfileEvent>()
    addService(
      message = strings.bootingProfileTimer,
      interfaceType = ProfileIdleTimerType::class.java,
      serviceConstructor = { this.createProfileIdleTimer(profileEvents) })

    addService(
      message = strings.bootingAudioBookManifestStrategiesService,
      interfaceType = AudioBookManifestStrategiesType::class.java,
      serviceConstructor = { return@addService AudioBookManifests })

    addServiceOptionally(
      message = strings.bootingFeedbooksSecretService,
      interfaceType = AudioBookFeedbooksSecretServiceType::class.java,
      serviceConstructor = { MainFeedbooksSecretService.createConditionally(context) })

    addServiceOptionally(
      message = strings.bootingOverdriveSecretService,
      interfaceType = AudioBookOverdriveSecretServiceType::class.java,
      serviceConstructor = { MainOverdriveSecretService.createConditionally(context) })

    val bookController = this.run {
      publishEvent(strings.bootingBookController)
      val execBooks =
        NamedThreadPools.namedThreadPool(1, "books", 19)
      val controller =
        Controller.createFromServiceDirectory(
          services = services.build(),
          cacheDirectory = context.cacheDir,
          contentResolver = context.contentResolver,
          profileEvents = profileEvents,
          accountEvents = accountEvents,
          executorService = execBooks
        )
      addService(
        message = strings.bootingBookController,
        interfaceType = ProfilesControllerType::class.java,
        serviceConstructor = { controller })
      addService(
        message = strings.bootingBookController,
        interfaceType = BooksControllerType::class.java,
        serviceConstructor = { controller })
      controller
    }

    publishEvent(strings.bootingReaderBookmarkService)
    val readerBookmarksService =
      this.createReaderBookmarksService(http, bookController)

    addService(
      message = strings.bootingReaderBookmarkService,
      interfaceType = ReaderBookmarkServiceType::class.java,
      serviceConstructor = { readerBookmarksService })
    addService(
      message = strings.bootingReaderBookmarkService,
      interfaceType = ReaderBookmarkServiceUsableType::class.java,
      serviceConstructor = { readerBookmarksService })

    val badgeLookup =
      addService(
        message = strings.bootingCoverBadgeProvider,
        interfaceType = BookCoverBadgeLookupType::class.java,
        serviceConstructor = {
          this.createBookCoverBadgeLookup(
            context = context,
            screenSize = screenSize
          )
        }
      )

    addService(
      message = strings.bootingCoverProvider,
      interfaceType = BookCoverProviderType::class.java,
      serviceConstructor = {
        this.createCoverProvider(
          context = context,
          bookRegistry = bookRegistry,
          bundledContentResolver = bundledContent,
          coverGenerator = coverGenerator,
          badgeLookup = badgeLookup
        )
      }
    )

    addService(
      message = strings.bootingScreenSize,
      interfaceType = ScreenSizeInformationType::class.java,
      serviceConstructor = { ScreenSizeInformation(context.resources) })

    addService(
      message = strings.bootingNetworkConnectivity,
      interfaceType = NetworkConnectivityType::class.java,
      serviceConstructor = { NetworkConnectivity.create(context) })

    publishEvent(strings.bootingBrandingServices)
    val brandingThemeOverride = this.loadOptionalBrandingThemeOverride()

    addService(
      message = strings.bootingThemeService,
      interfaceType = ThemeServiceType::class.java,
      serviceConstructor = {
        ThemeService(
          brandingThemeOverride = brandingThemeOverride
        )
      }
    )

    val idleTimerConfiguration =
      addService(
        message = strings.bootingIdleTimerConfigurationService,
        interfaceType = ProfileIdleTimerConfigurationServiceType::class.java,
        serviceConstructor = { this.findIdleTimerConfiguration() })

    val idleTimer = bookController.profileIdleTimer()
    idleTimer.setWarningIdleSecondsRemaining(idleTimerConfiguration.warningWhenSecondsRemaining)
    idleTimer.setMaximumIdleSeconds(idleTimerConfiguration.logOutAfterSeconds)

    addService(
      message = strings.bootingNotificationsService,
      interfaceType = NotificationsService::class.java,
      serviceConstructor = {
        this.createNotificationsService(context, profileEvents, bookRegistry)
      }
    )

    addServiceOptionally(
      message = strings.bootingAudioBookExtensions,
      interfaceType = AudioBookFeedbooksSecretServiceType::class.java,
      serviceConstructor = {
        this.optionalFromServiceLoader(AudioBookFeedbooksSecretServiceType::class.java)
      }
    )

    addServiceOptionally(
      message = strings.bootingCardCreatorService,
      interfaceType = CardCreatorServiceType::class.java,
      serviceConstructor = { this.createCardCreatorService(context) })

    this.showThreads()

    this.publishApplicationStartupEvent(context, analytics)
    val finalServices = services.build()
    Services.initialize(finalServices)
    this.logger.debug("boot completed")
    onProgress.invoke(BootEvent.BootCompleted(strings.bootCompleted))
    return finalServices
  }

  private fun showThreads() {
    val threadSet =
      Thread.getAllStackTraces()
        .keys
        .sortedBy { thread -> thread.name }

    for (thread in threadSet) {
      this.logger.debug("{}", String.format("[%d] %s", thread.id, thread.name))
    }
  }
}
