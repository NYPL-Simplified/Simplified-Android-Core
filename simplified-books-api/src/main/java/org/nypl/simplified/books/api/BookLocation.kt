package org.nypl.simplified.books.api

import java.io.Serializable

/**
 * The current page. A specific location in an EPUB is identified by a chapter index, or an
 * *idref* and a *content CFI*. In some cases, the *content CFI*
 * may not be present.
 *
 * Note: The type is [Serializable] purely because the Android API requires this
 * in order pass values of this type between activities. We make absolutely no guarantees
 * that serialized values of this class will be compatible with future releases.
 */

data class BookLocation(

  /**
   * The progress through the chapter, if progress is known.
   */

  val progress: BookChapterProgress?,

  /**
   * @return The content CFI, if any
   */

  val contentCFI: String?,

  /**
   * @return The IDRef
   */

  val idRef: String?
) : Serializable
