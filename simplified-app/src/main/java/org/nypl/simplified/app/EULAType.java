package org.nypl.simplified.app;

import java.net.URI;
import java.net.URL;

/**
 * The EULA interface.
 */

public interface EULAType
{
  /**
   * @return {@code true} iff the user has already agreed to the EULA.
   */

  boolean eulaHasAgreed();

  /**
   * Notify the interface that the URI of the latest version of the EULA is
   * {@code u}.
   *
   * @param u The URI of the latest version of the EULA
   */

  void eulaSetLatestURI(URI u);

  /**
   * @return The URL of the <i>currently available</i> EULA text. Note that this
   * is not the same as the {@link #eulaSetLatestURI(URI)}: The EULA is
   * downloaded from the <i>latest</i> URI and saved to the device storage for
   * reading. The saved version is the <i>currently available</i> one. If the
   * EULA has never been downloaded, then the URL refers to a bundled Android
   * asset.
   */

  URL eulaGetReadableURL();

  /**
   * Set whether or not the user has agreed to the EULA.
   *
   * @param t {@code true} iff the user has agreed.
   */

  void eulaSetHasAgreed(boolean t);
}
