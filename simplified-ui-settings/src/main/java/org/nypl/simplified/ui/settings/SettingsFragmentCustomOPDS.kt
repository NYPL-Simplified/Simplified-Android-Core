package org.nypl.simplified.ui.settings

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * A fragment that shows the set of accounts in the current profile.
 */

class SettingsFragmentCustomOPDS : Fragment() {

  private val logger =
    LoggerFactory.getLogger(SettingsFragmentCustomOPDS::class.java)

  @Volatile
  private var accountSubscription: Disposable? = null

  @Volatile
  private var future: FluentFuture<TaskResult<AccountType>>? = null

  private lateinit var create: Button
  private lateinit var feedURL: EditText
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var progress: ProgressBar
  private lateinit var progressText: TextView
  private lateinit var uiThread: UIThreadServiceType
  private lateinit var uriTextWatcher: URITextWatcher

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val services = Services.serviceDirectory()

    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.settings_custom_opds, container, false)

    this.feedURL =
      layout.findViewById(R.id.settingsCustomOPDSURL)
    this.create =
      layout.findViewById(R.id.settingsCustomOPDSCreate)
    this.progress =
      layout.findViewById(R.id.settingsCustomOPDSProgressBar)
    this.progressText =
      layout.findViewById(R.id.settingsCustomOPDSProgressText)

    this.create.isEnabled = false
    this.progressText.text = ""
    return layout
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar(this.requireActivity())

    this.uriTextWatcher =
      this.URITextWatcher()

    this.progress.visibility = View.INVISIBLE

    this.accountSubscription =
      this.profilesController.accountEvents()
        .subscribe(this::onAccountEvent)

    this.create.setOnClickListener {
      this.create.isEnabled = false
      this.progress.visibility = View.VISIBLE
      this.progressText.text = ""

      val nextFuture =
        this.profilesController.profileAccountCreateCustomOPDS(URI(this.feedURL.text.toString()))

      this.future = nextFuture
      nextFuture.addListener(
        Runnable { this.onCreationFinished() },
        MoreExecutors.directExecutor()
      )
    }

    this.feedURL.addTextChangedListener(this.uriTextWatcher)
  }

  private fun configureToolbar(activity: Activity) {
    this.supportActionBar?.apply {
      title = getString(R.string.settingsCustomOPDS)
      subtitle = null
    }
  }

  private fun onCreationFinished() {
    this.uiThread.runOnUIThread(
      Runnable {
        this.progress.visibility = View.INVISIBLE
        this.create.isEnabled = true
      }
    )
  }

  private fun onAccountEvent(event: AccountEvent) {
    this.uiThread.runOnUIThread(
      Runnable {
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
    )
  }

  private fun isValidURI(): Boolean {
    val text = this.feedURL.text
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

    this.accountSubscription?.dispose()
    this.create.setOnClickListener(null)
    this.feedURL.removeTextChangedListener(this.uriTextWatcher)
  }

  inner class URITextWatcher : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      this@SettingsFragmentCustomOPDS.create.isEnabled =
        this@SettingsFragmentCustomOPDS.isValidURI()
    }
  }

  private fun findNavigationController(): SettingsNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = SettingsNavigationControllerType::class.java
    )
  }
}
