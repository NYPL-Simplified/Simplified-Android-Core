package org.nypl.simplified.ui.accounts

import android.app.Instrumentation
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.cardcreator.CardCreatorActivity
import org.nypl.simplified.cardcreator.CardCreatorContract
import org.nypl.simplified.cardcreator.CardCreatorServiceType
import org.nypl.simplified.listeners.api.FragmentListenerFinder
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkSyncEnableStatus
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType
import java.net.URI

@RunWith(AndroidJUnit4::class)
class AccountDetailFragmentTest {

  private lateinit var scenario: FragmentScenario<AccountDetailFragment>

  @MockK
  private lateinit var mockAccountDetailViewModel: AccountDetailViewModel

  @MockK
  private lateinit var mockAccountDetailEventListener: FragmentListenerType<AccountDetailEvent>

  private var testAccountLiveData = MutableLiveData<AccountType>()
  private var testAccountSyncingLiveData = MutableLiveData<ReaderBookmarkSyncEnableStatus>()
  private lateinit var testProviderBasicAuthDescription: AccountProviderAuthenticationDescription.Basic

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    Intents.init()

    val testCardCreatorContract = CardCreatorContract(
      "testUsername",
      "testPassword",
      "testClientId",
      "testClientSecret"
    )

    mockkObject(Services)
    every {
      Services.serviceDirectory()
        .optionalService(CardCreatorServiceType::class.java)!!
        .getCardCreatorContract()
    } returns testCardCreatorContract

    every {
      Services.serviceDirectory().requireService(ImageLoaderType::class.java)
    } returns mockk(relaxed = true)

    // Mock image loading - skip for now
    // We need to use static mock here because of @JvmStatic annotation
    // mockkObject(ImageAccountIcons)
    mockkStatic(ImageAccountIcons::class)
    every { ImageAccountIcons.loadAccountLogoIntoView(any(), any(), any(), any()) } just runs

    mockkObject(FragmentListenerFinder)
    every {
      FragmentListenerFinder.findListener(any(), AccountDetailEvent::class.java)
    } returns mockAccountDetailEventListener

    // Mock ViewModel creation
    mockkConstructor(AccountDetailViewModelFactory::class)
    every {
      anyConstructed<AccountDetailViewModelFactory>().create(AccountDetailViewModel::class.java)
    } returns mockAccountDetailViewModel

    // Emit value to trigger reconfigureAccountUI() - but we don't use the value so a mock suffices
    every { mockAccountDetailViewModel.accountLive } returns testAccountLiveData
    every { mockAccountDetailViewModel.accountSyncingSwitchStatus } returns testAccountSyncingLiveData
    testAccountLiveData.value = mockk()

    // covers authenticationAlternativesMake() (onViewCreated)
    every { mockAccountDetailViewModel.account.provider.authenticationAlternatives } returns emptyList()
    // covers ImageAccountIcons.loadAccountLogoIntoView(...)
    every { mockAccountDetailViewModel.account.provider.toDescription() } returns mockk()
    // covers configureToolbar (onStart)
    every { mockAccountDetailViewModel.account.provider.displayName } returns "displayName"
    // covers authenticationViews.setCOPPAState(...)
    every { mockAccountDetailViewModel.isOver13 } returns true
    // covers configureReportIssue() (onStart)
    every { mockAccountDetailViewModel.account.provider.supportEmail } returns "supportEmail"

    val uriWithNYPLScheme = URI.create("nypl.card-creator://some-address")
    every { mockAccountDetailViewModel.account.provider.cardCreatorURI } returns uriWithNYPLScheme

    // covers initial reconfigureAccountUI state
    every { mockAccountDetailViewModel.account.loginState } returns
      AccountLoginState.AccountLoggedIn(
        mockk<AccountAuthenticationCredentials.Basic> {
          every { userName.value } returns "userName"
          every { password.value } returns "password"
        }
      )

    testProviderBasicAuthDescription = AccountProviderAuthenticationDescription.Basic(
      "description",
      "BARCODEFORMAT",
      keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
      passwordMaximumLength = 10,
      passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
      emptyMap(),
      URI.create("logoURI")
    )
    every { mockAccountDetailViewModel.account.provider.authentication } returns testProviderBasicAuthDescription
    every { mockAccountDetailViewModel.account.provider.subtitle } returns "subtitle"
    every { mockAccountDetailViewModel.account.preferences.catalogURIOverride } returns null

