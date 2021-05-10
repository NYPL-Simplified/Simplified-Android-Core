package org.nypl.simplified.ui.accounts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.io7m.junreachable.UnimplementedCodeException
import com.io7m.junreachable.UnreachableCodeException
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
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
import org.nypl.simplified.cardcreator.CardCreatorServiceType
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.oauth.OAuthCallbackIntentParsing
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.Basic
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryCancel
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryInitiate
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsCancelButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsCancelButtonEnabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLoginButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLoginButtonEnabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLogoutButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLogoutButtonEnabled
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A fragment that shows settings for a single account.
 */

class AccountDetailFragment : Fragment(R.layout.account) {

  private val logger =
    LoggerFactory.getLogger(AccountDetailFragment::class.java)

  private val subscriptions: CompositeDisposable =
    CompositeDisposable()

  private val listener: FragmentListenerType<org.nypl.simplified.ui.accounts.AccountDetailEvent> by fragmentListeners()

  private val parameters: AccountFragmentParameters by lazy {
    this.requireArguments()[PARAMETERS_ID] as AccountFragmentParameters
  }

  private val viewModel: AccountDetailViewModel by viewModels(
    factoryProducer = { AccountViewModelFactory(account = this.parameters.accountId) }
  )

  private lateinit var accountCustomOPDS: ViewGroup
  private lateinit var accountCustomOPDSField: TextView
  private lateinit var accountIcon: ImageView
  private lateinit var accountSubtitle: TextView
  private lateinit var accountTitle: TextView
  private lateinit var authentication: ViewGroup
  private lateinit var authenticationAlternatives: ViewGroup
  private lateinit var authenticationAlternativesButtons: ViewGroup
  private lateinit var authenticationViews: AccountAuthenticationViews
  private lateinit var bookmarkSync: ViewGroup
  private lateinit var bookmarkSyncCheck: SwitchCompat
  private lateinit var bookmarkSyncLabel: View
  private lateinit var eulaCheckbox: CheckBox
  private lateinit var imageLoader: ImageLoaderType
  private lateinit var loginProgress: ViewGroup
  private lateinit var loginButtonErrorDetails: Button
  private lateinit var loginProgressBar: ProgressBar
  private lateinit var loginProgressText: TextView
  private lateinit var loginTitle: ViewGroup
  private lateinit var reportIssueEmail: TextView
  private lateinit var reportIssueGroup: ViewGroup
  private lateinit var reportIssueItem: View
  private lateinit var settingsCardCreator: ConstraintLayout
  private lateinit var signUpButton: Button
  private lateinit var signUpLabel: TextView

  private val cardCreatorResultCode = 101
  private val closing = AtomicBoolean(false)
  private val imageButtonLoadingTag = "IMAGE_BUTTON_LOADING"
  private val nyplCardCreatorScheme = "nypl.card-creator"
  private var cardCreatorService: CardCreatorServiceType? = null

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.accounts.AccountFragment.parameters"

    /**
     * Create a new account fragment for the given parameters.
     */

