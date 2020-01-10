package org.librarysimplified.services.api

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

object Services : ServiceDirectoryProviderType {

  private val servicesLock: Any = Any()
  private var servicesDirectory: ServiceDirectoryType? = null
  private val servicesFuture: SettableFuture<ServiceDirectoryType> = SettableFuture.create()

  override fun serviceDirectory(): ServiceDirectoryType {
    return synchronized(this.servicesLock) {
      this.servicesDirectory ?: throw IllegalStateException("No service directory has been created!")
    }
  }

  fun serviceDirectoryFuture(): ListenableFuture<ServiceDirectoryType> =
    this.servicesFuture

  fun serviceDirectoryWaiting(
    time: Long,
    timeUnit: TimeUnit
  ): ServiceDirectoryType =
    this.servicesFuture.get(time, timeUnit)

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
