package org.nypl.simplified.ui.host

import org.librarysimplified.services.api.ServiceDirectoryType

/**
 * The readable interface exposed by the host view model. The "host" view model conceptually provides
 * a singleton used by various fragments to get access to application-scoped services (that is,
 * services that are typically initialized once on application startup and are present for the
 * entire run of the application).
 *
 * In practice, the single activity that makes up the application is expected to obtain an
 * activity-scoped value of this type using the ViewModelProviders API, and fragments will also
 * obtain access to the same value via the same API.
 */

interface HostViewModelReadableType {

  /**
   * The application service directory.
   */

  val services: ServiceDirectoryType

  /**
   * Retrieve a navigation controller previously registered using [HostViewModelType.updateNavigationController].
   */

  fun <T : HostNavigationControllerType> navigationController(
    navigationClass: Class<T>
  ): T
}
