package org.nypl.simplified.app.settings

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.junreachable.UnimplementedCodeException
import com.tenmiles.helpstack.HSHelpStack
import com.tenmiles.helpstack.gears.HSDeskGear
import org.joda.time.LocalDate
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.app.NavigationDrawerActivity
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ReportIssueActivity
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.WebViewActivity
import org.nypl.simplified.app.utilities.ErrorDialogUtilities
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.futures.FluentFutureExtensions.onException
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.slf4j.LoggerFactory

/**
 * An activity displaying settings for a specific account.
 */

class SettingsAccountActivity : NavigationDrawerActivity() {

  private val logger = LoggerFactory.getLogger(SettingsAccountActivity::class.java)

  private lateinit var ageCheckbox: CheckBox
  private lateinit var accountNameText: TextView
  private lateinit var accountSubtitleText: TextView
  private lateinit var accountIcon: ImageView
  private lateinit var barcodeText: EditText
  private lateinit var pinText: EditText
  private lateinit var tableWithCode: TableLayout
  private lateinit var tableSignup: TableLayout
  private lateinit var login: Button
  private lateinit var reportIssue: TableRow
  private lateinit var supportCenter: TableRow
  private lateinit var eulaCheckbox: CheckBox
  private lateinit var barcodeLabel: TextView
  private lateinit var pinLabel: TextView
  private lateinit var pinReveal: CheckBox
  private lateinit var signup: Button
  private lateinit var privacy: TableRow
  private lateinit var license: TableRow
  private lateinit var account: AccountType
  private lateinit var profile: ProfileReadableType
  private lateinit var syncSwitch: Switch
  private lateinit var actionLayout: ViewGroup
  private lateinit var actionText: TextView
  private lateinit var actionProgress: ProgressBar

  private var accountEventSubscription: ObservableSubscriptionType<AccountEvent>? = null

  override fun navigationDrawerGetActivityTitle(resources: Resources): String {
    return resources.getString(R.string.settings)
  }

