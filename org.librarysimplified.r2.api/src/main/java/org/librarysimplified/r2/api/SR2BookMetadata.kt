package org.librarysimplified.r2.api

/**
 * Information about the currently loaded book.
 */

data class SR2BookMetadata(
  /**
   * The unique identifier of the book.
   */
  val id: String,
  /**
   * The book title.
   */
  val title: String,
  /**
   * The table of contents of the book.
   */
  val tableOfContents: List<SR2TOCEntry>,
  /**
   * The list of print page entries from the EPUB's page-list navigation.
   * Each entry maps a print page label to a DOM anchor location in the content.
   *
   * This list is empty for titles that contain no print page list.
   *
   * @see SR2PrintPageEntry
   */
  val printPages: List<SR2PrintPageEntry> = emptyList(),
  /**
   * A locator pointing at the first chapter.
   */
  val start: SR2Locator,
)
