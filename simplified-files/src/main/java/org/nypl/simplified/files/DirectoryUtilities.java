package org.nypl.simplified.files;

import java.io.File;
import java.io.IOException;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Directory utility functions.
 */

public final class DirectoryUtilities
{
  public static File directoryCreateTemporary()
    throws IOException
  {
    final File f = NullCheck.notNull(File.createTempFile("tmp", ""));
    f.delete();
    f.mkdir();
    return f;
  }

  public static void directoryDelete(
    final File f)
    throws IOException
  {
    NullCheck.notNull(f);
    if (f.isDirectory()) {
      for (final File es : f.listFiles()) {
        DirectoryUtilities.directoryDelete(NullCheck.notNull(es));
      }
    }
    FileUtilities.fileDelete(f);
  }

  public static void directoryCreate(
    final File directory)
    throws IOException
  {
    directory.mkdirs();
    if (directory.isDirectory() == false) {
      throw new IOException(String.format(
        "Could not create directory '%s': Not a directory",
        directory));
    }
  }

  private DirectoryUtilities()
  {
    throw new UnreachableCodeException();
  }
}
