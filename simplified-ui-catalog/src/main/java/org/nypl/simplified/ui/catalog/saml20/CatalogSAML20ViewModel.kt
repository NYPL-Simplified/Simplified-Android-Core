package org.nypl.simplified.ui.catalog.saml20

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.webview.WebViewUtilities
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicReference

/**
 * View state for the SAML 2.0 fragment.
 */

class CatalogSAML20ViewModel(
  private val profilesController: ProfilesControllerType,
  private val booksController: BooksControllerType,
  private val bookRegistry: BookRegistryType,
  private val buildConfig: BuildConfigurationServiceType,
  private val listener: FragmentListenerType<CatalogSAML20Event>,
  private val parameters: CatalogSAML20FragmentParameters,
  private val webViewDataDir: File
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(CatalogSAML20ViewModel::class.java)
  private val book =
    this.bookRegistry.bookOrException(this.parameters.bookID)
  private val account =
    this.profilesController.profileCurrent().account(this.book.book.account)
  private val eventSubject =
    PublishSubject.create<WebClientEvent>()
  private val downloadInfo =
    AtomicReference<DownloadInfo>()
  private val webviewRequestMutable: MutableLiveData<WebviewRequest> =
    MutableLiveData()

  private data class DownloadInfo(
    val mimeType: String?
  )

  init {
    val bookWithStatus = this.bookRegistry.bookOrNull(this.parameters.bookID)

    if (bookWithStatus != null) {
      val book = bookWithStatus.book

      this.bookRegistry.update(
        BookWithStatus(
          book,
          BookStatus.DownloadExternalAuthenticationInProgress(
            id = this.parameters.bookID
          )
        )
      )
    }
  }

  /**
   * Events raised during the SAML login process.
   */

  private sealed class WebClientEvent {

    /**
     * The web view client is ready for use. The login page should not be loaded until this event has
     * fired.
     */

    object WebViewClientReady : WebClientEvent()

    /**
     * The login succeeded.
     */

    object Succeeded : WebClientEvent()
  }

  private val subscriptions =
    CompositeDisposable(
      this.eventSubject
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
          this::onSAMLEvent,
          this::onSAMLEventException,
          this::onSAMLEventFinished
        )
    )

  private fun onSAMLEvent(event: WebClientEvent) {
    return when (event) {
      is WebClientEvent.WebViewClientReady ->
        this.onWebViewClientReady()
      is WebClientEvent.Succeeded ->
        this.onSAMLEventSucceeded()
    }
  }

  private fun onWebViewClientReady() {
    this.loadLoginPage()
  }

  private fun onSAMLEventSucceeded() {
    this.listener.post(CatalogSAML20Event.LoginSucceeded)
  }

  private fun onSAMLEventException(exception: Throwable) {
    this.showErrorPage(this.makeLoginTaskSteps(exception.message ?: exception.javaClass.name))
  }

  private fun makeLoginTaskSteps(
    message: String
  ): List<TaskStep> {
    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep("Started SAML 2.0 book download login...")
    taskRecorder.currentStepFailed(message, "samlBookDownloadLoginFailed")
    return taskRecorder.finishFailure<Unit>().steps
  }

  private fun showErrorPage(taskSteps: List<TaskStep>) {
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

  private fun onSAMLEventFinished() {
    // Don't care
  }

  private fun loadLoginPage() {
    val headers = mutableMapOf<String, String>()
    val credentials = this.account.loginState.credentials

    if (credentials is AccountAuthenticationCredentials.SAML2_0) {
      headers["Authorization"] = "Bearer ${credentials.accessToken}"
    }

    this.webviewRequestMutable.value =
      WebviewRequest(
        url = this.parameters.downloadURI.toString(),
        headers = headers
      )
  }

  private class CatalogSAML20WebClient(
    private val logger: Logger,
    private val booksController: BooksControllerType,
    private val eventSubject: PublishSubject<WebClientEvent>,
    private val bookRegistry: BookRegistryType,
    private val bookID: BookID,
    private val downloadInfo: AtomicReference<DownloadInfo>,
    private val account: AccountType,
    private val webViewDataDir: File
  ) : WebViewClient() {

    var isReady = false

    init {
      /*
       * Remove any existing cookies from the web view, and add the cookies associated with this
       * account.
       */

      val cookieManager = CookieManager.getInstance()

      cookieManager.removeAllCookies {
        val credentials = account.loginState.credentials

        if (credentials is AccountAuthenticationCredentials.SAML2_0) {
          credentials.cookies.forEach { accountCookie ->
            cookieManager.setCookie(accountCookie.url, accountCookie.value)
          }
        }

        isReady = true

        this.eventSubject.onNext(
          WebClientEvent.WebViewClientReady
        )
      }
    }

    override fun onLoadResource(
      view: WebView,
      url: String
    ) {
      if (url.startsWith(AccountSAML20.callbackURI)) {
        val parsed = Uri.parse(url)

        val mimeType = parsed.getQueryParameter("mimeType")

        this.logger.debug("obtained download info")
        this.downloadInfo.set(
          DownloadInfo(
            mimeType = mimeType
          )
        )

        val cookies = WebViewUtilities.dumpCookiesAsAccountCookies(
          CookieManager.getInstance(),
          this.webViewDataDir
        )

        val loginState = account.loginState

        if (loginState is AccountLoginState.AccountLoggedIn) {
          val credentials = loginState.credentials

          if (credentials is AccountAuthenticationCredentials.SAML2_0) {
            account.setLoginState(
              loginState.copy(
                credentials = credentials.copy(
                  cookies = cookies
                )
              )
            )
          }
        }

        val bookWithStatus = this.bookRegistry.bookOrNull(this.bookID)

        if (bookWithStatus != null) {
          val book = bookWithStatus.book

          this.booksController.bookBorrow(
            accountID = book.account,
            entry = book.entry,
          )
        }

        this.eventSubject.onNext(
          WebClientEvent.Succeeded
        )
      }
    }
  }

  override fun onCleared() {
    super.onCleared()

    if (this.downloadInfo.get() == null) {
      this.logger.debug("no download info obtained; cancelling login")

      val bookWithStatus = this.bookRegistry.bookOrNull(this.parameters.bookID)

      if (bookWithStatus != null) {
        val book = bookWithStatus.book
        this.booksController.bookDownloadCancel(book.account, this.parameters.bookID)
        this.bookRegistry.update(BookWithStatus(book, BookStatus.fromBook(book)))
      }
    }

    this.eventSubject.onComplete()
    this.subscriptions.clear()
  }

  val webviewRequest: LiveData<WebviewRequest>
    get() = webviewRequestMutable

  val webViewClient: WebViewClient =
    CatalogSAML20WebClient(
      eventSubject = this.eventSubject,
      logger = this.logger,
      booksController = this.booksController,
      bookRegistry = this.bookRegistry,
      bookID = this.parameters.bookID,
      downloadInfo = this.downloadInfo,
      account = this.account,
      webViewDataDir = this.webViewDataDir
    )

  data class WebviewRequest(
    val url: String,
    val headers: Map<String, String>
  )

  fun downloadStarted(mimeType: String) {
    val url = buildString {
      this.append(AccountSAML20.callbackURI)
      this.append("?")
      this.append("mimeType=")
      this.append(URLEncoder.encode(mimeType, "utf-8"))
    }

    this.webviewRequestMutable.value =
      WebviewRequest(
        url = url,
        headers = emptyMap()
      )
  }
}
