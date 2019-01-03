package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;

import java.net.URI;

/**
 * <p>A description of the details of authentication.</p>
 */

@AutoValue
public abstract class AccountProviderAuthenticationDescription {

  AccountProviderAuthenticationDescription() {

  }

  /**
   * @return The required length of passcodes, or {@code 0} if no specific length is required
   */

  public abstract int passCodeLength();

  /**
   * @return {@code true} iff passcodes may contain letters
   */

  public abstract boolean passCodeMayContainLetters();

  /**
   * @return The login URI
   */

  public abstract URI loginURI();

  /**
   * The type of mutable builders for account providers.
   */

  @AutoValue.Builder
  public abstract static class Builder {

    Builder() {

    }

    /**
     * @see #loginURI()
     * @param uri The default login URI
     * @return The current builder
     */

    public abstract Builder setLoginURI(URI uri);

    /**
     * @param length The pass code length
     * @return The current builder
     * @see #passCodeLength()
     */

    public abstract Builder setPassCodeLength(int length);

    /**
     * @param letters {@code  true} iff the pass code may contain letters
     * @return The current builder
     * @see #passCodeMayContainLetters()
     */

    public abstract Builder setPassCodeMayContainLetters(boolean letters);

    /**
     * @return The constructed account provider
     */

    public abstract AccountProviderAuthenticationDescription build();
  }

  /**
   * @return A new account provider builder
   */

  public static Builder builder() {
    return new AutoValue_AccountProviderAuthenticationDescription.Builder();
  }
}
