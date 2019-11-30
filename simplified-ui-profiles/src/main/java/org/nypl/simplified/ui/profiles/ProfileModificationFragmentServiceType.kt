package org.nypl.simplified.ui.profiles

interface ProfileModificationFragmentServiceType {

  fun createModificationFragment(
    parameters: ProfileModificationFragmentParameters
  ): ProfileModificationAbstractFragment

}
