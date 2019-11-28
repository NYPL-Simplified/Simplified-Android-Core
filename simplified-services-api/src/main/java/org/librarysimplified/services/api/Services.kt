package org.librarysimplified.services.api

import com.google.common.base.Preconditions
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap

object Services : ServiceDirectoryProviderType {

  private val servicesLock : Any = Any()
  private var servicesDirectory: ServiceDirectoryType? = null

  override fun serviceDirectory(): ServiceDirectoryType {
    return synchronized(this.servicesLock) {
      this.servicesDirectory ?: throw IllegalStateException("No service directory has been created!")
    }
  }

  fun isInitialized(): Boolean {
    return synchronized(this.servicesLock) {
      this.servicesDirectory != null
    }
  }

  fun startInitializing(): ServiceDirectoryBuilderType {
    return Builder()
  }

  private class Directory(
    private val services: Map<Class<*>, List<Any>>
  ) : ServiceDirectoryType {

    override fun <T : Any> optionalServices(serviceClass: Class<T>): List<T> {
      return this.services[serviceClass] as List<T>? ?: listOf()
    }
  }

  private class Builder : ServiceDirectoryBuilderType {

    private val logger =
      LoggerFactory.getLogger(Builder::class.java)

    private val services: ConcurrentHashMap<Class<*>, MutableList<Any>> = ConcurrentHashMap(64)

    override fun build(): ServiceDirectoryType {
      synchronized(servicesLock) {
        if (servicesDirectory != null) {
          throw IllegalStateException("Services are already initialized!")
        }
        servicesDirectory = Directory(this.services.mapValues {
          entry -> entry.value.toList()
        })
        return servicesDirectory!!
      }
    }

    override fun <T : Any> addService(
      interfaces: List<Class<T>>,
      service: T
    ): ServiceDirectoryBuilderType {
      Preconditions.checkArgument(
        interfaces.isNotEmpty(),
        "Must supply at least one interface type")

      this.logger.debug("adding service {}", service.javaClass)
      for (inter in interfaces) {
        val existing = this.services[inter] ?: mutableListOf()
        existing.add(service)
        this.services[inter] = existing
      }
      return this
    }
  }
}