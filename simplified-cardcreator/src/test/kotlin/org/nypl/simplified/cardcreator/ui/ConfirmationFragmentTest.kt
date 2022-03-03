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
import androidx.test.espresso.action.ViewActions.*
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.nypl.simplified.cardcreator.CardCreatorActivity
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationData
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationEvent
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationViewModel
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ConfirmationFragmentTest {
  private lateinit var scenario: FragmentScenario<ConfirmationFragment>

  @MockK
  private lateinit var mockConfirmationViewModel: ConfirmationViewModel

  private lateinit var testEventsFlow: MutableSharedFlow<ConfirmationEvent>

  @Before
  fun setUp() {
    MockKAnnotations.init(this)

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

    val testStateFlow = MutableStateFlow(testData)
    testEventsFlow = MutableSharedFlow()
    every { mockConfirmationViewModel.state } returns testStateFlow
    every { mockConfirmationViewModel.events } returns testEventsFlow

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
    testEventsFlow.emit(ConfirmationEvent.CardConfirmed)

    scenario.getActivityResult()?.resultCode shouldBeEqualTo CardCreatorActivity.CARD_CREATED
    scenario.getActivityResult()?.resultData?.extras?.run {
      getString("barcode") shouldBe "testBarcode"
      getString("pin") shouldBe "testPassword"
    } ?: fail("Missing result data or extras")
  }

  @Test
  fun `on CardConfirmed event sets ChildCardCreated activity result if user is logged in`() = runBlockingTest {
    val loggedInIntent = Intent().apply { putExtra("isLoggedIn", true) }
    scenario.setActivityIntent(loggedInIntent)

    testEventsFlow.emit(ConfirmationEvent.CardConfirmed)

    scenario.getActivityResult()?.resultCode shouldBeEqualTo CardCreatorActivity.CHILD_CARD_CREATED
  }

  @Test
  fun `on CardConfirmed event clears cache and finishes activity`() = runBlockingTest {
    testEventsFlow.emit(ConfirmationEvent.CardConfirmed)

    scenario.onFragment {
      it.activity?.isFinishing shouldBe true

      verify {
        constructedWith<Cache>(EqMatcher(it.requireContext())).clear()
      }
    }
  }

  @Test
  fun `prev button (save Card) prepares to save card`() {
    justRun { mockConfirmationViewModel.prepareToSaveCard() }
    onView(withId(R.id.prev_btn)).perform(click())
    verify { mockConfirmationViewModel.prepareToSaveCard() }
  }

  @Ignore
  @Test
  fun `on SaveCardPermissionsCheck event prompts permissions request`() {
    TODO("Not yet implemented")
  }

  @Test
  fun `on AddSavedCardToGallery event launches intent to do so`() = runBlockingTest {
    var expectedIntent: Intent? = null
    InstrumentationRegistry.getInstrumentation().targetContext.registerReceiver(
      object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
          expectedIntent = intent
        }
      },
      IntentFilter(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    )

    val fileUri = Uri.parse("fileUri")
    testEventsFlow.emit(ConfirmationEvent.AddSavedCardToGallery(fileUri))

    expectedIntent?.let {
      it.data shouldBe fileUri
    }
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
