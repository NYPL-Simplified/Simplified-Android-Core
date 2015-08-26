package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;

/**
 * The predefined labels.
 */

public enum OPDSAuthenticationDocumentLabels
{
  /**
   * Alternate label for a login.
   */

  LABEL_LOGIN("login"),

  /**
   * Alternate label for a password.
   */

  LABEL_PASSWORD("password");

  private final String name;

  OPDSAuthenticationDocumentLabels(final String in_name)
  {
    this.name = NullCheck.notNull(in_name);
  }
}
