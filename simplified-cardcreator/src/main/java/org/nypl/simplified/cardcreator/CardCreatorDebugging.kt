package org.nypl.simplified.cardcreator

/**
 * The debugging interface to the Card Creator.
 */

object CardCreatorDebugging {

  /*
   * Set to `true` if the Card Creator should pretend that the user is in New York.
   */

  @Volatile
  var fakeNewYorkLocation: Boolean = true
}
