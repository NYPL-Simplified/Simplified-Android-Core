package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The unique identifier for a given book on disk. This is typically a SHA256
 * hash of the original book URI and is safe for use as a file or directory
 * name.
 */

public final class BookID implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final String id;
  private final String short_id;

  private BookID(
    final String in_id)
  {
    this.id = NullCheck.notNull(in_id);
    this.short_id = in_id.substring(0, Math.min(8, in_id.length()));
  }

  /**
   * Create a book ID consisting of the given string.
   *
   * @param id The string
   *
   * @return A new book ID
   */

  public static BookID exactString(
    final String id)
  {
    return new BookID(id);
  }

  /**
   * Construct a book ID derived from the hash of the given text.
   *
   * @param text The text
   *
   * @return A new book ID
   */

  public static BookID newFromText(
    final String text)
  {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(text.getBytes());
      final byte[] dg = md.digest();

      final StringBuilder b = new StringBuilder(64);
      for (int index = 0; index < dg.length; ++index) {
        final Byte bb = Byte.valueOf(dg[index]);
        b.append(String.format("%02x", bb));
      }

      return new BookID(NullCheck.notNull(b.toString()));
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Calculate a book ID from the given acquisition feed entry.
   *
   * @param e The entry
   *
   * @return A new book ID
   */

  public static BookID newIDFromEntry(
    final OPDSAcquisitionFeedEntry e)
  {
    NullCheck.notNull(e);
    return BookID.newFromText(e.getID());
  }

  /**
   * @return A shortened form of the main book ID (at most eight characters),
   * intended to make it somewhat easier to read book IDs when they appear in
   * log files
   */

  public String getShortID()
  {
    return this.short_id;
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final BookID other = (BookID) obj;
    return this.id.equals(other.id);
  }

  @Override public int hashCode()
  {
    return this.id.hashCode();
  }

  @Override public String toString()
  {
    return this.id;
  }
}
