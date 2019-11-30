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



    fun create(
      context: Context,
      onProgress: (BootEvent) -> Unit
    ): ServiceDirectoryType {




      publishEvent(strings.initializingInstabug)
      this.configureInstabug(context)









      publishEvent(strings.bootingBrandingServices)
      val brandingThemeOverride =
        this.loadOptionalBrandingThemeOverride()





















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





















      this.publishApplicationStartupEvent(context, services)

      val execBackground =
        NamedThreadPools.namedThreadPool(1, "background", 19)


      return SimplifiedServices(services)
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
