package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.Files;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class AccountBarcode
{
  public static AccountBarcode readFromFile(
    final File f)
    throws IOException
  {
    final String text =
      Files.readFirstLine(NullCheck.notNull(f), Charset.forName("UTF-8"));
    return new AccountBarcode(NullCheck.notNull(text));
  }

  private final String value;

  public AccountBarcode(
    final String in_value)
  {
    this.value = NullCheck.notNull(in_value);
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
    final AccountBarcode other = (AccountBarcode) obj;
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

  public void writeToFile(
    final File f,
    final File f_tmp)
    throws IOException
  {
    FileUtilities.fileWriteUTF8Atomically(f, f_tmp, this.toString());
  }
}
