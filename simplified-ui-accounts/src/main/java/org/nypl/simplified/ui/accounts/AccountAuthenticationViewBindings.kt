package org.nypl.simplified.ui.accounts

import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView.BufferType.EDITABLE
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Basic
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput.EMAIL_ADDRESS
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput.NO_INPUT
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput.NUMBER_PAD
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsCancelButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsCancelButtonEnabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLoginButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLoginButtonEnabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLogoutButtonDisabled
import org.nypl.simplified.ui.accounts.AccountLoginButtonStatus.AsLogoutButtonEnabled
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A type representing a set of bound views for each possible type of authentication.
 */

sealed class AccountAuthenticationViewBindings {

  /**
   * The view group representing the root of the view hierarchy for the given views.
   */

  abstract val viewGroup: ViewGroup

  /**
   * "Lock" the form views, preventing the user from interacting with them.
   */

  abstract fun lock()

  /**
   * "Unlock" the form views, allowing the user to interact with them.
   */

  abstract fun unlock()

  /**
   * Clear the views. This method should be called exactly once, and all other methods
   * will raise exceptions after this method has been called.
   */

  abstract fun clear()

  /**
   * Set the status of any relevant login button.
   */

  abstract fun setLoginButtonStatus(status: AccountLoginButtonStatus)

  abstract class Base : AccountAuthenticationViewBindings() {
    private val cleared = AtomicBoolean(false)

    protected abstract fun clearActual()

    override fun clear() {
      if (this.cleared.compareAndSet(false, true)) {
        this.clearActual()
      }
    }
  }

