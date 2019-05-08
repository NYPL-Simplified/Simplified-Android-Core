package org.nypl.simplified.tests.analytics.lfa

import android.test.mock.MockContext
import com.io7m.jfunctional.Option
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.analytics.api.AnalyticsConfiguration
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.lfa.LFAAnalyticsConfiguration
import org.nypl.simplified.analytics.lfa.LFAAnalyticsSystem
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.tests.http.MockingHTTP
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class LFAAnalyticsContract {

  private lateinit var executor: ExecutorService

  private val LOG =
    LoggerFactory.getLogger(LFAAnalyticsContract::class.java)

  @JvmField
  @Rule
  var expected = ExpectedException.none()

  @Before
  fun setup() {
    this.executor = Executors.newSingleThreadExecutor()
  }

  @After
  fun tearDown() {
    this.executor.shutdown()
  }

  /**
   * If the server fails to accept analytics data, the data stays in the output until it is
   * accepted.
   */

  @Test
  fun testSimpleRolloverFailure() {
    val context = object : MockContext() {

    }

    val file = File.createTempFile("lfa-analytics-", "dir")
    file.delete()
    file.mkdirs()

    val targetURI =
      URI.create("http://www.example.com/analytics")

    val http = MockingHTTP()
    val error =
      HTTPResultError<InputStream>(
        400,
        "OUCH!",
        0L,
        mutableMapOf(),
        0L,
        ByteArrayInputStream(ByteArray(0)),
        Option.none())

    for (i in 1..5) {
      http.addResponse(targetURI, error)
    }

    val lfaConfiguration =
      LFAAnalyticsConfiguration(
        targetURI = targetURI,
        token = "abcd",
        deviceID = "eaf06952-141c-4f16-8516-1a2c01503e87",
        logFileSizeLimit = 100)

    val config =
      AnalyticsConfiguration(context, http)

    val system =
      LFAAnalyticsSystem(
        baseConfiguration = config,
        lfaConfiguration = lfaConfiguration,
        baseDirectory = file,
        executor = this.executor)

    for (i in 1..10) {
      system.onAnalyticsEvent(
        AnalyticsEvent.ApplicationOpened(
          packageName = "com.example",
          packageVersion = "1.0.0",
          packageVersionCode = i))
    }

    Thread.sleep(1000)
    this.executor.submit(Callable { }).get()

    Assert.assertTrue(File(file, "outbox").list().size == 2)
  }


  /**
   * If the server accepts analytics data, the data is removed from the outbox.
   */

  @Test
  fun testSimpleRollover() {
    val context = object : MockContext() {

    }

    val file = File.createTempFile("lfa-analytics-", "dir")
    file.delete()
    file.mkdirs()

    val http = MockingHTTP()
    val analyticsURI =
      URI.create("http://www.example.com/analytics")

    http.addResponse(
      analyticsURI,
      HTTPResultOK<InputStream>(
        "OK",
        200,
        ByteArrayInputStream(ByteArray(0)),
        0L,
        mutableMapOf(),
        0L))

    val lfaConfiguration =
      LFAAnalyticsConfiguration(
        targetURI = analyticsURI,
        token = "abcd",
        deviceID = "eaf06952-141c-4f16-8516-1a2c01503e87",
        logFileSizeLimit = 100)

    val config =
      AnalyticsConfiguration(context, http)

    val system =
      LFAAnalyticsSystem(
        baseConfiguration = config,
        lfaConfiguration = lfaConfiguration,
        baseDirectory = file,
        executor = this.executor)

    Assert.assertTrue(!http.responsesNow().isEmpty())

    for (i in 1..5) {
      system.onAnalyticsEvent(
        AnalyticsEvent.ApplicationOpened(
          packageName = "com.example",
          packageVersion = "1.0.0",
          packageVersionCode = i))
    }

    Thread.sleep(2000)
    this.executor.submit(Callable { }).get()

    Assert.assertTrue(File(file, "outbox").list().size == 0)
    Assert.assertTrue(http.responsesNow()[analyticsURI]!!.isEmpty())
  }
}

