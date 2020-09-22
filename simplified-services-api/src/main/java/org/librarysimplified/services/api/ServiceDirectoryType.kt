package org.librarysimplified.services.api

/**
 * The service directory interface.
 *
 * A service directory interface provides a set of methods for retrieving references to
 * application services. A _service_ is a concrete implementation of an interface type, and
 * is retrieved by calling any of the methods in the directory with the interface type of
 * the service as an argument.
 *
 * The primary purpose of the service directory interface is to provide decoupling: Application
 * components may retrieve references to other components without knowing the precise implementations
 * that they're talking to.
 */

interface ServiceDirectoryType {

  /**
   * Retrieve a mandatory reference to the service implementing the given class. If multiple
   * services are available, the one that was registered first is picked.
   *
   * @throws ServiceConfigurationException If no service is available implementing the given class
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> requireService(
    serviceClass: Class<T>
  ): T =
    this.requireServices(serviceClass)[0]

  /**
   * Retrieve a list of services implementing the given class. The list is required to be
   * non-empty.
   *
   * @throws ServiceConfigurationException If no service is available implementing the given class
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> requireServices(
    serviceClass: Class<T>
  ): List<T> {
    val services = this.optionalServices(serviceClass)
    if (services.isEmpty()) {
      throw ServiceConfigurationException(
        buildString {
          this.append("No service implementation is available\n")
          this.append("  Service: ${serviceClass.canonicalName}\n")
          this.append("Note that this might indicate a circular dependency between services!\n")
        }
      )
    }
    return services
  }

  /**
   * Retrieve an optional reference to the service implementing the given class. If no service
   * is available, the function returns `null`. If multiple
   * services are available, the one that was registered first is picked.
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> optionalService(
    serviceClass: Class<T>
  ): T? =
    this.optionalServices(serviceClass).firstOrNull()

  /**
   * Retrieve a list of services implementing the given class. The list may be empty.
   */

  @Throws(ServiceConfigurationException::class)
  fun <T : Any> optionalServices(
    serviceClass: Class<T>
  ): List<T>

  /**
   * Create a new builder based on the current directory.
   */

  fun toBuilder(): ServiceDirectoryBuilderType
}
