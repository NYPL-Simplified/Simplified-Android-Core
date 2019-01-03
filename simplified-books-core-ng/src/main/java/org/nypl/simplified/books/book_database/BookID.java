package org.nypl.simplified.books.book_database;

import com.google.auto.value.AutoValue;
import com.io7m.jnull.NullCheck;

import java.io.Serializable;

/**
 * The unique identifier for a given book. This is typically a SHA256 hash of the original book
 * URI and is intended to be safe for use as a file or directory name.
 */

@AutoValue
public abstract class BookID implements Comparable<BookID>, Serializable {

  BookID() {

  }

  /**
   * Construct a book ID.
   *
   * @param in_value The raw book ID value
   */

  public static BookID create(final String in_value)
  {
    return new AutoValue_BookID(in_value);
  }

  /**
   * @return The actual PIN value
   */

  public abstract String value();

  /**
   * @return The brief form of the ID
   */

  public final String brief()
  {
    return this.value().substring(0, Math.min(8, value().length()));
  }

  @Override
  public String toString() {
    return value();
  }

  @Override
  public final int compareTo(final BookID other) {
    return this.value().compareTo(NullCheck.notNull(other, "Other").value());
  }
}
