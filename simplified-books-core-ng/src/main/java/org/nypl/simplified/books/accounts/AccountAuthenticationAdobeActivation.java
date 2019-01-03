package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;

/**
 * A token used by Adobe DRM to activate and deactivate devices.
 */

@AutoValue
public abstract class AccountAuthenticationAdobeActivation {

  AccountAuthenticationAdobeActivation() {

  }

  /**
   * Construct a patron.
   *
   * @param in_value The raw patron value
   */

  public static AccountAuthenticationAdobeActivation create(
      final String in_value) {
    return new AutoValue_AccountAuthenticationAdobeActivation(in_value);
  }

  /**
   * @return The raw token value
   */

  public abstract String value();

}
