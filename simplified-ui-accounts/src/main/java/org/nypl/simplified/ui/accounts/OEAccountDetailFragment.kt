package org.nypl.simplified.ui.accounts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.Basic
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryInitiate
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableResult.SYNC_DISABLED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableResult.SYNC_ENABLED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableResult.SYNC_ENABLE_NOT_SUPPORTED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableStatus
import org.slf4j.LoggerFactory
import java.net.URI
import org.nypl.simplified.ui.accounts.utils.hideSoftInput
import androidx.core.widget.doOnTextChanged
import org.nypl.simplified.android.ktx.viewLifecycleAware
import org.nypl.simplified.ui.accounts.databinding.OeAccountBinding

/**
 * A fragment that shows settings for a single account.
 */

class OEAccountDetailFragment : Fragment() {

  private var binding by viewLifecycleAware<OeAccountBinding>()

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

  private val firstBookAccessCodeMin = 10
  private val firstBookPinMin = 4

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

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = OeAccountBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.viewModel.accountLive.observe(this.viewLifecycleOwner) {
      this.reconfigureOEUI()
    }

    this.viewModel.accountSyncingSwitchStatus.observe(this.viewLifecycleOwner) { status ->
      this.reconfigureBookmarkSyncingSwitch(status)
    }

    (activity as AppCompatActivity).let {
      it.supportActionBar?.hide()
    }

    binding.oeLoginLanding.terms.setOnClickListener {
      launchWebView(WebViewActivity.TERMS_OF_USE)
    }

    binding.oeLoginLanding.privacyNotice.setOnClickListener {
      launchWebView(WebViewActivity.PRIVACY_NOTICE)
    }

    binding.oeLoginLanding.firstBookBtn.setOnClickListener {
      binding.oeLoginLanding.oeLogin.visibility = GONE
      binding.firstBookLogin.firstBookLoginCl.visibility = VISIBLE
      binding.firstBookLogin.firstBookLoginProgressBar.visibility = GONE
    }

    binding.firstBookLogin.firstBookBack.setOnClickListener {
      hideSoftInput()
      binding.oeLoginLanding.oeLogin.visibility = VISIBLE
      binding.firstBookLogin.firstBookLoginCl.visibility = GONE
      binding.firstBookLogin.oeAccountLoginProgress.visibility = GONE
    }

    binding.firstBookLogin.loginTrouble.setOnClickListener {
      launchWebView(WebViewActivity.LOGIN_TROUBLE)
    }

    binding.firstBookLogin.faq.setOnClickListener {
      launchWebView(WebViewActivity.FAQ)
    }

