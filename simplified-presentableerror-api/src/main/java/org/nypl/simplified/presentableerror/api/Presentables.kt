package org.nypl.simplified.presentableerror.api

/**
 * Functions over presentable values.
 */

object Presentables {

  /**
   * Merge all of the attributes of `map0` into `map`, making names unique as necessary.
   */

  fun mergeAttributes(
    map0: Map<String, String>,
    map1: Map<String, String>
  ): Map<String, String> {
    val attributes = mutableMapOf<String, String>()
    attributes.putAll(map0)
    for ((key, value) in map1) {
      putRetry(attributes, key, value)
    }
    return attributes.toMap()
  }

  /**
   * Try to put the given key/value into the map, making a reasonable effort to make keys unique
   * in the presence of duplicates.
   */

  fun putRetry(
    attributes: MutableMap<String, String>,
    key: String,
    value: String
  ) {
    if (!attributes.containsKey(key)) {
      attributes[key] = value
      return
    }

    for (i in 0..100) {
      val incKey = "$key ($i)"
      if (!attributes.containsKey(incKey)) {
        attributes[incKey] = value
        return
      }
    }
  }
}
