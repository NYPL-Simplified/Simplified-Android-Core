package org.nypl.drm.core;

import java.util.Objects;
import com.io7m.jnull.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of the {@link AdobeAdeptResourceProviderType}
 * interface, returning the bytes of a certificate for all requests.
 */

public final class AdobeAdeptResourceProvider
  implements AdobeAdeptResourceProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(AdobeAdeptResourceProvider.class);
  }

  private final byte[] certificate;

  private AdobeAdeptResourceProvider(final byte[] in_certificate)
  {
    this.certificate = Objects.requireNonNull(in_certificate);
  }

  /**
   * Construct a resource provider from the given certificate.
   *
   * @param in_certificate The certificate
   *
   * @return A new resource provider
   */

  public static AdobeAdeptResourceProviderType get(final byte[] in_certificate)
  {
    return new AdobeAdeptResourceProvider(in_certificate);
  }

  @Override public @Nullable byte[] getResourceAsBytes(
    final String name)
  {
    AdobeAdeptResourceProvider.LOG.debug("getResource: {}", name);

    if ("res:///ReaderClientCert.sig".equals(name)) {
      return this.certificate;
    }

    AdobeAdeptResourceProvider.LOG.debug(
      "getResource: {} returning null", name);
    return null;
  }
}
