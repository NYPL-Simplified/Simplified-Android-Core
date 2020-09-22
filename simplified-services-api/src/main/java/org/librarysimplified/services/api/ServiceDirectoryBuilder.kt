package org.librarysimplified.services.api

import com.google.common.base.Preconditions
import org.slf4j.LoggerFactory

internal class ServiceDirectoryBuilder(
  private val services: MutableMap<Class<*>, MutableList<Any>>
) : ServiceDirectoryBuilderType {

  private val logger =
    LoggerFactory.getLogger(ServiceDirectoryBuilder::class.java)

  override fun build(): ServiceDirectoryType {
    return ServiceDirectory(this.services.toMap())
  }

  override fun <T : Any> addService(
    interfaces: List<Class<T>>,
    service: T
  ): ServiceDirectoryBuilderType {
    Preconditions.checkArgument(
      interfaces.isNotEmpty(),
      "Must supply at least one interface type"
    )

    this.logger.debug("adding service {}", service.javaClass)
    for (inter in interfaces) {
      val existing = this.services[inter] ?: mutableListOf()
      existing.add(service)
      this.services[inter] = existing
    }
    return this
  }
}
