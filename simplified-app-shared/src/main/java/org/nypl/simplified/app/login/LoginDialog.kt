package org.nypl.simplified.app.login

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.io7m.jfunctional.Some
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.utilities.ErrorDialogUtilities
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory

class LoginDialog : AppCompatDialogFragment() {

  private lateinit var profiles: ProfilesControllerType
  private lateinit var loginActionText: TextView
  private lateinit var loginProgress: ProgressBar
  private lateinit var loginActionLayout: ViewGroup
  private lateinit var account: AccountType
  private lateinit var loginRequestNewCode: Button
  private lateinit var eulaCheckbox: CheckBox
  private lateinit var loginButton: Button
  private lateinit var pinEdit: EditText
  private lateinit var pinLabel: TextView
  private lateinit var barcodeScanButton: ImageButton
  private lateinit var barcodeEdit: EditText
  private lateinit var barcodeLabel: TextView
  private lateinit var text: TextView

  private var accountSubscription: ObservableSubscriptionType<AccountEvent>? = null
  private var shownAlert = true

  private val logger =
    LoggerFactory.getLogger(LoginDialog::class.java)

  private lateinit var listener: LoginDialogListenerType

  override fun onAttach(context: Context?) {
    super.onAttach(context)

    val activity = this.requireActivity()
    if (activity is LoginDialogListenerType) {
      this.listener = activity
      this.profiles = this.listener.onLoginDialogWantsProfilesController()
      this.account = this.profiles.profileAccountCurrent()
    } else {
      throw IllegalStateException(buildString {
        this.append("The activity hosting a login dialog must implement a listener interface")
        this.append('\n')
        this.append("  Hosting activity: ${activity::class.java.canonicalName}")
        this.append('\n')
        this.append("  Required interfaces: ")
        this.append(LoginDialogListenerType::class.java.canonicalName)
      })
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?): View {

    val layout =
      inflater.inflate(R.layout.login_dialog, container, false) as ViewGroup

    this.text =
      layout.findViewById(R.id.login_dialog_text)
    this.barcodeLabel =
      layout.findViewById(R.id.login_dialog_barcode_text_view)
    this.barcodeEdit =
      layout.findViewById(R.id.login_dialog_barcode_text_edit)
    this.barcodeScanButton =
      layout.findViewById(R.id.login_dialog_barcode_scan_button)
    this.pinLabel =
      layout.findViewById(R.id.login_dialog_pin_text_view)
    this.pinEdit =
      layout.findViewById(R.id.login_dialog_pin_text_edit)
    this.loginButton =
      layout.findViewById(R.id.login_dialog_ok)
    this.eulaCheckbox =
      layout.findViewById(R.id.eula_checkbox)
    this.loginRequestNewCode =
      layout.findViewById(R.id.request_new_codes)
    this.loginActionLayout =
      layout.findViewById(R.id.login_action_layout)
    this.loginProgress =
      this.loginActionLayout.findViewById(R.id.login_action_progress)
    this.loginActionText =
      this.loginActionLayout.findViewById(R.id.login_action_text)

    val account =
      this.listener.onLoginDialogWantsProfilesController()
        .profileAccountCurrent()

    val accountProvider =
      account.provider()
    val authenticationOpt =
      accountProvider.authentication()

    if (!(authenticationOpt is Some<AccountProviderAuthenticationDescription>)) {
      this.logger.error("Login dialog created for account that does not require authentication!")
      this.dismissAllowingStateLoss()
      return layout
    }

    /*
     * If the passcode is not allowed to contain letters, then don't let users enter them.
     */

    val authentication = authenticationOpt.get()
    if (!authentication.passCodeMayContainLetters()) {
      this.pinEdit.inputType =
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    /*
     * If the passcode has a known length, limit the input to that length.
     */

    if (authentication.passCodeLength() != 0) {
      this.pinEdit.filters =
        arrayOf<InputFilter>(InputFilter.LengthFilter(authentication.passCodeLength()))
    }

    /*
     * If a PIN is not required, hide the PIN entry elements.
     */

    if (!authentication.requiresPin()) {
      this.pinLabel.visibility = View.INVISIBLE
      this.pinEdit.visibility = View.INVISIBLE
    }

    /*
     * If the account provider supports barcode scanning, show the scan button.
     */

    if (accountProvider.supportsBarcodeScanner()) {
      this.barcodeScanButton.visibility = View.VISIBLE
    }

    this.loginButton.setOnClickListener { view ->
      this.disableUIElements()
      this.shownAlert = false
      this.profiles.profileAccountLogin(this.account.id(), this.credentialsFromUI())
    }

    this.configureUIForAccountState(this.account.loginState())
    return layout
  }

  private fun credentialsFromUI(): AccountAuthenticationCredentials {
    return AccountAuthenticationCredentials.builder(
      AccountPIN.create(this.pinEdit.text.toString()),
      AccountBarcode.create(this.barcodeEdit.text.toString()))
      .build()
  }

  override fun onStart() {
    super.onStart()

    this.account =
      this.profiles.profileAccountCurrent()

    this.accountSubscription =
      this.profiles
        .accountEvents()
        .subscribe { event -> this.onAccountEvent(event) }
  }

  override fun onStop() {
    super.onStop()

    this.accountSubscription?.unsubscribe()
  }

  private fun onAccountEvent(event: AccountEvent) {
    if (event is AccountEventLoginStateChanged) {
      if (event.accountID != this.account.id()) {
        return
      }
      return this.configureUIForAccountState(event.state)
    }
  }

  private fun configureUIForAccountState(state: AccountLoginState) {
    UIThread.runOnUIThread {
      when (state) {
        AccountLoginState.AccountNotLoggedIn -> {
          this.loginActionLayout.visibility = View.INVISIBLE
          this.enableUIElements()
        }

        AccountLoginState.AccountLoggingIn -> {
          this.loginActionLayout.visibility = View.VISIBLE
          this.loginProgress.visibility = View.VISIBLE
          this.loginActionText.setText(R.string.settings_login_in_progress)
          this.disableUIElements()
        }

        is AccountLoginState.AccountLoginFailed -> {
          if (!this.shownAlert) {
            ErrorDialogUtilities.showError(
              this.activity,
              this.logger,
              LoginErrorCodeStrings.stringOfLoginError(this.resources, state.errorCode),
              state.exception)
            this.shownAlert = true
          }

          this.loginActionLayout.visibility = View.VISIBLE
          this.loginProgress.visibility = View.GONE
          this.loginActionText.setText(R.string.settings_login_failed)
          this.enableUIElements()
        }

        is AccountLoginState.AccountLoggedIn -> {
          this.logger.debug("account {} is logged in: dismissing dialog", this.account.id())
          this.loginActionLayout.visibility = View.INVISIBLE
          this.disableUIElements()
          this.dismiss()
        }

        AccountLoginState.AccountLoggingOut -> {
          this.loginActionLayout.visibility = View.INVISIBLE
          this.loginProgress.visibility = View.VISIBLE
          this.loginActionText.setText(R.string.settings_logout_in_progress)
          this.disableUIElements()
        }

        is AccountLoginState.AccountLogoutFailed -> {
          if (!this.shownAlert) {
            ErrorDialogUtilities.showError(
              this.activity,
              this.logger,
              LoginErrorCodeStrings.stringOfLogoutError(this.resources, state.errorCode),
              state.exception)
            this.shownAlert = true
          }

          this.loginActionLayout.visibility = View.VISIBLE
          this.loginProgress.visibility = View.GONE
          this.loginActionText.setText(R.string.settings_logout_failed)
          this.enableUIElements()
        }
      }
    }
  }

  private fun disableUIElements() {
    UIThread.checkIsUIThread()

    this.barcodeEdit.isEnabled = false
    this.pinEdit.isEnabled = false
    this.loginButton.isEnabled = false
    this.barcodeScanButton.isEnabled = false
    this.eulaCheckbox.isEnabled = false
    this.loginRequestNewCode.isEnabled = false
  }

  private fun enableUIElements() {
    UIThread.checkIsUIThread()

    this.barcodeEdit.isEnabled = true
    this.pinEdit.isEnabled = true
    this.loginButton.isEnabled = true
    this.barcodeScanButton.isEnabled = true
    this.eulaCheckbox.isEnabled = true
    this.loginRequestNewCode.isEnabled = true
  }
}