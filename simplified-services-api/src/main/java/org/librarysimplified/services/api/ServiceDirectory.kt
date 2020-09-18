package org.librarysimplified.services.api

class ServiceDirectory internal constructor(
  private val services: Map<Class<*>, List<Any>>
) : ServiceDirectoryType {

  override fun toBuilder(): ServiceDirectoryBuilderType {
    return ServiceDirectoryBuilder(
      this.services.mapValues { v -> v.value.toMutableList() }
        .toMutableMap()
    )
  }

  override fun <T : Any> optionalServices(serviceClass: Class<T>): List<T> {
    return this.services[serviceClass] as List<T>? ?: listOf()
  }

  companion object {
    fun builder(): ServiceDirectoryBuilderType {
      return ServiceDirectoryBuilder(mutableMapOf())
    }
  }
}
