package org.nypl.drm.tests.core;

import android.net.Uri;

import com.io7m.junreachable.UnreachableCodeException;

import junit.framework.Assert;

import org.nypl.drm.core.AdobeAdeptJoinAccountDispatcher;
import org.nypl.drm.core.AdobeAdeptJoinAccountDispatcherListenerType;
import org.nypl.drm.core.AdobeAdeptJoinAccountDispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for the default join account dispatcher.
 */

public final class AdobeAdeptJoinAccountDispatcherTest {
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(AdobeAdeptJoinAccountDispatcherTest.class);
  }

  /**
   * Construct the test suite.
   */

  public AdobeAdeptJoinAccountDispatcherTest() {

  }

  /**
   * URI parsing works.
   *
   * @throws Exception On errors
   */

  public void testURIParsing()
    throws Exception {
    final String uri =
      "adobe:join-form-submit/%7B%22url%22%3A%22https%3A%2F%2Fadeactivate"
        + ".adobe.com%2Fadept%2FJoinAccountsFormSubmit%22%2C%22username%22%3A"
        + "%22LABS00000013%22%2C%22password%22%3A%226534%22%2C%22sessionId%22"
        + "%3A%227310fdd40cdf3741bff0355a7aaa7c11d29cd176%22%2C%22currentNonce"
        + "%22%3A%22b91d53984262bf0234b45a3b7b2addee5e7c6bf1%22%2C%22locale%22"
        + "%3A%22en%22%7D";

    final ExecutorService exec = Executors.newFixedThreadPool(1);
    final AdobeAdeptJoinAccountDispatcherType dispatcher =
      AdobeAdeptJoinAccountDispatcher.newDispatcher(exec);

    final AtomicReference<String> query = new AtomicReference<String>();
    final CountDownLatch latch = new CountDownLatch(1);

    dispatcher.onFormSubmit(
      uri, new AdobeAdeptJoinAccountDispatcherListenerType() {
        @Override
        public void onJoinAccountsException(final Throwable e) {
          AdobeAdeptJoinAccountDispatcherTest.LOG.debug(
            "exception raised: ",
            e);
          latch.countDown();
          throw new UnreachableCodeException();
        }

        @Override
        public boolean onPreparedQuery(final Uri.Builder builder) {
          final Uri u = builder.build();
          AdobeAdeptJoinAccountDispatcherTest.LOG.debug("uri: {}", u);

          query.set(u.getEncodedQuery());
          latch.countDown();
          return false;
        }

        @Override
        public void onReceivedHTMLPage(final String text) {
          latch.countDown();
          throw new UnreachableCodeException();
        }

        @Override
        public void onReceivedACSM(final String text) {
          latch.countDown();
          throw new UnreachableCodeException();
        }
      });

    latch.await();

    Assert.assertEquals(
      "username=LABS00000013&password=6534&sessionId"
        + "=7310fdd40cdf3741bff0355a7aaa7c11d29cd176&currentNonce"
        + "=b91d53984262bf0234b45a3b7b2addee5e7c6bf1&locale=en&responseType=acsm",
      query.get());
  }
}
