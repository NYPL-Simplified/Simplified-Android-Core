package org.nypl.simplified.boot.api

import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableReadableType
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * A class for starting up a set of services on a background thread and publishing
 * events as the services are started.
 */

class BootLoader<T>(

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

  override fun start(): FluentFuture<T> {
    return synchronized(this.bootLock) {
      if (this.boot == null) {
        this.boot = this.runBoot()
      }
      this.boot!!
    }
  }

  private fun runBoot(): FluentFuture<T> {
    val future = SettableFuture.create<T>()
    this.executor.execute {
      try {
        future.set(this.bootProcess.execute { event -> this.eventsActual.send(event) })
        this.logger.debug("finished executing boot")
      } catch (e: Throwable) {
        this.logger.error("boot failed: ", e)
        this.eventsActual.send(BootEvent.BootFailed(e.message ?: "", Exception(e)))
        future.setException(e)
      }
    }
    return future
  }
}
