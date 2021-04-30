package org.nypl.simplified.ui.catalog.saml20

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.ui.accounts.saml20.AccountSAML20
import org.nypl.simplified.webview.WebViewUtilities
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * View state for the SAML 2.0 fragment.
 */

class CatalogSAML20ViewModel(
  private val booksController: BooksControllerType,
  private val bookRegistry: BookRegistryType,
  private val account: AccountType,
  private val bookID: BookID,
  private val webViewDataDir: File
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(CatalogSAML20ViewModel::class.java)
  private val eventSubject =
    PublishSubject.create<CatalogSAML20InternalEvent>()
  private val downloadInfo =
    AtomicReference<DownloadInfo>()

  private data class DownloadInfo(
    val mimeType: String?
  )

  val events: Observable<CatalogSAML20InternalEvent> =
    this.eventSubject

  init {
    val bookWithStatus = this.bookRegistry.bookOrNull(this.bookID)

    if (bookWithStatus != null) {
      val book = bookWithStatus.book

      this.bookRegistry.update(
        BookWithStatus(
          book,
          BookStatus.DownloadExternalAuthenticationInProgress(
            id = this.bookID
          )
        )
      )
    }
  }

  private class CatalogSAML20WebClient(
    private val logger: Logger,
    private val booksController: BooksControllerType,
    private val eventSubject: PublishSubject<CatalogSAML20InternalEvent>,
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

      cookieManager.removeAllCookies({
        val credentials = account.loginState.credentials

        if (credentials is AccountAuthenticationCredentials.SAML2_0) {
          credentials.cookies.forEach { accountCookie ->
            cookieManager.setCookie(accountCookie.url, accountCookie.value)
          }
        }

        isReady = true

        this.eventSubject.onNext(
          CatalogSAML20InternalEvent.WebViewClientReady()
        )
      })
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

          try {
            this.booksController.bookBorrow(
              accountID = book.account,
              entry = book.entry,
            )
            Unit
          } catch (e: Throwable) {
            this.logger.error("failed to start borrow task: ", e)
            this.bookRegistry.updateIfStatusIsMoreImportant(
              BookWithStatus(book, BookStatus.FailedLoan(book.id, this.failMinimal(e)))
            )
          }
        }

        this.eventSubject.onNext(
          CatalogSAML20InternalEvent.Succeeded()
        )
      }
    }

    private fun failMinimal(
      exception: Throwable
    ): TaskResult.Failure<Unit> {
      val recorder = TaskRecorder.create()
      recorder.beginNewStep("Logging in...")
      recorder.currentStepFailed(
        message = exception.message ?: exception.javaClass.canonicalName ?: "unknown",
        errorCode = "borrowLoginFailed",
        exception = exception
      )
      return recorder.finishFailure()
    }
  }

  override fun onCleared() {
    super.onCleared()

    if (this.downloadInfo.get() == null) {
      this.logger.debug("no download info obtained; cancelling login")

      val bookWithStatus = this.bookRegistry.bookOrNull(this.bookID)

      if (bookWithStatus != null) {
        val book = bookWithStatus.book
        this.booksController.bookDownloadCancel(book.account, this.bookID)
        this.bookRegistry.update(BookWithStatus(book, BookStatus.fromBook(book)))
      }
    }

    this.eventSubject.onComplete()
  }

  val webViewClient: WebViewClient =
    CatalogSAML20WebClient(
      eventSubject = this.eventSubject,
      logger = this.logger,
      booksController = this.booksController,
      bookRegistry = this.bookRegistry,
      bookID = this.bookID,
      downloadInfo = this.downloadInfo,
      account = this.account,
      webViewDataDir = this.webViewDataDir
    )

  val isWebViewClientReady: Boolean
    get() = (this.webViewClient as CatalogSAML20WebClient).isReady
}
