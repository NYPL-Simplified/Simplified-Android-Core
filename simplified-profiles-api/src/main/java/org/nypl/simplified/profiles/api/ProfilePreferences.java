package org.nypl.simplified.profiles.api;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import org.joda.time.LocalDate;
import org.nypl.simplified.reader.api.ReaderPreferences;

/**
 * A set of preferences for a profile.
 */

@AutoValue
public abstract class ProfilePreferences {

  ProfilePreferences() {

  }

  /**
   * @return The gender of the reader (if one has been explicitly specified)
   */

  public abstract OptionType<String> gender();

  /**
   * @return The date of birth of the reader (if one has been explicitly specified)
   */

  public abstract OptionType<ProfileDateOfBirth> dateOfBirth();

  /**
   * @return The current value as a mutable builder
   */

  public abstract Builder toBuilder();

  /**
   * @return The reader-specific preferences
   */

  public abstract ReaderPreferences readerPreferences();

  /**
   * A mutable builder for the type.
   */

  @AutoValue.Builder
  public abstract static class Builder {

    Builder() {

    }

    /**
     * @param prefs The reader preferences
     * @return The current builder
     * @see #readerPreferences()
     */

    public abstract Builder setReaderPreferences(
      ReaderPreferences prefs);

    /**
     * @param gender The gender
     * @return The current builder
     * @see #gender()
     */

    public abstract Builder setGender(
      OptionType<String> gender);

    /**
     * @param gender The gender
     * @return The current builder
     * @see #gender()
     */

    public final Builder setGender(final String gender) {
      return setGender(Option.some(gender));
    }

    /**
     * @param date The date
     * @return The current builder
     * @see #dateOfBirth()
     */

    public abstract Builder setDateOfBirth(
      OptionType<ProfileDateOfBirth> date);

    /**
     * @param date The date
     * @return The current builder
     * @see #dateOfBirth()
     */

    public final Builder setDateOfBirth(final ProfileDateOfBirth date) {
      return setDateOfBirth(Option.some(date));
    }

    /**
     * @return A profile description based on the given parameters
     */

    public abstract ProfilePreferences build();
  }

  /**
   * @return A new builder
   */

  public static ProfilePreferences.Builder builder() {
    return new AutoValue_ProfilePreferences.Builder()
      .setReaderPreferences(ReaderPreferences.builder().build())
      .setGender(Option.none())
      .setDateOfBirth(Option.none());
  }
}
