package org.nypl.simplified.ui.catalog.saml20

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20
import org.nypl.simplified.ui.catalog.R
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import java.net.URLEncoder

/**
 * A fragment that performs the SAML 2.0 borrowing login workflow.
 */

class CatalogSAML20Fragment : Fragment() {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.catalog.saml20.CatalogSAML20Fragment"

    /**
     * Create a new borrowing login fragment for the given parameters.
     */

    fun create(parameters: CatalogSAML20FragmentParameters): CatalogSAML20Fragment {
      val arguments = Bundle()
      arguments.putSerializable(PARAMETERS_ID, parameters)
      val fragment = CatalogSAML20Fragment()
      fragment.arguments = arguments
      return fragment
    }
  }

  private val listener: FragmentListenerType<CatalogSAML20Event> by fragmentListeners()

  private lateinit var profiles: ProfilesControllerType
  private lateinit var booksController: BooksControllerType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var parameters: CatalogSAML20FragmentParameters
  private lateinit var progress: ProgressBar
  private lateinit var uiThread: UIThreadServiceType
  private lateinit var viewModel: CatalogSAML20ViewModel
  private lateinit var webView: WebView
  private lateinit var account: AccountType
  private val eventSubscriptions = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.parameters = this.requireArguments()[PARAMETERS_ID] as CatalogSAML20FragmentParameters

    val services = Services.serviceDirectory()

    this.booksController =
      services.requireService(BooksControllerType::class.java)
    this.bookRegistry =
      services.requireService(BookRegistryType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.profiles =
      services.requireService(ProfilesControllerType::class.java)

    val book = this.bookRegistry.bookOrException(this.parameters.bookID)
    val accountID = book.book.account

    this.account = this.profiles.profileCurrent().account(accountID)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout = inflater.inflate(R.layout.book_saml20, container, false)
    this.webView = layout.findViewById(R.id.saml20WebView)
    this.progress = layout.findViewById(R.id.saml20progressBar)
    return layout
  }

  override fun onStart() {
    super.onStart()

    this.viewModel =
      ViewModelProviders.of(
        this,
        CatalogSAML20ViewModelFactory(
          booksController = this.booksController,
          bookRegistry = this.bookRegistry,
          account = this.account,
          bookID = this.parameters.bookID,
          webViewDataDir = this.requireContext().getDir("webview", Context.MODE_PRIVATE)
        )
      ).get(CatalogSAML20ViewModel::class.java)

    this.eventSubscriptions.add(
      this.viewModel.events.subscribe(
        this::onSAMLEvent,
        this::onSAMLEventException,
        this::onSAMLEventFinished
      )
    )

    this.webView.webChromeClient = CatalogSAML20ChromeClient(this.progress)
    this.webView.webViewClient = this.viewModel.webViewClient
    this.webView.settings.javaScriptEnabled = true
    this.webView.setDownloadListener(this::onDownloadStart)

    if (this.viewModel.isWebViewClientReady) {
      this.loadLoginPage()
    }
  }

  private fun loadLoginPage() {
    val headers = mutableMapOf<String, String>()
    val credentials = this.account.loginState.credentials

    if (credentials is AccountAuthenticationCredentials.SAML2_0) {
      headers.put("Authorization", "Bearer ${credentials.accessToken}")
    }

    this.webView.loadUrl(this.parameters.downloadURI.toString(), headers)
  }

  private fun onDownloadStart(
    url: String,
    userAgent: String,
    contentDisposition: String,
    mimeType: String,
    contentLength: Long
  ) {
    val url = buildString {
      this.append(AccountSAML20.callbackURI)
      this.append("?")
      this.append("mimeType=")
      this.append(URLEncoder.encode(mimeType, "utf-8"))
    }

    this.webView.loadUrl(url)
  }

  private fun onSAMLEvent(event: CatalogSAML20InternalEvent) {
    return when (event) {
      is CatalogSAML20InternalEvent.WebViewClientReady ->
        this.onWebViewClientReady()
      is CatalogSAML20InternalEvent.Succeeded ->
        this.onSAMLEventSucceeded()
    }
  }

  private fun onWebViewClientReady() {
    this.loadLoginPage()
  }

  private fun onSAMLEventSucceeded() {
    this.uiThread.runOnUIThread {
      this.listener.post(CatalogSAML20Event.LoginSucceeded)
    }
  }

  private fun makeLoginTaskSteps(
    message: String
  ): List<TaskStep> {
    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep("Started SAML 2.0 book download login...")
    taskRecorder.currentStepFailed(message, "samlBookDownloadLoginFailed")
    return taskRecorder.finishFailure<Unit>().steps
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

    this.listener.post(CatalogSAML20Event.OpenErrorPage(parameters))
  }

  private fun onSAMLEventException(exception: Throwable) {
    this.uiThread.runOnUIThread {
      this.showErrorPage(this.makeLoginTaskSteps(exception.message ?: exception.javaClass.name))
    }
  }

  private fun onSAMLEventFinished() {
    // Don't care
  }

  override fun onStop() {
    super.onStop()
    this.eventSubscriptions.clear()
  }
}
