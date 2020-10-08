package org.nypl.simplified.tests.bundled_content;

import com.io7m.jnull.NullCheck;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.books.bundled.api.BundledURIs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Contract for the {@link org.nypl.simplified.books.bundled.api.BundledURIs} class.
 */

public abstract class BundledURIsContract {

  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BundledURIsContract.class));
  }

  @Rule public final ExpectedException expected = ExpectedException.none();

  /**
   * Construct a new contract.
   */

  public BundledURIsContract() {

  }

  /**
   * Test that URIs are categorized correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCategorize()
      throws Exception {

    Assert.assertFalse(
        BundledURIs.isBundledURI(URI.create("http://www.example.org")));
    Assert.assertTrue(
        BundledURIs.isBundledURI(URI.create("simplified-bundled://a/b/c")));
  }

  /**
   * Test that URIs are mapped to file URIs correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMapToFile()
      throws Exception {

    Assert.assertEquals(
        "file:///android_asset/a/b/c.png",
        BundledURIs.toAndroidAssetFileURI(URI.create("simplified-bundled://a/b/c.png")).toString());
  }

  /**
   * Test that URIs are mapped to file URIs correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMapToFileNotBundled()
      throws Exception {

    expected.expect(IllegalArgumentException.class);
    BundledURIs.toAndroidAssetFileURI(URI.create("file://a/b/c.png"));
  }
}
