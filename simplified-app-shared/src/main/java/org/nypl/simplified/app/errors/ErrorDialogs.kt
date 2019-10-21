package org.nypl.simplified.app.errors

import android.app.Activity
import android.app.AlertDialog
import androidx.annotation.UiThread
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/**
 * Convenience functions to open alert dialogs that are then capable of opening error pages.
 */

object ErrorDialogs {

  /**
   * Show a dialog that indicates that an account could not be deleted.
   */

  @UiThread
  fun showAccountDeletionError(
    activity: Activity,
    event: AccountEventDeletion.AccountEventDeletionFailed
  ) {
    val errorParameters =
      ErrorPageParameters(
        emailAddress = activity.resources.getString(R.string.feature_migration_report_email),
        body = "",
        subject = "Account deletion failed",
        attributes = event.attributes.toSortedMap(),
        taskSteps = event.taskResult.steps)

    UIThread.runOnUIThread {
      AlertDialog.Builder(activity)
        .setTitle(R.string.profiles_account_deletion_failed)
        .setMessage(R.string.profiles_account_deletion_error_general)
        .setPositiveButton(R.string.generic_ok, { dialog, which ->
          dialog.dismiss()
        })
        .setNeutralButton(R.string.generic_details, { dialog, which ->
          ErrorActivity.startActivity(activity, errorParameters)
          dialog.dismiss()
        })
        .create()
        .show()
    }
  }

  /**
   * Show a dialog that indicates that an account could not be created.
   */

  @UiThread
  fun showAccountCreationError(
    activity: Activity,
    event: AccountEventCreation.AccountEventCreationFailed
  ) {
    UIThread.checkIsUIThread()

    val errorParameters =
      ErrorPageParameters(
        emailAddress = activity.resources.getString(R.string.feature_migration_report_email),
        body = "",
        subject = "Account creation failed",
        attributes = event.attributes.toSortedMap(),
        taskSteps = event.taskResult.steps)

    UIThread.runOnUIThread {
      AlertDialog.Builder(activity)
        .setTitle(R.string.profiles_account_creation_failed)
        .setMessage(R.string.profiles_account_creation_error_general)
        .setPositiveButton(R.string.generic_ok, { dialog, which ->
          dialog.dismiss()
        })
        .setNeutralButton(R.string.generic_details, { dialog, which ->
          ErrorActivity.startActivity(activity, errorParameters)
          dialog.dismiss()
        })
        .create()
        .show()
    }
  }

  /**
   * Show a dialog that indicates that an account could not be logged in.
   */

  @UiThread
  fun showAccountLoginError(
    activity: Activity,
    account: AccountType,
    state: AccountLoginState.AccountLoginFailed
  ) {
    UIThread.checkIsUIThread()

    val errorParameters =
      ErrorPageParameters(
        emailAddress = activity.resources.getString(R.string.feature_migration_report_email),
        body = "",
        subject = "Login failed",
        attributes = sortedMapOf(
          Pair("Account Provider", account.provider.id.toASCIIString()),
          Pair("Account", account.provider.displayName)
        ),
        taskSteps = state.taskResult.steps)

    AlertDialog.Builder(activity)
      .setTitle(R.string.settings_login_failed)
      .setMessage(R.string.settings_login_failed_message)
      .setPositiveButton(R.string.generic_ok, { dialog, which ->
        dialog.dismiss()
      })
      .setNeutralButton(R.string.generic_details, { dialog, which ->
        ErrorActivity.startActivity(activity, errorParameters)
        dialog.dismiss()
      })
      .create()
      .show()
  }

  /**
   * Show a dialog that indicates that an account could not be logged out.
   */

  @UiThread
  fun showLogoutError(
    activity: Activity,
    account: AccountType,
    state: AccountLoginState.AccountLogoutFailed
  ) {
    UIThread.checkIsUIThread()

    val errorParameters =
      ErrorPageParameters(
        emailAddress = activity.resources.getString(R.string.feature_migration_report_email),
        body = "",
        subject = "Logout failed",
        attributes = sortedMapOf(
          Pair("Account Provider", account.provider.id.toASCIIString()),
          Pair("Account", account.provider.displayName)
        ),
        taskSteps = state.taskResult.steps)

    AlertDialog.Builder(activity)
      .setTitle(R.string.settings_logout_failed)
      .setMessage(R.string.settings_logout_failed_message)
      .setPositiveButton(R.string.generic_ok, { dialog, which ->
        dialog.dismiss()
      })
      .setNeutralButton(R.string.generic_details, { dialog, which ->
        ErrorActivity.startActivity(activity, errorParameters)
        dialog.dismiss()
      })
      .create()
      .show()
  }
}
