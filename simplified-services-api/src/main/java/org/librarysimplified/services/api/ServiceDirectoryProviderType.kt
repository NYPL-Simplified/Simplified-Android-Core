package org.librarysimplified.services.api

/**
 * An interface that can be implemented by objects that provide access to the service directory.
 *
 * Within the context of Android applications, this interface is typically implemented by the
 * Activity (or Activities) hosting Fragments. The Fragments then obtain a reference to the
 * current Activity, cast it to [ServiceDirectoryProviderType], and then obtain a reference to
 * the service directory. This avoids having Fragments coupled to a singleton or some other global
 * variable.
 */

interface ServiceDirectoryProviderType {

  /**
   * A reference to the current service directory.
   */

  fun serviceDirectory(): ServiceDirectoryType

}