  class ViewsForBasic(
    override val viewGroup: ViewGroup,
    val pass: TextInputEditText,
    val passLabel: TextInputLayout,
    val showPass: CheckBox,
    val user: TextInputEditText,
    val userLabel: TextInputLayout,
    val onUsernamePasswordChangeListener: (AccountUsername, AccountPassword) -> Unit,
    val loginButton: Button
  ) : Base() {

    private val logger = LoggerFactory.getLogger(ViewsForBasic::class.java)

    private val userTextListener =
      OnTextChangeListener(
        onChanged = { _, _, _, _ ->
          this.onUsernamePasswordChangeListener.invoke(
            AccountUsername(this.user.text.toString()),
            AccountPassword(this.pass.text.toString())
          )
        }
      )

    private val passTextListener =
      OnTextChangeListener(
        onChanged = { _, _, _, _ ->
          this.onUsernamePasswordChangeListener.invoke(
            AccountUsername(this.user.text.toString()),
            AccountPassword(this.pass.text.toString())
          )
        }
      )

    init {

      /*
       * Configure a checkbox listener that shows and hides the password field. Note that
       * this will trigger the "text changed" listener on the password field, so we lock this
       * checkbox during login/logout to avoid any chance of the UI becoming inconsistent.
       */

      this.showPass.setOnCheckedChangeListener { _, isChecked ->
        setPasswordVisible(isChecked)
      }

      this.user.addTextChangedListener(this.userTextListener)
      this.pass.addTextChangedListener(this.passTextListener)
    }

    private fun setPasswordVisible(visible: Boolean) {
      this.pass.transformationMethod =
        if (visible) {
          null
        } else {
          PasswordTransformationMethod.getInstance()
        }

      // Reset the cursor position
      this.pass.setSelection(this.pass.length())
    }

    override fun lock() {
      this.user.isEnabled = false
      this.pass.isEnabled = false
      this.showPass.isEnabled = false
    }

    override fun unlock() {
      this.user.isEnabled = true
      this.pass.isEnabled = true
      this.showPass.isEnabled = true
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      return when (status) {
        is AsLoginButtonEnabled -> {
          this.loginButton.setText(R.string.accountLogin)
          this.loginButton.isEnabled = true
          this.loginButton.setOnClickListener { status.onClick.invoke() }
        }
        AsLoginButtonDisabled -> {
          this.loginButton.setText(R.string.accountLogin)
          this.loginButton.isEnabled = false
        }
        is AsCancelButtonEnabled -> {
          this.loginButton.setText(R.string.accountCancel)
          this.loginButton.isEnabled = true
          this.loginButton.setOnClickListener { status.onClick.invoke() }
        }
        is AsLogoutButtonEnabled -> {
          this.loginButton.setText(R.string.accountLogout)
          this.loginButton.isEnabled = true
          this.loginButton.setOnClickListener { status.onClick.invoke() }
        }
        AsLogoutButtonDisabled -> {
          this.loginButton.setText(R.string.accountLogout)
          this.loginButton.isEnabled = false
        }
        AsCancelButtonDisabled -> {
          this.loginButton.setText(R.string.accountCancel)
          this.loginButton.isEnabled = false
        }
      }
    }

    override fun clearActual() {
      this.user.removeTextChangedListener(this.userTextListener)
      this.pass.removeTextChangedListener(this.passTextListener)
    }

    fun setUserAndPass(
      user: String,
      password: String
    ) {
      this.user.setText(user, EDITABLE)
      this.pass.setText(password, EDITABLE)
    }

    fun isSatisfied(description: Basic): Boolean {
      val noUserRequired =
        description.keyboard == NO_INPUT
      val noPasswordRequired =
        description.passwordKeyboard == NO_INPUT
      val userOk =
        !this.user.text.isNullOrBlank() || noUserRequired
      val passOk =
        !this.pass.text.isNullOrBlank() || noPasswordRequired
      return userOk && passOk
    }

    fun configureFor(description: Basic) {
      val res = this.viewGroup.resources

      // Set input labels
      this.userLabel.hint =
        description.labels["LOGIN"] ?: res.getString(R.string.accountUserName)
      this.passLabel.hint =
        description.labels["PASSWORD"] ?: res.getString(R.string.accountPassword)
      this.showPass.text =
        res.getString(
          R.string.accountPasswordShow,
          (description.labels["PASSWORD"] ?: res.getString(R.string.accountPassword))
        )

      // Set input types
      this.logger.debug("Setting {} for user input type", description.keyboard)
      this.user.inputType = when (description.keyboard) {
        DEFAULT, NO_INPUT ->
          (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL)
        EMAIL_ADDRESS ->
          (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        NUMBER_PAD ->
          (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL)
      }

      this.logger.debug("Setting {} for password input type", description.passwordKeyboard)
      this.pass.inputType = when (description.passwordKeyboard) {
        DEFAULT, NO_INPUT, EMAIL_ADDRESS ->
          (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        NUMBER_PAD ->
          (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
      }

      // Toggle visibility of fields
      this.userLabel.visibility =
        if (description.keyboard == NO_INPUT) View.GONE else View.VISIBLE
      this.passLabel.visibility =
        if (description.passwordKeyboard == NO_INPUT) View.GONE else View.VISIBLE
      this.showPass.visibility =
        if (description.passwordKeyboard == NO_INPUT) View.GONE else View.VISIBLE

      // Reset password visibility
      setPasswordVisible(this.showPass.isChecked)
    }

    fun getPassword(): AccountPassword {
      return AccountPassword(this.pass.text.toString().trim())
    }

    fun getUser(): AccountUsername {
      return AccountUsername(this.user.text.toString().trim())
    }

    companion object {
      fun bind(
        viewGroup: ViewGroup,
        onUsernamePasswordChangeListener: (AccountUsername, AccountPassword) -> Unit
      ): ViewsForBasic {
        return ViewsForBasic(
          viewGroup = viewGroup,
          pass = viewGroup.findViewById(R.id.authBasicPassField),
          passLabel = viewGroup.findViewById(R.id.authBasicPassLabel),
          user = viewGroup.findViewById(R.id.authBasicUserField),
          userLabel = viewGroup.findViewById(R.id.authBasicUserLabel),
          showPass = viewGroup.findViewById(R.id.authBasicShowPass),
          onUsernamePasswordChangeListener = onUsernamePasswordChangeListener,
          loginButton = viewGroup.findViewById(R.id.authBasicLogin)
        )
      }
    }
  }

  class ViewsForSAML2_0(
    override val viewGroup: ViewGroup,
    private val loginButton: Button
  ) : Base() {

    private var loginText =
      this.viewGroup.resources.getString(R.string.accountLogin)
    private val logoutText =
      this.viewGroup.resources.getString(R.string.accountLogout)
    private val cancelText =
      this.viewGroup.resources.getString(R.string.accountCancel)

    override fun lock() {
      // Nothing
    }

    override fun unlock() {
      // Nothing
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      return when (status) {
        is AsLoginButtonEnabled -> {
          this.loginButton.isEnabled = true
          this.loginButton.text = this.loginText
          this.loginButton.setOnClickListener { status.onClick.invoke() }
        }
        AsLoginButtonDisabled -> {
          this.loginButton.isEnabled = false
          this.loginButton.text = this.loginText
        }
        is AsCancelButtonEnabled -> {
          this.loginButton.isEnabled = true
          this.loginButton.text = this.cancelText
          this.loginButton.setOnClickListener { status.onClick.invoke() }
        }
        is AsLogoutButtonEnabled -> {
          this.loginButton.isEnabled = true
          this.loginButton.text = this.logoutText
          this.loginButton.setOnClickListener { status.onClick.invoke() }
        }
        AsLogoutButtonDisabled -> {
          this.loginButton.isEnabled = false
          this.loginButton.text = this.logoutText
        }
        AsCancelButtonDisabled -> {
          this.loginButton.isEnabled = false
          this.loginButton.text = this.cancelText
        }
      }
    }

    override fun clearActual() {
      // Nothing
    }

    fun configureFor(description: AccountProviderAuthenticationDescription.SAML2_0) {
      this.loginText =
        this.viewGroup.context.resources.getString(
          R.string.accountLoginWith, description.description
        )
      this.loginButton.text = this.loginText
    }

    companion object {
      fun bind(viewGroup: ViewGroup): ViewsForSAML2_0 {
        return ViewsForSAML2_0(
          viewGroup = viewGroup,
          loginButton = viewGroup.findViewById(R.id.authSAMLLogin)
        )
      }
    }
  }

  class ViewsForAnonymous(
    override val viewGroup: ViewGroup
  ) : Base() {

    override fun lock() {
      // Nothing
    }

    override fun unlock() {
      // Nothing
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      // Nothing
    }

    override fun clearActual() {
      // Nothing
    }

    companion object {
      fun bind(viewGroup: ViewGroup): ViewsForAnonymous {
        return ViewsForAnonymous(viewGroup)
      }
    }
  }

  class ViewsForCOPPAAgeGate(
    override val viewGroup: ViewGroup,
    val over13: SwitchCompat
  ) : Base() {

    override fun lock() {
      // Nothing
    }

    override fun unlock() {
      // Nothing
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      // Nothing
    }

    override fun clearActual() {
      this.over13.setOnClickListener {}
    }

    fun setState(
      isOver13: Boolean,
      onAgeCheckboxClicked: (View) -> Unit
    ) {
      this.over13.setOnClickListener {}
      this.over13.isChecked = isOver13
      this.over13.setOnClickListener(onAgeCheckboxClicked)
      this.over13.isEnabled = true
    }

    companion object {
      fun bind(viewGroup: ViewGroup): ViewsForCOPPAAgeGate {
        return ViewsForCOPPAAgeGate(
          viewGroup = viewGroup,
          over13 = viewGroup.findViewById(R.id.authCOPPASwitch)
        )
      }
    }
  }

  class ViewsForOAuthWithIntermediary(
    override val viewGroup: ViewGroup
  ) : Base() {

    override fun lock() {
      // Nothing
    }

    override fun unlock() {
      // Nothing
    }

    override fun setLoginButtonStatus(status: AccountLoginButtonStatus) {
      // Nothing
    }

    override fun clearActual() {
      // Nothing
    }

    companion object {
      fun bind(viewGroup: ViewGroup): ViewsForOAuthWithIntermediary {
        return ViewsForOAuthWithIntermediary(viewGroup)
      }
    }
  }
}
