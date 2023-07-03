package org.librarysimplified.r2.api

import org.joda.time.DateTime
import java.net.URI

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

  val locator: SR2Locator,

  /**
   * An estimate of the current progress through the entire book.
   */

  val bookProgress: Double?,

  /**
   * The URI of the bookmark, if any.
   */

  val uri: URI?,

  /**
   * A flag that indicates if the bookmark is being deleted or not.
   */

  var isBeingDeleted: Boolean = false
) {

  init {
    require(this.bookProgress == null || this.bookProgress in 0.0..1.0) {
      "Book progress must be in the range [0, 1]"
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SR2Bookmark

    if (date != other.date) return false
    if (type != other.type) return false
    if (title != other.title) return false
    if (locator != other.locator) return false
    if (bookProgress != other.bookProgress) return false
    if (uri != other.uri) return false

    return true
  }

  override fun hashCode(): Int {
    var result = date.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + title.hashCode()
    result = 31 * result + locator.hashCode()
    result = 31 * result + (bookProgress?.hashCode() ?: 0)
    result = 31 * result + (uri?.hashCode() ?: 0)
    return result
  }

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
