package org.nypl.simplified.books.reader;

import com.google.auto.value.AutoValue;

/**
 * The reader preferences.
 */

@AutoValue
public abstract class ReaderPreferences {

  /**
   * @return A new preferences builder
   */

  public static Builder builder() {
    final Builder builder = new AutoValue_ReaderPreferences.Builder();
    builder.setColorScheme(ReaderColorScheme.SCHEME_BLACK_ON_WHITE);
    builder.setFontFamily(ReaderFontSelection.READER_FONT_SANS_SERIF);
    builder.setBrightness(1.0);
    builder.setFontScale(100.0);
    return builder;
  }

  /**
   * @return The color scheme used for the reader
   */

  public abstract ReaderColorScheme colorScheme();

  /**
   * @return The font selection used for the reader
   */

  public abstract ReaderFontSelection fontFamily();

  /**
   * @return The font scale used for the reader
   */

  public abstract double fontScale();

  /**
   * @return The current value as a mutable builder
   */

  public abstract Builder toBuilder();

  /**
   *
   * @return The screen brightness value in the range {@code [0.0, 1.0]}
   */

  public abstract double brightness();

  /**
   * A mutable builder for reader preferences.
   */

  @AutoValue.Builder
  public static abstract class Builder {

    /**
     * @param color The new color scheme
     * @return This builder
     * @see #colorScheme()
     */

    public abstract Builder setColorScheme(ReaderColorScheme color);

    /**
     * @param font The font family
     * @return This builder
     * @see #fontFamily()
     */

    public abstract Builder setFontFamily(ReaderFontSelection font);

    /**
     * @param scale The font scale
     * @return This builder
     * @see #fontScale()
     */

    public abstract Builder setFontScale(double scale);

    /**
     * @param brightness The brightness
     * @return This builder
     * @see #brightness()
     */

    public abstract Builder setBrightness(double brightness);

    public abstract double brightness();

    public abstract double fontScale();

    /**
     * @return A set of preferences
     */

    public final ReaderPreferences build() {
      this.setFontScale(Math.min(200.0, Math.max(50.0, fontScale())));
      this.setBrightness(Math.min(1.0, Math.max(0.0, brightness())));
      return autoBuild();
    }

    abstract ReaderPreferences autoBuild();
  }
}
