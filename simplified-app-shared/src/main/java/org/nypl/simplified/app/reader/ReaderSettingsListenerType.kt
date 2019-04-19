package org.nypl.simplified.app.reader

import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.books.controller.ProfilesControllerType

/**
 * The listener interface that activities hosting [ReaderSettingsDialog] must implement.
 */

interface ReaderSettingsListenerType {

  /**
   * Get a reference to the current profiles controller.
   */

  fun profilesController(): ProfilesControllerType

  /**
   * Get a reference to the current screen size information.
   */

  fun screenSize(): ScreenSizeInformationType

}
