package org.nypl.simplified.boot.api

import android.content.Context

/**
 * A task that is executed prior to the boot process starting.
 *
 * Tasks should execute quickly, because they will be executed synchronously on the boot thread.
 * Any exceptions raised by tasks will simply be ignored.
 */

interface BootPreHookType {

  /**
   * Execute a task, passing it the Android application context.
   */

  fun execute(context: Context)
}
