package org.nypl.simplified.books.profiles;

import com.google.auto.value.AutoValue;

/**
 * The type of profile preferences events.
 */

@AutoValue
public abstract class ProfilePreferencesChanged extends ProfileEvent {

  /**
   * @return {@code true} if the reader bookmarks changed
   */

  public abstract boolean changedReaderBookmarks();

  /**
   * @return {@code true} if the reader preferences changed
   */

  public abstract boolean changedReaderPreferences();

  /**
   * The mutable event builder.
   */

  @AutoValue.Builder
  public static abstract class Builder {

    /**
     * @see #changedReaderBookmarks()
     * @param x The value
     * @return {@code true} if the reader bookmarks changed
     */

    public abstract Builder setChangedReaderBookmarks(boolean x);

    /**
     * @see #changedReaderPreferences()
     * @param x The value
     * @return {@code true} if the reader preferences changed
     */

    public abstract Builder setChangedReaderPreferences(boolean x);

    /**
     * @return The constructed event
     */

    public abstract ProfilePreferencesChanged build();
  }

  /**
   * @return A new mutable builder
   */

  public static ProfilePreferencesChanged.Builder builder()
  {
    final AutoValue_ProfilePreferencesChanged.Builder b =
        new AutoValue_ProfilePreferencesChanged.Builder();
    b.setChangedReaderBookmarks(false);
    b.setChangedReaderPreferences(false);
    return b;
  }
}