    val accountDetailArguments = AccountFragmentParameters(
      accountId = AccountID.generate(),
      closeOnLoginSuccess = false,
      showPleaseLogInTitle = false
    )
    scenario = launchFragmentInContainer(
      fragmentArgs = bundleOf(AccountDetailFragment.PARAMETERS_ID to accountDetailArguments),
      themeResId = R.style.SimplifiedTheme_ActionBar
    )
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  @Test
  fun `sign up button opens card creator activity when account provider cardCreatorURI is NYPL`() {
    onView(withId(R.id.accountCardCreatorSignUp)).check(matches(isEnabled()))
    onView(withId(R.id.accountCardCreatorSignUp)).perform(click())

    // We're kind of testing CardCreatorContract implicitly here as well...probably okay?
    // allOf(...) failure doesn't provide great feedback as to which condition was missed...
    intended(
      allOf(
        hasComponent(CardCreatorActivity::class.java.name),
        hasExtra("isLoggedIn", true),
        hasExtra("userIdentifier", "userName")
      )
    )
  }

  @Test
  fun `sign up button opens web card creator when account provider cardCreatorURI is not NYPL`() {
    every { mockAccountDetailViewModel.account.provider.cardCreatorURI } returns
      URI.create("non-nypl-cardCreatorUri")

    onView(withId(R.id.accountCardCreatorSignUp)).check(matches(isEnabled()))
    onView(withId(R.id.accountCardCreatorSignUp)).perform(click())

    intended(
      allOf(
        hasAction(Intent.ACTION_VIEW),
        hasData("non-nypl-cardCreatorUri")
      )
    )
  }

  @Test
  fun `on card creator result tries to do basicAuth login when card is newly created`() {
    val requestSlot = slot<ProfileAccountLoginRequest.Basic>()
    justRun { mockAccountDetailViewModel.tryLogin(capture(requestSlot)) }

    val testAccountID = AccountID.generate()
    every { mockAccountDetailViewModel.account.id } returns testAccountID

    val resultData = Intent().apply {
      putExtra("barcode", "testBarcode")
      putExtra("pin", "testPin")
    }
    val stubbedResult = Instrumentation.ActivityResult(CardCreatorActivity.CARD_CREATED, resultData)

    intending(hasComponent(CardCreatorActivity::class.java.name))
      .respondWith(stubbedResult)

    onView(withId(R.id.accountCardCreatorSignUp)).perform(click())

    requestSlot.isCaptured shouldBe true
    requestSlot.captured.apply {
      accountId shouldBe testAccountID
      description shouldBe testProviderBasicAuthDescription
      username.value shouldBeEqualTo "testBarcode"
      password.value shouldBeEqualTo "testPin"
    }
  }

  @Test
  fun `on card creator result does not attempt sign in when child card is created`() {
    val emptyIntent = Intent()
    val stubbedResult =
      Instrumentation.ActivityResult(CardCreatorActivity.CHILD_CARD_CREATED, emptyIntent)

    intending(hasComponent(CardCreatorActivity::class.java.name))
      .respondWith(stubbedResult)

    onView(withId(R.id.accountCardCreatorSignUp)).perform(click())

    verify(exactly = 0) { mockAccountDetailViewModel.tryLogin(any()) }
  }

  @Test
  fun `on card creator result does not attempt sign in when activity finishes with error`() {
    val emptyIntent = Intent()
    val stubbedResult =
      Instrumentation.ActivityResult(CardCreatorActivity.CARD_CREATION_ERROR, emptyIntent)

    intending(hasComponent(CardCreatorActivity::class.java.name))
      .respondWith(stubbedResult)

    onView(withId(R.id.accountCardCreatorSignUp)).perform(click())

    verify(exactly = 0) { mockAccountDetailViewModel.tryLogin(any()) }
  }

  @Test
  fun `on card creator result does not attempt sign in when activity is cancelled`() {
    val emptyIntent = Intent()
    val stubbedResult =
      Instrumentation.ActivityResult(CardCreatorActivity.CARD_CREATION_CANCELED, emptyIntent)

    intending(hasComponent(CardCreatorActivity::class.java.name))
      .respondWith(stubbedResult)

    onView(withId(R.id.accountCardCreatorSignUp)).perform(click())

    verify(exactly = 0) { mockAccountDetailViewModel.tryLogin(any()) }
  }
}