  override fun navigationDrawerShouldShowIndicator(): Boolean {
    return true
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent) {

    /*
     * Retrieve the PIN from the activity that was launched to collect it.
     */

    if (requestCode == 1) {
      val pinReveal = this.findViewById<CheckBox>(R.id.settings_reveal_password)

      if (resultCode == Activity.RESULT_OK) {
        val pinText = this.findViewById<TextView>(R.id.settings_pin_text)
        pinText.transformationMethod = HideReturnsTransformationMethod.getInstance()
        pinReveal.isChecked = true
      } else {
        // The user canceled or didn't complete the lock screen
        // operation. Go to error/cancellation flow.
        pinReveal.isChecked = false
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.show_eula) {
      val eulaIntent = Intent(this, WebViewActivity::class.java)
      this.account.provider.eula?.let { eula ->
        val argumentBundle = Bundle()
        WebViewActivity.setActivityArguments(
          arguments = argumentBundle,
          uri = eula.toString(),
          title = this.resources.getString(R.string.settings_eula))
        eulaIntent.putExtras(argumentBundle)
        this.startActivity(eulaIntent)
      }
      return true
    }

    return when (item.itemId) {
      android.R.id.home -> {
        this.onBackPressed()
        true
      }

      else -> {
        super.onOptionsItemSelected(item)
      }
    }
  }

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    val inflater = this.layoutInflater
    val contentArea = this.contentFrame
    val layout =
      inflater.inflate(R.layout.settings_account, contentArea, false) as ViewGroup
    contentArea.addView(layout)
    contentArea.requestLayout()

    val extras = this.intent.extras
    this.profile = Simplified.getProfilesController().profileCurrent()
    this.account = getAccount(extras)

    this.accountNameText =
      this.findViewById(android.R.id.text1)
    this.accountSubtitleText =
      this.findViewById(android.R.id.text2)
    this.accountIcon =
      this.findViewById(R.id.account_icon)
    this.tableWithCode =
      this.findViewById(R.id.settings_login_table_with_code)
    this.barcodeLabel =
      this.findViewById(R.id.settings_barcode_label)
    this.barcodeText =
      this.findViewById(R.id.settings_barcode_text)
    this.pinText =
      this.findViewById(R.id.settings_pin_text)
    this.pinLabel =
      this.findViewById(R.id.settings_pin_label)
    this.pinReveal =
      this.findViewById(R.id.settings_reveal_password)
    this.login =
      this.findViewById(R.id.settings_login)
    this.tableSignup =
      this.findViewById(R.id.settings_signup_table)
    this.reportIssue =
      this.findViewById(R.id.report_issue)
    this.supportCenter =
      this.findViewById(R.id.support_center)
    this.eulaCheckbox =
      this.findViewById(R.id.eula_checkbox)
    this.ageCheckbox =
      this.findViewById(R.id.age13_checkbox)
    this.signup =
      this.findViewById(R.id.settings_signup)
    this.privacy =
      this.findViewById(R.id.link_privacy)
    this.license =
      this.findViewById(R.id.link_license)
    this.syncSwitch =
      this.findViewById(R.id.sync_switch)
    this.actionLayout =
      this.findViewById(R.id.settings_action_layout)
    this.actionText =
      this.actionLayout.findViewById(R.id.settings_action_text)
    this.actionProgress =
      this.actionLayout.findViewById(R.id.settings_action_progress)

    val accountProvider = this.account.provider
    this.accountNameText.text = accountProvider.displayName

    val subtitle = accountProvider.subtitle
    if (subtitle != null) {
      this.accountSubtitleText.text = subtitle
    } else {
      this.accountSubtitleText.text = ""
    }

    /*
     * Show the "Support Center" section if the provider offers one.
     */

    if (accountProvider.supportEmail != null) {
      this.reportIssue.visibility = View.VISIBLE
      this.reportIssue.setOnClickListener {
        val intent = Intent(this, ReportIssueActivity::class.java)
        val argumentBundle = Bundle()
        argumentBundle.putSerializable("selected_account", this.account.id.uuid.toString())
        intent.putExtras(argumentBundle)
        this.startActivity(intent)
      }
    } else {
      this.reportIssue.visibility = View.GONE
    }

    /*
     * Show the "Help Center" section if the provider offers one.
     */

    if (accountProvider.supportsHelpCenter) {
      this.supportCenter.visibility = View.VISIBLE
      this.supportCenter.setOnClickListener {
        val stack = HSHelpStack.getInstance(this)
        val gear = HSDeskGear(" ", " ", null)
        stack.gear = gear
        stack.showHelp(this)
      }
    } else {
      this.supportCenter.visibility = View.GONE
    }

    /*
     * Show the "Card Creator" section if the provider supports it.
     */

    if (accountProvider.supportsCardCreator) {
      this.tableSignup.visibility = View.VISIBLE
      this.signup.setOnClickListener {
        throw UnimplementedCodeException()
      }
      this.signup.setText(R.string.need_card_button)
    } else {
      this.tableSignup.visibility = View.GONE
    }

    /*
     * Configure the barcode and PIN entry section. This will be hidden entirely if the
     * provider doesn't support/require authentication.
     */

    // XXX: Get labels from the current authentication document.
    // XXX: This should be per-account
    val docs = Simplified.getDocumentStore()
    this.pinText.transformationMethod = PasswordTransformationMethod.getInstance()
    this.handlePinReveal(this.pinText, this.pinReveal)

    if (accountProvider.authentication != null) {
      this.tableWithCode.visibility = View.VISIBLE
      this.login.visibility = View.VISIBLE
      this.configureLoginFieldVisibilityAndContents()
    } else {
      this.tableWithCode.visibility = View.GONE
      this.login.visibility = View.GONE
    }

    /*
     * Show the "Privacy Policy" section if the provider has one.
     */

    val privacyPolicy = accountProvider.privacyPolicy
    if (privacyPolicy != null) {
      this.privacy.visibility = View.VISIBLE
      this.privacy.setOnClickListener {
        val intent = Intent(this, WebViewActivity::class.java)
        val intentBundle = Bundle()
        WebViewActivity.setActivityArguments(
          intentBundle,
          privacyPolicy.toString(),
          "Privacy Policy")
        intent.putExtras(intentBundle)
        this.startActivity(intent)
      }
    } else {
      this.privacy.visibility = View.GONE
    }

    /*
     * Show the "Content License" section if the provider has one.
     */

    val license = accountProvider.license
    if (license != null) {
      this.license.visibility = View.VISIBLE
      this.license.setOnClickListener {
        val intent = Intent(this, WebViewActivity::class.java)
        val intentBundle = Bundle()
        WebViewActivity.setActivityArguments(
          intentBundle,
          license.toString(),
          "Content Licenses")
        intent.putExtras(intentBundle)
        this.startActivity(intent)
      }
    } else {
      this.license.visibility = View.GONE
    }

    /*
     * Configure the EULA views if there is one.
     */

    val eulaOpt = docs.eula
    if (eulaOpt is Some<EULAType>) {
      val eula = eulaOpt.get()
      this.eulaCheckbox.isChecked = eula.eulaHasAgreed()
      this.eulaCheckbox.isEnabled = true
      this.eulaCheckbox.setOnCheckedChangeListener { _, checked -> eula.eulaSetHasAgreed(checked) }

      if (eula.eulaHasAgreed()) {
        this.logger.debug("EULA: agreed")
      } else {
        this.logger.debug("EULA: not agreed")
      }
    } else {
      this.logger.debug("EULA: unavailable")
    }

    /*
     * The age-gate checkbox will be conditonally configured based on
     * the account state.
     */

    this.ageCheckbox.visibility = View.INVISIBLE

    /*
     * Configure the syncing switch.
     */

    if (accountProvider.supportsSimplyESynchronization) {
      this.syncSwitch.isEnabled = true
      this.syncSwitch.isChecked = this.account.preferences.bookmarkSyncingPermitted
      this.syncSwitch.setOnCheckedChangeListener { _, isEnabled ->
        this.account.setPreferences(
          this.account.preferences.copy(bookmarkSyncingPermitted = isEnabled))
      }
    } else {
      this.syncSwitch.isEnabled = false
      this.syncSwitch.isChecked = false
    }

    /*
     * Configure the logo.
     */

    val logo = accountProvider.logo
    if (logo != null) {
      Simplified.getLocalImageLoader()
        .load(logo.toString())
        .into(this.accountIcon)
    }
  }

