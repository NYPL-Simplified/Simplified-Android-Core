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

final class FileUtilities
{
  static void createDirectory(
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

  static String fileReadUTF8(
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

  static void fileRename(
    final File from,
    final File to)
    throws IOException
  {
    if (from.renameTo(to) == false) {
      throw new IOException(String.format(
        "Could not rename '%s' to '%s'",
        from,
        to));
    }
  }

  static void fileWriteUTF8(
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

  static void fileWriteUTF8Atomically(
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
}
