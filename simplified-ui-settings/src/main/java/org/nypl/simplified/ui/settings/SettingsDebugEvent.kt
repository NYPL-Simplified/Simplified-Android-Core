package org.nypl.simplified.ui.settings

import org.nypl.simplified.ui.errorpage.ErrorPageParameters

sealed class SettingsDebugEvent {

  /**
   * The debug settings screen wants to open sample error page.
   */

  data class OpenErrorPage(
    val parameters: ErrorPageParameters
  ) : SettingsDebugEvent()

  /**
   * The debug settings screen wants to create an account for a custom OPDS feed.
   */

  object OpenCustomOPDS : SettingsDebugEvent()
}
