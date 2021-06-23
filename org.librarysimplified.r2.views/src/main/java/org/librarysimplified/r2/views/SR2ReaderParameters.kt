package org.librarysimplified.r2.views

import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ScrollingMode
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
   * An identifier used to uniquely identify a publication. Unfortunately, identifier are optional
   * in EPUB files. For the sake of consistency, we require an identifier to always be provided.
   */

  val bookId: String,

  /**
   * The initial theme used for the reader.
   */

  val theme: SR2Theme,

  /**
   * A provider of controllers.
   */

  val controllers: SR2ControllerProviderType,

  /**
   * The book scrolling mode.
   */

  val scrollingMode: SR2ScrollingMode
)
