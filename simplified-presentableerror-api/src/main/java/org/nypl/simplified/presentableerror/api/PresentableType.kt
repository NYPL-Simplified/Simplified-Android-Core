package org.nypl.simplified.presentableerror.api

import net.jcip.annotations.ThreadSafe
import java.io.Serializable

/**
 * A presentable value.
 *
 * A value is _presentable_ if it is suitably formatted and translated such that the
 * application may display it onscreen directly. This is in contrast to, for example, raw
 * instances of [java.io.IOException] which have messages that are typically not localized and
 * often just contain a filename and no other information.
 *
 * Instances of this interface are required to be thread-safe, and encouraged to be immutable.
 *
 * Note: Instances of this interface are required to be [Serializable] in order to facilitate
 * passing values between Android fragments and activities. No guarantees are required with regards
 * to forward or backwards compatibility of serialized values.
 */

@ThreadSafe
interface PresentableType : Serializable {

  /**
   * The complete error message, fit for presentation to the screen.
   */

  val message: String

  /**
   * A list of attributes that are associated with the error. These are arbitrary values
   * that can be associated with the error and are intended to be included in saved error
   * reports to give more context when debugging.
   */

  val attributes: Map<String, String>
    get() = mapOf()
}
