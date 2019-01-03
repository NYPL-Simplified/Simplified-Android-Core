package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;

import org.nypl.simplified.http.core.HTTPOAuthToken;

/**
 * <p>A set of account credentials.</p>
 * <p>At a minimum, these contain an account barcode (username) and a PIN (password). Other credentials
 * may however be present, such as an OAuth token or DRM-specific information.</p>
 */

@AutoValue
public abstract class AccountAuthenticationCredentials {

  AccountAuthenticationCredentials() {

  }

  /**
   * @return A new account credentials builder
   */

  public static Builder builder(
      final AccountPIN pin,
      final AccountBarcode barcode) {

    return new AutoValue_AccountAuthenticationCredentials.Builder()
        .setPin(pin)
        .setBarcode(barcode)
        .setOAuthToken(Option.<HTTPOAuthToken>none())
        .setAuthenticationProvider(Option.<AccountAuthenticationProvider>none())
        .setPatron(Option.<AccountPatron>none())
        .setAdobeCredentials(Option.<AccountAuthenticationAdobePreActivationCredentials>none());
  }

  /**
   * @return The account PIN
   */

  public abstract AccountPIN pin();

  /**
   * @return The account barcode
   */

  public abstract AccountBarcode barcode();

  /**
   * @return The OAuth token, if one is present
   */

  public abstract OptionType<HTTPOAuthToken> oAuthToken();

  /**
   * @return The Adobe credentials, if any are present
   */

  public abstract OptionType<AccountAuthenticationAdobePreActivationCredentials> adobeCredentials();

  /**
   * @return The authentication provider, if any
   */

  public abstract OptionType<AccountAuthenticationProvider> authenticationProvider();

  /**
   * @return The patron information, if any
   */

  public abstract OptionType<AccountPatron> patron();

  /**
   * @return The current value as a mutable builder
   */

  public abstract Builder toBuilder();

  /**
   * @return The Adobe post-activation credentials, if any are present
   */

  public final OptionType<AccountAuthenticationAdobePostActivationCredentials> adobePostActivationCredentials() {
    return adobeCredentials().accept(
        new OptionVisitorType<AccountAuthenticationAdobePreActivationCredentials,
            OptionType<AccountAuthenticationAdobePostActivationCredentials>>() {
          @Override
          public OptionType<AccountAuthenticationAdobePostActivationCredentials>
          none(final None<AccountAuthenticationAdobePreActivationCredentials> none) {
            return Option.none();
          }

          @Override
          public OptionType<AccountAuthenticationAdobePostActivationCredentials>
          some(final Some<AccountAuthenticationAdobePreActivationCredentials> some) {
            return some.get().postActivationCredentials();
          }
        });
  }

  /**
   * @return {@code true} iff these credentials imply that a device has been activated via Adobe DRM
   */

  public final boolean hasActivatedAdobeDevice() {
    return adobePostActivationCredentials().isSome();
  }

  /**
   * A mutable builder for the type.
   */

  @AutoValue.Builder
  public abstract static class Builder {

    Builder() {

    }

    /**
     * @param pin The PIN value
     * @return The current builder
     * @see #pin()
     */

    public abstract Builder setPin(
        AccountPIN pin);

    /**
     * @param barcode The barcode value
     * @return The current builder
     * @see #barcode()
     */

    public abstract Builder setBarcode(
        AccountBarcode barcode);

    /**
     * @param token The token value
     * @return The current builder
     * @see #oAuthToken()
     */

    public abstract Builder setOAuthToken(
        OptionType<HTTPOAuthToken> token);

    /**
     * @param token The token value
     * @return The current builder
     * @see #oAuthToken()
     */

    public Builder setOAuthToken(HTTPOAuthToken token) {
      return setOAuthToken(Option.some(token));
    }

    /**
     * @param credentials The credentials
     * @return The current builder
     * @see #adobeCredentials()
     */

    public abstract Builder setAdobeCredentials(
        OptionType<AccountAuthenticationAdobePreActivationCredentials> credentials);

    /**
     * @param credentials The credentials
     * @return The current builder
     * @see #adobeCredentials()
     */

    public final Builder setAdobeCredentials(
        final AccountAuthenticationAdobePreActivationCredentials credentials) {
      return setAdobeCredentials(Option.some(credentials));
    }

    /**
     * @param provider The provider
     * @return The current builder
     * @see #authenticationProvider()
     */

    public abstract Builder setAuthenticationProvider(
        OptionType<AccountAuthenticationProvider> provider);

    /**
     * @param provider The provider
     * @return The current builder
     * @see #authenticationProvider()
     */

    public final Builder setAuthenticationProvider(AccountAuthenticationProvider provider) {
      return setAuthenticationProvider(Option.some(provider));
    }

    /**
     * @param patron The patron
     * @return The current builder
     * @see #patron()
     */

    public abstract Builder setPatron(
        OptionType<AccountPatron> patron);

    /**
     * @param patron The patron
     * @return The current builder
     * @see #patron()
     */

    public Builder setPatron(AccountPatron patron) {
      return setPatron(Option.some(patron));
    }

    /**
     * @return A constructed set of credentials
     */

    public abstract AccountAuthenticationCredentials build();
  }
}
