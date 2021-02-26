package org.librarysimplified.r2.views

import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2Theme
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.streamer.Streamer

/**
 * The parameters required to open an SR2 fragment.
 */

data class SR2ReaderParameters(

  /**
   * A Readium Streamer to open the book.
   */

  val streamer: Streamer,

  /**
   * The file containing the book to be opened.
   */

  val bookFile: FileAsset,

  /**
   * The initial theme used for the reader.
   */

  val theme: SR2Theme,

  /**
   * A provider of controllers.
   */

  val controllers: SR2ControllerProviderType
)
