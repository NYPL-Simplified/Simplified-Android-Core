package org.nypl.simplified.books.core;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class BookID implements Serializable
{
  private static final long serialVersionUID = 1L;

  public static BookID fromString(
    final String id)
  {
    return new BookID(id);
  }

  public static BookID newIDFromEntry(
    final OPDSAcquisitionFeedEntry e)
  {
    try {
      NullCheck.notNull(e);
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(e.getID().getBytes());
      final byte[] dg = md.digest();

      final StringBuilder b = new StringBuilder();
      for (int index = 0; index < dg.length; ++index) {
        final byte bb = dg[index];
        b.append(String.format("%02x", bb));
      }

      return new BookID(NullCheck.notNull(b.toString()));
    } catch (final NoSuchAlgorithmException x) {
      throw new IllegalStateException(x);
    }
  }

  private final String id;

  private BookID(
    final String in_id)
  {
    this.id = NullCheck.notNull(in_id);
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
