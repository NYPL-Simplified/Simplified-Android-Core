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
   * @return The current feed URI
   */

  URI getAdobeAuthURI();

  /**
   * Set the current URI of the root of the catalog.
   *
   * @param u The URI
   */

  void setAdobeAuthURI(URI u);

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

}
