package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.api.SR2BookChapter

internal object SR2Chapters {

  fun nextChapter(
    currentIndex: Int,
    readingOrder: List<SR2BookChapter>
  ): Int? {
    if (currentIndex < 0) {
      return 0
    }
    val nextIndex = currentIndex + 1
    if (nextIndex >= readingOrder.size) {
      return null
    }
    check(nextIndex < readingOrder.size) {
      "Next index $nextIndex must be < ${readingOrder.size}"
    }
    return nextIndex
  }

  fun previousChapter(
    currentIndex: Int,
    readingOrder: List<SR2BookChapter>
  ): Int? {
    val prevIndex = currentIndex - 1
    if (prevIndex < 0) {
      return null
    }
    check(prevIndex < readingOrder.size) {
      "Next index $prevIndex must be < ${readingOrder.size}"
    }
    return prevIndex
  }
}
