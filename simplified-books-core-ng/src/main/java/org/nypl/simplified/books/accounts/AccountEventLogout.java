package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;

/**
 * The type of account logout events.
 */

public abstract class AccountEventLogout extends AccountEvent {

  /**
   * Match the type of event.
   *
   * @param <A>        The type of returned values
   * @param <E>        The type of raised exceptions
   * @param on_success Called for {@code AccountLogoutSucceeded} values
   * @param on_failure Called for {@code AccountLogoutFailed} values
   * @return The value returned by the matcher
   * @throws E If the matcher raises {@code E}
   */

  public abstract <A, E extends Exception> A matchLogout(
    PartialFunctionType<AccountLogoutSucceeded, A, E> on_success,
    PartialFunctionType<AccountLogoutFailed, A, E> on_failure)
    throws E;

  /**
   * Logging in succeeded.
   */

  @AutoValue
  public abstract static class AccountLogoutSucceeded extends AccountEventLogout {

    /**
     * @return The ID of the account
     */

    public abstract AccountID id();

    @Override
    public final <A, E extends Exception> A matchLogout(
      final PartialFunctionType<AccountLogoutSucceeded, A, E> on_success,
      final PartialFunctionType<AccountLogoutFailed, A, E> on_failure)
      throws E {
      return on_success.call(this);
    }

    /**
     * @return An event
     */

    public static AccountLogoutSucceeded of(AccountID id) {
      return new AutoValue_AccountEventLogout_AccountLogoutSucceeded(id);
    }
  }

  /**
   * Logging in failed.
   */

  @AutoValue
  public abstract static class AccountLogoutFailed extends AccountEventLogout {

    /**
     * The error codes that can be raised
     */

    public enum ErrorCode {

      /**
       * A profile or account configuration problem occurred (such as the user not having
       * selected a profile).
       */

      ERROR_PROFILE_CONFIGURATION,

      ERROR_ACCOUNTS_DATABASE,

      /**
       * A general error code that is not specifically actionable (such as an I/O error
       * or a programming mistake).
       */

      ERROR_GENERAL
    }

    /**
     * @return The error code
     */

    public abstract AccountLogoutFailed.ErrorCode errorCode();

    /**
     * @return The exception raised during logging in, if any
     */

    public abstract OptionType<Exception> exception();

    @Override
    public final <A, E extends Exception> A matchLogout(
      final PartialFunctionType<AccountLogoutSucceeded, A, E> on_success,
      final PartialFunctionType<AccountLogoutFailed, A, E> on_failure)
      throws E {
      return on_failure.call(this);
    }

    /**
     * @param code      The error code
     * @param exception The exception raised, if any
     * @return An event
     */

    public static AccountLogoutFailed of(
      final AccountLogoutFailed.ErrorCode code,
      final OptionType<Exception> exception) {
      return new AutoValue_AccountEventLogout_AccountLogoutFailed(code, exception);
    }

    /*
     * @param exception The exception raised
     * @return An event
     */

    public static AccountLogoutFailed ofException(
      final Exception exception) {
      return new AutoValue_AccountEventLogout_AccountLogoutFailed(
        ErrorCode.ERROR_GENERAL, Option.some(exception));
    }
  }
}
