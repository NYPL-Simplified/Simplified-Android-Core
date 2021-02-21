package org.librarysimplified.r2.views

import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.streamer.Streamer

/**
 * The parameters required to open an SR2 fragment.
 */

data class SR2ReaderFragmentParameters(

  /**
   * A Readium Streamer to open the book.
   */

  val streamer: Streamer,

  /**
   * The file containing the book to be opened.
   */

  val bookFile: FileAsset

)