    fun create(parameters: AccountFragmentParameters): AccountDetailFragment {
      val fragment = AccountDetailFragment()
      fragment.arguments = bundleOf(this.PARAMETERS_ID to parameters)
      return fragment
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val services = Services.serviceDirectory()

    this.cardCreatorService =
      services.optionalService(CardCreatorServiceType::class.java)
    this.imageLoader =
      services.requireService(ImageLoaderType::class.java)
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

    this.authenticationAlternatives =
      view.findViewById(R.id.accountAuthAlternatives)
    this.authenticationAlternativesButtons =
      view.findViewById(R.id.accountAuthAlternativesButtons)

    this.bookmarkSync =
      view.findViewById(R.id.accountSyncBookmarks)
    this.bookmarkSyncCheck =
      this.bookmarkSync.findViewById(R.id.accountSyncBookmarksCheck)
    this.bookmarkSyncLabel =
      this.bookmarkSync.findViewById(R.id.accountSyncBookmarksLabel)

    this.loginTitle =
      view.findViewById(R.id.accountTitleAnnounce)
    this.loginProgress =
      view.findViewById(R.id.accountLoginProgress)
    this.loginProgressBar =
      view.findViewById(R.id.accountLoginProgressBar)
    this.loginProgressText =
      view.findViewById(R.id.accountLoginProgressText)
    this.loginButtonErrorDetails =
      view.findViewById(R.id.accountLoginButtonErrorDetails)
    this.eulaCheckbox =
      view.findViewById(R.id.accountEULACheckbox)
    this.signUpButton =
      view.findViewById(R.id.accountCardCreatorSignUp)
    this.signUpLabel =
      view.findViewById(R.id.accountCardCreatorLabel)
    this.settingsCardCreator =
      view.findViewById(R.id.accountCardCreator)

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

    this.loginButtonErrorDetails.visibility = View.GONE
    this.loginProgressBar.visibility = View.INVISIBLE
    this.setLoginButtonStatus(AsLoginButtonDisabled)

    if (savedInstanceState == null) {
      this.loginProgressText.text = ""
    }

    if (this.parameters.showPleaseLogInTitle) {
      this.loginTitle.visibility = View.VISIBLE
    } else {
      this.loginTitle.visibility = View.GONE
    }

    /*
    * Only show a EULA checkbox if there's actually a EULA.
    */

    val eula = this.viewModel.eula
    if (eula != null) {
      this.eulaCheckbox.visibility = View.VISIBLE
      this.eulaCheckbox.isChecked = eula.hasAgreed
      this.eulaCheckbox.setOnCheckedChangeListener { _, checked ->
        eula.hasAgreed = checked
        this.setLoginButtonStatus(this.determineLoginIsSatisfied())
      }
    } else {
      this.eulaCheckbox.visibility = View.GONE
    }

    this.hideCardCreatorForNonNYPL()

    this.accountTitle.text =
      this.viewModel.account.provider.displayName
    this.accountSubtitle.text =
      this.viewModel.account.provider.subtitle

    /*
     * Conditionally enable sign up button
     */

    val signUpEnabled = this.shouldSignUpBeEnabled()
    this.signUpButton.isEnabled = signUpEnabled
    this.signUpLabel.isEnabled = signUpEnabled

    /*
     * Instantiate views for alternative authentication methods.
     */

    this.authenticationAlternativesMake()

    /*
     * Show/hide the custom OPDS feed section.
     */

    val catalogURIOverride = this.viewModel.account.preferences.catalogURIOverride
    this.accountCustomOPDSField.text = catalogURIOverride?.toString() ?: ""
    this.accountCustomOPDS.visibility =
      if (catalogURIOverride != null) {
        View.VISIBLE
      } else {
        View.GONE
      }

    this.reconfigureAccountUI()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    this.cancelImageButtonLoading()
  }

  private fun onBasicUserPasswordChanged(
    username: AccountUsername,
    password: AccountPassword
  ) {
    this.setLoginButtonStatus(this.determineLoginIsSatisfied())
  }

  private fun determineLoginIsSatisfied(): AccountLoginButtonStatus {
    val authDescription = this.viewModel.account.provider.authentication
    val eulaOk = this.determineEULAIsSatisfied()
    val loginPossible = authDescription.isLoginPossible
    val satisfiedFor = this.authenticationViews.isSatisfiedFor(authDescription)

    this.logger.debug("eula: {}, possible: {}, satisfied: {}", eulaOk, loginPossible, satisfiedFor)
    return if (eulaOk && loginPossible && satisfiedFor) {
      AsLoginButtonEnabled {
        this.loginFormLock()
        this.tryLogin()
      }
    } else {
      AsLoginButtonDisabled
    }
  }

  private fun determineEULAIsSatisfied(): Boolean {
    val eula = this.viewModel.eula
    return eula?.hasAgreed ?: true
  }

