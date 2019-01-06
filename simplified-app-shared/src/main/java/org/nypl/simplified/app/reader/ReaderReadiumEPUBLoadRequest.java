package org.nypl.simplified.app.reader;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import java.io.File;

/**
 * A request to load an EPUB.
 */

@AutoValue
public abstract class ReaderReadiumEPUBLoadRequest {

  /**
   * @return The EPUB file
   */

  public abstract File epubFile();

  /**
   * @return The Adobe rights file, if any
   */

  public abstract OptionType<File> adobeRightsFile();

  /**
   * A mutable builder for producing requests.
   */

  @AutoValue.Builder
  public static abstract class Builder {

    /**
     * @param file The file
     * @return The current builder
     * @see #epubFile()
     */

    public abstract Builder setEpubFile(File file);

    /**
     * @param file The file
     * @return The current builder
     * @see #adobeRightsFile()
     */

    public abstract Builder setAdobeRightsFile(OptionType<File> file);

    /**
     * @return A new request
     */

    public abstract ReaderReadiumEPUBLoadRequest build();
  }

  /**
   * Create a new builder.
   *
   * @param file The EPUB file
   * @return A new builder
   */

  public static Builder builder(final File file) {
    final Builder b = new AutoValue_ReaderReadiumEPUBLoadRequest.Builder();
    b.setEpubFile(file);
    b.setAdobeRightsFile(Option.none());
    return b;
  }
}
