package org.librarysimplified.services.api

interface ServiceDirectoryBuilderType {

  fun <T : Any> addService(
    interfaces: List<Class<T>>,
    service: T
  ): ServiceDirectoryBuilderType

  fun <T : Any> addService(
    interfaceType: Class<T>,
    service: T
  ): ServiceDirectoryBuilderType =
    this.addService(listOf(interfaceType), service)

  fun build(): ServiceDirectoryType
}
