package org.nypl.simplified.accounts.api;

import com.google.auto.value.AutoValue;

/**
 * A provider of an authentication mechanism.
 */

@AutoValue
public abstract class AccountAuthenticationProvider {

  AccountAuthenticationProvider() {

  }

  /**
   * Construct a provider name.
   *
   * @param in_value The raw provider name value
   */

  public static AccountAuthenticationProvider create(final String in_value) {
    return new AutoValue_AccountAuthenticationProvider(in_value);
  }

  /**
   * @return The raw provider name value
   */

  public abstract String value();

  @Override
  public String toString() {
    return this.value();
  }

}
