package org.nypl.simplified.ui.accounts.saml20

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.ui.accounts.R
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/**
 * A fragment that performs the SAML 2.0 login workflow.
 */

class AccountSAML20Fragment : Fragment(R.layout.account_saml20) {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.accounts.saml20.AccountSAML20Fragment"

    /**
     * Create a new account fragment for the given parameters.
     */

    fun create(parameters: AccountSAML20FragmentParameters): AccountSAML20Fragment {
      val fragment = AccountSAML20Fragment()
      fragment.arguments = bundleOf(this.PARAMETERS_ID to parameters)
      return fragment
    }
  }

  private val eventSubscriptions: CompositeDisposable =
    CompositeDisposable()

  private val listener: FragmentListenerType<AccountSAML20Event> by fragmentListeners()

  private val parameters: AccountSAML20FragmentParameters by lazy {
    this.requireArguments()[PARAMETERS_ID] as AccountSAML20FragmentParameters
  }

  private val viewModel: AccountSAML20ViewModel by viewModels(
    factoryProducer = {
      AccountSAML20ViewModelFactory(
        application = this.requireActivity().application,
        account = this.parameters.accountID,
        description = this.parameters.authenticationDescription
      )
    }
  )

  private lateinit var progress: ProgressBar
  private lateinit var webView: WebView

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    this.webView = view.findViewById(R.id.saml20WebView)
    this.progress = view.findViewById(R.id.saml20progressBar)
  }

  override fun onStart() {
    super.onStart()

    this.eventSubscriptions.add(
      this.viewModel.events.subscribe(
        this::onSAMLEvent,
        this::onSAMLEventException,
        this::onSAMLEventFinished
      )
    )

    this.webView.webChromeClient = AccountSAML20ChromeClient(this.progress)
    this.webView.webViewClient = this.viewModel.webViewClient
    this.webView.settings.javaScriptEnabled = true

    if (this.viewModel.isWebViewClientReady) {
      this.loadLoginPage()
    }
  }

  private fun loadLoginPage() {
    this.webView.loadUrl(this.constructLoginURI())
  }

  private fun constructLoginURI(): String {
    return buildString {
      this.append(this@AccountSAML20Fragment.parameters.authenticationDescription.authenticate)
      this.append("&redirect_uri=")
      this.append(AccountSAML20.callbackURI)
    }
  }

  private fun onSAMLEvent(event: AccountSAML20InternalEvent) {
    return when (event) {
      is AccountSAML20InternalEvent.WebViewClientReady ->
        this.onWebViewClientReady()
      is AccountSAML20InternalEvent.Failed ->
        this.onSAMLEventFailed(event)
      is AccountSAML20InternalEvent.AccessTokenObtained ->
        this.onSAMLEventAccessTokenObtained()
    }
  }

  private fun onWebViewClientReady() {
    this.loadLoginPage()
  }

  private fun onSAMLEventAccessTokenObtained() {
    this.listener.post(AccountSAML20Event.AccessTokenObtained)
  }

  private fun onSAMLEventFailed(event: AccountSAML20InternalEvent.Failed) {
    val newDialog =
      AlertDialog.Builder(this.requireActivity())
        .setTitle(R.string.accountCreationFailed)
        .setMessage(R.string.accountCreationFailedMessage)
        .setPositiveButton(R.string.accountsDetails) { dialog, _ ->
          this.showErrorPage(this.makeLoginTaskSteps(event.message))
          dialog.dismiss()
        }.create()
    newDialog.show()
  }

  private fun makeLoginTaskSteps(
    message: String
  ): List<TaskStep> {
    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep("Started SAML 2.0 login...")
    taskRecorder.currentStepFailed(message, "samlAccountCreationFailed")
    return taskRecorder.finishFailure<AccountType>().steps
  }

  private fun showErrorPage(taskSteps: List<TaskStep>) {
    val parameters =
      ErrorPageParameters(
        emailAddress = this.viewModel.supportEmailAddress,
        body = "",
        subject = "[simplye-error-report]",
        attributes = sortedMapOf(),
        taskSteps = taskSteps
      )

    this.listener.post(AccountSAML20Event.OpenErrorPage(parameters))
  }

  private fun onSAMLEventException(exception: Throwable) {
    this.showErrorPage(this.makeLoginTaskSteps(exception.message ?: exception.javaClass.name))
  }

  private fun onSAMLEventFinished() {
    // Don't care
  }

  override fun onStop() {
    super.onStop()
    this.eventSubscriptions.clear()
  }
}