  override fun onStart() {
    super.onStart()

    this.navigationDrawerShowUpIndicatorUnconditionally()

    this.accountEventSubscription =
      Simplified.getProfilesController()
        .accountEvents()
        .subscribe { event -> this.onAccountEvent(event) }

    this.configureLoginFieldVisibilityAndContents()
  }

  override fun onStop() {
    super.onStop()

    this.accountEventSubscription?.unsubscribe()
  }

  private fun onAccountEvent(event: AccountEvent): Unit {
    return if (event is AccountEventLoginStateChanged) {
      if (event.accountID != this.account.id) {
        return Unit.unit()
      }

      when (val state = event.state) {
        AccountNotLoggedIn ->
          this.onAccountEventNotLoggedIn()
        is AccountLoggingIn ->
          this.onAccountEventLoggingIn()
        is AccountLoginFailed ->
          this.onAccountEventLoginFailed(state)
        is AccountLoggedIn ->
          this.onAccountEventLoginSucceeded()
        is AccountLoggingOut ->
          this.onAccountEventLoggingOut()
        is AccountLogoutFailed ->
          this.onAccountEventLogoutFailed(state)
      }
    } else {
      Unit.unit()
    }
  }

  private fun onAccountEventLoginFailed(failed: AccountLoginFailed): Unit {
    this.logger.debug("onAccountEventLoginFailed: {}", failed)

    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      this.logger,
      failed.steps.lastOrNull()?.resolution,
      failed.steps.lastOrNull()?.exception) {
      this.login.isEnabled = true
    }

