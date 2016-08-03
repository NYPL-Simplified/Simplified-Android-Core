package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.files.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * The type of account barcodes.
 *
 * Account barcodes are expected to be 5-14 digit numbers, but the type does not
 * (currently) enforce this fact.
 */

public final class AccountPatron implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final String value;

  /**
   * Construct a barcode.
   *
   * @param in_value The raw barcode value
   */

  public AccountPatron(
    final String in_value)
  {
    this.value = NullCheck.notNull(in_value);
  }




  /**
   * Read a barcode from the first line of the given file.
   *
   * @param f The file
   *
   * @return A barcode
   *
   * @throws IOException On I/O errors
   */

  public static AccountPatron readFromFile(
    final File f)
    throws IOException
  {
    final String text = FileUtilities.fileReadUTF8(f);
    return new AccountPatron(NullCheck.notNull(text));
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
    final AccountPatron other = (AccountPatron) obj;
    return this.value.equals(other.value);
  }

  @Override public int hashCode()
  {
    return this.value.hashCode();
  }

  @Override public String toString()
  {
    return this.value;
  }

  /**
   * Write the barcode to the {@code f_tmp}, atomically renaming {@code f_tmp}
   * to {@code f} on success. For platform independence, {@code f_tmp} and
   * {@code f} should be in the same directory.
   *
   * @param f     The resulting file
   * @param f_tmp The temporary file
   *
   * @throws IOException On I/O errors
   */

  public void writeToFile(
    final File f,
    final File f_tmp)
    throws IOException
  {
    FileUtilities.fileWriteUTF8Atomically(f, f_tmp, this.toString());
  }
}
