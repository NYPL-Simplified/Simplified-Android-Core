package org.nypl.simplified.app.utilities

import com.google.common.util.concurrent.ListeningExecutorService

class UIBackgroundExecutor(
  private val executor: ListeningExecutorService
) : UIBackgroundExecutorType, ListeningExecutorService by executor
