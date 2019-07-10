package org.nypl.simplified.app

import android.support.multidex.MultiDexApplication
import com.google.common.util.concurrent.ListenableFuture
import org.nypl.simplified.app.services.SimplifiedServices
import org.nypl.simplified.app.services.SimplifiedServicesType
import org.nypl.simplified.boot.api.BootEvent
import org.nypl.simplified.boot.api.BootLoader
import org.nypl.simplified.boot.api.BootProcessType
import org.nypl.simplified.observable.ObservableReadableType
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Global application state.
 */

class Simplified : MultiDexApplication() {

  companion object {
    private lateinit var INSTANCE : Simplified

    @JvmStatic
    val application : Simplified
      get() = this.INSTANCE

    @JvmStatic
    fun getServices() : SimplifiedServicesType =
      this.application.services()
  }

  private lateinit var bootFuture: ListenableFuture<SimplifiedServicesType>
  private val logger = LoggerFactory.getLogger(Simplified::class.java)
  private val boot: BootLoader<SimplifiedServicesType> =
    BootLoader(
      bootProcess = object : BootProcessType<SimplifiedServicesType> {
        override fun execute(onProgress: (BootEvent) -> Unit): SimplifiedServicesType {
          return SimplifiedServices.create(this@Simplified, onProgress)
        }
      })

  override fun onCreate() {
    super.onCreate()
    this.logger.debug("starting app: pid {}", android.os.Process.myPid())
    this.bootFuture = this.boot.start()
    INSTANCE = this
  }

  /**
   * An observable value that publishes events as the application is booting.
   */

  val servicesBootEvents: ObservableReadableType<BootEvent>
    get() = this.boot.events

  /**
   * @return A future representing the application's boot process.
   */

  val servicesBooting : ListenableFuture<SimplifiedServicesType>
    get() = this.bootFuture

  /**
   * Retrieve the application's services. Note that this method will block until the application
   * is fully booted.
   *
   * @return The application's services.
   */

  fun services() : SimplifiedServicesType =
    this.bootFuture.get(30L, TimeUnit.SECONDS)
}
