package org.nypl.simplified.app.services

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Environment
import androidx.core.content.ContextCompat
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.instabug.library.Instabug
import com.instabug.library.invocation.InstabugInvocationEvent
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.squareup.picasso.Picasso
import io.reactivex.subjects.PublishSubject
import org.joda.time.LocalDateTime
import org.librarysimplified.services.api.ServiceDirectoryType
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
import org.nypl.simplified.accounts.source.resolution.AccountProviderSourceResolutionStrings
import org.nypl.simplified.analytics.api.Analytics
import org.nypl.simplified.analytics.api.AnalyticsConfiguration
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.app.AdobeDRMServices
import org.nypl.simplified.app.Bugsnag
import org.nypl.simplified.app.BuildConfig
import org.nypl.simplified.app.BundledContentResolver
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.SimplifiedNetworkConnectivity
import org.nypl.simplified.app.catalog.CatalogBookBorrowStrings
import org.nypl.simplified.app.catalog.CatalogBookRevokeStrings
import org.nypl.simplified.app.catalog.CatalogCoverBadgeImages
import org.nypl.simplified.app.images.ImageAccountIconRequestHandler
import org.nypl.simplified.app.images.ImageLoaderType
import org.nypl.simplified.app.login.LoginStringResources
import org.nypl.simplified.app.login.LogoutStringResources
import org.nypl.simplified.app.notifications.NotificationResources
import org.nypl.simplified.app.profiles.ProfileAccountCreationStringResources
import org.nypl.simplified.app.profiles.ProfileAccountDeletionStringResources
import org.nypl.simplified.app.reader.ReaderHTTPMimeMap
import org.nypl.simplified.app.reader.ReaderHTTPServerAAsync
import org.nypl.simplified.app.reader.ReaderHTTPServerType
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoader
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType
import org.nypl.simplified.app.screen.ScreenSizeInformation
import org.nypl.simplified.app.utilities.UIBackgroundExecutor
import org.nypl.simplified.app.utilities.UIBackgroundExecutorType
import org.nypl.simplified.app.utilities.UIThread
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
import org.nypl.simplified.bugsnag.IfBugsnag
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
import org.nypl.simplified.notifications.NotificationsService
import org.nypl.simplified.notifications.NotificationsWrapper
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
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
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimer
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
import org.nypl.simplified.ui.theme.ThemeControl
import org.nypl.simplified.ui.theme.ThemeServiceType
import org.nypl.simplified.ui.theme.ThemeValue
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ServerSocket
import java.util.Properties
import java.util.ServiceLoader
import java.util.concurrent.ExecutorService

