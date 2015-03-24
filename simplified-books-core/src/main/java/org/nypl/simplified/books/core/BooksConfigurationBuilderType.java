package org.nypl.simplified.books.core;

import java.io.File;
import java.net.URI;

public interface BooksConfigurationBuilderType
{
  BooksConfiguration build();

  void setDirectory(
    final File directory);

  void setLoansURI(
    final URI uri);
}
