package org.nypl.simplified.tests.android.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity
import androidx.test.rule.ActivityTestRule
import org.junit.After
import org.junit.Rule
import org.slf4j.LoggerFactory

abstract class FragmentBaseTest<T : AppCompatActivity>(activityClass: Class<T>) {

  private val log = LoggerFactory.getLogger(FragmentBaseTest::class.java)

  @JvmField
  @Rule
  var activityRule: ActivityTestRule<T> =
    ActivityTestRule(activityClass, false, false)

  private var wakeLock: PowerManager.WakeLock? = null

  open fun setupExtra() {

  }

  open fun tearDownExtra() {

  }

  fun launchActivity(): T
  {
    this.log.debug("waking up device")

    val activity =
      this.activityRule.launchActivity(null)
    val keyguard =
      activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    keyguard.newKeyguardLock(this.javaClass.simpleName).disableKeyguard()

    this.log.debug("acquiring wake lock")
    val power = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
    this.wakeLock = power.newWakeLock(
      PowerManager.FULL_WAKE_LOCK
        or PowerManager.ACQUIRE_CAUSES_WAKEUP
        or PowerManager.ON_AFTER_RELEASE,
      this.javaClass.simpleName)
    this.wakeLock!!.acquire()
    return activity
  }

  @After
  fun tearDown() {
    this.log.debug("releasing wake lock")
    this.wakeLock?.release()
    this.tearDownExtra()
  }
}