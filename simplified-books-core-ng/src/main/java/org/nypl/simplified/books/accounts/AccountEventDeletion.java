package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;

/**
 * The type of account deletion events.
 */

public abstract class AccountEventDeletion extends AccountEvent {

  /**
   * Match the type of event.
   *
   * @param <A>        The type of returned values
   * @param <E>        The type of raised exceptions
   * @param on_success Called for {@code AccountDeletionSucceeded} values
   * @param on_failure Called for {@code AccountDeletionFailed} values
   * @return The value returned by the matcher
   * @throws E If the matcher raises {@code E}
   */

  public abstract <A, E extends Exception> A matchDeletion(
      PartialFunctionType<AccountDeletionSucceeded, A, E> on_success,
      PartialFunctionType<AccountDeletionFailed, A, E> on_failure)
      throws E;


  /**
   * Creating an account succeeded.
   */

  @AutoValue
  public abstract static class AccountDeletionSucceeded extends AccountEventDeletion {

    /**
     * @return An event
     */

    public static AccountDeletionSucceeded of(final AccountProvider provider) {
      return new AutoValue_AccountEventDeletion_AccountDeletionSucceeded(provider);
    }

    /**
     * @return The account provider
     */

    public abstract AccountProvider provider();

    @Override
    public final <A, E extends Exception> A matchDeletion(
        final PartialFunctionType<AccountDeletionSucceeded, A, E> on_success,
        final PartialFunctionType<AccountDeletionFailed, A, E> on_failure)
        throws E {
      return on_success.call(this);
    }
  }

  /**
   * Creating an account failed.
   */

  @AutoValue
  public abstract static class AccountDeletionFailed extends AccountEventDeletion {

    /**
     * @param code      The error code
     * @param exception The exception raised, if any
     * @return An event
     */

    public static AccountDeletionFailed of(
        final AccountDeletionFailed.ErrorCode code,
        final OptionType<Exception> exception) {
      return new AutoValue_AccountEventDeletion_AccountDeletionFailed(code, exception);
    }

    /**
     * @param exception The exception raised
     * @return An event
     */

    public static AccountEventDeletion ofException(final Exception exception) {
      return new AutoValue_AccountEventDeletion_AccountDeletionFailed(
          ErrorCode.ERROR_GENERAL, Option.some(exception));

    }

    /**
     * @return The error code
     */

    public abstract AccountDeletionFailed.ErrorCode errorCode();

    /**
     * @return The exception raised during logging in, if any
     */

    public abstract OptionType<Exception> exception();

    @Override
    public final <A, E extends Exception> A matchDeletion(
        final PartialFunctionType<AccountDeletionSucceeded, A, E> on_success,
        final PartialFunctionType<AccountDeletionFailed, A, E> on_failure)
        throws E {
      return on_failure.call(this);
    }

    /**
     * The error codes that can be raised
     */

    public enum ErrorCode {

      /**
       * The user attempted to delete the only remaining account.
       */

      ERROR_ACCOUNT_ONLY_ONE_REMAINING,

      /**
       * A general error code that is not specifically actionable (such as an I/O error
       * or a programming mistake).
       */

      ERROR_GENERAL
    }
  }
}
