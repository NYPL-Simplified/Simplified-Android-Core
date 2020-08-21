package org.nypl.simplified.main

import android.app.Application
import android.net.http.HttpResponseCache
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import ch.qos.logback.core.util.StatusPrinter
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Observable
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.boot.api.BootLoader
import org.nypl.simplified.boot.api.BootProcessType
import org.nypl.simplified.crashlytics.api.CrashlyticsLoggingAppender
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

  private fun configureLogging() {
    val lc = LoggerFactory.getILoggerFactory() as LoggerContext

    // Reset the default logging context so we can reconfigure it
    lc.stop()

    // Send logger status messages to System.out
    lc.statusManager.apply {
      this.add { StatusPrinter.print(listOf(it)) }
    }

    /** Configure a file based logging appender. */

    fun configureFileAppender(lc: LoggerContext, filename: String): Appender<ILoggingEvent> {
      val encoder =
        PatternLayoutEncoder().apply {
          this.context = lc
          this.pattern = "%d{\"yyyy-MM-dd'T'HH:mm:ss,SSS\"} %level %logger{128} - %msg%n"
          this.start()
        }
      val rollingPolicy =
        TimeBasedRollingPolicy<ILoggingEvent>().apply {
          this.context = lc
          this.fileNamePattern = "$filename.%d.gz"
          this.maxHistory = 7
          this.setTotalSizeCap(FileSize.valueOf("10MB"))
        }
      val fileAppender =
        RollingFileAppender<ILoggingEvent>().apply {
          this.context = lc
          this.encoder = encoder
          this.file = File(cacheDir, filename).absolutePath
          this.name = "FILE"
          this.rollingPolicy = rollingPolicy
        }

      // The `TimeBasedRollingPolicy` needs to have a parent set or it will throw a
      // `NullPointerException` when started.
      rollingPolicy.setParent(fileAppender)
      rollingPolicy.start()

      // The `RollingFileAppender` will refuse to start unless the `TriggeringPolicy`,
      // in this case `TimeBasedRollingPolicy`, has started first.
      fileAppender.start()

      return AsyncAppender().apply {
        this.context = lc
        this.name = "ASYNC_FILE"
        this.addAppender(fileAppender)
        this.start()
      }
    }

    /** Configure the default logcat appender. */

    fun configureLogcatAppender(lc: LoggerContext): Appender<ILoggingEvent> {
      val encoder =
        PatternLayoutEncoder().apply {
          this.context = lc
          this.pattern = "%msg%n"
          this.start()
        }
      return LogcatAppender().apply {
        this.context = lc
        this.name = "LOGCAT"
        this.encoder = encoder
        this.start()
      }
    }

    /** Configure the Crashlytics appender. */

    fun configureCrashlyticsAppender(lc: LoggerContext): Appender<ILoggingEvent> {
      return CrashlyticsLoggingAppender().apply {
        this.context = lc
        this.name = "CRASHLYTICS"
        this.start()
      }
    }

    val root =
      LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    // Set our log level
    root.level = if (BuildConfig.DEBUG) {
      Level.TRACE
    } else {
      Level.DEBUG
    }

    // Add appenders to the root logger
    root.apply {
      addAppender(configureFileAppender(lc, "log.txt"))
      addAppender(configureLogcatAppender(lc))
      addAppender(configureCrashlyticsAppender(lc))
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
