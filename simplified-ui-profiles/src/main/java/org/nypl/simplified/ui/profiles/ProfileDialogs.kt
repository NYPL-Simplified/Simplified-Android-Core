package org.nypl.simplified.ui.profiles

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Functions to instantiate dialogs.
 */

object ProfileDialogs {

  /**
   * Create a dialog that notifies users that they will be logged out shortly.
   */

  fun createTimeOutDialog(context: Context): AlertDialog {
    return AlertDialog.Builder(context)
      .setTitle(R.string.profilesTimeOutSoonTitle)
      .setMessage(R.string.profilesTimeOutSoon)
      .setIcon(R.drawable.profile_time)
      .create()
  }

  /**
   * Create a dialog that asks users if they really want to switch profiles.
   */

  fun createSwitchConfirmDialog(
    context: Context,
    onConfirm: () -> Unit
  ): AlertDialog {
    return AlertDialog.Builder(context)
      .setTitle(R.string.profilesSwitch)
      .setMessage(R.string.profilesSwitchConfirm)
      .setIcon(R.drawable.profile_icon)
      .setPositiveButton(R.string.profilesSwitch) { dialog, _ ->
        onConfirm.invoke()
        dialog.dismiss()
      }
      .create()
  }
}
