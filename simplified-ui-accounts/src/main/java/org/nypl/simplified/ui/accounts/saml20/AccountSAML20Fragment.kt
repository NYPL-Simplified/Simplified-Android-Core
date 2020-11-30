package org.nypl.simplified.ui.accounts.saml20

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.accounts.R
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.thread.api.UIThreadServiceType

/**
 * A fragment that performs the SAML 2.0 login workflow.
 */

class AccountSAML20Fragment : Fragment() {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.accounts.saml20.AccountSAML20Fragment"

    /**
     * Create a new account fragment for the given parameters.
     */

    fun create(parameters: AccountSAML20FragmentParameters): AccountSAML20Fragment {
      val arguments = Bundle()
      arguments.putSerializable(this.PARAMETERS_ID, parameters)
      val fragment = AccountSAML20Fragment()
      fragment.arguments = arguments
      return fragment
    }
  }

  private lateinit var profiles: ProfilesControllerType
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var parameters: AccountSAML20FragmentParameters
  private lateinit var progress: ProgressBar
  private lateinit var uiThread: UIThreadServiceType
  private lateinit var viewModel: AccountSAML20ViewModel
  private lateinit var webView: WebView
  private val eventSubscriptions = CompositeDisposable()

  private fun constructLoginURI(): String {
    return buildString {
      this.append(this@AccountSAML20Fragment.parameters.authenticationDescription.authenticate)
      this.append("&redirect_uri=")
      this.append(AccountSAML20.callbackURI)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.parameters = this.requireArguments()[PARAMETERS_ID] as AccountSAML20FragmentParameters

    val services = Services.serviceDirectory()

    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.profiles =
      services.requireService(ProfilesControllerType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout = inflater.inflate(R.layout.account_saml20, container, false)
    this.webView = layout.findViewById(R.id.saml20WebView)
    this.progress = layout.findViewById(R.id.saml20progressBar)
    return layout
  }

  override fun onStart() {
    super.onStart()

    this.viewModel =
      ViewModelProviders.of(
        this,
        AccountSAML20ViewModelFactory(
          profiles = this.profiles,
          account = this.parameters.accountID,
          description = this.parameters.authenticationDescription,
          resources = this.resources,
          webViewDataDir = this.requireContext().getDir("webview", Context.MODE_PRIVATE)
        )
      ).get(AccountSAML20ViewModel::class.java)

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

  private fun onSAMLEvent(event: AccountSAML20Event) {
    return when (event) {
      is AccountSAML20Event.WebViewClientReady ->
        this.onWebViewClientReady()
      is AccountSAML20Event.Failed ->
        this.onSAMLEventFailed(event)
      is AccountSAML20Event.AccessTokenObtained ->
        this.onSAMLEventAccessTokenObtained()
    }
  }

  private fun onWebViewClientReady() {
    this.loadLoginPage()
  }

  private fun onSAMLEventAccessTokenObtained() {
    this.uiThread.runOnUIThread {
      this.findNavigationController().popBackStack()
      Unit
    }
  }

  private fun onSAMLEventFailed(event: AccountSAML20Event.Failed) {
    this.uiThread.runOnUIThread {
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
  }

  private fun makeLoginTaskSteps(
    message: String
  ): List<TaskStep> {
    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep("Started SAML 2.0 login...")
    taskRecorder.currentStepFailed(message, "samlAccountCreationFailed")
    return taskRecorder.finishFailure<AccountType>().steps
  }

  @UiThread
  private fun showErrorPage(taskSteps: List<TaskStep>) {
    this.uiThread.checkIsUIThread()

    val parameters =
      ErrorPageParameters(
        emailAddress = this.buildConfig.supportErrorReportEmailAddress,
        body = "",
        subject = "[simplye-error-report]",
        attributes = sortedMapOf(),
        taskSteps = taskSteps
      )

    this.findNavigationController().openErrorPage(parameters)
  }

  private fun onSAMLEventException(exception: Throwable) {
    this.uiThread.runOnUIThread {
      this.showErrorPage(this.makeLoginTaskSteps(exception.message ?: exception.javaClass.name))
    }
  }

  private fun onSAMLEventFinished() {
    // Don't care
  }

  private fun findNavigationController(): AccountNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = AccountNavigationControllerType::class.java
    )
  }

  override fun onStop() {
    super.onStop()
    this.eventSubscriptions.clear()
  }
}
