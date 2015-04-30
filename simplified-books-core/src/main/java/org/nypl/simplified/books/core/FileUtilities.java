package org.nypl.simplified.books.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

public final class FileUtilities
{
  public static void createDirectory(
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

  public static void fileCopy(
    final File from,
    final File to)
    throws IOException
  {
    NullCheck.notNull(from);
    NullCheck.notNull(to);

    final byte[] buffer = new byte[8192];

    FileInputStream in = null;
    try {
      in = new FileInputStream(from);

      FileOutputStream out = null;
      try {
        out = new FileOutputStream(to);

        for (;;) {
          final int r = in.read(buffer);
          if (r == -1) {
            break;
          }
          out.write(buffer, 0, r);
          out.flush();
        }

      } finally {
        if (out != null) {
          out.flush();
          out.close();
        }
      }
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  public static String fileReadUTF8(
    final File file)
    throws IOException
  {
    NullCheck.notNull(file);

    final StringBuilder b = new StringBuilder((int) file.length());

    final BufferedReader in =
      new BufferedReader(new InputStreamReader(
        new FileInputStream(file),
        "UTF-8"));

    try {
      for (;;) {
        final String line = in.readLine();
        if (line == null) {
          break;
        }
        b.append(line);
      }
    } finally {
      in.close();
    }

    return NullCheck.notNull(b.toString());
  }

  public static void fileRename(
    final File from,
    final File to)
    throws IOException
  {
    NullCheck.notNull(from);
    NullCheck.notNull(to);

    if (from.renameTo(to) == false) {
      if (from.isFile() == false) {
        throw new IOException(
          String
            .format(
              "Could not rename '%s' to '%s' ('%s' does not exist or is not a file)",
              from,
              to,
              from));
      }

      final File to_parent = to.getParentFile();
      if (to_parent.isDirectory() == false) {
        throw new IOException(String.format(
          "Could not rename '%s' to '%s' ('%s' is not a directory)",
          from,
          to,
          to_parent));
      }

      throw new IOException(String.format(
        "Could not rename '%s' to '%s'",
        from,
        to));
    }
  }

  public static void fileWriteUTF8(
    final File file,
    final String text)
    throws IOException
  {
    NullCheck.notNull(file);
    NullCheck.notNull(text);

    final Writer out =
      new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream(file),
        "UTF-8"));

    try {
      out.write(text);
      out.flush();
    } finally {
      out.close();
    }
  }

  public static void fileWriteUTF8Atomically(
    final File f,
    final File f_tmp,
    final String text)
    throws IOException
  {
    NullCheck.notNull(f);
    NullCheck.notNull(f_tmp);
    NullCheck.notNull(text);
    FileUtilities.fileWriteUTF8(f_tmp, text);
    FileUtilities.fileRename(f_tmp, f);
  }

  private FileUtilities()
  {
    throw new UnreachableCodeException();
  }
}
