package org.nypl.simplified.ui.settings

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.android.ktx.supportActionBar
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * A fragment that allows to add a custom OPDS feed.
 */

class SettingsCustomOPDSFragment : Fragment(R.layout.settings_custom_opds) {

  private val logger = LoggerFactory.getLogger(SettingsCustomOPDSFragment::class.java)
  private val viewModel: SettingsCustomOPDSViewModel by viewModels()
  private val subscriptions: CompositeDisposable = CompositeDisposable()

  private lateinit var create: Button
  private lateinit var feedURL: EditText
  private lateinit var progress: ProgressBar
  private lateinit var progressText: TextView

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.feedURL =
      view.findViewById(R.id.settingsCustomOPDSURL)
    this.create =
      view.findViewById(R.id.settingsCustomOPDSCreate)
    this.progress =
      view.findViewById(R.id.settingsCustomOPDSProgressBar)
    this.progressText =
      view.findViewById(R.id.settingsCustomOPDSProgressText)

    if (savedInstanceState == null) {
      this.progressText.text = ""
    }

    this.viewModel.taskRunning.observe(viewLifecycleOwner, this::onTaskRunningChanged)
  }

  override fun onStart() {
    super.onStart()
    this.configureToolbar(this.requireActivity())

    this.feedURL.addTextChangedListener(this.URITextWatcher())

    this.viewModel.accountEvents
      .subscribe(this::onAccountEvent)
      .let { subscriptions.add(it) }

    this.create.setOnClickListener {
      this.viewModel.createCustomOPDSFeed(this.feedURL.text.toString())
    }
  }

  override fun onStop() {
    super.onStop()
    subscriptions.clear()
  }

  private fun configureToolbar(activity: Activity) {
    this.supportActionBar?.apply {
      title = getString(R.string.settingsCustomOPDS)
      subtitle = null
    }
  }

  private fun onTaskRunningChanged(running: Boolean) {
    if (running) {
      this.create.isEnabled = false
      this.progress.visibility = View.VISIBLE
      this.progressText.text = ""
    } else {
      this.progress.visibility = View.INVISIBLE
      this.create.isEnabled = this.isValidURI()
    }
  }

  private fun onAccountEvent(event: AccountEvent) {
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

  inner class URITextWatcher : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      this@SettingsCustomOPDSFragment.create.isEnabled =
        this@SettingsCustomOPDSFragment.isValidURI()
    }
  }
}
