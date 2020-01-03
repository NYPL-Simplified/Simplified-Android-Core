package org.nypl.simplified.main

import androidx.multidex.MultiDexApplication
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.rolling.RollingFileAppender
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Observable
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.boot.api.BootLoader
import org.nypl.simplified.boot.api.BootProcessType
import org.slf4j.LoggerFactory
import java.io.File

class MainApplication : MultiDexApplication() {

  companion object {
    private lateinit var INSTANCE: MainApplication

    @JvmStatic
    val application: MainApplication
      get() = this.INSTANCE

    /**
     * Checks if Simplified singleton has been initialized already.
     *
     * @return the one singleton instance of Simplified
     * @throws IllegalStateException if Simplified is not yet initialized
     */

    @JvmStatic
    fun checkInitialized(): MainApplication {
      val i = MainApplication()
      if (i == null) {
        throw IllegalStateException("Application is not yet initialized")
      } else {
        return i
      }
    }
  }

  private lateinit var bootFuture: ListenableFuture<ServiceDirectoryType>
  private val logger = LoggerFactory.getLogger(MainApplication::class.java)
  private val boot: BootLoader<ServiceDirectoryType> =
    BootLoader(
      bootProcess = object : BootProcessType<ServiceDirectoryType> {
        override fun execute(onProgress: (BootEvent) -> Unit): ServiceDirectoryType {
          return MainServices.setup(this@MainApplication, onProgress)
        }
      },
      bootStringResources = ::MainServicesStrings)

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
            externalCacheDir?.mkdirs()
            val path = File(externalCacheDir, "log.txt").absolutePath
            (appender as RollingFileAppender<*>).file = path
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

  val servicesBootEvents: Observable<BootEvent>
    get() = this.boot.events

  /**
   * @return A future representing the application's boot process.
   */

  val servicesBooting: ListenableFuture<ServiceDirectoryType>
    get() = this.bootFuture

}
