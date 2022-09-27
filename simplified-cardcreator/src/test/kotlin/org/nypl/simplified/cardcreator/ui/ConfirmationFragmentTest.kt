package org.nypl.simplified.cardcreator.ui

import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.EqMatcher
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.nypl.simplified.cardcreator.CardCreatorActivity
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationData
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationEvent.AddSavedCardToGallery
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationEvent.CardConfirmed
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationViewModel
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationViewModelFactory
import org.nypl.simplified.cardcreator.viewmodel.State
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ConfirmationFragmentTest {
  private lateinit var scenario: FragmentScenario<ConfirmationFragment>

  @MockK
  private lateinit var mockConfirmationViewModel: ConfirmationViewModel

  private lateinit var testViewModelState: MutableStateFlow<State>

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)

    mockkConstructor(Cache::class)
    mockkConstructor(ConfirmationViewModelFactory::class)
    every {
      anyConstructed<ConfirmationViewModelFactory>().create(ConfirmationViewModel::class.java)
    } returns mockConfirmationViewModel

    val testData = ConfirmationData(
      name = "testName",
      barcode = "testBarcode",
      password = "testPassword",
      message = "testMessage"
    )

    testViewModelState = MutableStateFlow(State(testData))
    every { mockConfirmationViewModel.state } returns testViewModelState

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

  @After
  fun tearDown() {
    /*
    * There are some issues with activityscenario state when an Activity's finish() is called
    * at certain lifecycle points so we only call close on non-finishing activities to avoid this.
    * Not closing the scenarios invoking finish does introduce potential waste, but as of right now
    * there is only this one test involving a finish() call.
    *
    * Reportedly this issue might be fixed by upgrading AndroidX Test Core to 1.5+
    *
    * Related/adjacent issues:
    * https://github.com/android/android-test/issues/676
    * https://github.com/android/android-test/issues/800
    * https://github.com/android/android-test/issues/835
    * https://github.com/android/android-test/issues/978
    */
    var finishing = false
    scenario.onFragment {
      if (it.activity?.isFinishing == true) {
        finishing = true
      }
    }
    if (!finishing) scenario.close()
  }

  @Test
  fun `onViewCreated correctly displays fragment args in card info`() {
    onView(withId(R.id.name_card)).check(matches(withText("testName")))
    onView(withId(R.id.card_barcode)).check(matches(withText("Card Number: testBarcode")))
    onView(withId(R.id.card_pin)).check(matches(withText("Password: testPassword")))
    onView(withId(R.id.header_status_desc_tv)).check(matches(withText("testMessage")))

    val formattedCurrentDay = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    onView(withId(R.id.issued)).check(matches(withText("Issued: $formattedCurrentDay")))
  }

  @Test
  fun `next button confirms card`() {
    justRun { mockConfirmationViewModel.confirmCard() }
    onView(withId(R.id.next_btn)).perform(click())
    verify { mockConfirmationViewModel.confirmCard() }
  }

  @Test
  fun `on CardConfirmed event sets CardCreated activity result`() = runBlockingTest {
    testViewModelState.update { it.copy(events = it.events + CardConfirmed) }

    scenario.getActivityResult()?.resultCode shouldBeEqualTo CardCreatorActivity.CARD_CREATED
    scenario.getActivityResult()?.resultData?.extras?.run {
      getString("barcode") shouldBe "testBarcode"
      getString("pin") shouldBe "testPassword"
    } ?: fail("Missing result data or extras")
  }

  @Test
  fun `on CardConfirmed event sets ChildCardCreated activity result if user is logged in`() {
      val loggedInIntent = Intent().apply { putExtra("isLoggedIn", true) }
      scenario.setActivityIntent(loggedInIntent)

      testViewModelState.update { it.copy(events = it.events + CardConfirmed) }

      scenario.getActivityResult()?.resultCode shouldBeEqualTo CardCreatorActivity.CHILD_CARD_CREATED
    }

  @Test
  fun `on CardConfirmed event clears cache and finishes activity`() = runBlockingTest {
    testViewModelState.update { it.copy(events = it.events + CardConfirmed) }

    scenario.onFragment {
      it.activity?.isFinishing shouldBe true

      verify {
        constructedWith<Cache>(EqMatcher(it.requireContext())).clear()
      }
    }
  }

  @Test
  fun `handling CardConfirmed event signals eventHasBeenHandled`() {
    val event = CardConfirmed
    testViewModelState.update { it.copy(events = it.events + event) }

    verify { mockConfirmationViewModel.eventHasBeenHandled(event.id) }
  }

  @Test
  fun `prev button (save Card) prepares to save card`() {
    justRun { mockConfirmationViewModel.prepareToSaveCard() }
    onView(withId(R.id.prev_btn)).perform(click())
    verify { mockConfirmationViewModel.prepareToSaveCard() }
  }

  @Ignore("Need to figure out how to test permissions checks")
  @Test
  fun `on SaveCardPermissionsCheck event prompts permissions request`() {
    TODO("Not yet implemented")
  }

  @Test
  fun `on AddSavedCardToGallery event launches intent to do so and calls eventHasBeenHandled`() = runBlockingTest {
    var expectedIntent: Intent? = null
    InstrumentationRegistry.getInstrumentation().targetContext.registerReceiver(
      object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
          expectedIntent = intent
        }
      },
      IntentFilter(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    )

    val fileUri = Uri.parse("fileUri")
    val event = AddSavedCardToGallery(fileUri)
    testViewModelState.update { it.copy(events = it.events + event) }

    expectedIntent?.let {
      it.data shouldBe fileUri
    }

    verify { mockConfirmationViewModel.eventHasBeenHandled(event.id) }
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

// Util function to invoke a block repeatedly until a condition is met or a timeout is reached.
fun pollAndDelayUntil(timeOut: Int = 5000, interval: Long = 250, condition: () -> Boolean) {
  val start = System.currentTimeMillis()
  runBlocking {
    var conditionIsMet = condition.invoke()
    while (!conditionIsMet && System.currentTimeMillis() < start + timeOut) {
      delay(interval)
      conditionIsMet = condition.invoke()
    }
    if (!conditionIsMet) {
      fail("Expected condition was never met after $timeOut ms")
    }
  }
}
