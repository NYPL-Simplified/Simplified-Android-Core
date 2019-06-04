package org.nypl.simplified.accounts.api;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.PartialFunctionType;

/**
 * The type of account creation events.
 */

public abstract class AccountEventCreation extends AccountEvent {

  /**
   * Match the type of event.
   *
   * @param <A>        The type of returned values
   * @param <E>        The type of raised exceptions
   * @param on_success Called for {@code AccountCreationSucceeded} values
   * @param on_failure Called for {@code AccountCreationFailed} values
   * @return The value returned by the matcher
   * @throws E If the matcher raises {@code E}
   */

  public abstract <A, E extends Exception> A matchCreation(
      PartialFunctionType<AccountCreationSucceeded, A, E> on_success,
      PartialFunctionType<AccountCreationFailed, A, E> on_failure)
      throws E;


  /**
   * Creating an account succeeded.
   */

  @AutoValue
  public abstract static class AccountCreationSucceeded extends AccountEventCreation {

    /**
     * @return The ID of the created account
     */

    public abstract AccountID id();

    /**
     * @return The account provider
     */

    public abstract AccountProviderType provider();

    @Override
    public final <A, E extends Exception> A matchCreation(
        final PartialFunctionType<AccountCreationSucceeded, A, E> on_success,
        final PartialFunctionType<AccountCreationFailed, A, E> on_failure)
        throws E {
      return on_success.call(this);
    }

    /**
     * @return An event
     */

    public static AccountCreationSucceeded of(
      final AccountID id,
      final AccountProviderType provider) {
      return new AutoValue_AccountEventCreation_AccountCreationSucceeded(id, provider);
    }
  }

  /**
   * Creating an account failed.
   */

  @AutoValue
  public abstract static class AccountCreationFailed extends AccountEventCreation {

    /**
     * @return The exception raised during account creation
     */

    public abstract Exception exception();

    @Override
    public final <A, E extends Exception> A matchCreation(
        final PartialFunctionType<AccountCreationSucceeded, A, E> on_success,
        final PartialFunctionType<AccountCreationFailed, A, E> on_failure)
        throws E {
      return on_failure.call(this);
    }

    /**
     * @param exception The exception raised
     * @return An event
     */

    public static AccountCreationFailed of(final Exception exception) {
      return new AutoValue_AccountEventCreation_AccountCreationFailed(exception);
    }
  }
}
