package org.nypl.simplified.books.core;

import java.io.File;
import java.net.URI;

/**
 * The type of mutable builders for book database configurations.
 */

public interface BooksControllerConfigurationBuilderType
{
  /**
   * @return A configuration based on the parameters given so far.
   */

  BooksControllerConfiguration build();

  /**
   * Set the book database directory.
   */

  void setDirectory(
    final File directory);

  /**
   * Set the loans URI.
   */

  void setLoansURI(
    final URI uri);
}