class SimplifiedServices private constructor(
  private val serviceDirectory: ServiceDirectoryType
) : ServiceDirectoryType by serviceDirectory {

  private class MutableServiceDirectory : ServiceDirectoryType {

    private val servicesLock = Object()
    private val services = HashMap<Class<*>, List<Any>>()

    override fun <T : Any> optionalServices(serviceClass: Class<T>): List<T> {
      return synchronized(this.servicesLock) {
        this.services[serviceClass] as List<T>? ?: listOf()
      }
    }

    internal fun <T : Any> publishService(
      interfaces: List<Class<T>>,
      service: T
    ) {
      Preconditions.checkArgument(
        interfaces.isNotEmpty(),
        "Must supply at least one interface type")

      logger.debug("registering service {}", service.javaClass.canonicalName)
      synchronized(this.servicesLock) {
        for (inter in interfaces) {
          val existing: List<Any> = this.services[inter] ?: listOf()
          this.services[inter] = existing.plus(service)
        }
      }
    }

    internal fun <T : Any> publishService(
      interfaceType: Class<T>,
      service: T
    ) = this.publishService(listOf(interfaceType), service)
  }

  companion object {

    private val logger = LoggerFactory.getLogger(SimplifiedServices::class.java)

    private fun themeForProfile(profile: OptionType<ProfileType>): ThemeValue {
      if (profile.isSome) {
        val currentProfile = (profile as Some<ProfileType>).get()
        val accountCurrent = currentProfile.accountCurrent()
        val theme = ThemeControl.themesByName[accountCurrent.provider.mainColor]
        if (theme != null) {
          return theme
        }
      }
      return ThemeControl.themeFallback
    }

    private class ThemeService(
      private val profilesDatabase: ProfilesDatabaseType,
      private val brandingThemeOverride: OptionType<ThemeValue>
    ) : ThemeServiceType {
      override fun findCurrentTheme(): ThemeValue {
        if (this.brandingThemeOverride.isSome) {
          return (this.brandingThemeOverride as Some<ThemeValue>).get()
        }
        return themeForProfile(this.profilesDatabase.currentProfile())
      }
    }

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
    const val SIMPLYE = "org.nypl.simplified.simplye"

    private data class Directories(
      val directoryPrivateBaseVersioned: File,
      val directoryStorageBaseVersioned: File,
      val directoryStorageDownloads: File,
      val directoryStorageDocuments: File,
      val directoryStorageProfiles: File
    )

    fun create(
      context: Context,
      onProgress: (BootEvent) -> Unit
    ): ServiceDirectoryType {

      fun publishEvent(message: String) {
        this.logger.debug("boot: {}", message)
        onProgress.invoke(BootEvent.BootInProgress(message))
        Thread.sleep(100)
      }

      BootFailureTesting.failBootProcessForTestingPurposesIfRequested(context)

      val assets = context.assets
      val strings = SimplifiedServicesStrings(context.resources)
      val services = MutableServiceDirectory()

      fun <T : Any> publishMandatoryService(
        message: String,
        interfaceType: Class<T>,
        serviceConstructor: () -> T
      ) {
        publishEvent(message)
        services.publishService(interfaceType, serviceConstructor.invoke())
      }

      fun <T : Any> publishOptionalService(
        message: String,
        interfaceType: Class<T>,
        serviceConstructor: () -> T?
      ) {
        publishEvent(message)
        val service = serviceConstructor.invoke()
        if (service != null) {
          services.publishService(interfaceType, service)
        }
      }

      publishMandatoryService(
        message = strings.bootingStrings("login"),
        interfaceType = AccountLoginStringResourcesType::class.java,
        serviceConstructor = { LoginStringResources(context.resources) })

      publishMandatoryService(
        message = strings.bootingStrings("logout"),
        interfaceType = AccountLogoutStringResourcesType::class.java,
        serviceConstructor = { LogoutStringResources(context.resources) })

      publishMandatoryService(
        message = strings.bootingStrings("resolution"),
        interfaceType = AccountProviderResolutionStringsType::class.java,
        serviceConstructor = { AccountProviderSourceResolutionStrings(context.resources) })

      publishMandatoryService(
        message = strings.bootingStrings("borrow"),
        interfaceType = BookBorrowStringResourcesType::class.java,
        serviceConstructor = { CatalogBookBorrowStrings(context.resources) })

      publishMandatoryService(
        message = strings.bootingStrings("account creation"),
        interfaceType = ProfileAccountCreationStringResourcesType::class.java,
        serviceConstructor = { ProfileAccountCreationStringResources(context.resources) })

      publishMandatoryService(
        message = strings.bootingStrings("account deletion"),
        interfaceType = ProfileAccountDeletionStringResourcesType::class.java,
        serviceConstructor = { ProfileAccountDeletionStringResources(context.resources) })

      publishMandatoryService(
        message = strings.bootingStrings("book revocation"),
        interfaceType = BookRevokeStringResourcesType::class.java,
        serviceConstructor = { CatalogBookRevokeStrings(context.resources) })

      publishMandatoryService(
        message = strings.bootingUIBackgroundExecutor,
        interfaceType = UIBackgroundExecutorType::class.java,
        serviceConstructor = { UIBackgroundExecutor(NamedThreadPools.namedThreadPool(1, "ui_background", 19)) }
      )

      publishMandatoryService(
        message = strings.bootingClock,
        interfaceType = ClockType::class.java,
        serviceConstructor = { Clock })

      publishEvent(strings.bootingDirectories)
      val directories = this.initializeDirectories(context)

      publishEvent(strings.bootingBugsnag)
      this.initBugsnag(context, Bugsnag.getApiToken(assets))

      publishEvent(strings.initializingInstabug)
      this.configureInstabug(context)

      publishOptionalService(
        message = strings.bootingAdobeDRM,
        interfaceType = AdobeAdeptExecutorType::class.java,
        serviceConstructor = {
          AdobeDRMServices.newAdobeDRMOrNull(
            context,
            AdobeDRMServices.getPackageOverride(context.resources))
        }
      )

      publishMandatoryService(
        message = strings.bootingScreenSize,
        interfaceType = ScreenSizeInformationType::class.java,
        serviceConstructor = { ScreenSizeInformation(context.resources) })

      publishMandatoryService(
        message = strings.bootingHTTP,
        interfaceType = HTTPType::class.java,
        serviceConstructor = { HTTP.newHTTP() })

      val execDownloader =
        NamedThreadPools.namedThreadPool(4, "downloader", 19)

      publishMandatoryService(
        message = strings.bootingDownloadService,
        interfaceType = DownloaderType::class.java,
        serviceConstructor = {
          this.createDownloader(execDownloader, directories, services)
        }
      )

      val bookRegistry = BookRegistry.create()
      publishMandatoryService(
        message = strings.bootingBookRegistry,
        interfaceType = BookRegistryType::class.java,
        serviceConstructor = { bookRegistry }
      )
      publishMandatoryService(
        message = strings.bootingBookRegistry,
        interfaceType = BookRegistryReadableType::class.java,
        serviceConstructor = { bookRegistry }
      )

      publishEvent(strings.bootingBrandingServices)
      val brandingThemeOverride =
        this.loadOptionalBrandingThemeOverride()

      publishMandatoryService(
        message = strings.bootingTenPrint,
        interfaceType = TenPrintGeneratorType::class.java,
        serviceConstructor = {
          TenPrintGenerator.newGenerator()
        }
      )

      publishMandatoryService(
        message = strings.bootingCoverGenerator,
        interfaceType = BookCoverGeneratorType::class.java,
        serviceConstructor = {
          BookCoverGenerator(services.requireService(TenPrintGeneratorType::class.java))
        }
      )

      publishMandatoryService(
        message = strings.bootingLocalImageLoader,
        interfaceType = ImageLoaderType::class.java,
        serviceConstructor = {
          this.createLocalImageLoader(context)
        }
      )

      publishMandatoryService(
        message = strings.bootingHTTPServer,
        interfaceType = ReaderHTTPServerType::class.java,
        serviceConstructor = {
          this.createHTTPServer(assets)
        }
      )

      publishMandatoryService(
        message = strings.bootingEPUBLoader,
        interfaceType = ReaderReadiumEPUBLoaderType::class.java,
        serviceConstructor = {
          this.createEPUBLoader(context)
        }
      )

      publishMandatoryService(
        message = strings.bootingDocumentStore,
        interfaceType = DocumentStoreType::class.java,
        serviceConstructor = {
          this.createDocumentStore(
            assets = assets,
            clock = services.requireService(ClockType::class.java),
            http = services.requireService(HTTPType::class.java),
            exec = execDownloader,
            directory = directories.directoryStorageDocuments)
        }
      )

      publishMandatoryService(
        message = strings.bootingAccountProviders,
        interfaceType = AccountProviderRegistryType::class.java,
        serviceConstructor = {
          this.createAccountProviderRegistry(context)
        }
      )

      publishMandatoryService(
        message = strings.bootingBundledCredentials,
        interfaceType = AccountBundledCredentialsType::class.java,
        serviceConstructor = {
          this.createAccountBundledCredentials(context)
        }
      )

      publishMandatoryService(
        message = strings.bootingCredentialStore,
        interfaceType = AccountAuthenticationCredentialsStoreType::class.java,
        serviceConstructor = {
          this.createAccountAuthenticationCredentialsStore(directories)
        }
      )

      val accountEvents = PublishSubject.create<AccountEvent>()
      publishMandatoryService(
        message = strings.bootingProfilesDatabase,
        interfaceType = ProfilesDatabaseType::class.java,
        serviceConstructor = {
          this.createProfileDatabase(
            context,
            context.resources,
            accountEvents,
            services.requireService(AccountProviderRegistryType::class.java),
            services.requireService(AccountBundledCredentialsType::class.java),
            services.requireService(AccountAuthenticationCredentialsStoreType::class.java),
            directories.directoryStorageProfiles)
        }
      )

      publishMandatoryService(
        message = strings.bootingThemeService,
        interfaceType = ThemeServiceType::class.java,
        serviceConstructor = {
          ThemeService(
            profilesDatabase = services.requireService(ProfilesDatabaseType::class.java),
            brandingThemeOverride = brandingThemeOverride
          )
        }
      )

      publishMandatoryService(
        message = strings.bootingBundledContent,
        interfaceType = BundledContentResolverType::class.java,
        serviceConstructor = {
          BundledContentResolver.create(context.assets)
        }
      )

      publishMandatoryService(
        message = strings.bootingFeedParser,
        interfaceType = OPDSFeedParserType::class.java,
        serviceConstructor = {
          this.createFeedParser()
        }
      )

      publishMandatoryService(
        message = strings.bootingFeedLoader,
        interfaceType = FeedLoaderType::class.java,
        serviceConstructor = {
          this.createFeedLoader(services)
        }
      )

      publishMandatoryService(
        message = strings.bootingAnalytics,
        interfaceType = AnalyticsType::class.java,
        serviceConstructor = {
          Analytics.create(AnalyticsConfiguration(
            context = context,
            http = services.requireService(HTTPType::class.java)))
        }
      )

      publishMandatoryService(
        message = strings.bootingPatronProfileParsers,
        interfaceType = PatronUserProfileParsersType::class.java,
        serviceConstructor = {
          this.oneFromServiceLoader(PatronUserProfileParsersType::class.java)
        }
      )

      publishMandatoryService(
        message = strings.bootingAuthenticationDocumentParsers,
        interfaceType = AuthenticationDocumentParsersType::class.java,
        serviceConstructor = {
          this.oneFromServiceLoader(AuthenticationDocumentParsersType::class.java)
        }
      )

      val profileEvents = PublishSubject.create<ProfileEvent>()
      publishMandatoryService(
        message = strings.bootingProfileTimer,
        interfaceType = ProfileIdleTimerType::class.java,
        serviceConstructor = {
          this.createProfileIdleTimer(profileEvents)
        }
      )

      publishEvent(strings.bootingBookController)
      val execBooks =
        NamedThreadPools.namedThreadPool(1, "books", 19)
      val bookController =
        Controller.createFromServiceDirectory(
          services = services,
          cacheDirectory = context.cacheDir,
          profileEvents = profileEvents,
          accountEvents = accountEvents,
          executorService = execBooks
        )

      publishMandatoryService(
        message = strings.bootingBookController,
        interfaceType = ProfilesControllerType::class.java,
        serviceConstructor = { bookController }
      )
      publishMandatoryService(
        message = strings.bootingBookController,
        interfaceType = BooksControllerType::class.java,
        serviceConstructor = { bookController }
      )

      publishEvent(strings.bootingReaderBookmarkService)
      val readerBookmarksService =
        this.createReaderBookmarksService(services, bookController)

      publishMandatoryService(
        message = strings.bootingReaderBookmarkService,
        interfaceType = ReaderBookmarkServiceType::class.java,
        serviceConstructor = { readerBookmarksService }
      )
      publishMandatoryService(
        message = strings.bootingReaderBookmarkService,
        interfaceType = ReaderBookmarkServiceUsableType::class.java,
        serviceConstructor = { readerBookmarksService }
      )

      publishMandatoryService(
        message = strings.bootingCoverBadgeProvider,
        interfaceType = BookCoverBadgeLookupType::class.java,
        serviceConstructor = {
          this.createBookCoverBadgeLookup(context, bookController, services)
        }
      )

      publishMandatoryService(
        message = strings.bootingCoverProvider,
        interfaceType = BookCoverProviderType::class.java,
        serviceConstructor = {
          this.createCoverProvider(context, services)
        }
      )

      publishMandatoryService(
        message = strings.bootingScreenSize,
        interfaceType = ScreenSizeInformationType::class.java,
        serviceConstructor = {
          ScreenSizeInformation(context.resources)
        }
      )

      /*
       * Log out the current profile after ten minutes, warning one minute before this happens.
       */

      bookController.profileIdleTimer().setWarningIdleSecondsRemaining(60)
      bookController.profileIdleTimer().setMaximumIdleSeconds(10 * 60)

      publishMandatoryService(
        message = strings.bootingNotificationsService,
        interfaceType = NotificationsService::class.java,
        serviceConstructor = {
          this.createNotificationsService(context, profileEvents, bookRegistry)
        }
      )

      publishMandatoryService(
        message = strings.bootingNetworkConnectivity,
        interfaceType = NetworkConnectivityType::class.java,
        serviceConstructor = {
          SimplifiedNetworkConnectivity(context)
        }
      )

      this.publishApplicationStartupEvent(context, services)

      val execBackground =
        NamedThreadPools.namedThreadPool(1, "background", 19)

      this.logger.debug("boot completed")
      onProgress.invoke(BootEvent.BootCompleted(strings.bootCompleted))
      return SimplifiedServices(services)
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
        notificationResourcesType = NotificationResources(context))
    }

    private fun createProfileIdleTimer(profileEvents: PublishSubject<ProfileEvent>): ProfileIdleTimerType {
      val execProfileTimer =
        NamedThreadPools.namedThreadPool(1, "profile-timer", 19)
      return ProfileIdleTimer.create(execProfileTimer, profileEvents)
    }

    private fun createReaderBookmarksService(
      services: ServiceDirectoryType,
      bookController: ProfilesControllerType
    ): ReaderBookmarkServiceType {
      val threadFactory: (Runnable) -> Thread = { runnable ->
        NamedThreadPools.namedThreadPoolFactory("reader-bookmarks", 19).newThread(runnable)
      }

      val httpCalls =
        ReaderBookmarkHTTPCalls(ObjectMapper(), services.requireService(HTTPType::class.java))

      return ReaderBookmarkService.createService(
        ReaderBookmarkServiceProviderType.Requirements(
          threads = threadFactory,
          events = PublishSubject.create(),
          httpCalls = httpCalls,
          profilesController = bookController
        ))
    }

    private fun publishApplicationStartupEvent(context: Context, services: ServiceDirectoryType) {
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

        services.requireService(AnalyticsType::class.java).publishEvent(event)
      } catch (e: PackageManager.NameNotFoundException) {
        this.logger.debug("could not get package info for analytics: ", e)
      }
    }

    private fun createCoverProvider(
      context: Context,
      services: ServiceDirectoryType
    ): BookCoverProviderType {
      val execCovers = NamedThreadPools.namedThreadPool(1, "cover", 19)
      return BookCoverProvider.newCoverProvider(
        context = context,
        bookRegistry = services.requireService(BookRegistryReadableType::class.java),
        coverGenerator = services.requireService(BookCoverGeneratorType::class.java),
        badgeLookup = services.requireService(BookCoverBadgeLookupType::class.java),
        executor = execCovers,
        debugCacheIndicators = false,
        debugLogging = true)
    }

    private fun createDownloader(
      execDownloader: ListeningScheduledExecutorService,
      directories: Directories,
      services: ServiceDirectoryType
    ): DownloaderType {
      return DownloaderHTTP.newDownloader(
        execDownloader,
        directories.directoryStorageDownloads,
        services.requireService(HTTPType::class.java))
    }

    private fun createBookCoverBadgeLookup(
      context: Context,
      bookController: Controller,
      services: ServiceDirectoryType
    ): BookCoverBadgeLookupType {
      return CatalogCoverBadgeImages.create(
        context.resources,
        { this.currentThemeColor(context, bookController) },
        services.requireService(ScreenSizeInformationType::class.java))
    }

    private fun createAccountAuthenticationCredentialsStore(directories: Directories): AccountAuthenticationCredentialsStoreType {
      val accountCredentialsStore = try {
        val credentials =
          File(directories.directoryPrivateBaseVersioned, "credentials.json")
        val credentialsTemp =
          File(directories.directoryPrivateBaseVersioned, "credentials.json.tmp")

        this.logger.debug("credentials store path: {}", credentials)
        AccountAuthenticationCredentialsStore.open(credentials, credentialsTemp)
      } catch (e: Exception) {
        this.logger.debug("could not initialize credentials store: ", e)
        throw IllegalStateException("could not initialize credentials store", e)
      }
      this.logger.debug("credentials loaded: {}", accountCredentialsStore.size())
      return accountCredentialsStore
    }

    private fun createAccountBundledCredentials(context: Context): AccountBundledCredentialsType {
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

    private fun createEPUBLoader(context: Context): ReaderReadiumEPUBLoaderType {
      val execEPUB =
        NamedThreadPools.namedThreadPool(1, "epub", 19)
      return ReaderReadiumEPUBLoader.newLoader(context, execEPUB)
    }

    private fun createHTTPServer(assets: AssetManager): ReaderHTTPServerType {
      val mime =
        ReaderHTTPMimeMap.newMap("application/octet-stream")
      return ReaderHTTPServerAAsync.newServer(assets, mime, this.fetchUnusedHTTPPort())
    }

    private fun createLocalImageLoader(context: Context): ImageLoaderType {
      val localImageLoader =
        Picasso.Builder(context)
          .indicatorsEnabled(false)
          .loggingEnabled(true)
          .addRequestHandler(ImageAccountIconRequestHandler())
          .build()

      return object : ImageLoaderType {
        override val loader: Picasso
          get() = localImageLoader
      }
    }

    private fun <T : Any> oneFromServiceLoader(interfaceType: Class<T>): T {
      return ServiceLoader.load(interfaceType)
        .iterator()
        .next()
    }

    private fun createFeedLoader(services: ServiceDirectoryType): FeedLoaderType {
      val execCatalogFeeds =
        NamedThreadPools.namedThreadPool(1, "catalog-feed", 19)
      val feedSearchParser =
        OPDSSearchParser.newParser()
      val feedTransport =
        FeedHTTPTransport.newTransport(services.requireService(HTTPType::class.java))
      return FeedLoader.create(
        exec = execCatalogFeeds,
        parser = services.requireService(OPDSFeedParserType::class.java),
        searchParser = feedSearchParser,
        transport = feedTransport,
        bookRegistry = services.requireService(BookRegistryType::class.java),
        bundledContent = services.requireService(BundledContentResolverType::class.java))
    }

    private fun currentThemeColor(
      context: Context,
      profilesController: ProfilesControllerType
    ): Int {
      val theme = try {
        val profile =
          profilesController.profileCurrent()
        val account =
          profile.accountCurrent()

        ThemeControl.themesByName[account.provider.mainColor] ?: ThemeControl.themeFallback
      } catch (e: Exception) {
        ThemeControl.themeFallback
      }

      val color = ContextCompat.getColor(context, theme.color)
      this.logger.trace("current theme color: 0x{}", String.format("%06x", color))
      return color
    }

    private fun createFeedParser(): OPDSFeedParserType {
      return OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
        BookFormats.supportedBookMimeTypes()))
    }

    @Throws(ProfileDatabaseException::class)
    private fun createProfileDatabase(
      context: Context,
      resources: Resources,
      accountEvents: PublishSubject<AccountEvent>,
      accountProviders: AccountProviderRegistryType,
      accountBundledCredentials: AccountBundledCredentialsType,
      accountCredentialsStore: AccountAuthenticationCredentialsStoreType,
      directory: File
    ): ProfilesDatabaseType {

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

      val directoryPrivateBase =
        context.filesDir
      val directoryPrivateBaseVersioned =
        File(directoryPrivateBase, this.CURRENT_DATA_VERSION)
      val directoryStorageBase =
        this.determineDiskDataDirectory(context)
      val directoryStorageBaseVersioned =
        File(directoryStorageBase, this.CURRENT_DATA_VERSION)
      val directoryStorageDownloads =
        File(directoryStorageBaseVersioned, "downloads")
      val directoryStorageDocuments =
        File(directoryStorageBaseVersioned, "documents")
      val directoryStorageProfiles =
        File(directoryStorageBaseVersioned, "profiles")

      this.logger.debug("directoryPrivateBase:          {}", directoryPrivateBase)
      this.logger.debug("directoryPrivateBaseVersioned: {}", directoryPrivateBaseVersioned)
      this.logger.debug("directoryStorageBase:          {}", directoryStorageBase)
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
          directoryPrivateBase,
          directoryPrivateBaseVersioned,
          directoryStorageBase,
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
        directoryPrivateBaseVersioned = directoryPrivateBaseVersioned,
        directoryStorageBaseVersioned = directoryStorageBaseVersioned,
        directoryStorageDownloads = directoryStorageDownloads,
        directoryStorageDocuments = directoryStorageDocuments,
        directoryStorageProfiles = directoryStorageProfiles)
    }

    private fun initBugsnag(
      context: Context,
      apiTokenOpt: OptionType<String>
    ) {
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
      directory: File
    ): DocumentStoreType {

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

    /**
     * Currently Instabug is available for SimplyE debug builds only
     */
    private fun configureInstabug(context: Context) {
      if (BuildConfig.DEBUG) {
        if (Simplified.application.packageName == this.SIMPLYE) {

          try {
            val inputStream = context.assets.open("instabug.conf")
            val p = Properties()
            p.load(inputStream)
            val instabugToken = p.getProperty("instabug.token")

            if (instabugToken.isNullOrBlank()) {
              throw IOException("instabug token not found!")
            }
            UIThread.runOnUIThread {
              Instabug.Builder(Simplified.application, instabugToken)
                .setInvocationEvents(InstabugInvocationEvent.SHAKE, InstabugInvocationEvent.SCREENSHOT)
                .build()
            }
          } catch (e: java.lang.Exception) {
            this.logger.debug("Error intializing Instabug", android.os.Process.myPid())
            e.printStackTrace()
          }
        }
      }
    }
  }
}
