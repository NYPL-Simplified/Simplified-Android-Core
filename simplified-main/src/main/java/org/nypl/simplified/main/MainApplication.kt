package org.nypl.simplified.main

import android.app.Application
import android.net.http.HttpResponseCache
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
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
import java.io.IOException

class MainApplication : Application() {

  companion object {
    private lateinit var INSTANCE: MainApplication

    @JvmStatic
    val application: MainApplication
      get() = this.INSTANCE
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
    this.configureHttpCache()
    this.configureStrictMode()
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
      for (logger in context.loggerList) {
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
   * Install a global HTTP cache.
   */

  private fun configureHttpCache() {
    if (BuildConfig.DEBUG) {
      val httpCacheDir = File(cacheDir, "http")
      val httpCacheSize = 10 * 1024 * 1024.toLong() // 10 MiB
      try {
        HttpResponseCache.install(httpCacheDir, httpCacheSize)
        this.logger.debug("Installed HTTP cache to {}", httpCacheDir)
      } catch (e: IOException) {
        this.logger.warn("Failed to install HTTP cache!", e)
      }
    }
  }

  /**
   * StrictMode is a developer tool which detects things you might be doing by accident and
   * brings them to your attention so you can fix them.
   *
   * StrictMode is most commonly used to catch accidental disk or network access on the
   * application's main thread, where UI operations are received and animations take place.
   */

  private fun configureStrictMode() {
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(ThreadPolicy.Builder()
        .detectDiskReads()
        .detectDiskWrites()
        .detectNetwork()
        .penaltyLog()
        .build())
      StrictMode.setVmPolicy(VmPolicy.Builder()
        .detectLeakedSqlLiteObjects()
        .detectLeakedClosableObjects()
        .penaltyLog()
        .build())
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
