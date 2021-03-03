package org.librarysimplified.r2.views

import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2Theme
import org.readium.r2.shared.publication.asset.PublicationAsset
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
   * The publication asset containing the book to be opened.
   */

  val bookFile: PublicationAsset,

  /**
   * The initial theme used for the reader.
   */

  val theme: SR2Theme,

  /**
   * A provider of controllers.
   */

  val controllers: SR2ControllerProviderType
)
