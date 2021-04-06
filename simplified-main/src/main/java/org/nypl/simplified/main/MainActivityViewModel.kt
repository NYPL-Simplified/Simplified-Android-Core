package org.nypl.simplified.main

import androidx.lifecycle.ViewModel
import org.slf4j.LoggerFactory

/**
 * A view model used by both MainActivity and MainFragment.
 *
 * As the MainFragmentViewModel depends on some services having been loaded,
 * it must not be instantiated before the startup has finished.
 */

class MainActivityViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(MainActivityViewModel::class.java)

  /** `true` if the history of tabs should be cleared. */

  var clearHistory: Boolean = true
    set(value) {
      this.logger.debug("clearHistory set to {}", value)
      field = value
    }
}
