package org.nypl.simplified.books.core;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeTraverser;
import com.google.common.io.Files;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class BooksDirectory
{
  private final File directory;
  private final File file_credentials;
  private final File file_credentials_tmp;

  public BooksDirectory(
    final File in_directory)
  {
    this.directory = NullCheck.notNull(in_directory);
    this.file_credentials = new File(this.directory, "credentials.txt");
    this.file_credentials_tmp =
      new File(this.directory, "credentials.txt.tmp");
  }

  public void create()
    throws IOException
  {
    FileUtilities.createDirectory(this.directory);
  }

  public boolean credentialsExist()
  {
    return this.file_credentials.isFile();
  }

  public Pair<AccountBarcode, AccountPIN> credentialsGet()
    throws IOException
  {
    final String text = FileUtilities.fileReadUTF8(this.file_credentials);
    final String[] segments = text.split(":");
    final AccountBarcode b =
      new AccountBarcode(NullCheck.notNull(segments[0]));
    final AccountPIN p = new AccountPIN(NullCheck.notNull(segments[1]));
    return Pair.pair(b, p);
  }

  public void credentialsSet(
    final AccountBarcode barcode,
    final AccountPIN pin)
    throws IOException
  {
    NullCheck.notNull(barcode);
    NullCheck.notNull(pin);

    final String text =
      NullCheck.notNull(String.format("%s:%s", barcode, pin));
    FileUtilities.fileWriteUTF8Atomically(
      this.file_credentials,
      this.file_credentials_tmp,
      text);
  }

  public void destroy()
    throws IOException
  {
    if (this.directory.isDirectory()) {
      final TreeTraverser<File> trav = Files.fileTreeTraverser();
      final ImmutableList<File> list =
        trav.postOrderTraversal(this.directory).toList();

      for (int index = 0; index < list.size(); ++index) {
        final File file = list.get(index);
        final boolean ok = file.delete();
        if (ok == false) {
          throw new IOException("Unable to delete: " + file);
        }
      }
    } else {
      throw new IllegalStateException("Not logged in");
    }
  }

  public BookDirectory getBookDirectory(
    final BookID book_id)
  {
    return new BookDirectory(this.directory, NullCheck.notNull(book_id));
  }

  public List<BookDirectory> getBooks()
  {
    final List<BookDirectory> xs = new ArrayList<BookDirectory>();

    if (this.directory.isDirectory()) {
      final File[] book_list = this.directory.listFiles(new FileFilter() {
        @Override public boolean accept(
          final @Nullable File path)
        {
          assert path != null;
          return path.isDirectory();
        }
      });

      for (final File f : book_list) {
        final BookID id = BookID.exactString(NullCheck.notNull(f.getName()));
        xs.add(new BookDirectory(this.directory, id));
      }
    }

    return xs;
  }

  public File getDirectory()
  {
    return this.directory;
  }
}
