package org.librarysimplified.instabug.spi

import android.app.Application

/**
 * A provider of Instabug instances.
 */

interface InstabugProviderType {

  /**
   * Create a new Instabug instance. Implementations of this method should load any
   * credentials needed, and initialize Instabug using the provided Application instance.
   * Implementations are permitted to return `null` if Instabug was deliberately not
   * initialized (due to being a production build, or for some other policy).
   */

  fun create(
    context: Application
  ): InstabugType?
}
