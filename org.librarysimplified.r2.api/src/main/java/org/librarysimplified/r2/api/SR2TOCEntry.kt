package org.librarysimplified.r2.api

/**
 * A flattened entry from the table of contents.
 */

data class SR2TOCEntry(
  val node: SR2NavigationNode.SR2NavigationTOCNode,
  val depth: Int
)
