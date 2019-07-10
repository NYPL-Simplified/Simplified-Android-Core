package org.nypl.simplified.boot.api

import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.observable.ObservableReadableType

/**
 * The type of boot loaders.
 *
 * A boot loader is a class that starts up application services on a background thread and
 * publishes events as the services start.
 */

interface BootLoaderType<T> {

  /**
   * An observable that publishes events during the boot process.
   */

  val events: ObservableReadableType<BootEvent>

  /**
   * Start the boot process if it has not already started, and return a future representing
   * the boot in progress.
   */

  fun start(): FluentFuture<T>

}
