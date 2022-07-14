package org.nypl.simplified.ui.accounts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
// import com.io7m.junreachable.UnimplementedCodeException
import com.io7m.junreachable.UnreachableCodeException
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.oauth.OAuthCallbackIntentParsing
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.Basic
// import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryCancel
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryInitiate
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableResult.SYNC_DISABLED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableResult.SYNC_ENABLED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableResult.SYNC_ENABLE_NOT_SUPPORTED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableStatus
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLoginButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLoginButtonEnabled
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType
import org.slf4j.LoggerFactory
import java.net.URI
import org.nypl.simplified.ui.accounts.utils.hideSoftInput
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.widget.doOnTextChanged

/**
 * A fragment that shows settings for a single account.
 */

class OEAccountDetailFragment : Fragment(R.layout.oe_account) {

  private val logger =
    LoggerFactory.getLogger(OEAccountDetailFragment::class.java)

  private val subscriptions: CompositeDisposable =
    CompositeDisposable()

  private val listener: FragmentListenerType<AccountDetailEvent> by fragmentListeners()

  private val parameters: AccountFragmentParameters by lazy {
    this.requireArguments()[PARAMETERS_ID] as AccountFragmentParameters
  }

  private val services = Services.serviceDirectory()

  private val viewModel: AccountDetailViewModel by viewModels(
    factoryProducer = {
      AccountDetailViewModelFactory(
        account = this.parameters.accountId,
        listener = this.listener
      )
    }
  )

  private val imageLoader: ImageLoaderType =
    services.requireService(ImageLoaderType::class.java)

  private lateinit var accountCustomOPDS: ViewGroup
  private lateinit var accountCustomOPDSField: TextView
  private lateinit var accountIcon: ImageView
  private lateinit var accountSubtitle: TextView
  private lateinit var accountTitle: TextView
  private lateinit var authentication: ViewGroup

  private lateinit var authenticationViews: AccountAuthenticationViews
  private lateinit var bookmarkSync: ViewGroup
  private lateinit var bookmarkSyncCheck: SwitchCompat
  private lateinit var bookmarkSyncProgress: ProgressBar
  private lateinit var oeLoginButtonErrorDetails: Button
  private lateinit var oeAccountLoginProgress: ViewGroup
  private lateinit var oeLoginProgressText: TextView
  private lateinit var loginTitle: ViewGroup
  private lateinit var reportIssueEmail: TextView
  private lateinit var reportIssueGroup: ViewGroup
  private lateinit var reportIssueItem: View

  private lateinit var accountMain: LinearLayout
  private lateinit var oeLogin: ConstraintLayout
  private lateinit var firstBookLogin: ConstraintLayout
  private lateinit var terms: TextView
  private lateinit var privacyNotice: TextView
  private lateinit var faq: TextView
  private lateinit var loginTrouble: TextView
  private lateinit var cleverBtn: Button
  private lateinit var firstBookBtn: Button
  private lateinit var firstBookBack: ImageView
  private lateinit var firstBookAcccessCode: EditText
  private lateinit var firstBookPin: EditText
  private lateinit var firstBookSignIn: Button
  private lateinit var firstBookLoginProgressBar: ProgressBar
  private lateinit var firstBookHeader: TextView
  private lateinit var firstBookLogo: ImageView
  private lateinit var oeLoggedIn: ConstraintLayout
  private lateinit var loginForm: ConstraintLayout
  private lateinit var supportEmail: TextView
  private val firstBookAccessCodeMin = 10
  private val firstBookPinMin = 4

  private val imageButtonLoadingTag = "IMAGE_BUTTON_LOADING"

