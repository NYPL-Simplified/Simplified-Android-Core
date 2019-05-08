package org.nypl.simplified.books.api;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Functions to construct book IDs.
 */

public final class BookIDs {

  private BookIDs() {
    throw new UnreachableCodeException();
  }

  /**
   * Construct a book ID derived from the hash of the given text.
   *
   * @param text The text
   * @return A new book ID
   */

  public static BookID newFromText(
      final String text) {

    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(text.getBytes());
      final byte[] dg = md.digest();

      final StringBuilder b = new StringBuilder(64);
      for (int index = 0; index < dg.length; ++index) {
        final byte bb = dg[index];
        b.append(String.format("%02x", bb));
      }

      return BookID.create(b.toString());
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

  public static BookID newFromOPDSEntry(
      final OPDSAcquisitionFeedEntry e)
  {
    return newFromText(NullCheck.notNull(e, "Entry").getID());
  }
}
