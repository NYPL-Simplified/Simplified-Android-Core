package org.nypl.simplified.books.profiles;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;

import org.nypl.simplified.books.accounts.AccountID;

/**
 * The type of events raised when an account is selected in a profile.
 */

public abstract class ProfileAccountSelectEvent extends ProfileEvent {

  /**
   * Match the type of event.
   *
   * @param <A>        The type of returned values
   * @param <E>        The type of raised exceptions
   * @param on_success Called for {@code ProfileAccountSelectSucceeded} values
   * @param on_failure Called for {@code ProfileAccountSelectFailed} values
   * @return The value returned by the matcher
   * @throws E If the matcher raises {@code E}
   */

  public abstract <A, E extends Exception> A matchSelect(
      PartialFunctionType<ProfileAccountSelectSucceeded, A, E> on_success,
      PartialFunctionType<ProfileAccountSelectFailed, A, E> on_failure)
      throws E;

  /**
   * A profile was created.
   */

  @AutoValue
  public abstract static class ProfileAccountSelectSucceeded extends ProfileAccountSelectEvent {

    /**
     * @return The previous account ID
     */

    public abstract AccountID accountPrevious();

    /**
     * @return The new account ID
     */

    public abstract AccountID accountCurrent();

    @Override
    public final <A, E extends Exception> A matchSelect(
        final PartialFunctionType<ProfileAccountSelectSucceeded, A, E> on_success,
        final PartialFunctionType<ProfileAccountSelectFailed, A, E> on_failure)
        throws E {
      return on_success.call(this);
    }

    /**
     * Create an event.
     *
     * @param prev The previous account ID
     * @param curr The new account ID
     * @return An event
     */

    public static ProfileAccountSelectSucceeded of(
        final AccountID prev,
        final AccountID curr) {
      return new AutoValue_ProfileAccountSelectEvent_ProfileAccountSelectSucceeded(prev, curr);
    }
  }

  /**
   * The account could not be changed.
   */

  @AutoValue
  public abstract static class ProfileAccountSelectFailed extends ProfileAccountSelectEvent {

    /**
     * The error code.
     */

    public enum ErrorCode {
      ERROR_PROFILE_NONE_CURRENT,
      ERROR_ACCOUNT_NONEXISTENT,
      ERROR_ACCOUNT_PROVIDER_UNKNOWN
    }

    /**
     * @return The error code
     */

    public abstract ProfileAccountSelectFailed.ErrorCode errorCode();

    /**
     * @return The exception raised, if any
     */

    public abstract OptionType<Exception> exception();

    @Override
    public final <A, E extends Exception> A matchSelect(
        final PartialFunctionType<ProfileAccountSelectSucceeded, A, E> on_success,
        final PartialFunctionType<ProfileAccountSelectFailed, A, E> on_failure)
        throws E {
      return on_failure.call(this);
    }

    /**
     * Create an event.
     *
     * @param error     The error code
     * @param exception The exception raised, if any
     * @return An event
     */

    public static ProfileAccountSelectFailed of(
        final ProfileAccountSelectFailed.ErrorCode error,
        OptionType<Exception> exception) {
      return new AutoValue_ProfileAccountSelectEvent_ProfileAccountSelectFailed(error, exception);
    }
  }
}
