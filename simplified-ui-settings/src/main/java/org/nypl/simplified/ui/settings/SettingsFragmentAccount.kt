package org.nypl.simplified.ui.settings

import android.app.AlertDialog
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
import androidx.fragment.app.Fragment
import com.io7m.jfunctional.Some
import io.reactivex.disposables.Disposable
import org.joda.time.LocalDate
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
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilePreferencesChanged
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory

/**
 * A fragment that shows settings for a single account.
 */

class SettingsFragmentAccount : Fragment() {

  private val logger = LoggerFactory.getLogger(SettingsFragmentAccount::class.java)

  private lateinit var account: AccountType
  private lateinit var accountIcon: ImageView
  private lateinit var accountSubtitle: TextView
  private lateinit var accountTitle: TextView
  private lateinit var authentication: ViewGroup
  private lateinit var authenticationBasic: ViewGroup
  private lateinit var authenticationBasicPass: EditText
  private lateinit var authenticationBasicPassListener: OnTextChangeListener
  private lateinit var authenticationBasicShowPass: CheckBox
  private lateinit var authenticationBasicUser: EditText
  private lateinit var authenticationBasicUserListener: OnTextChangeListener
  private lateinit var authenticationCOPPA: ViewGroup
  private lateinit var authenticationCOPPAOver13: Switch
  private lateinit var bookmarkSync: ViewGroup
  private lateinit var bookmarkSyncCheck: Switch
  private lateinit var documents: DocumentStoreType
  private lateinit var eulaCheckbox: CheckBox
  private lateinit var imageLoader: ImageLoaderType
  private lateinit var login: ViewGroup
  private lateinit var loginButton: Button
  private lateinit var loginProgress: ProgressBar
  private lateinit var loginProgressText: TextView
  private lateinit var parameters: SettingsFragmentAccountParameters
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType
  private var accountSubscription: Disposable? = null
  private var profileSubscription: Disposable? = null

  private val parametersId =
    org.nypl.simplified.ui.settings.SettingsFragmentAccount.Companion.PARAMETERS_ID

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.settings.SettingsFragmentAccount.parameters"

    /**
     * Create a new settings fragment for the given account parameters.
     */

    fun create(parameters: SettingsFragmentAccountParameters): SettingsFragmentAccount {
      val arguments = Bundle()
      arguments.putSerializable(this.PARAMETERS_ID, parameters)
      val fragment = SettingsFragmentAccount()
      fragment.arguments = arguments
      return fragment
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.parameters = this.arguments!![this.parametersId] as SettingsFragmentAccountParameters

    val services = Services.serviceDirectory()
    
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.documents =
      services.requireService(DocumentStoreType::class.java)
    this.imageLoader =
      services.requireService(ImageLoaderType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.settings_account, container, false)

    this.accountTitle =
      layout.findViewById(R.id.accountCellTitle)
    this.accountSubtitle =
      layout.findViewById(R.id.accountCellSubtitle)
    this.accountIcon =
      layout.findViewById(R.id.accountCellIcon)

    this.authentication =
      layout.findViewById(R.id.auth)

    this.authenticationCOPPA =
      this.authentication.findViewById(R.id.authCOPPA)
    this.authenticationCOPPAOver13 =
      this.authenticationCOPPA.findViewById(R.id.authCOPPASwitch)

    this.authenticationBasic =
      this.authentication.findViewById(R.id.authBasic)
    this.authenticationBasicUser =
      this.authenticationBasic.findViewById(R.id.authBasicUserNameField)
    this.authenticationBasicUserListener =
      OnTextChangeListener(this::onBasicUserChanged)
    this.authenticationBasicPass =
      this.authenticationBasic.findViewById(R.id.authBasicPasswordField)
    this.authenticationBasicPassListener =
      OnTextChangeListener(this::onBasicPasswordChanged)
    this.authenticationBasicShowPass =
      this.authenticationBasic.findViewById(R.id.authBasicPasswordShow)

    this.authenticationCOPPA.visibility = View.INVISIBLE

    this.bookmarkSync =
      layout.findViewById(R.id.settingsSyncBookmarks)
    this.bookmarkSyncCheck =
      this.bookmarkSync.findViewById(R.id.settingsSyncBookmarksCheck)

    this.login =
      layout.findViewById(R.id.settingsLogin)
    this.loginProgress =
      layout.findViewById(R.id.settingsLoginProgress)
    this.loginProgressText =
      layout.findViewById(R.id.settingsLoginProgressText)
    this.loginButton =
      layout.findViewById(R.id.settingsLoginButton)
    this.eulaCheckbox =
      layout.findViewById(R.id.settingsEULACheckbox)

    this.loginButton.isEnabled = false
    this.loginProgress.visibility = View.INVISIBLE
    this.loginProgressText.text = ""
    return layout
  }

