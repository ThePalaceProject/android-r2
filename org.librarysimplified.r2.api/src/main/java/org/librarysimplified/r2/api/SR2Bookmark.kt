package org.librarysimplified.r2.api

import org.joda.time.DateTime

/**
 * A bookmark.
 */

data class SR2Bookmark(

  /**
   * The date and time that the bookmark was created.
   */

  val date: DateTime,

  /**
   * The type of the bookmark.
   */

  val type: Type,

  /**
   * The title of the bookmark. This is typically a chapter title in the book.
   */

  val title: String,

  /**
   * The locator for the bookmark.
   */

  val locator: SR2Locator) {

  /**
   * The type of the bookmark.
   */

  enum class Type {

    /**
     * The bookmark is an explicitly-created bookmark.
     */

    EXPLICIT,

    /**
     * The bookmark is a last-read location.
     */

    LAST_READ
  }
}
