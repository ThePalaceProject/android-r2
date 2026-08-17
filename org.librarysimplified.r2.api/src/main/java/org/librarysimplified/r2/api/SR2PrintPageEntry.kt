package org.librarysimplified.r2.api

import org.readium.r2.shared.publication.Href

/**
 * A single entry from an EPUB publication's print page list.
 *
 * Page list entries come from the EPUB's nav element (page-list) and map
 * print page labels to DOM anchor locations in the content. The label is the human-readable
 * page identifier (e.g., "42", "iii", "A-5"); the href points to the content document
 * and fragment ID where the page break occurs.
 */

data class SR2PrintPageEntry(
  val label: String,
  val href: Href,
)