  @UiThread
  private fun onBasicUserChanged(
    sequence: CharSequence,
    start: Int,
    before: Int,
    count: Int
  ) {
    this.uiThread.checkIsUIThread()
    this.loginButton.isEnabled = this.determineLoginIsSatisfied()
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

        val userOk = this.authenticationBasicUser.text.isNotBlank()
        val passOk = this.authenticationBasicPass.text.isNotBlank()
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
  private fun onBasicPasswordChanged(
    sequence: CharSequence,
    start: Int,
    before: Int,
    count: Int
  ) {
    this.uiThread.checkIsUIThread()
    this.loginButton.isEnabled = this.determineLoginIsSatisfied()
  }

  override fun onStart() {
    super.onStart()

    try {
      this.account =
        this.profilesController.profileCurrent()
          .account(this.parameters.accountId)
    } catch (e: AccountsDatabaseNonexistentException) {
      this.logger.error("account no longer exists: ", e)
      this.findNavigationController().popBackStack()
      return
    }

    this.configureToolbar()

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
        this.loginButton.isEnabled = this.determineLoginIsSatisfied()
      }
    } else {
      this.eulaCheckbox.visibility = View.INVISIBLE
    }

    this.authenticationBasicUser.addTextChangedListener(this.authenticationBasicUserListener)
    this.authenticationBasicPass.addTextChangedListener(this.authenticationBasicPassListener)

    /*
     * Configure the COPPA age gate switch. If the user changes their age, a log out
     * is required.
     */

    this.authenticationCOPPAOver13.setOnClickListener {}
    this.authenticationCOPPAOver13.isChecked = this.isOver13()
    this.authenticationCOPPAOver13.setOnClickListener(onAgeCheckboxClicked())
    this.authenticationCOPPAOver13.isEnabled = true

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

    this.accountSubscription =
      this.profilesController.accountEvents()
        .subscribe(this::onAccountEvent)
    this.profileSubscription =
      this.profilesController.profileEvents()
        .subscribe(this::onProfileEvent)