  companion object {

    internal const val PARAMETERS_ID =
      "org.nypl.simplified.ui.accounts.AccountFragment.parameters"

    /**
     * Create a new account fragment for the given parameters.
     */

    fun create(parameters: AccountFragmentParameters): OEAccountDetailFragment {
      val fragment = OEAccountDetailFragment()
      fragment.arguments = bundleOf(this.PARAMETERS_ID to parameters)
      return fragment
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.accountTitle =
      view.findViewById(R.id.accountCellTitle)
    this.accountSubtitle =
      view.findViewById(R.id.accountCellSubtitle)
    this.accountIcon =
      view.findViewById(R.id.accountCellIcon)

    this.authentication =
      view.findViewById(R.id.auth)
    this.authenticationViews =
      AccountAuthenticationViews(
        viewGroup = this.authentication,
        onUsernamePasswordChangeListener = this::onBasicUserPasswordChanged
      )

    this.bookmarkSyncProgress =
      view.findViewById(R.id.accountSyncProgress)
    this.bookmarkSync =
      view.findViewById(R.id.accountSyncBookmarks)
    this.bookmarkSyncCheck =
      this.bookmarkSync.findViewById(R.id.accountSyncBookmarksCheck)

    this.loginTitle =
      view.findViewById(R.id.accountTitleAnnounce)
    this.oeAccountLoginProgress =
      view.findViewById(R.id.oeAccountLoginProgress)
    this.firstBookLoginProgressBar =
      view.findViewById(R.id.firstBookLoginProgressBar)
    this.firstBookLoginProgressBar =
      view.findViewById(R.id.firstBookLoginProgressBar)
    this.oeLoginProgressText =
      view.findViewById(R.id.oeLoginProgressText)
    this.oeLoginButtonErrorDetails =
      view.findViewById(R.id.oeLoginButtonErrorDetails)

    this.accountCustomOPDS =
      view.findViewById(R.id.accountCustomOPDS)
    this.accountCustomOPDSField =
      this.accountCustomOPDS.findViewById(R.id.accountCustomOPDSField)

    this.reportIssueGroup =
      view.findViewById(R.id.accountReportIssue)
    this.reportIssueItem =
      this.reportIssueGroup.findViewById(R.id.accountReportIssueText)
    this.reportIssueEmail =
      this.reportIssueGroup.findViewById(R.id.accountReportIssueEmail)

    this.accountMain = view.findViewById(R.id.accountMain)
    this.oeLogin = view.findViewById(R.id.oe_login)
    this.firstBookLogin = view.findViewById(R.id.first_book_login)
    this.firstBookBtn = view.findViewById(R.id.firstbook)
    this.cleverBtn = view.findViewById(R.id.clever)
    this.terms = view.findViewById(R.id.terms)
    this.privacyNotice = view.findViewById(R.id.privacyNotice)
    this.faq = view.findViewById(R.id.faq)
    this.loginTrouble = view.findViewById(R.id.loginTrouble)
    this.firstBookBack = view.findViewById(R.id.firstBookBack)
    this.firstBookAcccessCode = view.findViewById(R.id.et_access_code)
    this.firstBookPin = view.findViewById(R.id.et_pin)
    this.firstBookAcccessCode.filters += InputFilter.AllCaps()
    this.firstBookSignIn = view.findViewById(R.id.sign_in)
    this.firstBookHeader = view.findViewById(R.id.firstBookHeader)
    this.firstBookLogo = view.findViewById(R.id.firstBookLogo)
    this.oeLoggedIn = view.findViewById(R.id.oe_logged_in)
    this.loginForm = view.findViewById(R.id.loginForm)
    this.supportEmail = view.findViewById(R.id.supportEmail)

    if (this.parameters.showPleaseLogInTitle) {
      this.loginTitle.visibility = VISIBLE
    } else {
      this.loginTitle.visibility = GONE
    }

    /*
     * Instantiate views for alternative authentication methods.
     */

    this.authenticationAlternativesMake()

    ImageAccountIcons.loadAccountLogoIntoView(
      this.imageLoader.loader,
      this.viewModel.account.provider.toDescription(),
      R.drawable.account_default,
      this.accountIcon
    )

    this.viewModel.accountLive.observe(this.viewLifecycleOwner) {
      this.reconfigureOEUI()
    }

    this.viewModel.accountSyncingSwitchStatus.observe(this.viewLifecycleOwner) { status ->
      this.reconfigureBookmarkSyncingSwitch(status)
    }
  }

  override fun onResume() {
    super.onResume()
    this.oeLogin.visibility = VISIBLE
    this.accountMain.visibility = GONE
    (activity as AppCompatActivity).let {
      it.supportActionBar?.hide()
    }

    this.terms.setOnClickListener {
      launchWebView(WebViewActivity.TERMS_OF_USE)
    }

    this.privacyNotice.setOnClickListener {
      launchWebView(WebViewActivity.PRIVACY_NOTICE)
    }

    this.firstBookBtn.setOnClickListener {
      this.oeLogin.visibility = GONE
      this.firstBookLogin.visibility = VISIBLE
    }

    this.firstBookBack.setOnClickListener {
      hideSoftInput()
      this.oeLogin.visibility = VISIBLE
      this.firstBookLogin.visibility = GONE
      oeAccountLoginProgress.visibility = GONE
    }

    this.loginTrouble.setOnClickListener {
      launchWebView(WebViewActivity.LOGIN_TROUBLE)
    }

    this.faq.setOnClickListener {
      launchWebView(WebViewActivity.FAQ)
    }

    this.firstBookSignIn.setOnClickListener {
      logger.debug("Logging into First Book")
      oeAccountLoginProgress.visibility = GONE
      hideSoftInput()
      val accountUsername = AccountUsername(this.firstBookAcccessCode.toString().trim())
      val accountPassword = AccountPassword(this.firstBookPin.toString().trim())
      val description = AccountProviderAuthenticationDescription.Basic(
        description = "First Book",
        formDescription = AccountProviderAuthenticationDescription.FormDescription(
          barcodeFormat = null,
          keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          passwordMaximumLength = 0,
          passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          labels = linkedMapOf("LOGIN" to "First Book Access Code", "PASSWORD" to "First Book PIN")
        ),
        logoURI = URI.create("https://circulation.openebooks.us/images/FirstBookLoginButton280.png")
      )

      val request =
        Basic(
          accountId = this.viewModel.account.id,
          description = description,
          password = accountPassword,
          username = accountUsername
        )
      this.viewModel.tryLogin(request)
    }

    this.firstBookHeader.setOnClickListener {
      showLoggedOutUI() // TODO: Remove
    }

    this.firstBookLogo.setOnClickListener {
      showLoggedInUI() // TODO: Remove
    }

    this.firstBookPin.doOnTextChanged { _, _, _, _ ->
      updateLoginButton()
    }

    this.firstBookAcccessCode.doOnTextChanged { _, _, _, _ ->
      updateLoginButton()
    }
  }

  /**
   * Locks sign in form and shows bookmark & server buttons
   */
  private fun showLoggedInUI() {
    val params = loginForm.layoutParams as MarginLayoutParams
    params.topMargin = 16
    this.firstBookHeader.text = getString(R.string.settings)
    this.firstBookLogo.visibility = GONE
    this.loginTrouble.visibility = INVISIBLE
    this.faq.visibility = INVISIBLE
    this.oeLoggedIn.visibility = VISIBLE
    this.firstBookSignIn.text = getString(R.string.signOut)
    this.firstBookAcccessCode.isEnabled = false
    this.firstBookPin.isEnabled = false
  }

  /**
   * Unlocks sign in form and hides bookmark & server buttons
   */
  private fun showLoggedOutUI() {
    this.firstBookHeader.text = getString(R.string.signInHeader)
    this.firstBookLogo.visibility = VISIBLE
    this.loginTrouble.visibility = VISIBLE
    this.faq.visibility = VISIBLE
    this.oeLoggedIn.visibility = INVISIBLE
    this.firstBookSignIn.text = getString(R.string.signIn)
    this.firstBookAcccessCode.isEnabled = true
    this.firstBookPin.isEnabled = true
  }

  /**
   * Enable/Disable login button if form is valid
   */
  private fun updateLoginButton() {
    this.firstBookSignIn.isEnabled = this.firstBookAcccessCode.text.toString().trim().length == firstBookAccessCodeMin && this.firstBookPin.text.toString().trim().length == firstBookPinMin
  }

  /**
   * Launches WebView in activity
   */
  private fun launchWebView(page: String) {
    val intent = Intent(context, WebViewActivity::class.java)
    intent.putExtra(WebViewActivity.PAGE, page)
    startActivity(intent)
  }

  private fun reconfigureBookmarkSyncingSwitch(status: ReaderBookmarkSyncEnableStatus) {

    /*
     * Remove the checked-change listener, because setting `isChecked` will trigger the listener.
     */

    this.bookmarkSyncCheck.setOnCheckedChangeListener(null)

    /*
     * Otherwise, the switch is doing something that interests us...
     */

    val account = this.viewModel.account
    return when (status) {
      is ReaderBookmarkSyncEnableStatus.Changing -> {
        this.bookmarkSyncProgress.visibility = VISIBLE
        this.bookmarkSyncCheck.isEnabled = false
      }

      is ReaderBookmarkSyncEnableStatus.Idle -> {
        this.bookmarkSyncProgress.visibility = INVISIBLE

        when (status.status) {
          SYNC_ENABLE_NOT_SUPPORTED -> {
            this.bookmarkSyncCheck.isChecked = false
            this.bookmarkSyncCheck.isEnabled = false
          }

          SYNC_ENABLED,
          SYNC_DISABLED -> {
            val isPermitted = account.preferences.bookmarkSyncingPermitted
            val isSupported = account.loginState.credentials?.annotationsURI != null

            this.bookmarkSyncCheck.isChecked = isPermitted
            this.bookmarkSyncCheck.isEnabled = isSupported

            this.bookmarkSyncCheck.setOnCheckedChangeListener { _, isChecked ->
              this.viewModel.enableBookmarkSyncing(isChecked)
            }
          }
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    this.cancelImageButtonLoading()
    this.imageLoader.loader.cancelRequest(this.accountIcon)
    this.authenticationViews.clear()
  }

  private fun onBasicUserPasswordChanged(
    username: AccountUsername,
    password: AccountPassword
  ) {
  }

  private fun determineLoginIsSatisfied(): AccountLoginButtonStatus {
    val authDescription = this.viewModel.account.provider.authentication
    val loginPossible = authDescription.isLoginPossible
    val satisfiedFor = this.authenticationViews.isSatisfiedFor(authDescription)

    return if (loginPossible && satisfiedFor) {
      AsLoginButtonEnabled {
        this.loginFormLock()
        this.tryLogin()
      }
    } else {
      AsLoginButtonDisabled
    }
  }

  override fun onStart() {
    super.onStart()

    this.configureToolbar(requireActivity())

    /*
     * Configure the bookmark syncing switch to enable/disable syncing permissions.
     */

    this.bookmarkSyncCheck.setOnCheckedChangeListener { _, isChecked ->
      this.viewModel.enableBookmarkSyncing(isChecked)
    }

    /*
     * Configure the "Report issue..." item.
     */

    this.configureReportIssue()
  }

  private fun instantiateAlternativeAuthenticationViews() {
    for (alternative in this.viewModel.account.provider.authenticationAlternatives) {
      when (alternative) {
        is AccountProviderAuthenticationDescription.COPPAAgeGate ->
          this.logger.warn("COPPA age gate is not currently supported as an alternative.")
        is AccountProviderAuthenticationDescription.Basic ->
          this.logger.warn("Basic authentication is not currently supported as an alternative.")
        AccountProviderAuthenticationDescription.Anonymous ->
          this.logger.warn("Anonymous authentication makes no sense as an alternative.")
        is AccountProviderAuthenticationDescription.SAML2_0 ->
          this.logger.warn("SAML 2.0 is not currently supported as an alternative.")
        is AccountProviderAuthenticationDescription.OAuthClientCredentials ->
          this.logger.warn("OAuth Client Credentials is not currently supported as an alternative.")

        is AccountProviderAuthenticationDescription.OAuthWithIntermediary -> {
          this.cleverBtn.setOnClickListener {
            this.onTryOAuthWithIntermediaryLogin(alternative)
          }
        }
      }
    }
  }

  /**
   * If there's a support email, enable an option to use it.
   */

  private fun configureReportIssue() {
    val email = this.viewModel.account.provider.supportEmail
    if (email != null) {
      val address = email.removePrefix("mailto:")

      this.reportIssueGroup.visibility = VISIBLE
      this.reportIssueEmail.text = address
      this.reportIssueGroup.setOnClickListener {
        val emailIntent =
          Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null))
        val chosenIntent =
          Intent.createChooser(emailIntent, this.resources.getString(R.string.accountReportIssue))

        try {
          this.startActivity(chosenIntent)
        } catch (e: Exception) {
          this.logger.error("unable to start activity: ", e)
          val context = this.requireContext()
          AlertDialog.Builder(context)
            .setMessage(context.getString(R.string.accountReportFailed, address))
            .create()
            .show()
        }
      }
    } else {
      this.reportIssueGroup.visibility = GONE
    }
  }

  private fun configureImageButton(
    container: ViewGroup,
    buttonText: TextView,
    buttonImage: ImageView,
    text: String,
    logoURI: URI?,
    onClick: () -> Unit
  ) {
    buttonText.text = text
    buttonText.setOnClickListener { onClick.invoke() }
    buttonImage.setOnClickListener { onClick.invoke() }
    this.loadAuthenticationLogoIfNecessary(
      uri = logoURI,
      view = buttonImage,
      onSuccess = {
        container.background = null
        buttonImage.visibility = VISIBLE
        buttonText.visibility = GONE
      }
    )
  }

  private fun onTrySAML2Login(
    authenticationDescription: AccountProviderAuthenticationDescription.SAML2_0
  ) {
    this.viewModel.tryLogin(
      ProfileAccountLoginRequest.SAML20Initiate(
        accountId = this.parameters.accountId,
        description = authenticationDescription
      )
    )

    this.listener.post(
      AccountDetailEvent.OpenSAML20Login(this.parameters.accountId, authenticationDescription)
    )
  }

  private fun onTryOAuthWithIntermediaryLogin(
    authenticationDescription: AccountProviderAuthenticationDescription.OAuthWithIntermediary
  ) {
    this.viewModel.tryLogin(
      OAuthWithIntermediaryInitiate(
        accountId = this.viewModel.account.id,
        description = authenticationDescription
      )
    )
    this.sendOAuthIntent(authenticationDescription)
  }

  private fun onTryBasicLogin(description: AccountProviderAuthenticationDescription.Basic) {
    val accountPassword: AccountPassword =
      this.authenticationViews.getBasicPassword()
    val accountUsername: AccountUsername =
      this.authenticationViews.getBasicUser()

    val request =
      Basic(
        accountId = this.viewModel.account.id,
        description = description,
        password = accountPassword,
        username = accountUsername
      )

    this.viewModel.tryLogin(request)
  }

  private fun onTryOAuthClientCredentialsLogin(
    description: AccountProviderAuthenticationDescription.OAuthClientCredentials
  ) {
    val accountPassword: AccountPassword =
      this.authenticationViews.getBasicPassword()
    val accountUsername: AccountUsername =
      this.authenticationViews.getBasicUser()

    val request =
      Basic(
        accountId = this.viewModel.account.id,
        description = AccountProviderAuthenticationDescription.Basic(
          description = description.description,
          formDescription = description.formDescription,
          logoURI = description.logoURI
        ),
        password = accountPassword,
        username = accountUsername
      )

    this.viewModel.tryLogin(request)
  }

  private fun sendOAuthIntent(
    authenticationDescription: AccountProviderAuthenticationDescription.OAuthWithIntermediary
  ) {
    val callbackScheme =
      this.viewModel.buildConfig.oauthCallbackScheme.scheme
    val callbackUrl =
      OAuthCallbackIntentParsing.createUri(
        requiredScheme = callbackScheme,
        accountId = this.viewModel.account.id.uuid
      )

    /*
     * XXX: Is this correct for any other intermediary besides Clever?
     */

    val url = buildString {
      this.append(authenticationDescription.authenticate)
      this.append("&redirect_uri=$callbackUrl")
    }

    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(url)
    this.startActivity(i)
  }

  private fun configureToolbar(activity: Activity) {
    val providerName = this.viewModel.account.provider.displayName
    this.supportActionBar?.apply {
      title = getString(R.string.accounts)
      subtitle = providerName
    }
  }

  override fun onStop() {
    super.onStop()

    /*
     * Broadcast the login state. The reason for doing this is that consumers might be subscribed
     * to the account so that they can perform actions when the user has either attempted to log
     * in, or has cancelled without attempting it. The consumers have no way to detect the fact
     * that the user didn't even try to log in unless we tell the account to broadcast its current
     * state.
     */

    this.logger.debug("broadcasting login state")
    this.viewModel.account.setLoginState(this.viewModel.account.loginState)

    this.accountIcon.setImageDrawable(null)
    this.subscriptions.clear()
  }

  private fun reconfigureOEUI() {
    this.authenticationViews.showFor(this.viewModel.account.provider.authentication)
    this.disableSyncSwitchForLoginState(this.viewModel.account.loginState)

    return when (val loginState = this.viewModel.account.loginState) {
      AccountNotLoggedIn -> {
        this.oeAccountLoginProgress.visibility = GONE

        if (this.viewModel.pendingLogout) {
          this.authenticationViews.setBasicUserAndPass("", "")
          this.viewModel.pendingLogout = false
        }
        this.loginFormUnlock()
      }

      is AccountLoggingIn -> {
        this.oeAccountLoginProgress.visibility = VISIBLE
        this.firstBookLoginProgressBar.visibility = VISIBLE
        this.oeLoginProgressText.text = loginState.status
        this.oeLoginButtonErrorDetails.visibility = GONE
        this.loginFormLock()
      }

      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.oeAccountLoginProgress.visibility = VISIBLE
        this.firstBookLoginProgressBar.visibility = VISIBLE
        this.oeLoginProgressText.text = loginState.status
        this.oeLoginButtonErrorDetails.visibility = GONE
        this.loginFormLock()
      }

      is AccountLoginFailed -> {
        this.oeAccountLoginProgress.visibility = VISIBLE
        this.firstBookLoginProgressBar.visibility = GONE
        this.oeLoginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.loginFormUnlock()
        this.cancelImageButtonLoading()
        this.oeLoginButtonErrorDetails.visibility = VISIBLE
        this.oeLoginButtonErrorDetails.setOnClickListener {
          this.viewModel.openErrorPage(loginState.taskResult.steps)
        }
        this.authenticationAlternativesShow()
      }

      is AccountLoggedIn -> {
        when (val creds = loginState.credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            this.authenticationViews.setBasicUserAndPass(
              user = creds.userName.value,
              password = creds.password.value
            )
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
            // Nothing
          }
        }

        this.oeAccountLoginProgress.visibility = GONE
        this.loginFormLock()
        this.oeLoginButtonErrorDetails.visibility = GONE
        this.authenticationAlternativesHide()
        showLoggedInUI()
      }

      is AccountLoggingOut -> {
        when (val creds = loginState.credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            this.authenticationViews.setBasicUserAndPass(
              user = creds.userName.value,
              password = creds.password.value
            )
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
            // No UI
          }
        }

        this.oeAccountLoginProgress.visibility = VISIBLE
        this.oeLoginButtonErrorDetails.visibility = GONE
        this.firstBookLoginProgressBar.visibility = VISIBLE
        this.oeLoginProgressText.text = loginState.status
        this.loginFormLock()
      }

      is AccountLogoutFailed -> {
        when (val creds = loginState.credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            this.authenticationViews.setBasicUserAndPass(
              user = creds.userName.value,
              password = creds.password.value
            )
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
            // No UI
          }
        }

        this.oeAccountLoginProgress.visibility = VISIBLE
        this.firstBookLoginProgressBar.visibility = GONE
        this.oeLoginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.cancelImageButtonLoading()
        this.loginFormLock()

        this.oeLoginButtonErrorDetails.visibility = VISIBLE
        this.oeLoginButtonErrorDetails.setOnClickListener {
          this.viewModel.openErrorPage(loginState.taskResult.steps)
        }
      }
    }
  }

  private fun disableSyncSwitchForLoginState(loginState: AccountLoginState) {
    return when (loginState) {
      is AccountLoggedIn -> {
      }
      is AccountLoggingIn,
      is AccountLoggingInWaitingForExternalAuthentication,
      is AccountLoggingOut,
      is AccountLoginFailed,
      is AccountLogoutFailed,
      AccountNotLoggedIn -> {
        this.bookmarkSyncCheck.setOnCheckedChangeListener(null)
        this.bookmarkSyncCheck.isChecked = false
        this.bookmarkSyncCheck.isEnabled = false
      }
    }
  }

  private fun cancelImageButtonLoading() {
    this.imageLoader.loader.cancelTag(this.imageButtonLoadingTag)
  }

  private fun loadAuthenticationLogoIfNecessary(
    uri: URI?,
    view: ImageView,
    onSuccess: () -> Unit
  ) {
    if (uri != null) {
      view.setImageDrawable(null)
      view.visibility = VISIBLE
      this.imageLoader.loader.load(uri.toString())
        .fit()
        .tag(this.imageButtonLoadingTag)
        .into(
          view,
          object : com.squareup.picasso.Callback {
            override fun onSuccess() {
              onSuccess.invoke()
            }

            override fun onError(e: Exception) {
              this@OEAccountDetailFragment.logger.error("failed to load authentication logo: ", e)
              view.visibility = GONE
            }
          }
        )
    }
  }

  private fun loginFormLock() {

    this.authenticationViews.lock()

    this.authenticationAlternativesHide()
  }

  private fun loginFormUnlock() {

    this.authenticationViews.unlock()

    val loginSatisfied = this.determineLoginIsSatisfied()
    this.authenticationAlternativesShow()
  }

  private fun authenticationAlternativesMake() {
    if (this.viewModel.account.provider.authenticationAlternatives.isEmpty()) {
      this.authenticationAlternativesHide()
    } else {
      this.instantiateAlternativeAuthenticationViews()
      this.authenticationAlternativesShow()
    }
  }

  private fun authenticationAlternativesShow() {
    if (this.viewModel.account.provider.authenticationAlternatives.isNotEmpty()) {
      // this.authenticationAlternatives.visibility = VISIBLE
    }
  }

  private fun authenticationAlternativesHide() {
    // this.authenticationAlternatives.visibility = GONE
  }

  private fun tryLogin() {
    return when (val description = this.viewModel.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.SAML2_0 ->
        this.onTrySAML2Login(description)
      is AccountProviderAuthenticationDescription.OAuthWithIntermediary ->
        this.onTryOAuthWithIntermediaryLogin(description)
      is AccountProviderAuthenticationDescription.Basic ->
        this.onTryBasicLogin(description)
      is AccountProviderAuthenticationDescription.OAuthClientCredentials ->
        this.onTryOAuthClientCredentialsLogin(description)
      is AccountProviderAuthenticationDescription.Anonymous,
      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        throw UnreachableCodeException()
    }
  }
}
