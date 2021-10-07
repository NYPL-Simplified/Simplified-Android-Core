package org.nypl.simplified.cardcreator.model

import org.nypl.simplified.cardcreator.utils.checkFieldNotNull

data class Data(
  val dependent: Dependent,
  val parent: Parent
) {

  /**
   * Moshi might have set some missing fields to null instead of throwing an exception,
   * so we need to manually check every field is not null to prevent NullPointerExceptions where
   * no-one would expect them.
   */

  fun validate(): Data =
    this.apply {
      checkFieldNotNull(dependent, "dependent")
      dependent.validate()
      checkFieldNotNull(parent, "parent")
      parent.validate()
    }
}