  private fun shouldSignUpBeEnabled(): Boolean {
    val cardCreatorURI = this.viewModel.account.provider.cardCreatorURI

    /*
     * If there's any card creator URI, the button should be enabled...
     */

    return if (cardCreatorURI != null) {
      /*
       * Unless the URI refers to the NYPL Card Creator and we don't have that enabled
       * in this build.
       */

      if (cardCreatorURI.scheme == this.nyplCardCreatorScheme) {
        return this.cardCreatorService != null
      }
      true
    } else {
      false
    }
  }

  private fun openCardCreator() {
    val cardCreator = this.cardCreatorService
    val cardCreatorURI = this.viewModel.account.provider.cardCreatorURI
    if (cardCreatorURI != null) {
      if (cardCreatorURI.scheme == this.nyplCardCreatorScheme) {
        if (cardCreator != null) {
          cardCreator.openCardCreatorActivity(
            this,
            this.activity,
            this.cardCreatorResultCode,
            this.viewModel.account.loginState is AccountLoggedIn,
            this.authenticationViews.getBasicUser().value.trim()
          )
        } else {
          // We rely on [shouldSignUpBeEnabled] to have disabled the button
          throw UnreachableCodeException()
        }
      } else {
        val webCardCreator = Intent(Intent.ACTION_VIEW, Uri.parse(cardCreatorURI.toString()))
        this.startActivity(webCardCreator)
      }
    }
  }

