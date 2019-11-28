package org.nypl.simplified.ui.catalog

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.io7m.jfunctional.Some
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
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

class CatalogFragmentLoginDialog : DialogFragment() {

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
  private lateinit var dialogModel: CatalogLoginViewModel
  private lateinit var documents: DocumentStoreType
  private lateinit var eula: CheckBox
  private lateinit var fieldListener: OnTextChangeListener
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

    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.documents =
      services.requireService(DocumentStoreType::class.java)
    this.screenSize =
      services.requireService(ScreenSizeInformationType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val layout =
      inflater.inflate(R.layout.login_dialog, container, false)

    this.action =
      layout.findViewById(R.id.loginButton)
    this.eula =
      layout.findViewById(R.id.loginEULA)
    this.password =
      layout.findViewById(R.id.loginPassword)
    this.passwordLabel =
      layout.findViewById(R.id.loginPasswordLabel)
    this.progress =
      layout.findViewById(R.id.loginProgressBar)
    this.progressText =
      layout.findViewById(R.id.loginProgressText)
    this.userName =
      layout.findViewById(R.id.loginUserName)
    this.userNameLabel =
      layout.findViewById(R.id.loginUserNameLabel)

    this.fieldListener =
      OnTextChangeListener(this::onFieldChanged)

    this.action.isEnabled = false
    this.progress.visibility = View.INVISIBLE
    this.progressText.text = ""
    return layout
  }

  override fun onStart() {
    super.onStart()

    this.dialogModel =
      ViewModelProviders.of(this.requireActivity())
        .get(CatalogLoginViewModel::class.java)

    this.resizeDialog()

    /*
     * Re-fetch the account. Note that it could (theoretically) have been deleted, so we fetch
     * and re-fetch every time and close the dialog if the account is no longer there.
     */

    try {
      this.account =
        this.profilesController.profileCurrent()
          .account(this.parameters.accountId)
    } catch (e: AccountsDatabaseNonexistentException) {
      this.dismiss()
      return
    }

    /*
     * If logging in isn't required, abort early.
     */

    if (!this.account.requiresCredentials) {
      this.dismiss()
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

  /**
   * Android will typically make custom dialogs into a less-than-useful size. To combat
   * this, we resize the window containing the dialog to a proportion of the screen size.
   */

  private fun resizeDialog() {
    val dialog = this.dialog
    if (dialog != null) {
      val window = dialog.window
      if (window != null) {
        if (this.screenSize.isPortrait) {
          this.logger.debug(
            "screen portrait ({}x{})",
            this.screenSize.widthPixels,
            this.screenSize.heightPixels
          )
          window.setLayout(
            (this.screenSize.widthPixels * 0.8).toInt(),
            (this.screenSize.heightPixels * 0.4).toInt())
        } else {
          this.logger.debug(
            "screen landscape ({}x{})",
            this.screenSize.widthPixels,
            this.screenSize.heightPixels
          )
          window.setLayout(
            (this.screenSize.widthPixels * 0.6).toInt(),
            (this.screenSize.heightPixels * 0.8).toInt())
        }
        window.setGravity(Gravity.CENTER)
      }
    }
  }

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
    return when (this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        false

      is AccountProviderAuthenticationDescription.Basic -> {
        val eulaOpt = this.documents.eula
        val eulaOk = if (eulaOpt is Some<EULAType>) {
          eulaOpt.get().eulaHasAgreed()
        } else {
          true
        }

        val userOk = this.userName.text.isNotBlank()
        val passOk = this.password.text.isNotBlank()
        this.logger.debug("login: eula ok: {}", eulaOk)
        this.logger.debug("login: user ok: {}", userOk)
        this.logger.debug("login: pass ok: {}", passOk)
        userOk && passOk && eulaOk
      }

      null ->
        return false
    }
  }

  @UiThread
  private fun reconfigureUI() {
    this.uiThread.checkIsUIThread()

    return when (val state = this.account.loginState) {
      AccountLoginState.AccountNotLoggedIn -> {
        this.unlockForm()
        this.action.isEnabled = this.determineLoginIsSatisfied()
        this.progress.visibility = View.INVISIBLE
        this.progressText.visibility = View.INVISIBLE
        this.action.setOnClickListener {
          this.lockForm()
          this.tryLogin()
        }
      }

      is AccountLoginState.AccountLoggingIn -> {
        this.lockForm()
        this.progress.visibility = View.VISIBLE
        this.progressText.text = state.status
        this.progressText.visibility = View.VISIBLE
      }

      is AccountLoginState.AccountLoginFailed -> {
        this.unlockForm()
        this.action.isEnabled = this.determineLoginIsSatisfied()
        this.progress.visibility = View.INVISIBLE
        this.progressText.visibility = View.VISIBLE
        this.action.setOnClickListener {
          this.lockForm()
          this.tryLogin()
        }
      }

      is AccountLoginState.AccountLoggedIn,
      is AccountLoginState.AccountLoggingOut,
      is AccountLoginState.AccountLogoutFailed ->
        this.dismiss()
    }
  }

  private fun unlockForm() {
    this.userName.isEnabled = true
    this.action.isEnabled = true
    this.password.isEnabled = true
  }

  private fun lockForm() {
    this.userName.isEnabled = false
    this.action.isEnabled = false
    this.password.isEnabled = false
  }

  private fun tryLogin() {
    return when (this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        Unit
      null ->
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
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    this.dialogModel.loginDialogCompleted.onNext(Unit)
  }
}