package org.nypl.simplified.accounts.api;

import com.google.auto.value.AutoValue;

import java.net.URI;

/**
 * A description of an account.
 */

@AutoValue
public abstract class AccountDescription {

  AccountDescription() {

  }

  /**
   * Create an account description.
   *
   * @param provider    The account provider ID
   * @param preferences The account preferences
   * @return An account description
   */

  public static Builder builder(
    final URI provider,
    final AccountPreferences preferences) {
    return new AutoValue_AccountDescription.Builder()
      .setPreferences(preferences)
      .setProvider(provider);
  }

  /**
   * @return The account provider associated with the account
   */

  public abstract URI provider();

  /**
   * @return The account preferences
   */

  public abstract AccountPreferences preferences();

  /**
   * @return The current value as a mutable builder
   */

  public abstract Builder toBuilder();

  /**
   * The type of mutable builders for account descriptions.
   */

  @AutoValue.Builder
  public static abstract class Builder {

    /**
     * Set the provider.
     *
     * @param provider The provider
     * @return The current builder
     * @see #provider()
     */

    public abstract Builder setProvider(
      URI provider);

    /**
     * Set the preferences.
     *
     * @param preferences The preferences
     * @return The current builder
     * @see #preferences()
     */

    public abstract Builder setPreferences(
      AccountPreferences preferences);

    /**
     * @return A constructed account description
     */

    public abstract AccountDescription build();
  }
}
