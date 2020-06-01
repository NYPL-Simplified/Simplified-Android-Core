package org.nypl.simplified.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.io7m.jfunctional.Some
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory

/**
 * A login dialog used in catalogs.
 *
 * Fragments calling this dialog _must_ set themselves as the target fragment with [setTargetFragment]
 * and _must_ implement the [CatalogFragmentLoginDialogListenerType] interface in order to
 * receive a call when the dialog is closed.
 */

class CatalogFragmentLoginDialog : Fragment() {

  private val logger = LoggerFactory.getLogger(CatalogFragmentLoginDialog::class.java)

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.catalog.CatalogFragmentLoginDialog.parameters"

    /**
     * Create a login fragment for the given parameters.
     */

    fun create(parameters: CatalogFragmentLoginDialogParameters): CatalogFragmentLoginDialog {
      val arguments = Bundle()
      arguments.putSerializable(this.PARAMETERS_ID, parameters)
      val fragment = CatalogFragmentLoginDialog()
      fragment.arguments = arguments
      return fragment
    }
  }

  private lateinit var account: AccountType
  private lateinit var action: Button
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var dialogModel: CatalogLoginViewModel
  private lateinit var documents: DocumentStoreType
  private lateinit var errorDetails: Button
  private lateinit var eula: CheckBox
  private lateinit var fieldListener: OnTextChangeListener
  private lateinit var lockFormOverlay: View
  private lateinit var parameters: CatalogFragmentLoginDialogParameters
  private lateinit var password: EditText
  private lateinit var passwordLabel: TextView
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var progress: ProgressBar
  private lateinit var progressText: TextView
  private lateinit var screenSize: ScreenSizeInformationType
  private lateinit var uiThread: UIThreadServiceType
  private lateinit var userName: EditText
  private lateinit var userNameLabel: TextView
  private val parametersId = PARAMETERS_ID
  private var accountEventSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.parameters = this.arguments!![this.parametersId] as CatalogFragmentLoginDialogParameters

    val services = Services.serviceDirectory()

    this.profilesController = services.requireService(ProfilesControllerType::class.java)
    this.uiThread = services.requireService(UIThreadServiceType::class.java)
    this.documents = services.requireService(DocumentStoreType::class.java)
    this.screenSize = services.requireService(ScreenSizeInformationType::class.java)
    this.buildConfig = services.requireService(BuildConfigurationServiceType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val layout =
      inflater.inflate(R.layout.login_dialog, container, false)

    this.errorDetails = layout.findViewById<Button>(R.id.loginErrorDetailsButton)
    this.action = layout.findViewById(R.id.loginButton)
    this.eula = layout.findViewById(R.id.loginEULA)
    this.password = layout.findViewById(R.id.loginPassword)
    this.passwordLabel = layout.findViewById(R.id.loginPasswordLabel)
    this.progress = layout.findViewById(R.id.loginProgressBar)
    this.progressText = layout.findViewById(R.id.loginProgressText)
    this.userName = layout.findViewById(R.id.loginUserName)
    this.userNameLabel = layout.findViewById(R.id.loginUserNameLabel)
    this.lockFormOverlay = layout.findViewById(R.id.lockFormOverlay)

    this.fieldListener = OnTextChangeListener(this::onFieldChanged)

    this.action.isEnabled = false
    this.progress.visibility = View.INVISIBLE
    this.progressText.text = ""
    return layout
  }

  override fun onStart() {
    super.onStart()

    this.dialogModel = ViewModelProviders.of(this.requireActivity())
      .get(CatalogLoginViewModel::class.java)

    /*
     * Re-fetch the account. Note that it could (theoretically) have been deleted, so we fetch
     * and re-fetch every time and close the dialog if the account is no longer there.
     */

    try {
      this.account =
        this.profilesController.profileCurrent().account(this.parameters.accountId)
    } catch (e: AccountsDatabaseNonexistentException) {
      this.findNavigationController().popBackStack()
      return
    }

    /*
     * If logging in isn't required, abort early.
     */

    if (!this.account.requiresCredentials) {
      this.findNavigationController().popBackStack()
      return
    }

    /*
     * Only show a EULA checkbox if there's actually a EULA.
     */

    val eulaOpt = this.documents.eula
    if (eulaOpt is Some<EULAType>) {
      val eula = eulaOpt.get()
      this.eula.visibility = View.VISIBLE
      this.eula.isChecked = eula.eulaHasAgreed()
      this.eula.setOnCheckedChangeListener { _, checked ->
        eula.eulaSetHasAgreed(checked)
        this.action.isEnabled = this.determineLoginIsSatisfied()
      }
    } else {
      this.eula.visibility = View.INVISIBLE
    }

    this.userName.addTextChangedListener(this.fieldListener)
    this.password.addTextChangedListener(this.fieldListener)

    this.accountEventSubscription =
      this.profilesController.accountEvents()
        .subscribe(this::onAccountEvent)

    this.reconfigureUI()
  }

  private fun findNavigationController() =
    NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = CatalogNavigationControllerType::class.java
    )

  private fun onAccountEvent(event: AccountEvent) {
    return when (event) {
      is AccountEventLoginStateChanged ->
        this.uiThread.runOnUIThread {
          this.reconfigureUI()
        }
      else -> {
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  @UiThread
  private fun onFieldChanged(
    text: CharSequence,
    start: Int,
    before: Int,
    count: Int
  ) {
    this.uiThread.checkIsUIThread()
    this.action.isEnabled = this.determineLoginIsSatisfied()
  }

  @UiThread
  private fun determineLoginIsSatisfied(): Boolean {
    return when (val auth = this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        false

      is AccountProviderAuthenticationDescription.Basic -> {
        val eulaOpt = this.documents.eula
        val eulaOk = if (eulaOpt is Some<EULAType>) {
          eulaOpt.get().eulaHasAgreed()
        } else {
          true
        }

        val noUserRequired =
          auth.keyboard == KeyboardInput.NO_INPUT
        val noPasswordRequired =
          auth.passwordKeyboard == KeyboardInput.NO_INPUT
        val userOk =
          this.userName.text.isNotBlank() || noUserRequired
        val passOk =
          this.password.text.isNotBlank() || noPasswordRequired

        this.logger.debug("login: eula ok: {}, user ok: {}, pass ok: {}", eulaOk, userOk, passOk)
        userOk && passOk && eulaOk
      }

      AccountProviderAuthenticationDescription.Anonymous ->
        false

      is AccountProviderAuthenticationDescription.OAuthWithIntermediary ->
        false
    }
  }

  @UiThread
  private fun reconfigureUI() {
    this.uiThread.checkIsUIThread()

    when (val auth = this.account.provider.authentication) {
      AccountProviderAuthenticationDescription.Anonymous,
      is AccountProviderAuthenticationDescription.COPPAAgeGate -> {
        // Technically unreachable code...
      }

      is AccountProviderAuthenticationDescription.Basic -> {
        /*
         * Configure the presence of the individual fields based on keyboard input values
         * given in the authentication document.
         *
         * TODO: Add the extra input validation for the more precise types such as NUMBER_PAD.
         */

        when (auth.keyboard) {
          KeyboardInput.NO_INPUT -> {
            this.userNameLabel.visibility = View.GONE
            this.userName.visibility = View.GONE
          }
          KeyboardInput.DEFAULT,
          KeyboardInput.EMAIL_ADDRESS,
          KeyboardInput.NUMBER_PAD -> {
            this.userNameLabel.visibility = View.VISIBLE
            this.userName.visibility = View.VISIBLE
          }
        }

        when (auth.passwordKeyboard) {
          KeyboardInput.NO_INPUT -> {
            this.passwordLabel.visibility = View.GONE
            this.password.visibility = View.GONE
          }
          KeyboardInput.DEFAULT,
          KeyboardInput.EMAIL_ADDRESS,
          KeyboardInput.NUMBER_PAD -> {
            this.passwordLabel.visibility = View.VISIBLE
            this.password.visibility = View.VISIBLE
          }
        }

        this.userNameLabel.text = auth.labels["LOGIN"] ?: this.userNameLabel.text
        this.passwordLabel.text = auth.labels["PASSWORD"] ?: this.passwordLabel.text
      }
    }

    return when (val state = this.account.loginState) {
      AccountNotLoggedIn -> {
        this.unlockForm()
        this.action.isEnabled = this.determineLoginIsSatisfied()
        this.errorDetails.visibility = View.GONE
        this.progress.visibility = View.INVISIBLE
        this.progressText.visibility = View.INVISIBLE
        this.action.setOnClickListener {
          this.lockForm()
          this.tryLogin()
        }
      }

      is AccountLoggingInWaitingForExternalAuthentication -> {
        this.lockForm()
        this.errorDetails.visibility = View.GONE
        this.progress.visibility = View.VISIBLE
        this.progressText.text = state.status
        this.progressText.visibility = View.VISIBLE
      }

      is AccountLoggingIn -> {
        this.lockForm()
        this.errorDetails.visibility = View.GONE
        this.progress.visibility = View.VISIBLE
        this.progressText.text = state.status
        this.progressText.visibility = View.VISIBLE
      }

      is AccountLoginFailed -> {
        this.unlockForm()
        this.errorDetails.visibility = View.VISIBLE
        this.action.isEnabled = this.determineLoginIsSatisfied()
        this.progress.visibility = View.INVISIBLE
        this.progressText.visibility = View.VISIBLE
        this.progressText.text = state.taskResult.steps.last().resolution.message
        this.action.setOnClickListener {
          this.lockForm()
          this.tryLogin()
        }
        this.errorDetails.setOnClickListener {
          this.openErrorPage(state.taskResult.steps)
        }
      }

      is AccountLogoutFailed -> {
        this.lockForm()
        this.errorDetails.visibility = View.VISIBLE
        this.action.isEnabled = this.determineLoginIsSatisfied()
        this.progress.visibility = View.INVISIBLE
        this.progressText.visibility = View.VISIBLE
        this.progressText.text = state.taskResult.steps.last().resolution.message
        this.errorDetails.setOnClickListener {
          this.openErrorPage(state.taskResult.steps)
        }
      }

      is AccountLoggedIn,
      is AccountLoggingOut -> {
        this.findNavigationController().popBackStack()
        Unit
      }
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
        taskSteps = taskSteps)

    this.findNavigationController().openErrorPage(parameters)
  }

  private fun unlockForm() {
    this.lockFormOverlay.visibility = View.GONE
    this.userName.isEnabled = true
    this.action.isEnabled = true
    this.password.isEnabled = true
  }

  private fun lockForm() {
    this.lockFormOverlay.visibility = View.VISIBLE
    this.userName.isEnabled = false
    this.action.isEnabled = false
    this.password.isEnabled = false
  }

  private fun tryLogin() {
    return when (this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.OAuthWithIntermediary,
      is AccountProviderAuthenticationDescription.COPPAAgeGate,
      AccountProviderAuthenticationDescription.Anonymous ->
        Unit

      is AccountProviderAuthenticationDescription.Basic -> {
        val accountPin =
          AccountPIN.create(this.password.text.toString())
        val accountBarcode =
          AccountBarcode.create(this.userName.text.toString())
        val credentials =
          AccountAuthenticationCredentials.builder(accountPin, accountBarcode)
            .build()

        this.profilesController.profileAccountLogin(this.account.id, credentials)
        Unit
      }
    }
  }

  override fun onStop() {
    super.onStop()

    this.eula.setOnCheckedChangeListener(null)
    this.userName.removeTextChangedListener(this.fieldListener)
    this.password.removeTextChangedListener(this.fieldListener)
    this.accountEventSubscription?.dispose()

    this.dialogModel.loginDialogCompleted.onNext(Unit)
  }
}
