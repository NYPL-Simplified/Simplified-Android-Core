package org.nypl.simplified.app

import android.app.Activity
import android.content.Context
import androidx.multidex.MultiDexApplication
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.rolling.RollingFileAppender
import com.google.common.util.concurrent.ListenableFuture
import org.nypl.simplified.app.services.SimplifiedServices
import org.nypl.simplified.app.services.SimplifiedServicesStrings
import org.nypl.simplified.app.services.SimplifiedServicesType
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.boot.api.BootLoader
import org.nypl.simplified.boot.api.BootProcessType
import org.nypl.simplified.cardcreator.CardCreator
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.prefs.Prefs
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Global application state.
 */

class Simplified : MultiDexApplication() {

  companion object {
    private lateinit var INSTANCE: Simplified

    @JvmStatic
    val application: Simplified
      get() = this.INSTANCE

    @JvmStatic
    fun getServices(): SimplifiedServicesType =
            this.application.services()

    /**
     * Checks if Simplified singleton has been initialized already.
     *
     * @return the one singleton instance of Simplified
     * @throws IllegalStateException if Simplified is not yet initialized
     */
    @JvmStatic
    fun checkInitialized(): Simplified {
      val i = Simplified()
      if (i == null) {
        throw IllegalStateException("Application is not yet initialized")
      } else {
        return i
      }
    }

    /**
     * @return cardcreator
     */
    @JvmStatic
    fun getCardCreator(context: Context): CardCreator {
      val i = checkInitialized()
      return i.getActualCardCreator(context)
    }

    /**
     * @return Shared Preferences
     */
    @JvmStatic
    fun getSharedPrefs(activity: Activity): Prefs {
      val i = checkInitialized()
      return Prefs(activity)
    }
  }

  private lateinit var bootFuture: ListenableFuture<SimplifiedServicesType>
  private val logger = LoggerFactory.getLogger(Simplified::class.java)
  private val boot: BootLoader<SimplifiedServicesType> =
          BootLoader(
                  bootProcess = object : BootProcessType<SimplifiedServicesType> {
                    override fun execute(onProgress: (BootEvent) -> Unit): SimplifiedServicesType {
                      return SimplifiedServices.create(this@Simplified, onProgress)
                    }
                  },
                  bootStringResources = ::SimplifiedServicesStrings)

  override fun onCreate() {
    super.onCreate()

    this.configureLogging()
    this.logger.debug("starting app: pid {}", android.os.Process.myPid())
    this.bootFuture = this.boot.start(this)
    INSTANCE = this
  }

  /**
   * We apparently can't rely on the paths configured in logback.xml to actually work
   * correctly across different devices. This bit of code tries to configure the path
   * of the log file directly.
   */

  private fun configureLogging() {
    try {
      val context = LoggerFactory.getILoggerFactory() as LoggerContext
      for (logger in context.getLoggerList()) {
        val index = logger.iteratorForAppenders()
        while (index.hasNext()) {
          val appender = index.next()
          if (appender is RollingFileAppender<*>) {
            (appender as RollingFileAppender<*>).setFile(File(externalCacheDir, "log.txt").absolutePath)
            appender.start()
          }
        }
      }
      this.logger.debug("logging is configured")
    } catch (e: Exception) {
      this.logger.error("could not configure logging: ", e)
    }
  }

  /**
   * An observable value that publishes events as the application is booting.
   */

  val servicesBootEvents: ObservableReadableType<BootEvent>
    get() = this.boot.events

  /**
   * @return A future representing the application's boot process.
   */

  val servicesBooting: ListenableFuture<SimplifiedServicesType>
    get() = this.bootFuture

  /**
   * Retrieve the application's services. Note that this method will block until the application
   * is fully booted.
   *
   * @return The application's services.
   */

  fun services(): SimplifiedServicesType =
          this.bootFuture.get(30L, TimeUnit.SECONDS)

  @Synchronized
  private fun getActualCardCreator(context: Context): CardCreator {
      return CardCreator(context.assets, "prod", context.resources)
  }
}
