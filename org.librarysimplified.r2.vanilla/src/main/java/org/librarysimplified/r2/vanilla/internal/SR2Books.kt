package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2BookChapter
import org.librarysimplified.r2.api.SR2BookMetadata
import org.readium.r2.shared.publication.Publication

internal object SR2Books {

  fun makeMetadata(
    publication: Publication,
    bookId: String
  ): SR2BookMetadata {
    return SR2BookMetadata(
      id = bookId,
      title = publication.metadata.title,
      readingOrder = this.makeReadingOrder(publication)
    )
  }

  private fun makeReadingOrder(publication: Publication) =
    publication.readingOrder.mapIndexed { index, _ -> this.makeChapter(publication, index) }

  private fun makeChapter(
    publication: Publication,
    index: Int
  ): SR2BookChapter {
    val chapter =
      publication.readingOrder[index]
    val title =
      publication.tableOfContents.firstOrNull { it.href == chapter.href }?.title ?: ""
    val href =
      chapter.href

    return SR2BookChapter(
      chapterIndex = index,
      chapterHref = href,
      title = title
    )
  }
}
