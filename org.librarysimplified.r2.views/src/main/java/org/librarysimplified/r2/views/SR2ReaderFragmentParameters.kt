package org.librarysimplified.r2.views

import org.readium.r2.streamer.Streamer
import java.io.File

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

  val bookFile: File,

  /**
   * The file containing Adobe DRM rights information.
   */

  val adobeRightsFile: File? = null

)
