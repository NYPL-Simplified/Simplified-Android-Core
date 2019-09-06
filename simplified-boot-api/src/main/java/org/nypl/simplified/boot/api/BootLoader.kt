package org.nypl.simplified.boot.api

import android.content.Context
import android.content.res.Resources
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * A class for starting up a set of services on a background thread and publishing
 * events as the services are started.
 */

class BootLoader<T>(

  /**
   * The string resources used by the boot process.
   */

  private val bootStringResources: (Resources) -> BootStringResourcesType,

  /**
   * A function that sets up services.
   */

  private val bootProcess: BootProcessType<T>) : BootLoaderType<T> {

  private val logger = LoggerFactory.getLogger(BootLoader::class.java)

  private val executor =
    MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(1) { runnable ->
        val thread = Thread(runnable)
        thread.name = "simplified-boot-${thread.id}"
        thread
      })

  private val eventsActual = Observable.create<BootEvent>()
  private val bootLock: Any = Any()
  private var boot: FluentFuture<T>? = null

  override val events: ObservableReadableType<BootEvent> =
    this.eventsActual

  override fun start(context: Context): FluentFuture<T> {
    return synchronized(this.bootLock) {
      if (this.boot == null) {
        this.boot = this.runBoot(context)
      }
      this.boot!!
    }
  }

  private class PresentableException(
    override val message: String,
    override val cause: Throwable) : Exception(message, cause), PresentableErrorType

  private fun runBoot(context: Context): FluentFuture<T> {
    val future = SettableFuture.create<T>()
    this.executor.execute {
      val strings = this.bootStringResources.invoke(context.resources)

      try {
        future.set(this.bootProcess.execute { event -> this.eventsActual.send(event) })
        this.logger.debug("finished executing boot")
      } catch (e: Throwable) {
        this.logger.error("boot failed: ", e)
        val event = if (e is PresentableErrorType) {
          BootEvent.BootFailed(
            message = e.message,
            exception = PresentableException(e.message, e),
            causes = listOf(e),
            attributes = e.attributes)
        } else {
          BootEvent.BootFailed(
            message = strings.bootFailedGeneric,
            exception = PresentableException(strings.bootFailedGeneric, e))
        }

        this.eventsActual.send(event)
        future.setException(event.exception)
      }
    }
    return FluentFuture.from(future)
  }
}