    UIThread.runOnUIThread { this.configureLoginFieldVisibilityAndContents() }
    return Unit.unit()
  }

  private fun onAccountEventLoggingIn(): Unit {
    this.logger.debug("onAccountEventLoggingIn")

    UIThread.runOnUIThread { this.configureLoginFieldVisibilityAndContents() }
    return Unit.unit()
  }

  private fun onAccountEventLoginSucceeded(): Unit {
    this.logger.debug("onAccountEventLoginSucceeded")

    UIThread.runOnUIThread { this.configureLoginFieldVisibilityAndContents() }
    return Unit.unit()
  }

  private fun onAccountEventLogoutFailed(failed: AccountLogoutFailed): Unit {
    this.logger.debug("onAccountEventLogoutFailed: {}", failed)

    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      this.logger,
      failed.steps.lastOrNull()?.resolution,
      failed.steps.lastOrNull()?.exception) {
      this.login.isEnabled = true
    }

    UIThread.runOnUIThread { this.configureLoginFieldVisibilityAndContents() }
    return Unit.unit()
  }

  private fun onAccountEventLoggingOut(): Unit {
    this.logger.debug("onAccountEventLoggingOut")

    UIThread.runOnUIThread { this.configureLoginFieldVisibilityAndContents() }
    return Unit.unit()
  }

  private fun onAccountEventNotLoggedIn(): Unit {
    this.logger.debug("onAccountEventNotLoggedIn")

    UIThread.runOnUIThread { this.configureLoginFieldVisibilityAndContents() }
    return Unit.unit()
  }

  /**
   * Synthesize a fake date of birth based on the current date and given age in years.
   */

  private fun synthesizeDateOfBirth(years: Int): ProfileDateOfBirth =
    ProfileDateOfBirth(
      date = LocalDate.now().minusYears(years),
      isSynthesized = true)

  private fun configureLoginFieldVisibilityAndContents() {
    val state = this.account.loginState

    val ageGateRequired =
      this.account.provider.hasAgeGate()

    this.ageCheckbox.visibility =
      if (ageGateRequired) {
        View.VISIBLE
      } else {
        View.INVISIBLE
      }

    return when (state) {
      AccountNotLoggedIn -> {
        this.actionLayout.visibility = View.INVISIBLE
        this.pinText.setText("")
        this.barcodeText.setText("")
        this.ageCheckbox.isChecked = this.isOver13()
        this.ageCheckbox.isEnabled = true
        this.ageCheckbox.setOnClickListener(onAgeCheckboxClicked())

        this.configureEnableLoginForm()
      }

      is AccountLoggedIn -> {
        val credentials = state.credentials

        this.actionLayout.visibility = View.INVISIBLE
        this.ageCheckbox.isChecked = this.isOver13()
        this.ageCheckbox.isEnabled = true
        this.ageCheckbox.setOnClickListener(onAgeCheckboxClicked())

        this.pinText.setText(credentials.pin().value())
        this.pinText.isEnabled = false

        this.barcodeText.setText(credentials.barcode().value())
        this.barcodeText.isEnabled = false

        this.login.isEnabled = true
        this.login.setText(R.string.settings_log_out)
        this.login.setOnClickListener {
          this.configureDisableLoginForm()
          this.tryLogout()
        }
      }

      is AccountLoggingOut -> {
        this.actionLayout.visibility = View.VISIBLE
        this.actionProgress.visibility = View.VISIBLE
        this.actionText.text = state.status
        this.ageCheckbox.isChecked = this.isOver13()
        this.ageCheckbox.isEnabled = false
        this.configureDisableLoginForm()
      }

      is AccountLoggingIn -> {
        this.actionLayout.visibility = View.VISIBLE
        this.actionProgress.visibility = View.VISIBLE
        this.actionText.text = state.status
        this.ageCheckbox.isChecked = this.isOver13()
        this.ageCheckbox.isEnabled = false
        this.configureDisableLoginForm()
      }

      is AccountLoginFailed -> {
        this.actionLayout.visibility = View.VISIBLE
        this.actionProgress.visibility = View.GONE
        this.actionText.text = state.steps.last().resolution
        this.ageCheckbox.isChecked = this.isOver13()
        this.ageCheckbox.isEnabled = true
        this.ageCheckbox.setOnClickListener {}
        this.configureEnableLoginForm()
      }

      is AccountLogoutFailed -> {
        this.pinText.setText(state.credentials.pin().value())
        this.pinText.isEnabled = false

        this.barcodeText.setText(state.credentials.barcode().value())
        this.barcodeText.isEnabled = false

        this.actionLayout.visibility = View.VISIBLE
        this.actionProgress.visibility = View.GONE
        this.actionText.text = state.steps.last().resolution

        this.ageCheckbox.isChecked = this.isOver13()
        this.ageCheckbox.isEnabled = true
        this.ageCheckbox.setOnClickListener {}

        this.login.isEnabled = true
        this.login.setText(R.string.settings_log_out)
        this.login.setOnClickListener {
          this.configureDisableLoginForm()
          this.tryLogout()
        }
      }
    }
  }

  /**
   * A click listener for the age checkbox. If the user wants to change their age, then
   * this must trigger an account logout. If the user cancels the dialog, the checkbox must
   * be set to the opposite of what it was previously set to. This looks strange but it's actually
   * because the checkbox will be checked/unchecked when the user initially clicks it, and then
   * when the dialog is cancelled, the checkbox must be unchecked/checked again to return it to
   * the original state it was in.
   */

  private fun onAgeCheckboxClicked(): (View) -> kotlin.Unit = {
    AlertDialog.Builder(this)
      .setTitle(R.string.age_verification_confirm_title)
      .setMessage(R.string.age_verification_confirm_under13_check)
      .setNegativeButton(R.string.age_verification_cancel_delete, { _, _ ->
        this.ageCheckbox.isChecked = !this.ageCheckbox.isChecked
      })
      .setPositiveButton(R.string.age_verification_confirm_delete, { _, _ ->
        this.configureDisableLoginForm()
        if (this.ageCheckbox.isChecked) {
          this.setOver13()
        } else {
          this.setUnder13()
        }
        this.tryLogout()
      })
      .create()
      .show()
  }

  private fun setUnder13() {
    Simplified.getProfilesController()
      .profilePreferencesUpdate(
        this.profile.preferences()
          .toBuilder()
          .setDateOfBirth(this.synthesizeDateOfBirth(0))
          .build())
  }

  private fun setOver13() {
    Simplified.getProfilesController()
      .profilePreferencesUpdate(
        this.profile.preferences()
          .toBuilder()
          .setDateOfBirth(this.synthesizeDateOfBirth(14))
          .build())
  }

  private fun isOver13(): Boolean {
    val age = this.profile.preferences().dateOfBirth()
    return if (age is Some<ProfileDateOfBirth>) {
      age.get().yearsOld(LocalDate.now()) >= 13
    } else {
      false
    }
  }

  private fun configureEnableLoginForm() {
    this.pinText.isEnabled = true
    this.barcodeText.isEnabled = true

    this.login.isEnabled = true
    this.login.setText(R.string.settings_log_in)
    this.login.setOnClickListener {
      this.configureDisableLoginForm()
      this.tryLogin()
    }
  }

  private fun configureDisableLoginForm() {
    this.login.isEnabled = false
    this.pinText.isEnabled = false
    this.barcodeText.isEnabled = false
  }

  private fun tryLogout(): Unit {
    Simplified.getProfilesController()
      .profileAccountLogout(this.account.id)
      .onException(Exception::class.java) { exception: Exception ->
        this.logger.error("error during logout: ", exception)
        null
      }

    return Unit.unit()
  }

  private fun tryLogin(): Unit {
    val credentials =
      AccountAuthenticationCredentials.builder(
        AccountPIN.create(this.pinText.text.toString()),
        AccountBarcode.create(this.barcodeText.text.toString()))
        .build()

    Simplified.getProfilesController()
      .profileAccountLogin(this.account.id, credentials)
      .onException(Exception::class.java) { exception ->
        this.logger.error("error during login: ", exception)
        null
      }
    return Unit.unit()
  }

  private fun handlePinReveal(
    pinText: TextView,
    pinReveal: CheckBox) {

    /*
     * Add a listener that reveals/hides the password field.
     */

    pinReveal.setOnCheckedChangeListener { _, checked ->
      if (checked) {
        val keyguardManager =
          this.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isKeyguardSecure) {
          // Show a message that the user hasn't set up a lock screen.
          Toast.makeText(this, R.string.settings_screen_Lock_not_setup, Toast.LENGTH_LONG).show()
          pinReveal.isChecked = false
        } else {
          val intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null)
          if (intent != null) {
            this.startActivityForResult(intent, 1)
          }
        }
      } else {
        pinText.transformationMethod = PasswordTransformationMethod.getInstance()
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = this.menuInflater
    inflater.inflate(R.menu.eula, menu)
    return true
  }

  companion object {

    const val ACCOUNT_ID = "org.nypl.simplified.app.MainSettingsAccountActivity.account_id"

    /**
     * Get either the currently selected account, or the account that was passed explicitly to the
     * activity.
     */

    private fun getAccount(extras: Bundle?): AccountType {
      return try {
        val profile = Simplified.getProfilesController().profileCurrent()
        if (extras != null && extras.containsKey(this.ACCOUNT_ID)) {
          val accountID = extras.getSerializable(this.ACCOUNT_ID) as AccountID
          profile.accounts()[accountID]!!
        } else {
          profile.accountCurrent()
        }
      } catch (e: ProfileNoneCurrentException) {
        throw IllegalStateException(e)
      }
    }
  }
}
