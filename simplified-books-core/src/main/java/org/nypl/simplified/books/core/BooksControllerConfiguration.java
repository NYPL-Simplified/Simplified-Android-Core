package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

import java.io.File;
import java.net.URI;

/**
 * Immutable configuration data for the books database.
 */

@SuppressWarnings("synthetic-access")
public final class BooksControllerConfiguration
{
  private final URI  auth_uri;
  private final File directory;

  private BooksControllerConfiguration(
    final File in_directory,
    final URI in_auth_uri)
  {
    this.directory = NullCheck.notNull(in_directory);
    this.auth_uri = NullCheck.notNull(in_auth_uri);
  }

  /**
   * @param in_directory The base directory
   *
   * @return A new controller configuration builder
   */

  public static BooksControllerConfigurationBuilderType newBuilder(
    final File in_directory)
  {
    return new Builder(in_directory);
  }

  /**
   * @return The book database directory
   */

  public File getDirectory()
  {
    return this.directory;
  }

  /**
   * @return The URI used to retrieve the current list of loans
   */

  public URI getLoansURI()
  {
    return this.auth_uri;
  }

  private static final class Builder
    implements BooksControllerConfigurationBuilderType
  {
    private File directory;
    private URI  loans_uri;

    Builder(
      final File in_directory)
    {
      this.directory = NullCheck.notNull(in_directory);
      this.loans_uri = NullCheck.notNull(
        URI.create("http://circulation.alpha.librarysimplified.org/loans/"));
    }

    @Override public BooksControllerConfiguration build()
    {
      return new BooksControllerConfiguration(this.directory, this.loans_uri);
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
}
