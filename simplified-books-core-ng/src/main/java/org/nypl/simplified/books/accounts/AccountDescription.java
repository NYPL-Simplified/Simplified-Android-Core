package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

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
   * @param provider The account provider ID
   * @return An account description
   */

  public static Builder builder(final URI provider) {
    return new AutoValue_AccountDescription.Builder()
        .setCredentials(Option.none())
        .setProvider(provider);
  }

  /**
   * @return The account provider associated with the account
   */

  public abstract URI provider();

  /**
   * @return The account credentials
   */

  public abstract OptionType<AccountAuthenticationCredentials> credentials();

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
     * Set the credentials.
     *
     * @param credentials The credentials
     * @return The current builder
     * @see #credentials()
     */

    public abstract Builder setCredentials(
        OptionType<AccountAuthenticationCredentials> credentials);

    /**
     * Set the credentials.
     *
     * @param credentials The credentials
     * @return The current builder
     * @see #credentials()
     */

    public final Builder setCredentials(
        final AccountAuthenticationCredentials credentials)
    {
      return setCredentials(Option.some(credentials));
    }

    /**
     * @return A constructed account description
     */

    public abstract AccountDescription build();
  }
}
