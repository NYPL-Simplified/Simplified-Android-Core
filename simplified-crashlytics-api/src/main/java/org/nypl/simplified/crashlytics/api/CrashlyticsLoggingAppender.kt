package org.nypl.simplified.crashlytics.api

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.status.InfoStatus
import ch.qos.logback.core.status.WarnStatus
import java.util.ServiceLoader

/**
 * A Logback appender that writes exceptions to a Crashlytics service backend.
 *
 * When instantiated without an explicit [CrashlyticsServiceType] the appender will
 * attempt to load a valid service from the system's [ServiceLoader].
 *
 * The appender will not be started if no valid service can be found.
 */

class CrashlyticsLoggingAppender(
  private var service: CrashlyticsServiceType? = null
) : AppenderBase<ILoggingEvent>() {

  private fun initialize() {
    if (this.service == null) {
      addStatus(
        InfoStatus("Loading ${CrashlyticsServiceType::class.qualifiedName}", this)
      )
      val loader =
        ServiceLoader.load(CrashlyticsServiceType::class.java)

      loader.count().let { count ->
        if (count > 1) {
          addStatus(WarnStatus("More than one [$count] usable service!", this))
        }
      }
      this.service = loader.firstOrNull()
    }
  }

  override fun start() {
    this.initialize()

    if (this.service == null) {
      addStatus(
        WarnStatus("Found no usable ${CrashlyticsServiceType::class.simpleName}!", this)
      )
    } else {
      this.started = true
    }
  }

  override fun append(event: ILoggingEvent) {
    val proxy = event.throwableProxy as? ThrowableProxy
    val throwable = proxy?.throwable

    // Crashlytics isn't really a valid target for a log appender. The service only associates
    // metadata with an exception. Thus, this appender only cares about events with instances
    // of a `Throwable`.

    if (throwable != null) {
      this.service?.log(event.message)
      this.service?.recordException(throwable)
    }
  }
}
