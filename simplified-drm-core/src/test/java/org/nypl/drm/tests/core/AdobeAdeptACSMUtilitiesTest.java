package org.nypl.drm.tests.core;

import junit.framework.Assert;

import org.nypl.drm.core.AdobeAdeptACSMUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ACSM utility tests.
 */

public final class AdobeAdeptACSMUtilitiesTest {
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(AdobeAdeptACSMUtilitiesTest.class);
  }

  /**
   * Construct test suite.
   */

  public AdobeAdeptACSMUtilitiesTest() {

  }

  /**
   * Parsing a success ACSM works.
   *
   * @throws Exception On errors
   */

  public void testSuccessful()
    throws Exception {
    final StringBuilder b = new StringBuilder(64);
    b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    b.append("<success xmlns=\"http://ns.adobe.com/adept\"/>\n");

    Assert.assertTrue(AdobeAdeptACSMUtilities.acsmIsSuccessful(b.toString()));
  }

  /**
   * Parsing a failure ACSM works.
   *
   * @throws Exception On errors
   */

  public void testNotSuccessful()
    throws Exception {
    final StringBuilder b = new StringBuilder(64);
    b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    b.append("<failure xmlns=\"http://ns.adobe.com/adept\"/>\n");

    Assert.assertFalse(AdobeAdeptACSMUtilities.acsmIsSuccessful(b.toString()));
  }

  /**
   * Parsing a broken ACSM fails.
   *
   * @throws Exception On errors
   */

  public void testParseError()
    throws Exception {
    final StringBuilder b = new StringBuilder(64);
    b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    b.append("<broken></unusable>\n");

    AdobeAdeptACSMUtilities.acsmIsSuccessful(b.toString());
  }
}
