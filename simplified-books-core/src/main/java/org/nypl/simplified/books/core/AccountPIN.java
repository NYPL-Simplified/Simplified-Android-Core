package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOException;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class AccountPIN
{
  private final String value;

  public AccountPIN(
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
    final AccountPIN other = (AccountPIN) obj;
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
    final File file_pin,
    final File file_pin_tmp)
    throws IOException
  {
    FileUtilities.fileWriteUTF8Atomically(file_pin, file_pin_tmp, this.value);
  }
}
