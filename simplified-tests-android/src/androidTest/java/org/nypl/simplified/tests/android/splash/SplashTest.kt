package org.nypl.simplified.tests.android.splash

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.MediumTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.v7.app.AppCompatActivity
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.eula.EULAType
import org.nypl.simplified.books.profiles.ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.splash.R
import org.nypl.simplified.splash.SplashEULAFragment
import org.nypl.simplified.splash.SplashEvent
import org.nypl.simplified.splash.SplashEvent.SplashEULAEvent.SplashEULADisagreed
import org.nypl.simplified.splash.SplashImageFragment
import org.nypl.simplified.splash.SplashListenerType
import org.nypl.simplified.splash.SplashMainFragment
import org.nypl.simplified.splash.SplashParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@MediumTest
class SplashTest {

  companion object {
    lateinit var testObservable: ObservableType<SplashEvent>
    var testEula: MockEULA? = null
    var testProfilesController: ProfilesControllerType? = null
    var testOpenedProfiles = false
    var testOpenedCatalog: AccountType? = null
    var testFinishLatch: CountDownLatch = CountDownLatch(1)
    var testEvent : SplashEvent? = null
  }

  private var instrumentationContext: Context? = null
  private val log = LoggerFactory.getLogger(SplashTest::class.java)

  @JvmField
  @Rule
  var activityRule: ActivityTestRule<MockSplashActivity> =
    ActivityTestRule<MockSplashActivity>(MockSplashActivity::class.java, false, false)

  private lateinit var wakeLock: PowerManager.WakeLock

  /**
   * Acquire a wake lock so that the device doesn't go to sleep during testing.
   */

  @Before
  fun setup() {
    this.instrumentationContext = InstrumentationRegistry.getContext()!!
    val context = this.instrumentationContext!!

    this.log.debug("setup: waking up device")

    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    keyguard.newKeyguardLock(this.javaClass.simpleName).disableKeyguard()

    this.log.debug("setup: acquiring wake lock")
    val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    this.wakeLock = power.newWakeLock(
      PowerManager.FULL_WAKE_LOCK
        or PowerManager.ACQUIRE_CAUSES_WAKEUP
        or PowerManager.ON_AFTER_RELEASE,
      this.javaClass.simpleName)
    this.wakeLock.acquire()

    this.log.debug("setup: finished")
  }

  @After
  fun tearDown() {
    this.log.debug("releasing wake lock")
    this.wakeLock.release()
  }

  /**
   * If anonymous profiles are enabled, and the user hasn't agreed to a supplied EULA: The
   * user is prompted to agree to a EULA and is redirected to the catalog afterwards.
   */

  @Test(timeout = 15_000L)
  fun onAnonymousEULANotAgreed() {
    val account =
      Mockito.mock(AccountType::class.java)
    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(account.id())
      .thenReturn(AccountID.create(23))
    Mockito.`when`(profiles.profileAnonymousEnabled())
      .thenReturn(ANONYMOUS_PROFILE_ENABLED)
    Mockito.`when`(profiles.profileAccountCurrent())
      .thenReturn(account)

    val eula = MockEULA()
    eula.url = URL("http://www.example.com")
    eula.agreed = false

    testFinishLatch = CountDownLatch(1)
    testObservable = Observable.create<SplashEvent>()
    testProfilesController = profiles
    testEula = eula

    this.activityRule.launchActivity(null)
    TimeUnit.SECONDS.sleep(3L)

    Espresso.onView(ViewMatchers.withId(R.id.eula_agree))
      .perform(ViewActions.click())

    testFinishLatch.await()

    this.log.debug("finished waiting for UI")
    Assert.assertEquals(account.id(), testOpenedCatalog?.id())
    Assert.assertTrue("EULA agreed", eula.agreed)
  }

  /**
   * If anonymous profiles are enabled, and the user has already agreed to a supplied EULA: The
   * user is edirected to the catalog.
   */

  @Test(timeout = 15_000L)
  fun onAnonymousEULAAgreed() {
    val account =
      Mockito.mock(AccountType::class.java)
    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(account.id())
      .thenReturn(AccountID.create(23))
    Mockito.`when`(profiles.profileAnonymousEnabled())
      .thenReturn(ANONYMOUS_PROFILE_ENABLED)
    Mockito.`when`(profiles.profileAccountCurrent())
      .thenReturn(account)

    val eula = MockEULA()
    eula.url = URL("http://www.example.com")
    eula.agreed = true

    testFinishLatch = CountDownLatch(1)
    testObservable = Observable.create<SplashEvent>()
    testProfilesController = profiles
    testEula = eula

    this.activityRule.launchActivity(null)
    TimeUnit.SECONDS.sleep(3L)

    testFinishLatch.await()

    this.log.debug("finished waiting for UI")
    Assert.assertEquals(account.id(), testOpenedCatalog?.id())
    Assert.assertTrue("EULA agreed", eula.agreed)
  }

  /**
   * If anonymous profiles are enabled, and there isn't a supplied EULA: The
   * user is redirected to the catalog.
   */

