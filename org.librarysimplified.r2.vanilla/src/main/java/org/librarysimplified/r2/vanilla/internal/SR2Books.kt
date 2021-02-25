package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2BookChapter
import org.librarysimplified.r2.api.SR2BookMetadata
import org.readium.r2.shared.publication.Publication

internal object SR2Books {

  fun makeMetadata(
    publication: Publication
  ): SR2BookMetadata {
    return SR2BookMetadata(
      id = publication.metadata.identifier!!, // FIXME : identifier is not mandatory in RWPM.
      readingOrder = makeReadingOrder(publication)
    )
  }

  private fun makeReadingOrder(publication: Publication) =
    publication.readingOrder.mapIndexed { index, _ -> this.makeChapter(publication, index) }

  private fun makeChapter(
    publication: Publication,
    index: Int
  ): SR2BookChapter {
    return SR2BookChapter(
      chapterIndex = index,
      title = this.makeChapterTitleOf(publication, index)
    )
  }

  /**
   * Return the title of the given chapter.
   */

  fun makeChapterTitleOf(
    publication: Publication,
    index: Int
  ): String {
    val chapter = publication.readingOrder[index]

    // The title is actually part of the table of contents; however, there may not be a
    // one-to-one mapping between chapters and table of contents entries. We do a lookup
    // based on the chapter href.
    return publication.tableOfContents.firstOrNull { it.href == chapter.href }?.title ?: ""
  }
}
