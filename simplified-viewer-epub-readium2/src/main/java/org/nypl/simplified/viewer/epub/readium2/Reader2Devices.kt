package org.nypl.simplified.viewer.epub.readium2

import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

object Reader2Devices {

  /**
   * Return the device ID for the account that owns `bookID`.
   */

  fun deviceId(
    profilesController: ProfilesControllerType,
    bookID: BookID
  ): String {
    val account = profilesController.profileAccountForBook(bookID)
    val state = account.loginState
    val credentials = state.credentials
    if (credentials != null) {
      val preActivation = credentials.adobeCredentials
      if (preActivation != null) {
        val postActivation = preActivation.postActivationCredentials
        if (postActivation != null) {
          return postActivation.deviceID.value
        }
      }
    }
    // Yes, really return a string that says "null"
    return "null"
  }
}
