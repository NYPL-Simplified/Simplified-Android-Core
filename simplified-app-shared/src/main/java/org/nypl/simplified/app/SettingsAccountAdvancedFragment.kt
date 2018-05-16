package org.nypl.simplified.app

import android.app.AlertDialog
import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TableRow
import java.util.*


class SettingsAccountAdvancedFragment : Fragment() {

  //TODO add the title bar (same from other activities)

  lateinit var progressBar: ProgressBar
  lateinit var syncSwitch: TableRow

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    super.onCreateView(inflater, container, savedInstanceState)

    val root = inflater.inflate(R.layout.activity_settings_account_advanced, null)
    progressBar = root.findViewById(R.id.advanced_sync_progress_bar)
    syncSwitch = root.findViewById(R.id.advanced_sync_switch)
    syncSwitch.setOnClickListener {
      presentConfirmationDialog()
    }
    return root
  }

  private fun presentConfirmationDialog() {
    val act = Objects.requireNonNull(activity as? MainSettingsAccountActivity)
    val builder = AlertDialog.Builder(act as Context)
    with (builder) {
      setMessage(R.string.advanced_sync_message)
      setNegativeButton(R.string.catalog_book_delete) { _, _ ->
        progressBar.visibility = View.VISIBLE
        syncSwitch.isEnabled = false
        act.annotationsManager.updateServerSyncPermissionStatus(false) { success ->
          if (success) {
            val transaction = act.fragmentManager.beginTransaction()
            transaction.remove(this@SettingsAccountAdvancedFragment)
            transaction.commit()
          } else {
            presentErrorDialog()
            progressBar.visibility = View.INVISIBLE
            syncSwitch.isEnabled = true
          }
        }
      }
      setNeutralButton(R.string.catalog_book_revoke_cancel) { _, _ ->
        //Do Nothing, Dismiss
      }
      create()
      show()
    }
  }

  private fun presentErrorDialog() {
    val act = Objects.requireNonNull(activity as? MainSettingsAccountActivity)
    val builder = AlertDialog.Builder(act as Context)
    with(builder) {
      setTitle(R.string.catalog_load_error_title)
      setMessage(R.string.advanced_sync_error_message)
      setNeutralButton("OK", null)
    }
    val dialog = builder.create()
    dialog.show()
  }


}
