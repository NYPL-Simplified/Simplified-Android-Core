package org.nypl.simplified.tests.android.ui.settings

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.tests.MockProfilesController
import org.nypl.simplified.tests.android.NavigationHostActivity
import org.nypl.simplified.tests.android.R
import org.nypl.simplified.tests.android.ui.FragmentBaseTest
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType

@RunWith(AndroidJUnit4::class)
class SettingsFragmentAccountsTest
  : FragmentBaseTest<SettingsFragmentAccountsActivity>(SettingsFragmentAccountsActivity::class.java) {

  /**
   * Check that the accounts view has a basic set of properties.
   */

  @Test(timeout = 20_000L)
  fun testVisualProperties()
  {
    val profilesController =
      MockProfilesController
    val navigation =
      Mockito.mock(SettingsNavigationControllerType::class.java)

    val services = NavigationHostActivity.services
    services.clear()
    services.putService(ProfilesControllerType::class.java, profilesController)
    services.putService(SettingsNavigationControllerType::class.java, navigation)
    services.putService(UIThreadServiceType::class.java, object: UIThreadServiceType {})

    val profile =
      profilesController.profileCurrent()

    this.launchActivity()

    Espresso.onView(ViewMatchers.withId(R.id.accountCurrentTitle))
      .check(matches(ViewMatchers.isDisplayed()))

    /*
     * Check that the account list has all of the accounts except the current one.
     */

    Espresso.onView(ViewMatchers.withId(R.id.accountList))
      .check { view, _ ->
        Assert.assertEquals(
          (view as RecyclerView).adapter!!.itemCount,
          profile.accounts().size - 1
        )
      }

  }
}