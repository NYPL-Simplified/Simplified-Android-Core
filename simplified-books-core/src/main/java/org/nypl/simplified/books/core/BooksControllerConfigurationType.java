package org.nypl.simplified.books.core;

import java.net.URI;

/**
 * Mutable configuration data for the book controller.
 */

public interface BooksControllerConfigurationType
{
  /**
   * @return The current feed URI
   */

  URI getCurrentRootFeedURI();

  /**
   * Set the current URI of the root of the catalog.
   *
   * @param u The URI
   */

  void setCurrentRootFeedURI(URI u);

  /**
   * @return The current loans URI
   */

  URI getCurrentLoansURI();

  /**
   * Set the current loans URI.
   *
   * @param u The URI
   */

  void setCurrentLoansURI(URI u);



  /**
   * @return The alternate feed URI
   */

  URI getAlternateRootFeedURI();

  /**
   * Set the alternate URI of the root of the catalog.
   *
   * @param u The URI
   */

  void setAlternateRootFeedURI(URI u);

  /**
   * @return The alternate loans URI
   */

  URI getAlternateLoansURI();

  /**
   * Set the alternate loans URI.
   *
   * @param u The URI
   */

  void setAlternateLoansURI(URI u);


}
