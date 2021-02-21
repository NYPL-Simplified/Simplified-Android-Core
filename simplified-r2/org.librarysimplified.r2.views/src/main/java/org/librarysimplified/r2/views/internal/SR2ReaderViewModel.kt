package org.librarysimplified.r2.views.internal

import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ControllerType
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

internal class SR2ReaderViewModel : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(SR2ReaderViewModel::class.java)

  private val controllerLock = Any()
  private var controller: SR2ControllerType? = null

  /**
   * An I/O executor used for executing commands on the controller.
   */

  val ioExecutor: ListeningExecutorService =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1) { runnable ->
      val thread = Thread(runnable)
      thread.name = "org.librarysimplified.r2.io"
      thread
    })

  override fun onCleared() {
    super.onCleared()

    synchronized(this.controllerLock) {
      this.controller?.close()
      this.controller = null
      this.ioExecutor.shutdown()
    }
  }

  fun get(): SR2ControllerType? {
    return synchronized(this.controllerLock) {
      this.controller
    }
  }

  fun createOrGet(
    configuration: SR2ControllerConfiguration,
    controllers: SR2ControllerProviderType
  ): ListenableFuture<SR2ControllerReference> {

    /*
     * If there's an existing controller, then return it.
     */

    synchronized(this.controllerLock) {
      val existing = this.controller
      if (existing != null) {
        return Futures.immediateFuture(SR2ControllerReference(
          controller = existing,
          isFirstStartup = false
        ))
      }
    }

    /*
     * Otherwise, asynchronously create a new controller.
     */

    val refFuture =
      SettableFuture.create<SR2ControllerReference>()
    val future =
      controllers.create(configuration)

    future.addListener(
      Runnable {
        try {
          val newController = future.get()
          synchronized(this.controllerLock) {
            this.controller = newController
          }
          refFuture.set(SR2ControllerReference(
            controller = newController,
            isFirstStartup = true
          ))
        } catch (e: Throwable) {
          this.logger.error("unable to create controller: ", e)
          refFuture.setException(e)
        }
      },
      MoreExecutors.directExecutor())

    return refFuture
  }
}
