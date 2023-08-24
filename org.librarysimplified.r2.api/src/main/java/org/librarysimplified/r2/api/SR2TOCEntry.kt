package org.librarysimplified.r2.api

/**
 * A flattened entry from the table of contents.
 */

data class SR2TOCEntry(
  val title: String,
  val href: String,
  val depth: Int,
) {
  init {
    check(depth >= 0) {
      "Depth $depth must be non-negative"
    }
  }
}
