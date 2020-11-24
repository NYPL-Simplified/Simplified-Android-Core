package org.librarysimplified.services.api

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object Services : ServiceDirectoryProviderType {

  private val logger = LoggerFactory.getLogger(Services::class.java)
  private val servicesLock: Any = Any()
  private var servicesDirectory: ServiceDirectoryType? = null
  private val servicesFuture: SettableFuture<ServiceDirectoryType> = SettableFuture.create()

  override fun serviceDirectory(): ServiceDirectoryType {
    try {
      return this.servicesFuture.get(30L, TimeUnit.SECONDS)
    } catch (e: Exception) {
      this.logger.error("unable to fetch service directory: ", e)
      throw e
    }
  }

  fun serviceDirectoryFuture(): ListenableFuture<ServiceDirectoryType> =
    this.servicesFuture

  fun isInitialized(): Boolean {
    return synchronized(this.servicesLock) {
      this.servicesDirectory != null
    }
  }

  fun initialize(services: ServiceDirectoryType) {
    return synchronized(this.servicesLock) {
      check(this.servicesDirectory == null) {
        "Service directory has already been initialized!"
      }
      this.servicesDirectory = services
      this.servicesFuture.set(services)
    }
  }
}
