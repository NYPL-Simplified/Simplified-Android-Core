package org.nypl.simplified.files;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * File utility functions.
 */

public final class FileUtilities
{
  private FileUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Copy the file {@code from} to {@code to}.
   *
   * @param from The source file
   * @param to   The target file
   *
   * @throws IOException On I/O errors
   */

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

        while (true) {
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

  /**
   * Delete the file {@code f} if it exists.
   *
   * @param f The file
   *
   * @throws IOException On I/O errors
   */

  public static void fileDelete(
    final File f)
    throws IOException
  {
    NullCheck.notNull(f);
    f.delete();
    if (f.exists()) {
      throw new IOException(String.format("Could not delete '%s'", f));
    }
  }

  /**
   * Read the entire contents of the given file, assuming that it is UTF-8
   * text.
   *
   * @param file The file
   *
   * @return The contents of the file
   *
   * @throws IOException On I/O errors
   */

  public static String fileReadUTF8(
    final File file)
    throws IOException
  {
    NullCheck.notNull(file);

    final StringBuilder b = new StringBuilder((int) file.length());

    final BufferedReader in = new BufferedReader(
      new InputStreamReader(
        new FileInputStream(file), "UTF-8"));

    try {
      while (true) {
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

  /**
   * Rename the file {@code from} to {@code to}.
   *
   * @param from The source file
   * @param to   The target file
   *
   * @throws IOException On I/O errors
   */

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
          String.format(
            "Could not rename '%s' to '%s' ('%s' does not exist or is not a "
            + "file)", from, to, from));
      }

      final File to_parent = to.getParentFile();
      if (to_parent.isDirectory() == false) {
        throw new IOException(
          String.format(
            "Could not rename '%s' to '%s' ('%s' is not a directory)",
            from,
            to,
            to_parent));
      }

      throw new IOException(
        String.format(
          "Could not rename '%s' to '%s'", from, to));
    }
  }

  /**
   * Write the given string to the given file, completely replacing it if it
   * already exists.
   *
   * @param file The file
   * @param text The text
   *
   * @throws IOException On I/O errors
   */

  public static void fileWriteUTF8(
    final File file,
    final String text)
    throws IOException
  {
    NullCheck.notNull(file);
    NullCheck.notNull(text);

    final Writer out = new BufferedWriter(
      new OutputStreamWriter(
        new FileOutputStream(file), "UTF-8"));

    try {
      out.write(text);
      out.flush();
    } finally {
      out.close();
    }
  }

  /**
   * Write the given string to the given file, completely replacing it if it
   * already exists. The file {@code f_tmp} is used as a temporary file and is
   * atomically renamed to {@code f} on writing.
   *
   * @param f     The file
   * @param f_tmp The temporary intermediate file
   * @param text  The text
   *
   * @throws IOException On I/O errors
   */

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

  /**
   * Read the entire contents of the given file, returning it as a byte array.
   *
   * @param file The file
   *
   * @return The contents of the file
   *
   * @throws IOException On I/O errors
   */

  public static byte[] fileReadBytes(final File file)
    throws IOException
  {
    NullCheck.notNull(file);

    final FileInputStream fs = new FileInputStream(file);
    try {
      final ByteArrayOutputStream bao = new ByteArrayOutputStream();
      try {
        final byte[] buffer = new byte[8192];
        while (true) {
          final int r = fs.read(buffer);
          if (r == -1) {
            break;
          }
          bao.write(buffer, 0, r);
        }
        return bao.toByteArray();
      } finally {
        bao.close();
      }
    } finally {
      fs.close();
    }
  }

  /**
   * Write the given bytes to the given file.
   *
   * @param data The data
   * @param file The file
   *
   * @throws IOException On I/O errors
   */

  public static void fileWriteBytes(
    final byte[] data,
    final File file)
    throws IOException
  {
    NullCheck.notNull(data);
    NullCheck.notNull(file);

    final FileOutputStream fs = new FileOutputStream(file);
    try {
      fs.write(data);
      fs.flush();
      fs.close();
    } finally {
      fs.close();
    }
  }

  /**
   * Write {@code stream} to {@code file_tmp}, atomically renaming {@code
   * file_tmp} to {@code file} on success. For portability, {@code file_tmp} and
   * {@code file} should be in the same directory.
   *
   * @param file     The file
   * @param file_tmp The temporary file
   * @param stream   The input stream
   *
   * @throws IOException On I/O errors
   */

  public static void fileWriteStreamAtomically(
    final File file,
    final File file_tmp,
    final InputStream stream)
    throws IOException
  {
    NullCheck.notNull(file);
    NullCheck.notNull(file_tmp);
    NullCheck.notNull(stream);

    FileUtilities.fileWriteStream(file_tmp, stream);
    FileUtilities.fileRename(file_tmp, file);
  }

  /**
   * Write {@code stream} to {@code file}.
   *
   * @param file   The file
   * @param stream The input stream
   *
   * @throws IOException On I/O errors
   */

  public static void fileWriteStream(
    final File file,
    final InputStream stream)
    throws IOException
  {
    NullCheck.notNull(file);
    NullCheck.notNull(stream);

    final FileOutputStream fs = new FileOutputStream(file);
    try {
      final byte[] buffer = new byte[8192];
      while (true) {
        final int r = stream.read(buffer);
        if (r == -1) {
          break;
        }
        fs.write(buffer, 0, r);
      }
      fs.flush();
      fs.close();
    } finally {
      fs.close();
    }
  }

  /**
   * Write {@code data} to {@code file_tmp}, atomically renaming {@code
   * file_tmp} to {@code file} on success. For portability, {@code file_tmp} and
   * {@code file} should be in the same directory.
   *
   * @param file     The file
   * @param file_tmp The temporary file
   * @param data     The input data
   *
   * @throws IOException On I/O errors
   */

  public static void fileWriteBytesAtomically(
    final File file,
    final File file_tmp,
    final byte[] data)
    throws IOException
  {
    NullCheck.notNull(file);
    NullCheck.notNull(file_tmp);
    NullCheck.notNull(data);

    FileUtilities.fileWriteBytes(data, file_tmp);
    FileUtilities.fileRename(file_tmp, file);
  }
}
