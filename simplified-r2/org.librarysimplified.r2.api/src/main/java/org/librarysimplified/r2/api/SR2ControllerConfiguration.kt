package org.librarysimplified.r2.api

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import org.readium.r2.streamer.Streamer
import org.readium.r2.shared.util.File

/**
 * Configuration values for an R2 controller.
 */

data class SR2ControllerConfiguration(

  /**
   * A file containing a book.
   */

  val bookFile: File,

  /**
   * The current Android application context.
   */

  val context: Context,

  /**
   * A Readium Streamer to open the book.
   */

  val streamer: Streamer,

  /**
   * An executor service used to execute I/O code on one or more background threads.
   */

  val ioExecutor: ListeningExecutorService,

  /**
   * A function that executes `f` on the Android UI thread.
   */

  val uiExecutor: (f: () -> Unit) -> Unit
)
