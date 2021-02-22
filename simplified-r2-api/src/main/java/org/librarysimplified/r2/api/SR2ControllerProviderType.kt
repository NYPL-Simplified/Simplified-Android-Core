package org.librarysimplified.r2.api

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Callable

/**
 * A provider of R2 controllers.
 */

interface SR2ControllerProviderType {

  /**
   * Create a new R2 controller on a thread provided by the given I/O executor.
   */

  fun create(
    configuration: SR2ControllerConfiguration
  ): ListenableFuture<SR2ControllerType> {
    return configuration.ioExecutor.submit(
      Callable {
        this.createHere(configuration)
      }
    )
  }

  /**
   * Create a new controller on the current thread.
   *
   * Note that, as most implementations will perform I/O upon initialization, this method
   * should _not_ be called on the Android UI thread.
   */

  fun createHere(
    configuration: SR2ControllerConfiguration
  ): SR2ControllerType
}
