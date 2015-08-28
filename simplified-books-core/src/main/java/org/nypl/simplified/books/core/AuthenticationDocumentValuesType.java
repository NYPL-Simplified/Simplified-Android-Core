package org.nypl.simplified.books.core;

/**
 * The type of default values for authentication documents.
 */

public interface AuthenticationDocumentValuesType
{
  /**
   * @return The label defined for the user ID field of the login form
   */

  String getLabelLoginUserID();

  /**
   * @return The label defined for the password field of the login form
   */

  String getLabelLoginPassword();
}
