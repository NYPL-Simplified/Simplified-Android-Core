package org.nypl.simplified.tests.android.ui.settings

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.io7m.jfunctional.Option
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountBarcode
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPIN
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderImmutable
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.tests.MockEULA
import org.nypl.simplified.tests.MockProfilesController
import org.nypl.simplified.tests.android.NavigationHostActivity
import org.nypl.simplified.tests.android.R
import org.nypl.simplified.tests.android.ui.FragmentBaseTest
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import java.net.URI

@RunWith(AndroidJUnit4::class)
class SettingsFragmentAccountTest
  : FragmentBaseTest<SettingsFragmentAccountActivity>(SettingsFragmentAccountActivity::class.java) {

  /**
   * A basic-auth account that isn't logged in has the correct view configuration.
   */

  @Test(timeout = 20_000L)
  fun testBasicNotLoggedInNoEULA()
  {
    val profilesController =
      MockProfilesController
    val documents =
      Mockito.mock(DocumentStoreType::class.java)
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(DocumentStoreType::class.java, documents)
    services.putService(ProfilesControllerType::class.java, profilesController)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object: UIThreadServiceType {})

    val accountCurrent = profilesController.profileAccountCurrent()
    accountCurrent.loginState as AccountLoginState.AccountNotLoggedIn
    SettingsFragmentAccountActivity.initialAccountId = accountCurrent.id

    this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.accountCellTitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.displayName, view.text)
      }
    Espresso.onView(ViewMatchers.withId(R.id.accountCellSubtitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.subtitle, view.text)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authBasicPasswordField))
      .check { view, _ ->
        view as EditText
        Assert.assertEquals("", view.text.toString())
        Assert.assertTrue(ViewMatchers.isDisplayed().matches(view))
      }
    Espresso.onView(ViewMatchers.withId(R.id.authBasicUserNameField))
      .check { view, _ ->
        view as EditText
        Assert.assertEquals("", view.text.toString())
        Assert.assertTrue(ViewMatchers.isDisplayed().matches(view))
      }
    Espresso.onView(ViewMatchers.withId(R.id.settingsEULACheckbox))
      .check { view, _ ->
        view as CheckBox
        Assert.assertEquals(false, view.isChecked)
        Assert.assertEquals(View.INVISIBLE, view.visibility)
      }
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(false, view.isEnabled)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authBasic))
      .check { view, _ ->
        Assert.assertEquals(View.VISIBLE, view.visibility)
      }
    Espresso.onView(ViewMatchers.withId(R.id.authCOPPA))
      .check { view, _ ->
        Assert.assertEquals(View.INVISIBLE, view.visibility)
      }

    /*
     * Typing in the user name doesn't unlock the login button.
     */

    Espresso.onView(ViewMatchers.withId(R.id.authBasicUserNameField))
      .perform(ViewActions.typeText("user"))
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(false, view.isEnabled)
      }

    /*
     * Typing in the password unlocks the login button.
     */

    Espresso.onView(ViewMatchers.withId(R.id.authBasicPasswordField))
      .perform(ViewActions.typeText("pass"))
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(true, view.isEnabled)
      }

    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .perform(ViewActions.click())

    val expectedCall =
      MockProfilesController.ProfileAccountLogin(
        accountCurrent.id,
        AccountAuthenticationCredentials.builder(
          AccountPIN.create("pass"),
          AccountBarcode.create("user")
        ).build())

    Assert.assertEquals(1, profilesController.profileAccountLogins.size)
    Assert.assertEquals(expectedCall, profilesController.profileAccountLogins[0])
  }

  /**
   * A basic-auth account that isn't logged in has the correct view configuration.
   */

  @Test(timeout = 20_000L)
  fun testBasicNotLoggedInWithEULA()
  {
    val profilesController =
      MockProfilesController
    val documents =
      Mockito.mock(DocumentStoreType::class.java)
    val eula =
      MockEULA()
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)

    Mockito.`when`(documents.eula)
      .thenReturn(Option.some(eula))

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(DocumentStoreType::class.java, documents)
    services.putService(ProfilesControllerType::class.java, profilesController)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object: UIThreadServiceType {})

    val accountCurrent = profilesController.profileAccountCurrent()
    accountCurrent.loginState as AccountLoginState.AccountNotLoggedIn
    SettingsFragmentAccountActivity.initialAccountId = accountCurrent.id

    this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.accountCellTitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.displayName, view.text)
      }
    Espresso.onView(ViewMatchers.withId(R.id.accountCellSubtitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.subtitle, view.text)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authBasicPasswordField))
      .check { view, _ ->
        view as EditText
        Assert.assertEquals("", view.text.toString())
        Assert.assertTrue(ViewMatchers.isDisplayed().matches(view))
      }
    Espresso.onView(ViewMatchers.withId(R.id.authBasicUserNameField))
      .check { view, _ ->
        view as EditText
        Assert.assertEquals("", view.text.toString())
        Assert.assertTrue(ViewMatchers.isDisplayed().matches(view))
      }
    Espresso.onView(ViewMatchers.withId(R.id.settingsEULACheckbox))
      .check { view, _ ->
        view as CheckBox
        Assert.assertEquals(false, view.isChecked)
      }
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(false, view.isEnabled)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authBasic))
      .check { view, _ ->
        Assert.assertEquals(View.VISIBLE, view.visibility)
      }
    Espresso.onView(ViewMatchers.withId(R.id.authCOPPA))
      .check { view, _ ->
        Assert.assertEquals(View.INVISIBLE, view.visibility)
      }

    /*
     * Typing in the user name doesn't unlock the login button.
     */

    Espresso.onView(ViewMatchers.withId(R.id.authBasicUserNameField))
      .perform(ViewActions.typeText("user"))
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(false, view.isEnabled)
      }

    /*
     * Typing in the password doesn't unlock the login button.
     */

    Espresso.onView(ViewMatchers.withId(R.id.authBasicPasswordField))
      .perform(ViewActions.typeText("pass"))
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(false, view.isEnabled)
      }

    /*
     * Agreeing to the EULA unlocks the login button.
     */

    Espresso.onView(ViewMatchers.withId(R.id.settingsEULACheckbox))
      .perform(ViewActions.click())
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(true, view.isEnabled)
      }

    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .perform(ViewActions.click())

    val expectedCall =
      MockProfilesController.ProfileAccountLogin(
        accountCurrent.id,
        AccountAuthenticationCredentials.builder(
          AccountPIN.create("pass"),
          AccountBarcode.create("user")
        ).build())

    Assert.assertEquals(1, profilesController.profileAccountLogins.size)
    Assert.assertEquals(expectedCall, profilesController.profileAccountLogins[0])
    Assert.assertEquals(true, eula.eulaHasAgreed())
  }

  /**
   * Logging out of a basic-auth account clears the necessary fields.
   */

  @Test(timeout = 20_000L)
  fun testBasicLogOut()
  {
    val profilesController =
      MockProfilesController
    val documents =
      Mockito.mock(DocumentStoreType::class.java)
    val eula =
      MockEULA()
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)

    Mockito.`when`(documents.eula)
      .thenReturn(Option.some(eula))

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(DocumentStoreType::class.java, documents)
    services.putService(ProfilesControllerType::class.java, profilesController)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object: UIThreadServiceType {})

    val accountCurrent = profilesController.profileAccountCurrent()
    accountCurrent.setLoginState(
      AccountLoginState.AccountLoggedIn(
        AccountAuthenticationCredentials.builder(
          AccountPIN.create("pass"),
          AccountBarcode.create("user")
        ).build()
      )
    )

    eula.eulaSetHasAgreed(true)
    accountCurrent.loginState as AccountLoginState.AccountLoggedIn
    SettingsFragmentAccountActivity.initialAccountId = accountCurrent.id

    this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.accountCellTitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.displayName, view.text)
      }
    Espresso.onView(ViewMatchers.withId(R.id.accountCellSubtitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.subtitle, view.text)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authBasicPasswordField))
      .check { view, _ ->
        view as EditText
        Assert.assertEquals("pass", view.text.toString())
        Assert.assertTrue(ViewMatchers.isDisplayed().matches(view))
        Assert.assertEquals(false, view.isEnabled)
      }
    Espresso.onView(ViewMatchers.withId(R.id.authBasicUserNameField))
      .check { view, _ ->
        view as EditText
        Assert.assertEquals("user", view.text.toString())
        Assert.assertTrue(ViewMatchers.isDisplayed().matches(view))
        Assert.assertEquals(false, view.isEnabled)
      }
    Espresso.onView(ViewMatchers.withId(R.id.settingsEULACheckbox))
      .check { view, _ ->
        view as CheckBox
        Assert.assertEquals(true, view.isChecked)
        Assert.assertEquals(false, view.isEnabled)
      }
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(true, view.isEnabled)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authBasic))
      .check { view, _ ->
        Assert.assertEquals(View.VISIBLE, view.visibility)
      }
    Espresso.onView(ViewMatchers.withId(R.id.authCOPPA))
      .check { view, _ ->
        Assert.assertEquals(View.INVISIBLE, view.visibility)
      }

    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .perform(ViewActions.click())

    Assert.assertEquals(0, profilesController.profileAccountLogins.size)
    Assert.assertEquals(1, profilesController.profileAccountLogouts.size)
    Assert.assertEquals(accountCurrent.id, profilesController.profileAccountLogouts[0])

    accountCurrent.setLoginState(AccountLoginState.AccountNotLoggedIn)
    profilesController.accountEventSource.onNext(AccountEventLoginStateChanged(
      message = "Logged out!",
      accountID = accountCurrent.id,
      state = AccountLoginState.AccountNotLoggedIn
    ))

    /*
     * If we've logged out successfully, the user and password fields should now be empty.
     * The login button should also be locked, as a result.
     */

    Espresso.onView(ViewMatchers.withId(R.id.authBasicPasswordField))
      .check { view, _ ->
        view as EditText
        Assert.assertEquals("", view.text.toString())
        Assert.assertTrue(ViewMatchers.isDisplayed().matches(view))
        Assert.assertEquals(true, view.isEnabled)
      }
    Espresso.onView(ViewMatchers.withId(R.id.authBasicUserNameField))
      .check { view, _ ->
        view as EditText
        Assert.assertEquals("", view.text.toString())
        Assert.assertTrue(ViewMatchers.isDisplayed().matches(view))
        Assert.assertEquals(true, view.isEnabled)
      }
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(false, view.isEnabled)
      }
  }

  /**
   * It's not possible to "log in" to a COPPA age-gated account.
   */

  @Test(timeout = 20_000L)
  fun testCOPPALogin()
  {
    val profilesController =
      MockProfilesController
    val documents =
      Mockito.mock(DocumentStoreType::class.java)
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(DocumentStoreType::class.java, documents)
    services.putService(ProfilesControllerType::class.java, profilesController)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object: UIThreadServiceType {})

    val accountCurrent = profilesController.profileAccountCurrent()
    accountCurrent.loginState as AccountLoginState.AccountNotLoggedIn
    accountCurrent.setAccountProvider(
      AccountProviderImmutable.copy(accountCurrent.provider)
        .copy(authentication = AccountProviderAuthenticationDescription.COPPAAgeGate(
          greaterEqual13 = URI.create("urn:over13"),
          under13 = URI.create("urn:under13")
        )))
    SettingsFragmentAccountActivity.initialAccountId = accountCurrent.id

    this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.accountCellTitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.displayName, view.text)
      }
    Espresso.onView(ViewMatchers.withId(R.id.accountCellSubtitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.subtitle, view.text)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authBasic))
      .check { view, _ ->
        Assert.assertEquals(View.INVISIBLE, view.visibility)
      }
    Espresso.onView(ViewMatchers.withId(R.id.authCOPPA))
      .check { view, _ ->
        Assert.assertEquals(View.VISIBLE, view.visibility)
      }
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(false, view.isEnabled)
      }
  }

  /**
   * Changing your age requires deleting local books ("logging out").
   */

  @Test(timeout = 20_000L)
  fun testCOPPAAgeChange()
  {
    val profilesController =
      MockProfilesController
    val documents =
      Mockito.mock(DocumentStoreType::class.java)
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(DocumentStoreType::class.java, documents)
    services.putService(ProfilesControllerType::class.java, profilesController)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object: UIThreadServiceType {})

    val accountCurrent = profilesController.profileAccountCurrent()
    accountCurrent.loginState as AccountLoginState.AccountNotLoggedIn
    accountCurrent.setAccountProvider(
      AccountProviderImmutable.copy(accountCurrent.provider)
        .copy(authentication = AccountProviderAuthenticationDescription.COPPAAgeGate(
          greaterEqual13 = URI.create("urn:over13"),
          under13 = URI.create("urn:under13")
        )))
    SettingsFragmentAccountActivity.initialAccountId = accountCurrent.id

    this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.accountCellTitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.displayName, view.text)
      }
    Espresso.onView(ViewMatchers.withId(R.id.accountCellSubtitle))
      .check { view, _ ->
        view as TextView
        Assert.assertEquals(View.VISIBLE, view.visibility)
        Assert.assertEquals(accountCurrent.provider.subtitle, view.text)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authBasic))
      .check { view, _ ->
        Assert.assertEquals(View.INVISIBLE, view.visibility)
      }
    Espresso.onView(ViewMatchers.withId(R.id.authCOPPA))
      .check { view, _ ->
        Assert.assertEquals(View.VISIBLE, view.visibility)
      }
    Espresso.onView(ViewMatchers.withId(R.id.settingsLoginButton))
      .check { view, _ ->
        view as Button
        Assert.assertEquals(false, view.isEnabled)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authCOPPASwitch))
      .check { view, _ ->
        view as Switch
        Assert.assertEquals(false, view.isChecked)
      }

    Espresso.onView(ViewMatchers.withId(R.id.authCOPPASwitch))
      .perform(ViewActions.click())

    /*
     * Check that a dialog opened, and click the confirm button.
     */

    Espresso.onView(ViewMatchers.withText(R.string.settingsCOPPADeleteBooksConfirm))
      .inRoot(RootMatchers.isDialog())
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    Espresso.onView(ViewMatchers.withText(R.string.settingsDelete))
      .inRoot(RootMatchers.isDialog())
      .perform(ViewActions.click())

    Assert.assertEquals(1, profilesController.profileAccountLogouts.size)
    Assert.assertEquals(accountCurrent.id, profilesController.profileAccountLogouts[0])
  }
}