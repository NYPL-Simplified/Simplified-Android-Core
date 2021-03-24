package org.nypl.simplified.files;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.File;
import java.io.IOException;

/**
 * Directory utility functions.
 */

public final class DirectoryUtilities
{
  private DirectoryUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Create a temporary directory.
   *
   * @return The directory
   *
   * @throws IOException On I/O errors
   */

  public static File directoryCreateTemporary()
    throws IOException
  {
    final File f = NullCheck.notNull(File.createTempFile("tmp", ""));
    f.delete();
    f.mkdir();
    return f;
  }

  /**
   * Delete the given directory, including all subdirectories and files.
   *
   * @param f The directory
   *
   * @throws IOException On I/O errors
   */

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

  /**
   * Create a directory if it does not already exist.
   *
   * @param directory The directory
   *
   * @throws IOException On I/O errors
   */

  public static void directoryCreate(
    final File directory)
    throws IOException
  {
    if (directory.mkdirs() == false) {
      if (directory.isDirectory() == false) {
        throw new IOException(
          String.format(
            "Could not create directory '%s': Not a directory", directory));
      }
    }
  }

  /**
   * Copy the directory {@code from} to {@code to}, including all subdirectories and files.
   *
   * @param from The source directory
   * @param to   The target directory
   *
   * @throws IOException On I/O errors
   */

  public static void directoryCopy(
    final File from,
    final File to)
    throws IOException
  {
    if (from.isDirectory()) {
      directoryCreate(to);
      for (String child : from.list()) {
        directoryCopy(new File(from, child), new File(to, child));
      }
    } else {
      FileUtilities.fileCopy(from, to);
    }
  }
}