    this.reconfigureAccountUI()
  }

  private fun configureToolbar() {
    val host = this.activity
    if (host is ToolbarHostType) {
      host.toolbarClearMenu()
      host.toolbarSetTitleSubtitle(
        title = this.requireContext().getString(R.string.settingsAccounts),
        subtitle = this.account.provider.displayName
      )
      host.toolbarSetBackArrowConditionally(
        context = host,
        shouldArrowBePresent = {
          this.findNavigationController().backStackSize() > 1
        },
        onArrowClicked = {
          this.findNavigationController().popBackStack()
        })
    } else {
      throw IllegalStateException("The activity ($host) hosting this fragment must implement ${ToolbarHostType::class.java}")
    }
  }

  override fun onStop() {
    super.onStop()

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
      is ProfilePreferencesChanged -> {
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

    this.bookmarkSyncCheck.isChecked = this.account.preferences.bookmarkSyncingPermitted
    this.bookmarkSyncCheck.isEnabled = this.account.provider.supportsSimplyESynchronization

    when (this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate -> {
        this.authentication.visibility = View.VISIBLE
        this.authenticationCOPPA.visibility = View.VISIBLE
        this.authenticationBasic.visibility = View.INVISIBLE
      }
      is AccountProviderAuthenticationDescription.Basic -> {
        this.authentication.visibility = View.VISIBLE
        this.authenticationCOPPA.visibility = View.INVISIBLE
        this.authenticationBasic.visibility = View.VISIBLE
      }
      null -> {
        this.authentication.visibility = View.GONE
        this.login.visibility = View.GONE
        this.loginButton.isEnabled = false
      }
    }

    when (val loginState = this.account.loginState) {
      AccountLoginState.AccountNotLoggedIn -> {
        this.authenticationBasicUser.setText("")
        this.authenticationBasicPass.setText("")
        this.loginProgress.visibility = View.INVISIBLE
        this.loginProgressText.text = ""
        this.loginButton.setText(R.string.settingsLogin)
        this.loginFormUnlock()
        this.loginButton.setOnClickListener {
          this.loginFormLock()
          this.tryLogin()
        }
      }

      is AccountLoginState.AccountLoggingIn -> {
        this.loginProgress.visibility = View.VISIBLE
        this.loginProgressText.text = loginState.status
        this.loginButton.setText(R.string.settingsLogin)
        this.loginFormLock()
      }

      is AccountLoginState.AccountLoginFailed -> {
        this.loginProgress.visibility = View.INVISIBLE
        this.loginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.loginButton.setText(R.string.settingsLogin)
        this.loginFormUnlock()
      }

      is AccountLoginState.AccountLoggedIn -> {
        this.authenticationBasicUser.setText(
          loginState.credentials.barcode().value())
        this.authenticationBasicPass.setText(
          loginState.credentials.pin().value())

        this.loginProgress.visibility = View.INVISIBLE
        this.loginProgressText.text = ""

        this.loginFormLock()
        this.loginButton.setText(R.string.settingsLogout)
        this.loginButton.isEnabled = true
        this.loginButton.setOnClickListener {
          this.loginFormLock()
          this.tryLogout()
        }
      }

      is AccountLoginState.AccountLoggingOut -> {
        this.authenticationBasicUser.setText(
          loginState.credentials.barcode().value())
        this.authenticationBasicPass.setText(
          loginState.credentials.pin().value())

        this.loginProgress.visibility = View.VISIBLE
        this.loginProgressText.text = loginState.status
        this.loginButton.setText(R.string.settingsLogout)
        this.loginFormLock()
      }

      is AccountLoginState.AccountLogoutFailed -> {
        this.authenticationBasicUser.setText(
          loginState.credentials.barcode().value())
        this.authenticationBasicPass.setText(
          loginState.credentials.pin().value())

        this.loginProgress.visibility = View.INVISIBLE
        this.loginProgressText.text = loginState.taskResult.steps.last().resolution.message
        this.loginButton.setText(R.string.settingsLogout)
        this.loginFormLock()
        this.loginButton.isEnabled = true
      }
    }
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
    this.loginButton.isEnabled = false
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
    this.loginButton.isEnabled = this.determineLoginIsSatisfied()
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
    return when (this.account.provider.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        Unit
      null ->
        Unit
      is AccountProviderAuthenticationDescription.Basic -> {
        val accountPin =
          AccountPIN.create(this.authenticationBasicPass.text.toString())
        val accountBarcode =
          AccountBarcode.create(this.authenticationBasicUser.text.toString())
        val credentials =
          AccountAuthenticationCredentials.builder(accountPin, accountBarcode)
            .build()

        this.profilesController.profileAccountLogin(this.account.id, credentials)
        Unit
      }
    }
  }

  private fun tryLogout() {
    return when (this.account.provider.authentication) {
      null ->
        Unit

      /*
       * Although COPPA age-gated accounts don't require "logging in" or "logging out" exactly,
       * we *do* want local books to be deleted as part of a logout attempt.
       */

      is AccountProviderAuthenticationDescription.COPPAAgeGate,
      is AccountProviderAuthenticationDescription.Basic -> {
        this.profilesController.profileAccountLogout(this.account.id)
        Unit
      }
    }
  }

  private fun setUnder13() {
    val profile = this.profilesController.profileCurrent()
    this.profilesController.profilePreferencesUpdate(
      profile.preferences()
        .toBuilder()
        .setDateOfBirth(this.synthesizeDateOfBirth(0))
        .build())
  }

  private fun setOver13() {
    val profile = this.profilesController.profileCurrent()
    this.profilesController.profilePreferencesUpdate(
      profile.preferences()
        .toBuilder()
        .setDateOfBirth(this.synthesizeDateOfBirth(14))
        .build())
  }

  private fun isOver13(): Boolean {
    val profile = this.profilesController.profileCurrent()
    val age = profile.preferences().dateOfBirth()
    return if (age is Some<ProfileDateOfBirth>) {
      age.get().yearsOld(LocalDate.now()) >= 13
    } else {
      false
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

  private fun onAgeCheckboxClicked(): (View) -> Unit = {
    AlertDialog.Builder(this.requireContext())
      .setTitle(R.string.settingsCOPPADeleteBooks)
      .setMessage(R.string.settingsCOPPADeleteBooksConfirm)
      .setNegativeButton(R.string.settingsCancel) { _, _ ->
        this.authenticationCOPPAOver13.isChecked = !this.authenticationCOPPAOver13.isChecked
      }
      .setPositiveButton(R.string.settingsDelete) { _, _ ->
        this.loginFormLock()
        if (this.authenticationCOPPAOver13.isChecked) {
          this.setOver13()
        } else {
          this.setUnder13()
        }
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
      date = LocalDate.now().minusYears(years),
      isSynthesized = true)

  private fun findNavigationController(): SettingsNavigationControllerType {
    return NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = SettingsNavigationControllerType::class.java
    )
  }
}
