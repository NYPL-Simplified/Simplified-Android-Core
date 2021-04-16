package org.nypl.simplified.tests

import org.junit.jupiter.api.Assertions

object ExtraAssertions {

  fun assertInstanceOf(
    x: Any,
    c: Class<*>
  ) {
    if (!x.javaClass.isAssignableFrom(c)) {
      Assertions.fail<Any>(
        String.format("Value %s (%s) must be an instance of %s", x, x.javaClass, c)
      )
    }
  }
}
