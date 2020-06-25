package org.librarysimplified.r2.api

sealed class SR2Locator {

  abstract val chapterIndex: Int

  data class SR2LocatorPercent(
    override val chapterIndex: Int,
    val chapterProgress: Double
  ) : SR2Locator()

  data class SR2LocatorChapterEnd(
    override val chapterIndex: Int
  ) : SR2Locator()
}
