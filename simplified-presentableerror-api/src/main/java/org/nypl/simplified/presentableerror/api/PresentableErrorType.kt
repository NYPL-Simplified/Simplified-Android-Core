package org.nypl.simplified.presentableerror.api

import net.jcip.annotations.ThreadSafe

/**
 * A presentable error.
 *
 * An error is _presentable_ if it is suitably formatted and translated such that the
 * application may display it onscreen directly. This is in contrast to, for example, raw
 * instances of [java.io.IOException] which have messages that are typically not localized and
 * often just contain a filename and no other information.
 *
 * Instances of this interface are required to be thread-safe, and encouraged to be immutable.
 */

@ThreadSafe
interface PresentableErrorType : PresentableType {

  /**
   * An exception associated with the error, if any. This is primarily useful for providing
   * a stack trace as opposed to communicating other useful information.
   */

  val exception: Throwable?
    get() = null

  /**
   * A list of causes for the error.
   */

  val causes: List<PresentableErrorType>
    get() = listOf()
}