  @Test(timeout = 15_000L)
  fun onAnonymousEULANotAvailable() {
    val account =
      Mockito.mock(AccountType::class.java)
    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(account.id())
      .thenReturn(AccountID.create(23))
    Mockito.`when`(profiles.profileAnonymousEnabled())
      .thenReturn(ANONYMOUS_PROFILE_ENABLED)
    Mockito.`when`(profiles.profileAccountCurrent())
      .thenReturn(account)

    testFinishLatch = CountDownLatch(1)
    testObservable = Observable.create<SplashEvent>()
    testProfilesController = profiles
    testEula = null

    this.activityRule.launchActivity(null)
    TimeUnit.SECONDS.sleep(3L)

    testFinishLatch.await()

    this.log.debug("finished waiting for UI")
    Assert.assertEquals(account.id(), testOpenedCatalog?.id())
  }

  /**
   * If anonymous profiles are enabled, and the user refuses to agree to the EULA... The lights
   * go out.
   */

  @Test(timeout = 15_000L)
  fun onAnonymousEULANotAgreedRefused() {
    val account =
      Mockito.mock(AccountType::class.java)
    val profiles =
      Mockito.mock(ProfilesControllerType::class.java)

    Mockito.`when`(account.id())
      .thenReturn(AccountID.create(23))
    Mockito.`when`(profiles.profileAnonymousEnabled())
      .thenReturn(ANONYMOUS_PROFILE_ENABLED)
    Mockito.`when`(profiles.profileAccountCurrent())
      .thenReturn(account)

    val eula = MockEULA()
    eula.url = URL("http://www.example.com")
    eula.agreed = false

    testFinishLatch = CountDownLatch(1)
    testObservable = Observable.create<SplashEvent>()
    testObservable.subscribe { event ->
      when (event) {
        is SplashEvent.SplashImageEvent.SplashImageTimedOut -> Unit
        is SplashEvent.SplashEULAEvent.SplashEULAAgreed -> Unit
        is SplashEvent.SplashEULAEvent.SplashEULADisagreed -> {
          testFinishLatch.countDown()
          testEvent = event
        }
      }
    }
    testProfilesController = profiles
    testEula = eula

    this.activityRule.launchActivity(null)
    TimeUnit.SECONDS.sleep(3L)

    Espresso.onView(ViewMatchers.withId(R.id.eula_disagree))
      .perform(ViewActions.click())

    testFinishLatch.await()

    this.log.debug("finished waiting for UI")
    Assert.assertThat(testEvent, IsInstanceOf(SplashEULADisagreed::class.java))
    Assert.assertFalse("EULA disagreed", eula.agreed)
  }

  class MockSplashActivity : AppCompatActivity(), SplashListenerType {

    override fun onSplashOpenProfileAnonymous() {

    }

    override val profileController: ProfilesControllerType
      get() = testProfilesController!!

    override fun onSplashOpenProfileSelector() {
      testOpenedProfiles = true
      testFinishLatch.countDown()
    }

    override fun onSplashOpenCatalog(account: AccountType) {
      testOpenedCatalog = account
      testFinishLatch.countDown()
    }

    override fun onSplashEULAIsProvided(): Boolean {
      return testEula != null
    }

    override val splashEvents: ObservableType<SplashEvent>
      get() = testObservable

    override fun onSplashImageCreateFragment() {
      this.log.debug("onSplashImageCreateFragment")

      this.splashImageFragment = SplashImageFragment.newInstance(this.parameters)

      this.supportFragmentManager.beginTransaction()
        .replace(
          org.nypl.simplified.tests.android.R.id.splash_fragment_holder,
          this.splashImageFragment,
          "SPLASH_IMAGE")
        .commit()
    }

    override fun onSplashEULACreateFragment() {
      this.log.debug("onSplashEULACreateFragment")

      this.splashEULAFragment =
        SplashEULAFragment.newInstance(this.parameters)

      this.supportFragmentManager.beginTransaction()
        .replace(
          org.nypl.simplified.tests.android.R.id.splash_fragment_holder,
          this.splashEULAFragment,
          "SPLASH_EULA")
        .commit()
    }

    override fun onSplashEULARequested(): EULAType {
      this.log.debug("onSplashEULARequested")
      return testEula!!
    }

    override val backgroundExecutor: ListeningScheduledExecutorService =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor())

    private val log: Logger = LoggerFactory.getLogger(MockSplashActivity::class.java)

    private lateinit var splashImageFragment: SplashImageFragment
    private lateinit var splashEULAFragment: SplashEULAFragment
    private lateinit var parameters: SplashParameters
    private lateinit var splashMainFragment: SplashMainFragment

    override fun onCreate(state: Bundle?) {
      this.log.debug("onCreate")
      super.onCreate(null)

      this.setTheme(org.nypl.simplified.tests.android.R.style.Theme_AppCompat_Light)
      this.setContentView(org.nypl.simplified.tests.android.R.layout.splash_base)

      this.parameters =
        SplashParameters(
          textColor = Color.RED,
          background = Color.WHITE,
          splashImageResource = org.nypl.simplified.tests.android.R.drawable.empty,
          splashImageSeconds = 2L)

      this.splashMainFragment =
        SplashMainFragment.newInstance(this.parameters)

      this.supportFragmentManager.beginTransaction()
        .add(this.splashMainFragment, "SPLASH_MAIN")
        .commit()
    }

    override fun onDestroy() {
      this.log.debug("onDestroy")
      super.onDestroy()
      this.backgroundExecutor.shutdown()
    }
  }
}
