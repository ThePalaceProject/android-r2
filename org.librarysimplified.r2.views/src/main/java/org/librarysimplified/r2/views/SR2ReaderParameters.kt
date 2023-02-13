package org.librarysimplified.r2.views

import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2PageNumberingMode
import org.librarysimplified.r2.api.SR2ScrollingMode
import org.librarysimplified.r2.api.SR2Theme
import org.readium.r2.shared.publication.ContentProtection
import org.readium.r2.shared.publication.asset.PublicationAsset

/**
 * The parameters required to open an SR2 fragment.
 */

data class SR2ReaderParameters(

  /**
   * Content protections to provide the Readium Streamer with.
   */

  val contentProtections: List<ContentProtection>,

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
   * A flag that indicates if the reader will be open for a book preview or not in order to know
   * what options should be displayed to the user.
   */

  val isPreview: Boolean,

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

  val scrollingMode: SR2ScrollingMode,

  /**
   * The pagination numbering mode.
   */

  val pageNumberingMode: SR2PageNumberingMode
)
