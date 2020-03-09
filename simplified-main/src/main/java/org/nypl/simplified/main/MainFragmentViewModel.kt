package org.nypl.simplified.main

import androidx.lifecycle.ViewModel
import org.slf4j.LoggerFactory

/**
 * The view model for the main fragment.
 */

class MainFragmentViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(MainFragmentViewModel::class.java)

  /**
   * `true` if the history of tabs should be cleared.
   */

  var clearHistory: Boolean = true
    set(value) {
      logger.debug("clearHistory set to {}", value)
      field = value
    }
}
