package org.nypl.simplified.app.settings

import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.app.NavigationDrawerActivity
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.net.URI

class SettingsCustomOPDSActivity : NavigationDrawerActivity() {

  private val logger = LoggerFactory.getLogger(SettingsCustomOPDSActivity::class.java)

  @Volatile
  private var accountSubscription: ObservableSubscriptionType<AccountEvent>? = null

  @Volatile
  private var future: FluentFuture<TaskResult<AccountCreateErrorDetails, AccountType>>? = null

  private lateinit var profilesController: ProfilesControllerType
  private lateinit var progressText: TextView
  private lateinit var progress: ProgressBar
  private lateinit var create: Button
  private lateinit var feedURL: EditText

  override fun navigationDrawerShouldShowIndicator(): Boolean =
    true

  override fun navigationDrawerGetActivityTitle(resources: Resources): String =
    resources.getString(R.string.settingsCustomOPDS)

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    this.setContentView(R.layout.settings_custom_opds)

    this.feedURL =
      this.findViewById(R.id.settingsCustomOPDSURL)
    this.create =
      this.findViewById(R.id.settingsCustomOPDSCreate)
    this.progress =
      this.findViewById(R.id.settingsCustomOPDSProgressBar)
    this.progressText =
      this.findViewById(R.id.settingsCustomOPDSProgressText)

    this.create.isEnabled = false
    this.progressText.text = ""

    this.profilesController =
      Simplified.application.services()
        .profilesController
  }

  override fun onStart() {
    super.onStart()

    this.progress.visibility = View.INVISIBLE

    this.accountSubscription =
      this.profilesController.accountEvents()
        .subscribe { event -> this.onAccountEvent(event) }

    this.create.setOnClickListener {
      this.create.isEnabled = false
      this.progress.visibility = View.VISIBLE
      this.progressText.text = ""

      val nextFuture =
        this.profilesController.profileAccountCreateCustomOPDS(URI(this.feedURL.text.toString()))
      this.future = nextFuture
      nextFuture.addListener(
        Runnable { this.onCreationFinished() },
        Simplified.application.services().backgroundExecutor)
    }

    this.feedURL.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
      }

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        this@SettingsCustomOPDSActivity.create.isEnabled = isValidURI()
      }
    })
  }

  private fun onCreationFinished() {
    UIThread.runOnUIThread {
      this.progress.visibility = View.INVISIBLE
      this.create.isEnabled = true
    }
  }

  private fun onAccountEvent(event: AccountEvent) {
    UIThread.runOnUIThread {
      this.progressText.append(event.message)
      this.progressText.append("\n")

      for (name in event.attributes.keys) {
        this.progressText.append("    ")
        this.progressText.append(name)
        this.progressText.append(": ")
        this.progressText.append(event.attributes[name])
        this.progressText.append("\n")
      }
    }
  }

  private fun isValidURI(): Boolean {
    val text = feedURL.text
    return if (text.isNotEmpty()) {
      try {
        URI(text.toString())
        this.feedURL.setError(null, null)
        true
      } catch (e: Exception) {
        this.logger.error("not a valid URI: ", e)
        this.feedURL.error = this.resources.getString(R.string.settingsCustomOPDSInvalidURI)
        false
      }
    } else {
      false
    }
  }

  override fun onStop() {
    super.onStop()
    this.accountSubscription?.unsubscribe()
  }
}

