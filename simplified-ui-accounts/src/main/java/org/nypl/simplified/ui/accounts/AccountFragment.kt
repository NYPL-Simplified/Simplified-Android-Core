package org.nypl.simplified.ui.accounts

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnimplementedCodeException
import com.io7m.junreachable.UnreachableCodeException
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.cardcreator.CardCreatorServiceType
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.oauth.OAuthCallbackIntentParsing
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.Basic
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryCancel
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest.OAuthWithIntermediaryInitiate
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.threads.NamedThreadPools
import org.nypl.simplified.ui.accounts.AccountFragment.LoginButtonStatus.AsCancelButtonDisabled
import org.nypl.simplified.ui.accounts.AccountFragment.LoginButtonStatus.AsCancelButtonEnabled
import org.nypl.simplified.ui.accounts.AccountFragment.LoginButtonStatus.AsLoginButtonDisabled
import org.nypl.simplified.ui.accounts.AccountFragment.LoginButtonStatus.AsLoginButtonEnabled
import org.nypl.simplified.ui.accounts.AccountFragment.LoginButtonStatus.AsLogoutButtonDisabled
import org.nypl.simplified.ui.accounts.AccountFragment.LoginButtonStatus.AsLogoutButtonEnabled
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A fragment that shows settings for a single account.
 */

class AccountFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(AccountFragment::class.java)

  private lateinit var account: AccountType
  private lateinit var accountIcon: ImageView
  private lateinit var accountSubtitle: TextView
  private lateinit var accountTitle: TextView
  private lateinit var authentication: ViewGroup
  private lateinit var authenticationAlternatives: ViewGroup
  private lateinit var authenticationAlternativesButtons: ViewGroup
  private lateinit var authenticationBasic: ViewGroup
  private lateinit var authenticationBasicLogo: Button
  private lateinit var authenticationBasicPass: EditText
  private lateinit var authenticationBasicPassLabel: TextView
  private lateinit var authenticationBasicPassListener: OnTextChangeListener
  private lateinit var authenticationBasicShowPass: CheckBox
  private lateinit var authenticationBasicUser: EditText
  private lateinit var authenticationBasicUserLabel: TextView
  private lateinit var authenticationBasicUserListener: OnTextChangeListener
  private lateinit var authenticationCOPPA: ViewGroup
  private lateinit var authenticationCOPPAOver13: Switch
  private lateinit var backgroundExecutor: ListeningScheduledExecutorService
  private lateinit var bookmarkSync: ViewGroup
  private lateinit var bookmarkSyncCheck: Switch
  private lateinit var bookmarkSyncLabel: View
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var documents: DocumentStoreType
  private lateinit var eulaCheckbox: CheckBox
  private lateinit var imageLoader: ImageLoaderType
  private lateinit var login: ViewGroup
  private lateinit var loginButton: Button
  private lateinit var loginButtonErrorDetails: Button
  private lateinit var loginProgress: ProgressBar
  private lateinit var loginProgressText: TextView
  private lateinit var loginTitle: ViewGroup
  private lateinit var parameters: AccountFragmentParameters
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var settingsCardCreator: ConstraintLayout
  private lateinit var signUpButton: Button
  private lateinit var signUpLabel: TextView
  private lateinit var uiThread: UIThreadServiceType
  private lateinit var viewModel: AccountFragmentViewModel
  private val cardCreatorResultCode = 101
  private val closing = AtomicBoolean(false)
  private val imageButtonLoadingTag = "IMAGE_BUTTON_LOADING"
  private var accountSubscription: Disposable? = null
  private var cardCreatorService: CardCreatorServiceType? = null
  private var profileSubscription: Disposable? = null
  private val nyplCardCreatorScheme = "nypl.card-creator"

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.accounts.AccountFragment.parameters"

    /**
     * Create a new account fragment for the given parameters.
     */

    fun create(parameters: AccountFragmentParameters): AccountFragment {
      val arguments = Bundle()
      arguments.putSerializable(PARAMETERS_ID, parameters)
      val fragment = AccountFragment()
      fragment.arguments = arguments
      return fragment
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.parameters = this.requireArguments()[PARAMETERS_ID] as AccountFragmentParameters

    val services = Services.serviceDirectory()

    this.cardCreatorService =
      services.optionalService(CardCreatorServiceType::class.java)
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.documents =
      services.requireService(DocumentStoreType::class.java)
    this.imageLoader =
      services.requireService(ImageLoaderType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)

    this.viewModel =
      ViewModelProviders.of(this)
        .get(AccountFragmentViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.account, container, false)

    this.accountTitle =
      layout.findViewById(R.id.accountCellTitle)
    this.accountSubtitle =
      layout.findViewById(R.id.accountCellSubtitle)
    this.accountIcon =
      layout.findViewById(R.id.accountCellIcon)

    this.authentication =
      layout.findViewById(R.id.auth)
    this.authenticationAlternatives =
      layout.findViewById(R.id.accountAuthAlternatives)
    this.authenticationAlternativesButtons =
      layout.findViewById(R.id.accountAuthAlternativesButtons)

    this.authenticationCOPPA =
      this.authentication.findViewById(R.id.authCOPPA)
    this.authenticationCOPPAOver13 =
      this.authenticationCOPPA.findViewById(R.id.authCOPPASwitch)

    this.authenticationBasic =
      this.authentication.findViewById(R.id.authBasic)
    this.authenticationBasicLogo =
      this.authenticationBasic.findViewById(R.id.authBasicLogo)
    this.authenticationBasicUser =
      this.authenticationBasic.findViewById(R.id.authBasicUserNameField)
    this.authenticationBasicUserLabel =
      this.authenticationBasic.findViewById(R.id.authBasicUserNameLabel)
    this.authenticationBasicUserListener =
      OnTextChangeListener(this::onBasicUserChanged)
    this.authenticationBasicPass =
      this.authenticationBasic.findViewById(R.id.authBasicPasswordField)
    this.authenticationBasicPassLabel =
      this.authenticationBasic.findViewById(R.id.authBasicPasswordLabel)
    this.authenticationBasicPassListener =
      OnTextChangeListener(this::onBasicPasswordChanged)
    this.authenticationBasicShowPass =
      this.authenticationBasic.findViewById(R.id.authBasicPasswordShow)

    this.authenticationCOPPA.visibility = View.INVISIBLE

    this.bookmarkSync =
      layout.findViewById(R.id.accountSyncBookmarks)
    this.bookmarkSyncCheck =
      this.bookmarkSync.findViewById(R.id.accountSyncBookmarksCheck)
    this.bookmarkSyncLabel =
      this.bookmarkSync.findViewById(R.id.accountSyncBookmarksLabel)

    this.loginTitle =
      layout.findViewById(R.id.accountTitleAnnounce)
    this.login =
      layout.findViewById(R.id.accountLogin)
    this.loginProgress =
      layout.findViewById(R.id.accountLoginProgress)
    this.loginProgressText =
      layout.findViewById(R.id.accountLoginProgressText)
    this.loginButton =
      layout.findViewById(R.id.accountLoginButton)
    this.loginButtonErrorDetails =
      layout.findViewById(R.id.accountLoginButtonErrorDetails)
    this.eulaCheckbox =
      layout.findViewById(R.id.accountEULACheckbox)
    this.signUpButton =
      layout.findViewById(R.id.accountCardCreatorSignUp)
    this.signUpLabel =
      layout.findViewById(R.id.accountCardCreatorLabel)
    this.settingsCardCreator =
      layout.findViewById(R.id.accountCardCreator)

    this.loginButtonErrorDetails.visibility = View.GONE
    this.loginProgress.visibility = View.INVISIBLE
    this.loginProgressText.text = ""
    this.setLoginButtonStatus(AsLoginButtonDisabled)

    if (this.parameters.showPleaseLogInTitle) {
      this.loginTitle.visibility = View.VISIBLE
    } else {
      this.loginTitle.visibility = View.GONE
    }
    return layout
  }

  @Suppress("UNUSED_PARAMETER")
  @UiThread
  private fun onBasicUserChanged(
    sequence: CharSequence,
    start: Int,
    before: Int,
    count: Int
  ) {
    this.uiThread.checkIsUIThread()
    this.setLoginButtonStatus(this.determineLoginIsSatisfied())
  }

  @UiThread
  private fun determineLoginIsSatisfied(): LoginButtonStatus {
    return when (val auth = this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.Anonymous,
      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        AsLoginButtonDisabled

      is AccountProviderAuthenticationDescription.OAuthWithIntermediary -> {
        if (this.determineEULAIsSatisfied()) {
          AsLoginButtonEnabled {
            this.loginFormLock()
            this.tryLogin()
          }
        } else {
          AsLoginButtonDisabled
        }
      }

      is AccountProviderAuthenticationDescription.Basic -> {
        val eulaOk =
          this.determineEULAIsSatisfied()
        val noUserRequired =
          auth.keyboard == KeyboardInput.NO_INPUT
        val noPasswordRequired =
          auth.passwordKeyboard == KeyboardInput.NO_INPUT
        val userOk =
          this.authenticationBasicUser.text.isNotBlank() || noUserRequired
        val passOk =
          this.authenticationBasicPass.text.isNotBlank() || noPasswordRequired

        this.logger.debug("login: eula ok: {}, user ok: {}, pass ok: {}", eulaOk, userOk, passOk)
        if (userOk && passOk && eulaOk) {
          AsLoginButtonEnabled {
            this.loginFormLock()
            this.tryLogin()
          }
        } else {
          AsLoginButtonDisabled
        }
      }
    }
  }

  private fun determineEULAIsSatisfied(): Boolean {
    val eulaOpt = this.documents.eula
    return if (eulaOpt is Some<EULAType>) {
      eulaOpt.get().eulaHasAgreed()
    } else {
      true
    }
  }

  @Suppress("UNUSED_PARAMETER")
  @UiThread
  private fun onBasicPasswordChanged(
    sequence: CharSequence,
    start: Int,
    before: Int,
    count: Int
  ) {
    this.uiThread.checkIsUIThread()
    this.setLoginButtonStatus(this.determineLoginIsSatisfied())
  }

  private fun shouldSignUpBeEnabled(): Boolean {
    val cardCreatorURI = this.account.provider.cardCreatorURI

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
    val cardCreatorURI = this.account.provider.cardCreatorURI
    if (cardCreatorURI != null) {
      if (cardCreatorURI.scheme == this.nyplCardCreatorScheme) {
        if (cardCreator != null) {
          cardCreator.openCardCreatorActivity(
            this,
            this.activity,
            this.cardCreatorResultCode,
            this.account.loginState is AccountLoggedIn,
            this.authenticationBasicUser.text.toString().trim()
          )
        } else {
          // We rely on [shouldSignUpBeEnabled] to have disabled the button
          throw UnreachableCodeException()
        }
      } else {
        val webCardCreator = Intent(Intent.ACTION_VIEW, Uri.parse(cardCreatorURI.toString()))
        startActivity(webCardCreator)
      }
    }
  }

  override fun onStart() {
    super.onStart()

    this.backgroundExecutor =
      NamedThreadPools.namedThreadPool(1, "simplified-accounts-io", 19)

    try {
      this.account =
        this.profilesController.profileCurrent()
          .account(this.parameters.accountId)
    } catch (e: AccountsDatabaseNonexistentException) {
      this.logger.error("account no longer exists: ", e)
      this.explicitlyClose()
      return
    }

    this.configureToolbar()
    this.hideCardCreatorForNonNYPL()

    this.accountTitle.text =
      this.account.provider.displayName
    this.accountSubtitle.text =
      this.account.provider.subtitle

    ImageAccountIcons.loadAccountLogoIntoView(
      this.imageLoader.loader,
      this.account.provider.toDescription(),
      R.drawable.account_default,
      this.accountIcon
    )

    /*
     * Only show a EULA checkbox if there's actually a EULA.
     */

    val eulaOpt = this.documents.eula
    if (eulaOpt is Some<EULAType>) {
      val eula = eulaOpt.get()
      this.eulaCheckbox.visibility = View.VISIBLE
      this.eulaCheckbox.isChecked = eula.eulaHasAgreed()
      this.eulaCheckbox.setOnCheckedChangeListener { _, checked ->
        eula.eulaSetHasAgreed(checked)
        this.setLoginButtonStatus(this.determineLoginIsSatisfied())
      }
    } else {
      this.eulaCheckbox.visibility = View.GONE
    }

    this.authenticationBasicUser.addTextChangedListener(this.authenticationBasicUserListener)
    this.authenticationBasicPass.addTextChangedListener(this.authenticationBasicPassListener)

    /*
     * Configure the COPPA age gate switch. If the user changes their age, a log out
     * is required.
     */

    this.authenticationCOPPAOver13.setOnClickListener {}
    this.authenticationCOPPAOver13.isChecked = this.isOver13()
    this.authenticationCOPPAOver13.setOnClickListener(this.onAgeCheckboxClicked())
    this.authenticationCOPPAOver13.isEnabled = true

    /*
     * Conditionally enable sign up button
     */

    val signUpEnabled = this.shouldSignUpBeEnabled()
    this.signUpButton.isEnabled = signUpEnabled
    this.signUpLabel.isEnabled = signUpEnabled

    /*
     * Launch Card Creator
     */

    this.signUpButton.setOnClickListener { this.openCardCreator() }

    /*
     * Configure a checkbox listener that shows and hides the password field. Note that
     * this will trigger the "text changed" listener on the password field, so we lock this
     * checkbox during login/logout to avoid any chance of the UI becoming inconsistent.
     */

    this.authenticationBasicShowPass.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        this.authenticationBasicPass.transformationMethod =
          null
      } else {
        this.authenticationBasicPass.transformationMethod =
          PasswordTransformationMethod.getInstance()
      }
    }

    /*
     * Configure the bookmark syncing switch to enable/disable syncing permissions.
     */

    this.bookmarkSyncCheck.setOnCheckedChangeListener { _, isChecked ->
      this.backgroundExecutor.execute {
        this.account.setPreferences(
          this.account.preferences.copy(bookmarkSyncingPermitted = isChecked)
        )
      }
    }

    /*
     * Instantiate views for alternative authentication methods.
     */

    this.authenticationAlternativesMake()

    this.accountSubscription =
      this.profilesController.accountEvents()
        .subscribe(this::onAccountEvent)
    this.profileSubscription =
      this.profilesController.profileEvents()
        .subscribe(this::onProfileEvent)

    this.reconfigureAccountUI()
  }

  private fun instantiateAlternativeAuthenticationViews() {
    for (alternative in this.account.provider.authenticationAlternatives) {
      when (alternative) {
        is AccountProviderAuthenticationDescription.COPPAAgeGate ->
          this.logger.warn("COPPA age gate is not currently supported as an alternative.")
        is AccountProviderAuthenticationDescription.Basic ->
          this.logger.warn("Basic authentication is not currently supported as an alternative.")
        AccountProviderAuthenticationDescription.Anonymous ->
          this.logger.warn("Anonymous authentication makes no sense as an alternative.")

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

  private fun onTryOAuthLogin(
    authenticationDescription: AccountProviderAuthenticationDescription.OAuthWithIntermediary
  ) {
    this.viewModel.loginExplicitlyRequested = true
    this.profilesController.profileAccountLogin(
      OAuthWithIntermediaryInitiate(
        accountId = this.account.id,
        description = authenticationDescription
      )
    )
    this.sendOAuthIntent(authenticationDescription)
  }

  private fun sendOAuthIntent(
    authenticationDescription: AccountProviderAuthenticationDescription.OAuthWithIntermediary
  ) {
    val callbackScheme =
      this.buildConfig.oauthCallbackScheme.scheme
    val callbackUrl =
      OAuthCallbackIntentParsing.createUri(
        requiredScheme = callbackScheme,
        accountId = this.account.id.uuid
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

  private fun configureToolbar() {
    val host = this.activity
    if (host is ToolbarHostType) {
      host.toolbarClearMenu()
      host.toolbarSetTitleSubtitle(
        title = this.requireContext().getString(R.string.accounts),
        subtitle = this.account.provider.displayName
      )
      host.toolbarSetBackArrowConditionally(
        context = host,
        shouldArrowBePresent = {
          this.findNavigationController().backStackSize() > 1
        },
        onArrowClicked = this@AccountFragment::explicitlyClose
      )
    } else {
      throw IllegalStateException("The activity ($host) hosting this fragment must implement ${ToolbarHostType::class.java}")
    }
  }

  private fun explicitlyClose() {
    if (this.closing.compareAndSet(false, true)) {
      this.findNavigationController().popBackStack()
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
    this.account.setLoginState(this.account.loginState)

    this.backgroundExecutor.shutdown()
    this.accountIcon.setImageDrawable(null)
    this.eulaCheckbox.setOnCheckedChangeListener(null)
    this.authenticationCOPPAOver13.setOnClickListener {}
    this.authenticationBasicUser.removeTextChangedListener(this.authenticationBasicUserListener)
    this.authenticationBasicPass.removeTextChangedListener(this.authenticationBasicPassListener)
    this.accountSubscription?.dispose()
    this.profileSubscription?.dispose()
  }

  private fun onProfileEvent(event: ProfileEvent) {
    return when (event) {
      is ProfileUpdated -> {
        this.uiThread.runOnUIThread(Runnable {
          this.reconfigureAccountUI()
        })
      }
      else -> {
      }
    }
  }

  @UiThread
  private fun reconfigureAccountUI() {
    this.uiThread.checkIsUIThread()

    val isPermitted = this.account.preferences.bookmarkSyncingPermitted
    val isSupported = this.account.provider.supportsSimplyESynchronization
    this.bookmarkSyncCheck.isChecked = isPermitted
    this.bookmarkSyncCheck.isEnabled = isSupported
    this.bookmarkSyncLabel.isEnabled = isSupported

    when (val auth = this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate -> {
        this.authentication.visibility = View.VISIBLE
        this.authenticationCOPPA.visibility = View.VISIBLE
        this.authenticationBasic.visibility = View.INVISIBLE
      }

      is AccountProviderAuthenticationDescription.Basic -> {
        this.authentication.visibility = View.VISIBLE
        this.authenticationCOPPA.visibility = View.INVISIBLE
        this.authenticationBasic.visibility = View.VISIBLE

        /*
         * Configure the presence of the individual fields based on keyboard input values
         * given in the authentication document.
         *
         * TODO: Add the extra input validation for the more precise types such as NUMBER_PAD.
         */

        when (auth.keyboard) {
          KeyboardInput.NO_INPUT -> {
            this.authenticationBasicUserLabel.visibility = View.GONE
            this.authenticationBasicUser.visibility = View.GONE
          }
          KeyboardInput.DEFAULT,
          KeyboardInput.EMAIL_ADDRESS,
          KeyboardInput.NUMBER_PAD -> {
            this.authenticationBasicUserLabel.visibility = View.VISIBLE
            this.authenticationBasicUser.visibility = View.VISIBLE
          }
        }

        when (auth.passwordKeyboard) {
          KeyboardInput.NO_INPUT -> {
            this.authenticationBasicPassLabel.visibility = View.GONE
            this.authenticationBasicPass.visibility = View.GONE
            this.authenticationBasicShowPass.visibility = View.GONE
          }
          KeyboardInput.DEFAULT,
          KeyboardInput.EMAIL_ADDRESS,
          KeyboardInput.NUMBER_PAD -> {
            this.authenticationBasicPassLabel.visibility = View.VISIBLE
            this.authenticationBasicPass.visibility = View.VISIBLE
            this.authenticationBasicShowPass.visibility = View.VISIBLE
          }
        }

        this.authenticationBasicUserLabel.text =
          auth.labels["LOGIN"] ?: this.authenticationBasicUserLabel.text
        this.authenticationBasicPassLabel.text =
          auth.labels["PASSWORD"] ?: this.authenticationBasicPassLabel.text
      }

      is AccountProviderAuthenticationDescription.OAuthWithIntermediary -> {
        this.authentication.visibility = View.GONE
      }

      is AccountProviderAuthenticationDescription.Anonymous -> {
        this.authentication.visibility = View.GONE
        this.login.visibility = View.GONE
        this.setLoginButtonStatus(AsLoginButtonDisabled)
      }
    }

    return when (val loginState = this.account.loginState) {
      AccountNotLoggedIn -> {
        this.authenticationBasicUser.setText("")
        this.authenticationBasicPass.setText("")
        this.loginButtonErrorDetails.visibility = View.GONE
        this.loginProgress.visibility = View.INVISIBLE
        this.loginProgressText.text = ""
        this.setLoginButtonStatus(AsLoginButtonEnabled {
          this.loginFormLock()
          this.tryLogin()
        })
        this.loginFormUnlock()
      }

      is AccountLoggingIn -> {
        this.loginProgress.visibility = View.VISIBLE
        this.loginProgressText.text = loginState.status
        this.loginButtonErrorDetails.visibility = View.GONE
        this.loginFormLock()

        if (loginState.cancellable) {
          this.setLoginButtonStatus(AsCancelButtonEnabled {
            // We don't really support this yet.
            throw UnimplementedCodeException()
          })
        } else {
          this.setLoginButtonStatus(AsCancelButtonDisabled)
        }
      }

      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.loginProgress.visibility = View.VISIBLE
        this.loginProgressText.text = loginState.status
        this.loginButtonErrorDetails.visibility = View.GONE
        this.loginFormLock()
        this.setLoginButtonStatus(AsCancelButtonEnabled {
          this.profilesController.profileAccountLogin(
            OAuthWithIntermediaryCancel(
              accountId = this.account.id,
              description = loginState.description as AccountProviderAuthenticationDescription.OAuthWithIntermediary
            )
          )
        })
      }

      is AccountLoginFailed -> {
        this.loginProgress.visibility = View.INVISIBLE
        this.loginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.loginFormUnlock()
        this.cancelImageButtonLoading()
        this.setLoginButtonStatus(AsLoginButtonEnabled {
          this.loginFormLock()
          this.tryLogin()
        })
        this.loginButtonErrorDetails.visibility = View.VISIBLE
        this.loginButtonErrorDetails.setOnClickListener {
          this.openErrorPage(loginState.taskResult.steps)
        }
        this.authenticationAlternativesShow()
      }

      is AccountLoggedIn -> {
        when (val creds = loginState.credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            this.authentication.visibility = View.VISIBLE
            this.authenticationCOPPA.visibility = View.INVISIBLE
            this.authenticationBasic.visibility = View.VISIBLE

            this.authenticationBasicUser.setText(creds.userName.value)
            this.authenticationBasicPass.setText(creds.password.value)
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
            this.authentication.visibility = View.GONE
            this.authenticationCOPPA.visibility = View.INVISIBLE
            this.authenticationBasic.visibility = View.INVISIBLE
          }
        }

        this.loginProgress.visibility = View.INVISIBLE
        this.loginProgressText.text = ""

        this.loginFormLock()
        this.loginButtonErrorDetails.visibility = View.GONE
        this.setLoginButtonStatus(AsLogoutButtonEnabled {
          this.loginFormLock()
          this.tryLogout()
        })
        this.authenticationAlternativesHide()

        if (this.viewModel.loginExplicitlyRequested && this.parameters.closeOnLoginSuccess) {
          this.logger.debug("scheduling explicit close of account fragment")
          this.uiThread.runOnUIThreadDelayed({
            this.explicitlyClose()
          }, 2_000L)
          return
        } else {
          // Doing nothing.
        }
      }

      is AccountLoggingOut -> {
        when (val creds = loginState.credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            this.authenticationBasicUser.setText(creds.userName.value)
            this.authenticationBasicPass.setText(creds.password.value)
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
            // No UI
          }
        }

        this.loginButtonErrorDetails.visibility = View.GONE
        this.loginProgress.visibility = View.VISIBLE
        this.loginProgressText.text = loginState.status
        this.loginFormLock()
        this.setLoginButtonStatus(AsLogoutButtonDisabled)
      }

      is AccountLogoutFailed -> {
        when (val creds = loginState.credentials) {
          is AccountAuthenticationCredentials.Basic -> {
            this.authenticationBasicUser.setText(creds.userName.value)
            this.authenticationBasicPass.setText(creds.password.value)
          }
          is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
            // No UI
          }
        }

        this.loginProgress.visibility = View.INVISIBLE
        this.loginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.cancelImageButtonLoading()
        this.loginFormLock()
        this.setLoginButtonStatus(AsLogoutButtonEnabled {
          this.loginFormLock()
          this.tryLogout()
        })

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

  sealed class LoginButtonStatus {
    data class AsLogoutButtonEnabled(
      val onClick: () -> Unit
    ) : LoginButtonStatus()

    object AsLogoutButtonDisabled : LoginButtonStatus()

    data class AsLoginButtonEnabled(
      val onClick: () -> Unit
    ) : LoginButtonStatus()

    object AsLoginButtonDisabled : LoginButtonStatus()

    data class AsCancelButtonEnabled(
      val onClick: () -> Unit
    ) : LoginButtonStatus()

    object AsCancelButtonDisabled : LoginButtonStatus()
  }

  private fun setLoginButtonStatus(
    status: LoginButtonStatus
  ) {
    return when (status) {
      is AsLoginButtonEnabled -> {
        this.loginButton.setText(R.string.accountLogin)
        this.signUpLabel.setText(R.string.accountCardCreatorLabel)
        this.loginButton.isEnabled = true
        this.loginButton.setOnClickListener { status.onClick.invoke() }
      }
      AsLoginButtonDisabled -> {
        this.loginButton.setText(R.string.accountLogin)
        this.signUpLabel.setText(R.string.accountCardCreatorLabel)
        this.loginButton.isEnabled = false
      }
      is AsCancelButtonEnabled -> {
        this.loginButton.setText(R.string.accountCancel)
        this.loginButton.isEnabled = true
        this.loginButton.setOnClickListener { status.onClick.invoke() }
      }
      is AsLogoutButtonEnabled -> {
        this.loginButton.setText(R.string.accountLogout)
        this.signUpLabel.setText(R.string.accountWantChildCard)
        this.loginButton.isEnabled = true
        this.loginButton.setOnClickListener { status.onClick.invoke() }
      }
      AsLogoutButtonDisabled -> {
        this.loginButton.setText(R.string.accountLogout)
        this.signUpLabel.setText(R.string.accountWantChildCard)
        this.loginButton.isEnabled = false
      }
      AsCancelButtonDisabled -> {
        this.loginButton.setText(R.string.accountCancel)
        this.loginButton.isEnabled = false
      }
    }
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
        .into(view, object : com.squareup.picasso.Callback {
          override fun onSuccess() {
            this@AccountFragment.uiThread.runOnUIThread {
              onSuccess.invoke()
            }
          }

          override fun onError(e: Exception) {
            this@AccountFragment.logger.error("failed to load authentication logo: ", e)
            this@AccountFragment.uiThread.runOnUIThread {
              view.visibility = View.GONE
            }
          }
        })
    }
  }

  @UiThread
  private fun <E : PresentableErrorType> openErrorPage(taskSteps: List<TaskStep<E>>) {
    this.uiThread.checkIsUIThread()

    val parameters =
      ErrorPageParameters(
        emailAddress = this.buildConfig.errorReportEmail,
        body = "",
        subject = "[simplye-error-report]",
        attributes = sortedMapOf(),
        taskSteps = taskSteps
      )

    this.findNavigationController().openErrorPage(parameters)
  }

  private fun loginFormLock() {
    this.authenticationCOPPAOver13.setOnClickListener {}
    this.authenticationCOPPAOver13.isChecked = this.isOver13()
    this.authenticationCOPPAOver13.setOnClickListener(this.onAgeCheckboxClicked())
    this.authenticationCOPPAOver13.isEnabled = false

    this.authenticationBasicUser.isEnabled = false
    this.authenticationBasicPass.isEnabled = false
    this.authenticationBasicShowPass.isEnabled = false
    this.eulaCheckbox.isEnabled = false

    this.setLoginButtonStatus(AsLoginButtonDisabled)
    this.authenticationAlternativesHide()
  }

  private fun loginFormUnlock() {
    this.authenticationCOPPAOver13.setOnClickListener {}
    this.authenticationCOPPAOver13.isChecked = this.isOver13()
    this.authenticationCOPPAOver13.setOnClickListener(this.onAgeCheckboxClicked())
    this.authenticationCOPPAOver13.isEnabled = true

    this.authenticationBasicUser.isEnabled = true
    this.authenticationBasicPass.isEnabled = true
    this.authenticationBasicShowPass.isEnabled = true
    this.eulaCheckbox.isEnabled = true

    val loginSatisfied = this.determineLoginIsSatisfied()
    this.setLoginButtonStatus(loginSatisfied)
    this.authenticationAlternativesShow()
  }

  private fun authenticationAlternativesMake() {
    this.authenticationAlternativesButtons.removeAllViews()
    if (this.account.provider.authenticationAlternatives.isEmpty()) {
      this.authenticationAlternativesHide()
    } else {
      this.instantiateAlternativeAuthenticationViews()
      this.authenticationAlternativesShow()
    }
  }

  private fun authenticationAlternativesShow() {
    if (this.account.provider.authenticationAlternatives.isNotEmpty()) {
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
          this.uiThread.runOnUIThread(Runnable {
            this.reconfigureAccountUI()
          })
        } else {
        }
      else -> {
      }
    }
  }

  private fun tryLogin() {
    this.viewModel.loginExplicitlyRequested = true

    return when (val description = this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.OAuthWithIntermediary ->
        this.onTryOAuthLogin(description)

      is AccountProviderAuthenticationDescription.Anonymous,
      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        throw UnreachableCodeException()

      is AccountProviderAuthenticationDescription.Basic -> {
        val accountPassword =
          AccountPassword(this.authenticationBasicPass.text.toString())
        val accountUsername =
          AccountUsername(this.authenticationBasicUser.text.toString())
        val request =
          Basic(
            accountId = this.account.id,
            description = description,
            password = accountPassword,
            username = accountUsername
          )

        this.profilesController.profileAccountLogin(request)
        Unit
      }
    }
  }

  private fun tryLogout() {
    return when (this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.Anonymous ->
        Unit

      /*
       * Although COPPA age-gated accounts don't require "logging in" or "logging out" exactly,
       * we *do* want local books to be deleted as part of a logout attempt.
       */

      is AccountProviderAuthenticationDescription.OAuthWithIntermediary,
      is AccountProviderAuthenticationDescription.COPPAAgeGate,
      is AccountProviderAuthenticationDescription.Basic -> {
        this.profilesController.profileAccountLogout(this.account.id)
        Unit
      }
    }
  }

  private fun setUnder13() {
    this.profilesController.profileUpdate { description ->
      description.copy(
        preferences = description.preferences.copy(
          dateOfBirth = this.synthesizeDateOfBirth(0)
        )
      )
    }
  }

  private fun setOver13() {
    this.profilesController.profileUpdate { description ->
      description.copy(
        preferences = description.preferences.copy(
          dateOfBirth = this.synthesizeDateOfBirth(14)
        )
      )
    }
  }

  private fun isOver13(): Boolean {
    val profile = this.profilesController.profileCurrent()
    val age = profile.preferences().dateOfBirth
    return if (age != null) {
      age.yearsOld(DateTime.now()) >= 13
    } else {
      false
    }
  }

  /**
   * Hides or show sign up options if is user in accessing the NYPL
   */
  private fun hideCardCreatorForNonNYPL() {
    if (this.account.provider.cardCreatorURI != null) {
      this.settingsCardCreator.visibility = View.VISIBLE
    }
  }

  /**
   * A click listener for the age checkbox. If the user wants to change their age, then
   * this must trigger an account logout.
   */

  private fun onAgeCheckboxClicked(): (View) -> Unit = {
    val isOver13 = this.isOver13()
    AlertDialog.Builder(this.requireContext())
      .setTitle(R.string.accountCOPPADeleteBooks)
      .setMessage(R.string.accountCOPPADeleteBooksConfirm)
      .setNegativeButton(R.string.accountCancel) { _, _ ->
        this.authenticationCOPPAOver13.isChecked = isOver13
      }
      .setPositiveButton(R.string.accountDelete) { _, _ ->
        this.loginFormLock()
        if (!isOver13) this.setOver13() else this.setUnder13()
        this.tryLogout()
      }
      .create()
      .show()
  }

  /**
   * Synthesize a fake date of birth based on the current date and given age in years.
   */

  private fun synthesizeDateOfBirth(years: Int): ProfileDateOfBirth =
    ProfileDateOfBirth(
      date = DateTime.now().minusYears(years),
      isSynthesized = true
    )

  private fun findNavigationController(): AccountNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = AccountNavigationControllerType::class.java
    )
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == this.cardCreatorResultCode) {

      when (resultCode) {
        Activity.RESULT_OK -> {
          if (data != null) {
            val barcode = data.getStringExtra("barcode")
            val pin = data.getStringExtra("pin")
            this.authenticationBasicUser.setText(barcode, TextView.BufferType.EDITABLE)
            this.authenticationBasicPass.setText(pin, TextView.BufferType.EDITABLE)
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
