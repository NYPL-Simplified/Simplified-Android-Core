package org.nypl.simplified.books.profiles;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import org.joda.time.LocalDate;
import org.nypl.simplified.books.reader.ReaderBookmarks;
import org.nypl.simplified.books.reader.ReaderPreferences;

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

  public abstract OptionType<LocalDate> dateOfBirth();

  /**
   * @return The current value as a mutable builder
   */

  public abstract Builder toBuilder();

  /**
   * @return The reader-specific preferences
   */

  public abstract ReaderPreferences readerPreferences();

  /**
   * @return The reader bookmarks
   * @deprecated Use the book database epub format handle to store these
   */

  @Deprecated
  public abstract ReaderBookmarks readerBookmarks();

  /**
   * Update bookmarks.
   *
   * @param bookmarks The new bookmarks
   * @return A new set of preferences based on the current preferences but with the given bookmarks
   */

  public final ProfilePreferences withReaderBookmarks(final ReaderBookmarks bookmarks) {
    return this.toBuilder().setReaderBookmarks(bookmarks).build();
  }

  /**
   * A mutable builder for the type.
   */

  @AutoValue.Builder
  public abstract static class Builder {

    Builder() {

    }

    /**
     * @param bookmarks The reader bookmarks
     * @return The current builder
     * @see #readerBookmarks()
     */

    public abstract Builder setReaderBookmarks(
      ReaderBookmarks bookmarks);

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
      OptionType<LocalDate> date);

    /**
     * @param date The date
     * @return The current builder
     * @see #dateOfBirth()
     */

    public final Builder setDateOfBirth(final LocalDate date) {
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
      .setReaderPreferences(
        ReaderPreferences.builder()
          .build())
      .setReaderBookmarks(ReaderBookmarks.create(ImmutableMap.of()))
      .setGender(Option.none())
      .setDateOfBirth(Option.none());
  }
}
