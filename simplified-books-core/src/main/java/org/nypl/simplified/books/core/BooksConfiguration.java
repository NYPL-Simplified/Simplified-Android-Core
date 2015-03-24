package org.nypl.simplified.books.core;

import java.io.File;
import java.net.URI;

import com.io7m.jnull.NullCheck;

@SuppressWarnings("synthetic-access") public final class BooksConfiguration
{
  private static final class Builder implements BooksConfigurationBuilderType
  {
    private File directory;
    private URI  loans_uri;

    Builder(
      final File in_directory)
    {
      this.directory = NullCheck.notNull(in_directory);
      this.loans_uri =
        NullCheck.notNull(URI
          .create("http://circulation.alpha.librarysimplified.org/loans/"));
    }

    @Override public BooksConfiguration build()
    {
      return new BooksConfiguration(this.directory, this.loans_uri);
    }

    @Override public void setDirectory(
      final File in_directory)
    {
      this.directory = NullCheck.notNull(in_directory);
    }

    @Override public void setLoansURI(
      final URI uri)
    {
      this.loans_uri = NullCheck.notNull(uri);
    }
  }

  public static BooksConfigurationBuilderType newBuilder(
    final File in_directory)
  {
    return new Builder(in_directory);
  }

  private final URI  auth_uri;
  private final File directory;

  private BooksConfiguration(
    final File in_directory,
    final URI in_auth_uri)
  {
    this.directory = NullCheck.notNull(in_directory);
    this.auth_uri = NullCheck.notNull(in_auth_uri);
  }

  public File getDirectory()
  {
    return this.directory;
  }

  public URI getLoansURI()
  {
    return this.auth_uri;
  }
}
