package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;

/**
 * The type of account login events.
 */

public abstract class AccountEventLogin extends AccountEvent {

  /**
   * Match the type of event.
   *
   * @param <A>        The type of returned values
   * @param <E>        The type of raised exceptions
   * @param on_success Called for {@code AccountLoginSucceeded} values
   * @param on_failure Called for {@code AccountLoginFailed} values
   * @return The value returned by the matcher
   * @throws E If the matcher raises {@code E}
   */

  public abstract <A, E extends Exception> A matchLogin(
    PartialFunctionType<AccountLoginSucceeded, A, E> on_success,
    PartialFunctionType<AccountLoginFailed, A, E> on_failure)
    throws E;

  /**
   * Logging in succeeded.
   */

  @AutoValue
  public abstract static class AccountLoginSucceeded extends AccountEventLogin {

    /**
     * @return The ID of the account
     */

    public abstract AccountID id();

    /**
     * @return The accepted credentials
     */

    public abstract AccountAuthenticationCredentials credentials();

    @Override
    public final <A, E extends Exception> A matchLogin(
      final PartialFunctionType<AccountLoginSucceeded, A, E> on_success,
      final PartialFunctionType<AccountLoginFailed, A, E> on_failure)
      throws E {
      return on_success.call(this);
    }

    /**
     * @return An event
     */

    public static AccountLoginSucceeded of(
      final AccountID id,
      final AccountAuthenticationCredentials credentials) {
      return new AutoValue_AccountEventLogin_AccountLoginSucceeded(id, credentials);
    }
  }

  /**
   * Logging in failed.
   */

  @AutoValue
  public abstract static class AccountLoginFailed extends AccountEventLogin {

    /**
     * The error codes that can be raised
     */

    public enum ErrorCode {

      /**
       * A profile or account configuration problem occurred (such as the user not having
       * selected a profile).
       */

      ERROR_PROFILE_CONFIGURATION,

      /**
       * A network problem occurred whilst trying to contact a remote server.
       */

      ERROR_NETWORK_EXCEPTION,

      /**
       * The provided credentials were rejected by the server.
       */

      ERROR_CREDENTIALS_INCORRECT,

      /**
       * The server responded with an error.
       */

      ERROR_SERVER_ERROR,

      /**
       * The specified account does not exist.
       */

      ERROR_ACCOUNT_NONEXISTENT,

      /**
       * A general error code that is not specifically actionable (such as an I/O error
       * or a programming mistake).
       */

      ERROR_GENERAL
    }

    /**
     * @return The error code
     */

    public abstract AccountLoginFailed.ErrorCode errorCode();

    /**
     * @return The exception raised during logging in, if any
     */

    public abstract OptionType<Exception> exception();

    @Override
    public final <A, E extends Exception> A matchLogin(
      final PartialFunctionType<AccountLoginSucceeded, A, E> on_success,
      final PartialFunctionType<AccountLoginFailed, A, E> on_failure)
      throws E {
      return on_failure.call(this);
    }

    /**
     *
     * @param code      The error code
     * @param exception The exception raised, if any
     * @return An event
     */

    public static AccountLoginFailed of(
      final AccountLoginFailed.ErrorCode code,
      final OptionType<Exception> exception) {
      return new AutoValue_AccountEventLogin_AccountLoginFailed(code, exception);
    }

    /*
     *
     * @param exception The exception raised
     * @return An event
     */

    public static AccountLoginFailed ofException(
      final Exception exception) {
      return new AutoValue_AccountEventLogin_AccountLoginFailed(
        ErrorCode.ERROR_GENERAL, Option.some(exception));
    }
  }
}
