package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2BookMetadata
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.api.SR2TOCEntry
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

object SR2Books {

  fun makeMetadata(
    publication: Publication,
    bookId: String
  ): SR2BookMetadata {
    val startLocator =
      SR2Locator.SR2LocatorPercent(
        chapterHref = publication.readingOrder.first().href,
        chapterProgress = 0.0
      )

    return SR2BookMetadata(
      id = bookId,
      title = publication.metadata.title,
      tableOfContents = this.makeTableOfContents(publication),
      start = startLocator
    )
  }

  /**
   * Generates a list of SR2TOCEntry based on publication's tableOfContents.
   * If tableOfContents is empty will try to use readingOrder instead.
   */

  private fun makeTableOfContents(
    publication: Publication
  ): List<SR2TOCEntry> {
    val toc = this.flattenTOC(publication.tableOfContents)
    return if (toc.isNotEmpty()) {
      toc
    } else {
      this.flattenTOC(publication.readingOrder)
    }
  }

  private fun flattenTOC(
    tableOfContents: List<Link>
  ): List<SR2TOCEntry> {
    val results = mutableListOf<SR2TOCEntry>()
    tableOfContents.forEach { node -> this.flattenTOCNode(node, 0, results) }
    return results.toList()
  }

  private fun flattenTOCNode(
    link: Link,
    depth: Int,
    results: MutableList<SR2TOCEntry>
  ) {
    results.add(SR2TOCEntry(link.title.orEmpty(), link.href, depth))
    for (child in link.children) {
      this.flattenTOCNode(child, depth + 1, results)
    }
  }
}
