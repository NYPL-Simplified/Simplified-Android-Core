package org.nypl.simplified.books.core;

import java.io.File;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;

public final class Books implements BooksType
{
  public static BooksType newBooks(
    final File in_directory)
  {
    return new Books(in_directory);
  }

  private final File directory;

  private Books(
    final File in_directory)
  {
    this.directory = NullCheck.notNull(in_directory);
  }

  @Override public BookID bookAdd(
    final OPDSAcquisitionFeedEntry e)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }
}
