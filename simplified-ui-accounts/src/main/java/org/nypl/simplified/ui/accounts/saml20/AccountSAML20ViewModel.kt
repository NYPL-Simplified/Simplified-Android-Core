package org.nypl.simplified.ui.accounts.saml20

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountCookie
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
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
  private val application: Application,
  private val account: AccountID,
  private val description: AccountProviderAuthenticationDescription.SAML2_0,
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(AccountSAML20ViewModel::class.java)

  private val resources: Resources =
    application.resources

  private val webViewDataDir: File =
    this.application.getDir("webview", Context.MODE_PRIVATE)

  private val eventSubject =
    PublishSubject.create<AccountSAML20Event>()

  private val authInfo =
    AtomicReference<AuthInfo>()

  private val services =
    Services.serviceDirectory()

  private val buildConfig =
    services.requireService(BuildConfigurationServiceType::class.java)

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val subscriptions = CompositeDisposable(
    eventSubject
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        { event -> this.events.onNext(event) },
        { error -> this.events.onError(error) },
        { this.events.onComplete() }
      )
  )

  private data class AuthInfo(
    val token: String,
    val patronInfo: String,
    val cookies: List<AccountCookie>
  )

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

      CookieManager.getInstance().removeAllCookies {
        isReady = true

        this.eventSubject.onNext(
          AccountSAML20Event.WebViewClientReady()
        )
      }
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

        val cookies = WebViewUtilities.dumpCookiesAsAccountCookies(
          CookieManager.getInstance(),
          this.webViewDataDir
        )

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
      this.profilesController.profileAccountLogin(
        ProfileAccountLoginRequest.SAML20Cancel(
          accountId = this.account,
          description = this.description
        )
      )
    }

    this.eventSubject.onComplete()
    subscriptions.clear()
  }

  val webViewClient: WebViewClient =
    AccountSAML20WebClient(
      account = this.account,
      eventSubject = this.eventSubject,
      logger = this.logger,
      profiles = this.profilesController,
      resources = this.resources,
      authInfo = this.authInfo,
      webViewDataDir = this.webViewDataDir
    )

  val isWebViewClientReady: Boolean
    get() = (this.webViewClient as AccountSAML20WebClient).isReady

  val events: UnicastWorkSubject<AccountSAML20Event> =
    UnicastWorkSubject.create()

  val supportEmailAddress: String =
    buildConfig.supportErrorReportEmailAddress
}
