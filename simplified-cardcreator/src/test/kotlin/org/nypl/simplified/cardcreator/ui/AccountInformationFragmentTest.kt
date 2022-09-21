package org.nypl.simplified.cardcreator.ui

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.viewmodel.AccountInformationViewModel

@RunWith(AndroidJUnit4::class)
class AccountInformationFragmentTest {
  private lateinit var scenario: FragmentScenario<AccountInformationFragment>

  @MockK
  private lateinit var mockViewModel: AccountInformationViewModel
  private val testViewModelState = MutableStateFlow(AccountInformationViewModel.State())

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)

    every { mockViewModel.state } returns testViewModelState

    val testNavController = TestNavHostController(
      ApplicationProvider.getApplicationContext()
    )

    scenario = launchFragmentInContainer {
      AccountInformationFragment {
        mockk {
          every { create(AccountInformationViewModel::class.java) } returns mockViewModel
        }
      }.also { fragment ->
        fragment.viewLifecycleOwnerLiveData.observeForever {
          if (it != null) {
            testNavController.setGraph(R.navigation.nav_graph)
            testNavController.setCurrentDestination(R.id.destination_account_information)
            Navigation.setViewNavController(fragment.requireView(), testNavController)
          }
        }
      }
    }
  }

  @After
  fun tearDown() {
    scenario.close()
  }

  @Test
  fun `changing password text triggers validation`() {
    onView(withId(R.id.password_et)).perform(typeText("p"))
    verify { mockViewModel.validatePassword("p") }
  }

  @Test
  fun `changing username text triggers validation`() {
    // Espresso TypeText does not work with inputType="number" attribute, for some reason
    // See https://github.com/robolectric/robolectric/issues/4149
    onView(withId(R.id.username_et)).perform(replaceText("p"))
    verify { mockViewModel.validateUsername("p") }
  }

  @Test
  fun `next button is enabled when viewmodel emits state with validUsername and validPassword`() {
    onView(withId(R.id.next_btn)).check(matches(isNotEnabled()))

    testViewModelState.value = testViewModelState.value.copy(
      validPassword = true,
      validUsername = true
    )

    onView(withId(R.id.next_btn)).check(matches(isEnabled()))
  }

  @Test
  fun `loading view becomes visible when viewmodel emits state with pendingRequest`() {
    onView(withId(R.id.loading)).check(matches(not(isDisplayed())))
    testViewModelState.value = testViewModelState.value.copy(pendingRequest = true)
    onView(withId(R.id.loading)).check(matches(isDisplayed()))
  }
}
