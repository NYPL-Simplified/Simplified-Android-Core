package org.nypl.simplified.ui.profiles

/**
 * An SPI that allows for builds to register replacement fragments for the profile
 * creation/modification screen.
 */

interface ProfileModificationFragmentServiceType {

  /**
   * Create a new profile modification screen/fragment based on the given parameters.
   */

  fun createModificationFragment(
    parameters: ProfileModificationFragmentParameters
  ): ProfileModificationAbstractFragment
}
