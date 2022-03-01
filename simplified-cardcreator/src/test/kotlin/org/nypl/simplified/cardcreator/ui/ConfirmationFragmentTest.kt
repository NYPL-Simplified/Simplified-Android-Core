package org.nypl.simplified.cardcreator.ui

import android.app.Instrumentation
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.EqMatcher
import io.mockk.mockkConstructor
import io.mockk.verify
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.nypl.simplified.cardcreator.CardCreatorActivity
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.utils.Cache

@RunWith(AndroidJUnit4::class)
class ConfirmationFragmentTest {
  private lateinit var scenario: FragmentScenario<ConfirmationFragment>

  @Before
  fun setUp() {
    mockkConstructor(Cache::class)

    scenario = launchFragmentInContainer(
      fragmentArgs = bundleOf(
        "username" to "testUser",
        "barcode" to "testBarcode",
        "password" to "testPassword",
        "message" to "testMessage",
        "name" to "testName"
      ),
    )
  }

  @Test
  fun `onViewCreated correctly displays fragment args in card info`() {
    onView(withId(R.id.name_card)).check(matches(withText("testName")))
    onView(withId(R.id.card_barcode)).check(matches(withText("Card Number: testBarcode")))
    onView(withId(R.id.card_pin)).check(matches(withText("Password: testPassword")))
    onView(withId(R.id.header_status_desc_tv)).check(matches(withText("testMessage")))

    // set up time stub
    // onView(withId(R.id.header_status_desc_tv)).check(matches(withText("testTime")))
  }

  @Test
  fun `next button sets CardCreated activity result`() {
    onView(withId(R.id.next_btn)).perform(ViewActions.click())

    scenario.getActivityResult()?.resultCode shouldBe CardCreatorActivity.CARD_CREATED

    scenario.getActivityResult()?.resultData?.extras?.run {
      getString("barcode") shouldBe "testBarcode"
      getString("pin") shouldBe "testPassword"

      getString("username") shouldBe "testUser"
      getString("message") shouldBe "testMessage"
    } ?: fail("Missing result data or extras")
  }

  @Test
  fun `next button sets ChildCardCreated activity result when user is already logged in`() {
    val loggedInIntent = Intent().apply { putExtra("isLoggedIn", true) }
    scenario.setActivityIntent(loggedInIntent)

    onView(withId(R.id.next_btn)).perform(ViewActions.click())

    scenario.getActivityResult()?.resultCode shouldBe CardCreatorActivity.CHILD_CARD_CREATED
  }

  @Test
  fun `next button clears cache and finishes activity`() {
    onView(withId(R.id.next_btn)).perform(ViewActions.click())

    scenario.onFragment {
      it.activity?.isFinishing shouldBe true

      verify {
        constructedWith<Cache>(EqMatcher(it.requireContext())).clear()
      }
    }
  }

  @Ignore
  @Test
  fun `prev button (save Card) requests external storage permissions`() {
    TODO("Not yet implemented")
  }

  @Ignore
  @Test
  fun `prev button (save Card) writes card image to device`() {
    TODO("Not yet implemented")
  }
}

// Marginally better than launching our own activityScenario? Maybe...
fun <F : Fragment> FragmentScenario<F>.getActivityResult(): Instrumentation.ActivityResult? {
  return javaClass.getDeclaredField("activityScenario").let {
    it.isAccessible = true
    return@let (it.get(this) as ActivityScenario<*>).result
  }
}

// Marginally better than launching our own activityScenario? Maybe...
fun <F : Fragment> FragmentScenario<F>.setActivityIntent(customIntent: Intent) {
  javaClass.getDeclaredField("activityScenario").let {
    it.isAccessible = true
    (it.get(this) as ActivityScenario<*>).onActivity { activity ->
      activity.intent = customIntent
    }
  }
}
