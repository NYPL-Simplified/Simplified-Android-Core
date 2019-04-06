package org.nypl.simplified.app.login

import android.app.DialogFragment
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.zxing.integration.android.IntentIntegrator
import com.io7m.jfunctional.None
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.OptionVisitorType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.jnull.Nullable
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.app.R
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountAuthenticationProvider
import org.nypl.simplified.books.accounts.AccountBarcode
import org.nypl.simplified.books.accounts.AccountEventLogin
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_ACCOUNT_NONEXISTENT
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_CREDENTIALS_INCORRECT
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_GENERAL
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_NETWORK_EXCEPTION
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_PROFILE_CONFIGURATION
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_SERVER_ERROR
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginSucceeded
import org.nypl.simplified.books.accounts.AccountPIN
import org.nypl.simplified.books.accounts.AccountProviderAuthenticationDescription
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.document_store.DocumentStoreType
import org.nypl.simplified.books.eula.EULAType
import org.nypl.simplified.books.logging.LogUtilities
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A reusable login dialog.
 */

/**
 * Construct a new dialog.
 */

class LoginDialog : AppCompatDialogFragment() {

  private val logger = LoggerFactory.getLogger(LoginDialog::class.java)

  private lateinit var barcodeEdit: EditText
  private lateinit var scan: ImageButton
  private lateinit var login: Button
  private lateinit var pinEdit: EditText
  private lateinit var text: TextView
  private lateinit var cancel: Button
  private lateinit var controller: ProfilesControllerType
  private lateinit var account: AccountType
  private lateinit var executor: ListeningExecutorService
  private lateinit var loginTask: FluentFuture<Unit>
  private lateinit var onLoginSuccess: LoginSucceededType
  private lateinit var onLoginFailure: LoginFailedType
  private lateinit var onLoginCancelled: LoginCancelledType
  private lateinit var authentication: AccountProviderAuthenticationDescription
  private lateinit var documents: DocumentStoreType

  private fun setRequiredArguments(
    controller: ProfilesControllerType,
    executor: ListeningExecutorService,
    documents: DocumentStoreType,
    account: AccountType,
    authentication: AccountProviderAuthenticationDescription,
    on_login_success: LoginSucceededType,
    on_login_cancelled: LoginCancelledType,
    on_login_failure: LoginFailedType) {

    this.controller = controller
    this.executor = executor
    this.account = account
    this.authentication = authentication
    this.documents = documents
    this.onLoginSuccess = on_login_success
    this.onLoginFailure = on_login_failure
    this.onLoginCancelled = on_login_cancelled
  }

  private fun onAccountLoginFailure(
    error: OptionType<Exception>,
    message: String): Unit {

    LogUtilities.errorWithOptionalException(this.logger, "login failed: ${message}", error)

    UIThread.runOnUIThread {
      this.text.text = message
      this.barcodeEdit.isEnabled = true
      this.pinEdit.isEnabled = true
      this.login.isEnabled = true
      this.cancel.isEnabled = true
      try {
        this.onLoginFailure.onLoginFailed(error, message)
      } catch (e: Exception) {
        this.logger.error("ignored exception in login failed callback: {}", e)
      }
    }

    return Unit.unit()
  }

  override fun onResume() {
    super.onResume()

    val rr = this.resources
    val h = rr.getDimension(R.dimen.login_dialog_height).toInt()
    val w = rr.getDimension(R.dimen.login_dialog_width).toInt()

    val dialog = this.dialog
    val window = dialog.window!!
    window.setLayout(w, h)
    window.setGravity(Gravity.CENTER)
  }

  override fun onCancel(@Nullable dialog: DialogInterface?) {
    this.logger.debug("login aborted")

    val task = this.loginTask
    if (task != null) {
      task.cancel(true)
    }

    UIThread.runOnUIThread {
      try {
        this.onLoginCancelled.onLoginCancelled()
      } catch (e: Exception) {
        this.logger.error("ignored exception in cancelled callback: ", e)
      }
    }
  }

