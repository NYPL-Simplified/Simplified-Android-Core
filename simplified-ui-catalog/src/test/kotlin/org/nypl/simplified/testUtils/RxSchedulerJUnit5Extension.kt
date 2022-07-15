package org.nypl.simplified.testUtils

import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.Callable

// Taken from https://gist.github.com/adityaladwa/54f8240ca7cadfbfe60c6e6810f2eb69
/**
 * NOTE: You MUST use this rule in every test class that targets app code that uses RxJava.
 * Even when that code doesn't use any scheduler. The RxJava {@link Schedulers} class is setup
 * once and caches the schedulers in memory. So if one of the test classes doesn't use this rule
 * by the time this rule runs it may be too late to override the schedulers. This is really not
 * ideal but currently there isn't a better approach.
 * More info here: https://github.com/ReactiveX/RxJava/issues/2297
 * <p>
 * This rule registers SchedulerHooks for RxJava and RxAndroid to ensure that subscriptions
 * always subscribeOn and observeOn Schedulers.immediate().
 * Warning, this rule will reset RxAndroidPlugins and RxJavaPlugins before and after each test so
 * if the application code uses RxJava plugins this may affect the behaviour of the testing method.
 */

class RxSchedulerJUnit5Extension : AfterEachCallback, BeforeEachCallback {

  private val mSchedulerInstance = Schedulers.trampoline()
  private val schedulerFunc: (Scheduler) -> Scheduler = { mSchedulerInstance }
  private val schedulerLazyFunc: (Callable<Scheduler>) -> Scheduler = { mSchedulerInstance }

  @Throws(Exception::class)
  override fun beforeEach(context: ExtensionContext?) {
    RxAndroidPlugins.reset()
    RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerLazyFunc)

    RxJavaPlugins.reset()
    RxJavaPlugins.setIoSchedulerHandler(schedulerFunc)
    RxJavaPlugins.setNewThreadSchedulerHandler(schedulerFunc)
    RxJavaPlugins.setComputationSchedulerHandler(schedulerFunc)
  }

  @Throws(Exception::class)
  override fun afterEach(context: ExtensionContext?) {
    RxAndroidPlugins.reset()
    RxJavaPlugins.reset()
  }
}
