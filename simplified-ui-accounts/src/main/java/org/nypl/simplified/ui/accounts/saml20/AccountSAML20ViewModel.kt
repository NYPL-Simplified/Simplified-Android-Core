package org.nypl.simplified.ui.accounts.saml20

import android.content.res.Resources
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.nypl.simplified.accounts.api.AccountCookie
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.accounts.R
import org.nypl.simplified.webview.WebViewUtilities
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * View state for the SAML 2.0 fragment.
 */

class AccountSAML20ViewModel(
  private val profiles: ProfilesControllerType,
  private val account: AccountID,
  private val description: AccountProviderAuthenticationDescription.SAML2_0,
  private val resources: Resources,
  private val webViewDataDir: File
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(AccountSAML20ViewModel::class.java)
  private val eventSubject =
    PublishSubject.create<AccountSAML20Event>()
  private val authInfo =
    AtomicReference<AuthInfo>()

  private data class AuthInfo(
    val token: String,
    val patronInfo: String,
    val cookies: List<AccountCookie>
  )

  val events: Observable<AccountSAML20Event> =
    this.eventSubject

  private class AccountSAML20WebClient(
    private val logger: Logger,
    private val resources: Resources,
    private val eventSubject: PublishSubject<AccountSAML20Event>,
    private val authInfo: AtomicReference<AuthInfo>,
    private val profiles: ProfilesControllerType,
    private val account: AccountID,
    private val webViewDataDir: File
  ) : WebViewClient() {

    var isReady = false

    init {
      /*
       * The web view may be harboring session cookies that are still valid, which could make the
       * login page go straight through to the success redirect when loaded. Since we're trying to
       * do a fresh log in, we need to make sure existing session cookies are not sent. We don't
       * know which cookies are which, so they all need to be removed.
       */

      CookieManager.getInstance().removeAllCookies({
        isReady = true

        this.eventSubject.onNext(
          AccountSAML20Event.WebViewClientReady()
        )
      })
    }

    override fun shouldOverrideUrlLoading(
      view: WebView,
      url: String
    ): Boolean {
      return this.handleUrl(view, url)
    }

    override fun shouldOverrideUrlLoading(
      view: WebView,
      request: WebResourceRequest
    ): Boolean {
      return this.handleUrl(view, request.url.toString())
    }

    private fun handleUrl(
      view: WebView,
      url: String
    ): Boolean {
      if (url.startsWith(AccountSAML20.callbackURI)) {
        val parsed = Uri.parse(url)

        val accessToken = parsed.getQueryParameter("access_token")
        if (accessToken == null) {
          val message = this.resources.getString(R.string.accountSAML20NoAccessToken)
          this.logger.error("{}", message)
          this.eventSubject.onNext(AccountSAML20Event.Failed(message))
          return true
        }

        val patronInfo = parsed.getQueryParameter("patron_info")
        if (patronInfo == null) {
          val message = this.resources.getString(R.string.accountSAML20NoPatronInfo)
          this.logger.error("{}", message)
          this.eventSubject.onNext(AccountSAML20Event.Failed(message))
          return true
        }

        val cookies = WebViewUtilities.dumpCookiesAsAccountCookies(this.webViewDataDir)

        this.logger.debug("obtained access token")
        this.authInfo.set(
          AuthInfo(
            token = accessToken,
            patronInfo = patronInfo,
            cookies = cookies
          )
        )

        this.profiles.profileAccountLogin(
          ProfileAccountLoginRequest.SAML20Complete(
            accountId = this.account,
            accessToken = accessToken,
            patronInfo = patronInfo,
            cookies = cookies
          )
        )
        this.eventSubject.onNext(
          AccountSAML20Event.AccessTokenObtained(
            token = accessToken,
            patronInfo = patronInfo,
            cookies = cookies
          )
        )
        return true
      }
      return false
    }
  }

  override fun onCleared() {
    super.onCleared()

    if (this.authInfo.get() == null) {
      this.logger.debug("no access token obtained; cancelling login")
      this.profiles.profileAccountLogin(
        ProfileAccountLoginRequest.SAML20Cancel(
          accountId = this.account,
          description = this.description
        )
      )
    }

    this.eventSubject.onComplete()
  }

  val webViewClient: WebViewClient =
    AccountSAML20WebClient(
      account = this.account,
      eventSubject = this.eventSubject,
      logger = this.logger,
      profiles = this.profiles,
      resources = this.resources,
      authInfo = this.authInfo,
      webViewDataDir = this.webViewDataDir
    )

  val isWebViewClientReady: Boolean
    get() = (this.webViewClient as AccountSAML20WebClient).isReady
}
