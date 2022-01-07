package org.nypl.simplified.cardcreator

import android.location.Location

/**
 * The debugging interface to the Card Creator.
 */

object CardCreatorDebugging {

  private val newYorkCityLocation: Location =
    Location("").apply {
      latitude = 40.6454199
      longitude = -73.9537622
    }

  private val albanyLocation: Location =
    Location("").apply {
      latitude = 42.6680631
      longitude = -73.8807209
    }

  /*
 * Set to `newYorkCityLocation` or `albanyLocation` to pretend the user is in New York City or Albany.
 */

  val fakeLocation: Location? = null
}
