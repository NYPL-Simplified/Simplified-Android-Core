package org.nypl.simplified.books.profiles;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;

/**
 * The type of events raised on profile creation.
 */

public abstract class ProfileCreationEvent extends ProfileEvent {

  /**
   * Match the type of event.
   *
   * @param <A>                The type of returned values
   * @param <E>                The type of raised exceptions
   * @param on_created         Called for {@code ProfileEventCreated} values
   * @param on_creation_failed Called for {@code ProfileEventCreationFailed} values
   * @return The value returned by the matcher
   * @throws E If the matcher raises {@code E}
   */

  public abstract <A, E extends Exception> A matchCreation(
      PartialFunctionType<ProfileCreationSucceeded, A, E> on_created,
      PartialFunctionType<ProfileCreationFailed, A, E> on_creation_failed)
      throws E;

  /**
   * A profile was created.
   */

  @AutoValue
  public abstract static class ProfileCreationSucceeded extends ProfileCreationEvent {

    /**
     * @return The profile display name
     */

    public abstract String displayName();

    /**
     * @return The new profile ID
     */

    public abstract ProfileID id();

    @Override
    public final <A, E extends Exception> A matchCreation(
        final PartialFunctionType<ProfileCreationSucceeded, A, E> on_created,
        final PartialFunctionType<ProfileCreationFailed, A, E> on_creation_failed)
        throws E {
      return on_created.call(this);
    }

    /**
     * Create an event.
     *
     * @param display_name The profile display name
     * @param id           The profile ID
     * @return An event
     */

    public static ProfileCreationSucceeded of(
        final String display_name,
        final ProfileID id) {
      return new AutoValue_ProfileCreationEvent_ProfileCreationSucceeded(display_name, id);
    }
  }

  /**
   * The creation of a profile failed.
   */

  @AutoValue
  public abstract static class ProfileCreationFailed extends ProfileCreationEvent {

    /**
     * The error code.
     */

    public enum ErrorCode {

      /**
       * A profile already exists with the given display name.
       */

      ERROR_DISPLAY_NAME_ALREADY_USED,

      /**
       * A general error code that is not specifically actionable (such as an I/O error
       * or a programming mistake).
       */

      ERROR_GENERAL
    }

    /**
     * @return The profile display name
     */

    public abstract String displayName();

    /**
     * @return The error code
     */

    public abstract ProfileCreationFailed.ErrorCode errorCode();

    /**
     * @return The exception raised, if any
     */

    public abstract OptionType<Exception> exception();

    @Override
    public final <A, E extends Exception> A matchCreation(
        final PartialFunctionType<ProfileCreationSucceeded, A, E> on_created,
        final PartialFunctionType<ProfileCreationFailed, A, E> on_creation_failed)
        throws E {
      return on_creation_failed.call(this);
    }

    /**
     * Create an event.
     *
     * @param display_name The profile display name
     * @param error        The error code
     *                     @param exception The exception raised, if any
     * @return An event
     */

    public static ProfileCreationFailed of(
        final String display_name,
        final ProfileCreationFailed.ErrorCode error,
        final OptionType<Exception> exception) {
      return new AutoValue_ProfileCreationEvent_ProfileCreationFailed(
          display_name, error, exception);
    }
  }
}