  override fun onStart() {
    super.onStart()

    this.configureToolbar(requireActivity())

    ImageAccountIcons.loadAccountLogoIntoView(
      this.imageLoader.loader,
      this.viewModel.account.provider.toDescription(),
      R.drawable.account_default,
      this.accountIcon
    )

    /*
     * Configure the COPPA age gate switch. If the user changes their age, a log out
     * is required.
     */

    this.authenticationViews.setCOPPAState(
      isOver13 = this.viewModel.isOver13,
      onAgeCheckboxClicked = this.onAgeCheckboxClicked()
    )

    /*
     * Launch Card Creator
     */

    this.signUpButton.setOnClickListener { this.openCardCreator() }

    /*
     * Configure the bookmark syncing switch to enable/disable syncing permissions.
     */

    this.bookmarkSyncCheck.setOnCheckedChangeListener { _, isChecked ->
      this.viewModel.bookmarkSyncingPermitted = isChecked
    }

    /*
     * Configure the "Report issue..." item.
     */

    this.configureReportIssue()

    this.viewModel.accountEvents
      .subscribe(this::onAccountEvent)
      .let { this.subscriptions.add(it) }

    this.viewModel.profileEvents
      .subscribe(this::onProfileEvent)
      .let { this.subscriptions.add(it) }
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

        is AccountProviderAuthenticationDescription.OAuthWithIntermediary -> {
          val layout =
            this.layoutInflater.inflate(
              R.layout.auth_oauth,
              this.authenticationAlternativesButtons,
              false
            )

          this.configureImageButton(
            container = layout.findViewById(R.id.authOAuthIntermediaryLogo),
            buttonText = layout.findViewById(R.id.authOAuthIntermediaryLogoText),
            buttonImage = layout.findViewById(R.id.authOAuthIntermediaryLogoImage),
            text = this.getString(R.string.accountLoginWith, alternative.description),
            logoURI = alternative.logoURI,
            onClick = {
              this.onTryOAuthLogin(alternative)
            }
          )
          this.authenticationAlternativesButtons.addView(layout)
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

      this.reportIssueGroup.visibility = View.VISIBLE
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
      this.reportIssueGroup.visibility = View.GONE
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
        buttonImage.visibility = View.VISIBLE
        buttonText.visibility = View.GONE
      }
    )
  }

  private fun onTrySAML2Login(
    authenticationDescription: AccountProviderAuthenticationDescription.SAML2_0
  ) {
    this.viewModel.loginExplicitlyRequested = true
    this.viewModel.tryLogin(
      ProfileAccountLoginRequest.SAML20Initiate(
        accountId = this.viewModel.account.id,
        description = authenticationDescription
      )
    )

    this.listener.post(
      AccountDetailEvent.OpenSAML20Login(this.viewModel.account.id, authenticationDescription)
    )
  }

  private fun onTryOAuthLogin(
    authenticationDescription: AccountProviderAuthenticationDescription.OAuthWithIntermediary
  ) {
    this.viewModel.loginExplicitlyRequested = true
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

  private fun explicitlyClose() {
    if (this.closing.compareAndSet(false, true)) {
      this.logger.debug("explicitlyClose: popping backstack")
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
    this.authenticationViews.clear()
    this.subscriptions.clear()
  }

  private fun onProfileEvent(event: ProfileEvent) {
    return when (event) {
      is ProfileUpdated -> {
        this.reconfigureAccountUI()
      }
      else -> {
      }
    }
  }

  private fun reconfigureAccountUI() {
    val isPermitted = this.viewModel.account.preferences.bookmarkSyncingPermitted
    val isSupported = this.viewModel.account.provider.supportsSimplyESynchronization
    this.bookmarkSyncCheck.isChecked = isPermitted
    this.bookmarkSyncCheck.isEnabled = isSupported
    this.bookmarkSyncLabel.isEnabled = isSupported

    this.authenticationViews.showFor(this.viewModel.account.provider.authentication)

    return when (val loginState = this.viewModel.account.loginState) {
      AccountNotLoggedIn -> {
        this.authenticationViews.setBasicUserAndPass("", "")

        this.loginProgress.visibility = View.GONE
        this.setLoginButtonStatus(
          AsLoginButtonEnabled {
            this.loginFormLock()
            this.tryLogin()
          }
        )
        this.loginFormUnlock()
      }

      is AccountLoggingIn -> {
        this.loginProgress.visibility = View.VISIBLE
        this.loginProgressBar.visibility = View.VISIBLE
        this.loginProgressText.text = loginState.status
        this.loginButtonErrorDetails.visibility = View.GONE
        this.loginFormLock()

        if (loginState.cancellable) {
          this.setLoginButtonStatus(
            AsCancelButtonEnabled {
              // We don't really support this yet.
              throw UnimplementedCodeException()
            }
          )
        } else {
          this.setLoginButtonStatus(AsCancelButtonDisabled)
        }
      }

      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.loginProgress.visibility = View.VISIBLE
        this.loginProgressBar.visibility = View.VISIBLE
        this.loginProgressText.text = loginState.status
        this.loginButtonErrorDetails.visibility = View.GONE
        this.loginFormLock()
        this.setLoginButtonStatus(
          AsCancelButtonEnabled {
            this.viewModel.tryLogin(
              OAuthWithIntermediaryCancel(
                accountId = this.viewModel.account.id,
                description = loginState.description as AccountProviderAuthenticationDescription.OAuthWithIntermediary
              )
            )
          }
        )
      }

      is AccountLoginFailed -> {
        this.loginProgress.visibility = View.VISIBLE
        this.loginProgressBar.visibility = View.GONE
        this.loginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.loginFormUnlock()
        this.cancelImageButtonLoading()
        this.setLoginButtonStatus(
          AsLoginButtonEnabled {
            this.loginFormLock()
            this.tryLogin()
          }
        )
        this.loginButtonErrorDetails.visibility = View.VISIBLE
        this.loginButtonErrorDetails.setOnClickListener {
          this.openErrorPage(loginState.taskResult.steps)
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

        this.loginProgress.visibility = View.GONE
        this.loginFormLock()
        this.loginButtonErrorDetails.visibility = View.GONE
        this.setLoginButtonStatus(
          AsLogoutButtonEnabled {
            this.loginFormLock()
            this.tryLogout()
          }
        )
        this.authenticationAlternativesHide()

        if (this.viewModel.loginExplicitlyRequested) {
          this.logger.debug("scheduling explicit close of account fragment")
          this.listener.post(AccountDetailEvent.LoginSucceeded)
        } else {
          // Doing nothing.
        }
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

        this.loginProgress.visibility = View.VISIBLE
        this.loginButtonErrorDetails.visibility = View.GONE
        this.loginProgressBar.visibility = View.VISIBLE
        this.loginProgressText.text = loginState.status
        this.loginFormLock()
        this.setLoginButtonStatus(AsLogoutButtonDisabled)
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

        this.loginProgress.visibility = View.VISIBLE
        this.loginProgressBar.visibility = View.GONE
        this.loginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.cancelImageButtonLoading()
        this.loginFormLock()
        this.setLoginButtonStatus(
          AsLogoutButtonEnabled {
            this.loginFormLock()
            this.tryLogout()
          }
        )

        this.loginButtonErrorDetails.visibility = View.VISIBLE
        this.loginButtonErrorDetails.setOnClickListener {
          this.openErrorPage(loginState.taskResult.steps)
        }
      }
    }
  }

  private fun cancelImageButtonLoading() {
    this.imageLoader.loader.cancelTag(this.imageButtonLoadingTag)
  }

  private fun setLoginButtonStatus(
    status: AccountLoginButtonStatus
  ) {
    this.authenticationViews.setLoginButtonStatus(status)

    return when (status) {
      is AsLoginButtonEnabled -> {
        this.signUpLabel.setText(R.string.accountCardCreatorLabel)
      }
      is AsLoginButtonDisabled -> {
        this.signUpLabel.setText(R.string.accountCardCreatorLabel)
        this.signUpLabel.isEnabled = true
      }
      is AsLogoutButtonEnabled -> {
        this.signUpLabel.setText(R.string.accountWantChildCard)
        this.signUpLabel.isEnabled = isNypl()
        this.signUpButton.isEnabled = isNypl()
        if (isNypl()) {
          this.signUpLabel.setText(R.string.accountWantChildCard)
        } else {
          this.signUpLabel.setText(R.string.accountCardCreatorLabel)
        }
      }
      is AsLogoutButtonDisabled -> {
        if (isNypl()) {
          this.signUpLabel.setText(R.string.accountWantChildCard)
        } else {
          this.signUpLabel.setText(R.string.accountCardCreatorLabel)
        }
      }
      is AsCancelButtonEnabled,
      AsCancelButtonDisabled -> {
        // Nothing
      }
    }
  }

  /**
   * Returns if the user is viewing the NYPL account
   */
  private fun isNypl(): Boolean {
    var isNypl = false
    val cardCreatorURI = this.viewModel.account.provider.cardCreatorURI
    if (cardCreatorURI != null) {
      isNypl = cardCreatorURI.scheme == this.nyplCardCreatorScheme
    }
    return isNypl
  }

  private fun loadAuthenticationLogoIfNecessary(
    uri: URI?,
    view: ImageView,
    onSuccess: () -> Unit
  ) {
    if (uri != null) {
      view.setImageDrawable(null)
      view.visibility = View.VISIBLE
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
              this@AccountDetailFragment.logger.error("failed to load authentication logo: ", e)
              view.visibility = View.GONE
            }
          }
        )
    }
  }

  private fun openErrorPage(taskSteps: List<TaskStep>) {
    val parameters =
      ErrorPageParameters(
        emailAddress = this.viewModel.buildConfig.supportErrorReportEmailAddress,
        body = "",
        subject = "[simplye-error-report]",
        attributes = sortedMapOf(),
        taskSteps = taskSteps
      )

    this.listener.post(AccountDetailEvent.OpenErrorPage(parameters))
  }

  private fun loginFormLock() {
    this.authenticationViews.setCOPPAState(
      isOver13 = this.viewModel.isOver13,
      onAgeCheckboxClicked = this.onAgeCheckboxClicked()
    )

    this.authenticationViews.lock()
    this.eulaCheckbox.isEnabled = false

    this.setLoginButtonStatus(AsLoginButtonDisabled)
    this.authenticationAlternativesHide()
  }

  private fun loginFormUnlock() {
    this.authenticationViews.setCOPPAState(
      isOver13 = this.viewModel.isOver13,
      onAgeCheckboxClicked = this.onAgeCheckboxClicked()
    )

    this.authenticationViews.unlock()
    this.eulaCheckbox.isEnabled = true

    val loginSatisfied = this.determineLoginIsSatisfied()
    this.setLoginButtonStatus(loginSatisfied)
    this.authenticationAlternativesShow()
    this.signUpButton.isEnabled = true
    this.signUpLabel.isEnabled = true
  }

  private fun authenticationAlternativesMake() {
    this.authenticationAlternativesButtons.removeAllViews()
    if (this.viewModel.account.provider.authenticationAlternatives.isEmpty()) {
      this.authenticationAlternativesHide()
    } else {
      this.instantiateAlternativeAuthenticationViews()
      this.authenticationAlternativesShow()
    }
  }

  private fun authenticationAlternativesShow() {
    if (this.viewModel.account.provider.authenticationAlternatives.isNotEmpty()) {
      this.authenticationAlternatives.visibility = View.VISIBLE
    }
  }

  private fun authenticationAlternativesHide() {
    this.authenticationAlternatives.visibility = View.GONE
  }

  private fun onAccountEvent(accountEvent: AccountEvent) {
    return when (accountEvent) {
      is AccountEventLoginStateChanged ->
        if (accountEvent.accountID == this.parameters.accountId) {
          this.reconfigureAccountUI()
        } else {
          // Don't care about events for other accounts
        }
      else -> {
        // Don't care about other events
      }
    }
  }

  private fun tryLogin() {
    this.viewModel.loginExplicitlyRequested = true

    return when (val description = this.viewModel.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.SAML2_0 ->
        this.onTrySAML2Login(description)
      is AccountProviderAuthenticationDescription.OAuthWithIntermediary ->
        this.onTryOAuthLogin(description)
      is AccountProviderAuthenticationDescription.Basic ->
        this.onTryBasicLogin(description)

      is AccountProviderAuthenticationDescription.Anonymous,
      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        throw UnreachableCodeException()
    }
  }

  private fun tryLogout() {
    return when (this.viewModel.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.Anonymous ->
        Unit

      /*
       * Although COPPA age-gated accounts don't require "logging in" or "logging out" exactly,
       * we *do* want local books to be deleted as part of a logout attempt.
       */

      is AccountProviderAuthenticationDescription.SAML2_0,
      is AccountProviderAuthenticationDescription.OAuthWithIntermediary,
      is AccountProviderAuthenticationDescription.COPPAAgeGate,
      is AccountProviderAuthenticationDescription.Basic -> {
        this.viewModel.tryLogout(this.viewModel.account.id)
      }
    }
  }

  /**
   * Hides or show sign up options if is user in accessing the NYPL
   */
  private fun hideCardCreatorForNonNYPL() {
    if (this.viewModel.account.provider.cardCreatorURI != null) {
      this.settingsCardCreator.visibility = View.VISIBLE
    }
  }

  /**
   * A click listener for the age checkbox. If the user wants to change their age, then
   * this must trigger an account logout.
   */

  private fun onAgeCheckboxClicked(): (View) -> Unit = {
    val isOver13 = this.viewModel.isOver13
    AlertDialog.Builder(this.requireContext())
      .setTitle(R.string.accountCOPPADeleteBooks)
      .setMessage(R.string.accountCOPPADeleteBooksConfirm)
      .setNegativeButton(R.string.accountCancel) { _, _ ->
        this.authenticationViews.setCOPPAState(
          isOver13 = isOver13,
          onAgeCheckboxClicked = this.onAgeCheckboxClicked()
        )
      }
      .setPositiveButton(R.string.accountDelete) { _, _ ->
        this.loginFormLock()
        this.viewModel.isOver13 = !isOver13
        this.tryLogout()
      }
      .create()
      .show()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == this.cardCreatorResultCode) {
      when (resultCode) {
        Activity.RESULT_OK -> {
          if (data != null) {
            val barcode = data.getStringExtra("barcode")
            val pin = data.getStringExtra("pin")

            this.authenticationViews.setBasicUserAndPass(
              user = barcode,
              password = pin
            )
            this.tryLogin()
          }
        }
        Activity.RESULT_CANCELED -> {
          this.logger.debug("User has exited the card creator")
        }
      }
    }
  }
}
