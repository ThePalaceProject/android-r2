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
   * A locator pointing at the first chapter.
   */

  val start: SR2Locator
)