    binding.firstBookLogin.signIn.setOnClickListener {
      if (binding.firstBookLogin.signIn.text == getString(R.string.signIn)) {
        logger.debug("Logging into First Book")
        binding.firstBookLogin.oeAccountLoginProgress.visibility = GONE
        hideSoftInput()
        val accountUsername = AccountUsername(binding.firstBookLogin.etAccessCode.text.toString().trim())
        val accountPassword = AccountPassword(binding.firstBookLogin.etPin.text.toString().trim())
        val description = AccountProviderAuthenticationDescription.Basic(
          description = "First Book",
          formDescription = AccountProviderAuthenticationDescription.FormDescription(
            barcodeFormat = null,
            keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
            passwordMaximumLength = 0,
            passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
            labels = linkedMapOf(
              "LOGIN" to "First Book Access Code",
              "PASSWORD" to "First Book PIN"
            )
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
      } else {
        logger.debug("Logging out of First Book")
        this.viewModel.tryLogout()
      }
    }

    binding.firstBookLogin.etPin.doOnTextChanged { _, _, _, _ ->
      updateLoginButton()
    }

    binding.firstBookLogin.etAccessCode.doOnTextChanged { _, _, _, _ ->
      updateLoginButton()
    }
    reconfigureOEUI()

    val cleverAuth = this.viewModel.account.provider.authenticationAlternatives.filterIsInstance<AccountProviderAuthenticationDescription.OAuthWithIntermediary>().first()
    cleverAuth.let { provider ->
      binding.oeLoginLanding.clever.setOnClickListener {
        this.onTryOAuthWithIntermediaryLogin(provider)
      }
    }

    binding.firstBookLogin.supportEmail.setOnClickListener {
      val emailIntent =
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getString(R.string.supportEmail), null))
      val chosenIntent =
        Intent.createChooser(emailIntent, this.resources.getString(R.string.accountReportIssue))

      try {
        this.startActivity(chosenIntent)
      } catch (e: Exception) {
        this.logger.error("unable to start activity: ", e)
        val context = this.requireContext()
        AlertDialog.Builder(context)
          .setMessage(context.getString(R.string.accountReportFailed, getString(R.string.supportEmail)))
          .create()
          .show()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    (activity as AppCompatActivity).let {
      it.supportActionBar?.hide()
    }
  }

  /**
   * Locks sign in form and shows bookmark & server buttons
   */
  private fun showLoggedInUI() {
    binding.firstBookLogin.signIn.isEnabled = true
    binding.firstBookLogin.signIn.text = getString(R.string.signOut)
    binding.firstBookLogin.etAccessCode.isEnabled = false
    binding.firstBookLogin.etPin.isEnabled = false
    binding.firstBookLogin.firstBookHeader.text = getString(R.string.settings)
    binding.firstBookLogin.firstBookLogo.visibility = GONE
    binding.firstBookLogin.loginTrouble.visibility = INVISIBLE
    binding.firstBookLogin.faq.visibility = INVISIBLE
    binding.firstBookLogin.loggedIn.visibility = VISIBLE
    binding.oeLoginLanding.oeLogin.visibility = GONE
    binding.firstBookLogin.firstBookLoginCl.visibility = VISIBLE
    binding.firstBookLogin.firstBookBack.visibility = GONE
    binding.firstBookLogin.firstBookLoginProgressBar.visibility = GONE
  }

  /**
   * Unlocks sign in form and hides bookmark & server buttons
   */
  private fun showLoggedOutUI() {
    binding.firstBookLogin.firstBookHeader.text = getString(R.string.signInHeader)
    binding.firstBookLogin.firstBookLogo.visibility = VISIBLE
    binding.firstBookLogin.loginTrouble.visibility = VISIBLE
    binding.firstBookLogin.faq.visibility = VISIBLE
    binding.firstBookLogin.loggedIn.visibility = INVISIBLE
    binding.firstBookLogin.signIn.text = getString(R.string.signIn)
    binding.firstBookLogin.etAccessCode.isEnabled = true
    binding.firstBookLogin.etPin.isEnabled = true
    binding.oeLoginLanding.oeLogin.visibility = VISIBLE
    binding.firstBookLogin.firstBookLoginCl.visibility = GONE
    binding.firstBookLogin.firstBookBack.visibility = VISIBLE
    binding.firstBookLogin.firstBookLoginProgressBar.visibility = GONE
  }

  /**
   * Enable/Disable login button if form is valid
   */
  private fun updateLoginButton() {
    binding.firstBookLogin.signIn.isEnabled = binding.firstBookLogin.etAccessCode.text.toString().trim().length == firstBookAccessCodeMin &&
      binding.firstBookLogin.etPin.text.toString().trim().length == firstBookPinMin
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

    binding.firstBookLogin.bookmarkSync.setOnCheckedChangeListener(null)

    /*
     * Otherwise, the switch is doing something that interests us...
     */

    val account = this.viewModel.account
    return when (status) {
      is ReaderBookmarkSyncEnableStatus.Changing -> {
        binding.firstBookLogin.bookmarkProgress.visibility = VISIBLE
        binding.firstBookLogin.bookmarkSync.isEnabled = false
      }

      is ReaderBookmarkSyncEnableStatus.Idle -> {
        binding.firstBookLogin.bookmarkProgress.visibility = INVISIBLE

        when (status.status) {
          SYNC_ENABLE_NOT_SUPPORTED -> {
            binding.firstBookLogin.bookmarkSync.isChecked = false
            binding.firstBookLogin.bookmarkSync.isEnabled = false
          }

          SYNC_ENABLED,
          SYNC_DISABLED -> {
            val isPermitted = account.preferences.bookmarkSyncingPermitted
            val isSupported = account.loginState.credentials?.annotationsURI != null

            binding.firstBookLogin.bookmarkSync.isChecked = isPermitted
            binding.firstBookLogin.bookmarkSync.isEnabled = isSupported

            binding.firstBookLogin.bookmarkSync.setOnCheckedChangeListener { _, isChecked ->
              this.viewModel.enableBookmarkSyncing(isChecked)
            }
          }
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()

    this.configureToolbar(requireActivity())

    /*
     * Configure the bookmark syncing switch to enable/disable syncing permissions.
     */

    binding.firstBookLogin.bookmarkSync.setOnCheckedChangeListener { _, isChecked ->
      this.viewModel.enableBookmarkSyncing(isChecked)
    }
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
    val accountPassword = AccountPassword(binding.firstBookLogin.etPin.text.toString().trim())
    val accountUsername = AccountUsername(binding.firstBookLogin.etAccessCode.text.toString().trim())
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
    val accountPassword = AccountPassword(binding.firstBookLogin.etPin.text.toString().trim())
    val accountUsername = AccountUsername(binding.firstBookLogin.etAccessCode.text.toString().trim())

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

    this.subscriptions.clear()
  }

  private fun reconfigureOEUI() {
    this.disableSyncSwitchForLoginState(this.viewModel.account.loginState)

    return when (val loginState = this.viewModel.account.loginState) {
      AccountNotLoggedIn -> {
        binding.firstBookLogin.oeAccountLoginProgress.visibility = GONE

        if (this.viewModel.pendingLogout) {
          setBasicUserAndPass("", "")
          this.viewModel.pendingLogout = false
        }
        this.loginFormUnlock()
        showLoggedOutUI()
      }

      is AccountLoggingIn -> {
        binding.firstBookLogin.oeAccountLoginProgress.visibility = VISIBLE
        binding.firstBookLogin.firstBookLoginProgressBar.visibility = VISIBLE
        binding.firstBookLogin.oeLoginProgressText.text = loginState.status
        binding.firstBookLogin.oeLoginButtonErrorDetails.visibility = GONE
        this.loginFormLock()
      }

      is AccountLoggingInWaitingForExternalAuthentication -> {
        binding.firstBookLogin.oeAccountLoginProgress.visibility = VISIBLE
        binding.firstBookLogin.firstBookLoginProgressBar.visibility = VISIBLE
        binding.firstBookLogin.oeLoginProgressText.text = loginState.status
        binding.firstBookLogin.oeLoginButtonErrorDetails.visibility = GONE
        this.loginFormLock()
      }

      is AccountLoginFailed -> {
        binding.firstBookLogin.oeAccountLoginProgress.visibility = VISIBLE
        binding.firstBookLogin.firstBookLoginProgressBar.visibility = GONE
        binding.firstBookLogin.oeLoginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.loginFormUnlock()
        binding.firstBookLogin.oeLoginButtonErrorDetails.visibility = VISIBLE
        binding.firstBookLogin.oeLoginButtonErrorDetails.setOnClickListener {
          this.viewModel.openErrorPage(loginState.taskResult.steps)
        }
      }

      is AccountLoggedIn -> {
        when (val creds = loginState.credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            setBasicUserAndPass(
              user = creds.userName.value,
              password = creds.password.value
            )
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
            // Nothing
          }
        }

        binding.firstBookLogin.oeAccountLoginProgress.visibility = GONE
        this.loginFormLock()
        binding.firstBookLogin.oeLoginButtonErrorDetails.visibility = GONE
        showLoggedInUI()
      }

      is AccountLoggingOut -> {
        when (val creds = loginState.credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            setBasicUserAndPass(
              user = creds.userName.value,
              password = creds.password.value
            )
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
            // No UI
          }
        }

        binding.firstBookLogin.oeAccountLoginProgress.visibility = VISIBLE
        binding.firstBookLogin.oeLoginButtonErrorDetails.visibility = GONE
        binding.firstBookLogin.firstBookLoginProgressBar.visibility = VISIBLE
        binding.firstBookLogin.oeLoginProgressText.text = loginState.status
        binding.firstBookLogin.loggedIn.visibility = GONE
        this.loginFormLock()
      }

      is AccountLogoutFailed -> {
        when (val creds = loginState.credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            setBasicUserAndPass(
              user = creds.userName.value,
              password = creds.password.value
            )
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
            // No UI
          }
        }

        binding.firstBookLogin.oeAccountLoginProgress.visibility = VISIBLE
        binding.firstBookLogin.firstBookLoginProgressBar.visibility = GONE
        // binding.firstBookLogin.oeLoginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.loginFormLock()

        binding.firstBookLogin.oeLoginButtonErrorDetails.visibility = VISIBLE
        binding.firstBookLogin.oeLoginButtonErrorDetails.setOnClickListener {
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
        binding.firstBookLogin.bookmarkSync.setOnCheckedChangeListener(null)
        binding.firstBookLogin.bookmarkSync.isChecked = false
        binding.firstBookLogin.bookmarkSync.isEnabled = false
      }
    }
  }

  /**
   * Disables form EditTexts and buttons
   */
  fun loginFormLock() {
    binding.firstBookLogin.etAccessCode.isEnabled = false
    binding.firstBookLogin.etPin.isEnabled = false
  }

  /**
   * Enables form EditTexts and buttons
   */
  fun loginFormUnlock() {
    binding.firstBookLogin.etPin.isEnabled = true
    binding.firstBookLogin.etPin.isEnabled = true
  }

  /**
   * Sets the username and password text in the input form fields
   */
  fun setBasicUserAndPass(user: String, password: String) {
    binding.firstBookLogin.etAccessCode.setText(user)
    binding.firstBookLogin.etPin.setText(password)
  }
}