  override fun onCreate(
    @Nullable state: Bundle?) {
    super.onCreate(state)
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedLoginDialog)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {

    val layout =
      inflater.inflate(R.layout.login_dialog, container, false) as ViewGroup
    val text =
      layout.findViewById<TextView>(R.id.login_dialog_text)
    val barcodeLabel =
      layout.findViewById<TextView>(R.id.login_dialog_barcode_text_view)
    val barcodeEdit =
      layout.findViewById<EditText>(R.id.login_dialog_barcode_text_edit)
    val barcodeScanButton =
      layout.findViewById<ImageButton>(R.id.login_dialog_barcode_scan_button)
    val pinLabel =
      layout.findViewById<TextView>(R.id.login_dialog_pin_text_view)
    val pinEdit =
      layout.findViewById<EditText>(R.id.login_dialog_pin_text_edit)

    /*
     * If the passcode is not allowed to contain letters, then don't let users enter them.
     */

    if (!this.authentication.passCodeMayContainLetters()) {
      pinEdit.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    /*
     * If the passcode has a known length, limit the input to that length.
     */

    if (this.authentication.passCodeLength() != 0) {
      pinEdit.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(this.authentication.passCodeLength()))
    }

    /*
     * If a PIN is not required, hide the PIN entry elements.
     */

    if (!this.authentication.requiresPin()) {
      pinLabel.visibility = View.INVISIBLE
      pinEdit.visibility = View.INVISIBLE
    }

    val loginButton =
      layout.findViewById<Button>(R.id.login_dialog_ok)
    val loginCancelButton =
      layout.findViewById<Button>(R.id.login_dialog_cancel)
    val eulaCheckbox =
      layout.findViewById<CheckBox>(R.id.eula_checkbox)
    val loginRequestNewCode =
      layout.findViewById<Button>(R.id.request_new_codes)

    // XXX: This "authentication document" is supposed to be part of the account provider
    val auth_doc = this.documents.authenticationDocument
    barcodeLabel.text = auth_doc.labelLoginUserID
    pinLabel.text = auth_doc.labelLoginPassword

    val rr = this.resources
    val adobe_vendor = Option.some(AdobeVendorID(rr.getString(R.string.feature_adobe_vendor_id)))

    /*
     * If the account provider supports barcode scanning, show the scan button.
     */

    if (this.account.provider().supportsBarcodeScanner()) {
      barcodeScanButton.visibility = View.VISIBLE
    }

    // XXX: Where does this information come from?
    // in_text.setText(initial_txt);
    // in_barcode_edit.setText(initial_bar.toString());
    // in_pin_edit.setText(initial_pin.toString());

    loginButton.isEnabled = false
    loginButton.setOnClickListener { button ->
      barcodeEdit.isEnabled = false
      pinEdit.isEnabled = false
      loginButton.isEnabled = false
      loginCancelButton.isEnabled = false
      barcodeScanButton.isEnabled = false

      val barcode_edit_text = barcodeEdit.text
      val pin_edit_text = pinEdit.text

      val barcode =
        AccountBarcode.create(barcode_edit_text.toString())

      val pin: AccountPIN
      if (!this.authentication.requiresPin()) {
        // Server requires blank string for No-PIN accounts
        pin = AccountPIN.create("")
      } else {
        pin = AccountPIN.create(pin_edit_text.toString())
      }

      val provider = AccountAuthenticationProvider.create(
        rr.getString(R.string.feature_default_auth_provider_name))

      val accountCreds = AccountAuthenticationCredentials.builder(pin, barcode)
        .setAuthenticationProvider(provider)
        .build()

      this.loginTask = this.controller.profileAccountLogin(this.account.id(), accountCreds)
        .catching(Exception::class.java, com.google.common.base.Function<Exception, AccountEventLogin> { event -> this.onLoginFailed(event!!) }, this.executor)
        .transform(com.google.common.base.Function<AccountEventLogin, Unit> { event -> this.onAccountEvent(event!!) }, this.executor)
    }

    loginCancelButton.setOnClickListener { view ->
      this.onCancel(null)
      this.dismiss()
    }

    barcodeScanButton.setOnClickListener { v ->
      // IntentIntegrator exit will fire on Scan or Back and hit the onActivityResult method.
      IntentIntegrator
        .forSupportFragment(this)
        .setPrompt(this.getString(R.string.barcode_scanner_prompt))
        .setBeepEnabled(false)
        .initiateScan()
    }

    val requestNewCode =
      rr.getBoolean(R.bool.feature_default_auth_provider_request_new_code)

    if (requestNewCode) {
      loginRequestNewCode.setOnClickListener { v ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rr.getString(R.string.feature_default_auth_provider_request_new_code_uri)))
        this.startActivity(intent)
      }
    } else {
      // card creator (currently deactivated)
      //      in_login_request_new_code.setOnClickListener(
      //        new OnClickListener() {
      //          @Override
      //          public void onClick(
      //            final @Nullable View v) {
      //            final Intent cardcreator = new Intent(LoginDialog.this.getActivity(), CardCreatorActivity.class);
      //            startActivity(cardcreator);
      //          }
      //        });
      //      in_login_request_new_code.setText("Sign Up");
      loginRequestNewCode.visibility = View.GONE
    }

    val in_barcode_empty = AtomicBoolean(true)
    val in_pin_empty = AtomicBoolean(this.authentication.requiresPin())

    val eulaOpt = this.documents.eula
    if (eulaOpt.isSome) {
      val someEula = eulaOpt as Some<EULAType>
      val eula = someEula.get()
      eulaCheckbox.isChecked = eula.eulaHasAgreed()
      eulaCheckbox.setOnCheckedChangeListener { button, checked ->
        eula.eulaSetHasAgreed(checked)
        loginButton.isEnabled = (!in_barcode_empty.get()
          && !in_pin_empty.get()
          && eulaCheckbox.isChecked)
      }
      if (eula.eulaHasAgreed()) {
        this.logger.debug("EULA: agreed")
      } else {
        this.logger.debug("EULA: not agreed")
      }
    } else {
      this.logger.debug("EULA: unavailable")
    }

    barcodeEdit.addTextChangedListener(
      object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
          // Nothing
        }

        override fun beforeTextChanged(
          s: CharSequence,
          start: Int,
          count: Int,
          after: Int) {
          // Nothing
        }

        override fun onTextChanged(
          s: CharSequence,
          start: Int,
          before: Int,
          count: Int) {
          loginButton.isEnabled =
            !in_barcode_empty.get() && !in_pin_empty.get() && eulaCheckbox.isChecked
        }
      })

    pinEdit.addTextChangedListener(
      object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
          // Nothing
        }

        override fun beforeTextChanged(
          s: CharSequence,
          start: Int,
          count: Int,
          after: Int) {
          // Nothing
        }

        override fun onTextChanged(
          s: CharSequence,
          start: Int,
          before: Int,
          count: Int) {
          loginButton.isEnabled =
            !in_barcode_empty.get() && !in_pin_empty.get() && eulaCheckbox.isChecked
        }
      })

    this.barcodeEdit = barcodeEdit
    this.scan = barcodeScanButton
    this.pinEdit = pinEdit
    this.login = loginButton
    this.cancel = loginCancelButton
    this.text = text

    val d = this.dialog
    d?.setCanceledOnTouchOutside(true)

    return layout
  }

  private fun onAccountEvent(event: AccountEventLogin): Unit {
    return event.matchLogin<Unit, RuntimeException>(
      { event -> this.onAccountEventLoginSuccess(event) },
      { event -> this.onAccountLoginFailure(event.exception(), loginErrorCodeToLocalizedMessage(this.resources, event.errorCode())) })
  }

  private fun onAccountEventLoginSuccess(
    success: AccountLoginSucceeded): Unit {
    this.logger.debug("login succeeded")

    UIThread.runOnUIThread {
      try {
        this.onLoginSuccess.onLoginSucceeded(success.credentials())
      } catch (e: Exception) {
        this.logger.error("ignored exception in succeeded callback: ", e)
      }

      this.dismiss()
    }
    return Unit.unit()
  }

  private fun onLoginFailed(exception: Exception): AccountEventLogin {
    return AccountLoginFailed.of(ERROR_GENERAL, Option.of(exception))
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
    if (result == null) {
      super.onActivityResult(requestCode, resultCode, data)
      return
    }

    if (result.contents == null) {
      Toast.makeText(this.activity, R.string.barcode_scanning_error, Toast.LENGTH_LONG).show()
    } else {
      this.barcodeEdit.setText(result.contents)
      this.pinEdit.requestFocus()
    }
  }

  companion object {

    /**
     * Transform the given login error code to a localized message.
     *
     * @param resources The resources
     * @param error     The error code
     * @return A localized message
     */

    fun loginErrorCodeToLocalizedMessage(
      resources: Resources,
      error: AccountLoginFailed.ErrorCode): String {

      return when (error) {
        ERROR_PROFILE_CONFIGURATION ->
          /// XXX: This is not correct, need a new translation string for network errors
          resources.getString(R.string.settings_login_failed_server)
        ERROR_NETWORK_EXCEPTION ->
          /// XXX: This is not correct, need a new translation string for network errors
          resources.getString(R.string.settings_login_failed_server)
        ERROR_CREDENTIALS_INCORRECT ->
          resources.getString(R.string.settings_login_failed_credentials)
        ERROR_SERVER_ERROR ->
          resources.getString(R.string.settings_login_failed_server)
        ERROR_ACCOUNT_NONEXISTENT ->
          resources.getString(R.string.settings_login_failed_credentials)
        ERROR_GENERAL ->
          resources.getString(R.string.settings_login_failed_server)
      }
    }

    /**
     * @param resources      The application resources
     * @param message The error message returned by the device activation code
     * @return An appropriate humanly-readable error message
     */

    fun getDeviceActivationErrorMessage(
      resources: Resources,
      message: String): String {

      /*
     * This is absolutely not the way to do this. The nypl-drm-adobe
     * interfaces should be expanded to return values of an enum type. For now,
     * however, these are the only error codes that can be assigned useful
     * messages.
     */

      return if (message.startsWith("E_ACT_TOO_MANY_ACTIVATIONS")) {
        resources.getString(R.string.settings_login_failed_adobe_device_limit)
      } else if (message.startsWith("E_ADEPT_REQUEST_EXPIRED")) {
        resources.getString(
          R.string.settings_login_failed_adobe_device_bad_clock)
      } else {
        resources.getString(R.string.settings_login_failed_device)
      }
    }

    /**
     * Create a new login dialog. The given callback functions will be executed on the UI thread with
     * the results of the login operation. Any strings passed to the callbacks will be properly
     * localized and do not require further processing.
     *
     * @param on_login_success   A function evaluated on login success
     * @param on_login_cancelled A function evaluated on login cancellation
     * @param on_login_failure   A function evaluated on login failure
     * @return A new dialog
     */

    fun newDialog(
      controller: ProfilesControllerType,
      executor: ListeningExecutorService,
      documents: DocumentStoreType,
      account: AccountType,
      on_login_success: LoginSucceededType,
      on_login_cancelled: LoginCancelledType,
      on_login_failure: LoginFailedType): LoginDialog {

      return account.provider().authentication().accept(
        object : OptionVisitorType<AccountProviderAuthenticationDescription, LoginDialog> {
          override fun none(none: None<AccountProviderAuthenticationDescription>): LoginDialog {
            throw IllegalArgumentException(
              "Attempted to log in on an account that does not require authentication!")
          }

          override fun some(some: Some<AccountProviderAuthenticationDescription>): LoginDialog {
            val authentication = some.get()

            val d = LoginDialog()
            d.setRequiredArguments(
              controller,
              executor,
              documents,
              account,
              authentication,
              on_login_success,
              on_login_cancelled,
              on_login_failure)
            return d
          }
        })
    }
  }
}// Fragments must have no-arg constructors.
