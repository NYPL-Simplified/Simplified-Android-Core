package org.nypl.simplified.ui.host

/**
 * The interface exposed by the host view model. The "host" view model conceptually provides
 * a singleton used by various fragments to get access to application-scoped services (that is,
 * services that are typically initialized once on application startup and are present for the
 * entire run of the application).
 *
 * In practice, the single activity that makes up the application is expected to obtain an
 * activity-scoped value of this type using the ViewModelProviders API, and fragments will also
 * obtain access to the same value via the same API.
 */

interface HostViewModelType : HostViewModelReadableType {

  /**
   * Register a navigation controller, or replace an existing one, that supports the navigation
   * interface specified by [navigationInterface].
   *
   * In practice, this method is called every time the application's single activity is (re)created;
   * navigation controller implementations tend to be tied to the lifetimes of activities as they
   * tend to manipulate fragments within those activities.
   */

  fun <T : HostNavigationControllerType> updateNavigationController(
    navigationInterface: Class<T>,
    navigationInstance: T
  )

  /**
   * Deregister a navigation controller that was previously registered with [updateNavigationController].
   * Has no effect if no controller exists.
   *
   * In practice, this method is called every time the application's single activity is destroyed;
   * navigation controller implementations tend to be tied to the lifetimes of activities as they
   * tend to manipulate fragments within those activities.
   */

  fun <T : HostNavigationControllerType> removeNavigationController(
    navigationClass: Class<T>
  )
}
