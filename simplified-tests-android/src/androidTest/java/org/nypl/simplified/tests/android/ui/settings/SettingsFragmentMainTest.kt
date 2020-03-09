package org.nypl.simplified.tests.android.ui.settings

import androidx.preference.Preference
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.io7m.jfunctional.Option
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.internal.verification.Times
import org.nypl.simplified.documents.eula.EULAType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.documents.synced.SyncedDocumentType
import org.nypl.simplified.tests.android.NavigationHostActivity
import org.nypl.simplified.tests.android.R
import org.nypl.simplified.tests.android.ui.FragmentBaseTest
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType

@RunWith(AndroidJUnit4::class)
class SettingsFragmentMainTest :
  FragmentBaseTest<SettingsFragmentMainActivity>(SettingsFragmentMainActivity::class.java) {

  /**
   * If no documents are present, then all of the information links are disabled.
   */

  @Test
  fun testDocumentLinksNotPresent() {
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)
    val documents =
      Mockito.mock(DocumentStoreType::class.java)

    Mockito.`when`(documents.about)
      .thenReturn(Option.none())
    Mockito.`when`(documents.acknowledgements)
      .thenReturn(Option.none())
    Mockito.`when`(documents.eula)
      .thenReturn(Option.none())
    Mockito.`when`(documents.licenses)
      .thenReturn(Option.none())
    Mockito.`when`(documents.privacyPolicy)
      .thenReturn(Option.none())

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(DocumentStoreType::class.java, documents)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object : UIThreadServiceType {})

    val activity = this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.navigationHostFragmentHolder))
      .check(matches(ViewMatchers.isDisplayed()))

    val fragment = activity.currentFragment

    for (name in listOf(
      "settingsAcknowledgements",
      "settingsAbout",
      "settingsEULA",
      "settingsFaq",
      "settingsLicense"
    )) {
      val pref = fragment.findPreference<Preference>(name)!!
      Assert.assertEquals(false, pref.isEnabled)
    }
  }

  /**
   * If no documents are present, then all of the information links are disabled.
   */

  @Test
  fun testDocumentLinksPresent() {
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)
    val document =
      Mockito.mock(SyncedDocumentType::class.java)
    val eula =
      Mockito.mock(EULAType::class.java)
    val documents =
      Mockito.mock(DocumentStoreType::class.java)

    Mockito.`when`(documents.about)
      .thenReturn(Option.some(document))
    Mockito.`when`(documents.acknowledgements)
      .thenReturn(Option.some(document))
    Mockito.`when`(documents.eula)
      .thenReturn(Option.some(eula))
    Mockito.`when`(documents.licenses)
      .thenReturn(Option.some(document))
    Mockito.`when`(documents.privacyPolicy)
      .thenReturn(Option.some(document))

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(DocumentStoreType::class.java, documents)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object : UIThreadServiceType {})

    val activity = this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.navigationHostFragmentHolder))
      .check(matches(ViewMatchers.isDisplayed()))

    val fragment = activity.currentFragment

    for (name in listOf(
      "settingsAcknowledgements",
      "settingsAbout",
      "settingsEULA",
      "settingsLicense"
    )) {
      val pref = fragment.findPreference<Preference>(name)!!
      Assert.assertEquals(true, pref.isEnabled)
    }
  }

  /**
   * Clicking the accounts link takes you to the accounts screen.
   */

  @Test
  fun testAccounts() {
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)
    val documents =
      Mockito.mock(DocumentStoreType::class.java)

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(DocumentStoreType::class.java, documents)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object : UIThreadServiceType {})

    this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.navigationHostFragmentHolder))
      .check(matches(ViewMatchers.isDisplayed()))

    Espresso.onView(ViewMatchers.withText(R.string.settingsAccounts))
      .perform(ViewActions.click())

    Mockito.verify(navigation, Times(1))
      .openSettingsAccounts()
  }

  /**
   * Clicking the version link takes you to the version screen.
   */

  @Test
  fun testVersion() {
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)
    val documents =
      Mockito.mock(DocumentStoreType::class.java)

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(DocumentStoreType::class.java, documents)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object : UIThreadServiceType {})

    this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.navigationHostFragmentHolder))
      .check(matches(ViewMatchers.isDisplayed()))

    Espresso.onView(ViewMatchers.withText(R.string.settingsVersion))
      .perform(ViewActions.click())

    Mockito.verify(navigation, Times(1))
      .openSettingsVersion()
  }
}
