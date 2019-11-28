package org.nypl.simplified.viewer.epub.readium1

import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.screen.ScreenSizeInformationType

/**
 * The listener interface that activities hosting [ReaderSettingsDialog] must implement.
 */

interface ReaderSettingsListenerType {

  /**
   * Get a reference to the current profiles controller.
   */

  fun onReaderSettingsDialogWantsProfilesController(): ProfilesControllerType

  /**
   * Get a reference to the current screen size information.
   */

  fun onReaderSettingsDialogWantsScreenSize(): ScreenSizeInformationType
}
