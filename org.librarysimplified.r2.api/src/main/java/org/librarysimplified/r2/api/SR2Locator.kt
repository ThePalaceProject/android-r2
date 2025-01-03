package org.librarysimplified.r2.api

import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.util.Url
import java.util.regex.Pattern

/**
 * A location within a book.
 */

sealed class SR2Locator : Comparable<SR2Locator> {

  companion object {
    private val leadingSlashes: Pattern =
      Pattern.compile("^/+")

    private fun deslash(
      href: Href,
    ): Href {
      val asText = href.toString()
      if (asText.startsWith('/')) {
        return Href(Url(leadingSlashes.matcher(asText).replaceFirst(""))!!)
      }
      return href
    }
  }

  abstract val chapterHref: Href

  data class SR2LocatorPercent private constructor(
    override val chapterHref: Href,
    val chapterProgress: Double,
  ) : SR2Locator() {

    init {
      require(this.chapterProgress in 0.0..1.0) {
        "${this.chapterProgress} must be in the range [0, 1]"
      }
      require(!this.chapterHref.toString().startsWith("/")) {
        "Chapter ${this.chapterHref} is not permitted to start with '/'"
      }
    }

    companion object {

      fun start(href: Href): SR2LocatorPercent {
        return SR2LocatorPercent(
          chapterProgress = 0.0,
          chapterHref = deslash(href),
        )
      }

      fun create(
        chapterHref: Href,
        chapterProgress: Double,
      ): SR2LocatorPercent {
        return SR2LocatorPercent(
          chapterProgress = chapterProgress,
          chapterHref = deslash(chapterHref),
        )
      }
    }

    override fun compareTo(other: SR2Locator): Int {
      val indexCmp = this.chapterHref.toString().compareTo(other.chapterHref.toString())
      return if (indexCmp == 0) {
        when (other) {
          is SR2LocatorPercent ->
            this.chapterProgress.compareTo(other.chapterProgress)

          is SR2LocatorChapterEnd ->
            this.chapterProgress.compareTo(1.0)
        }
      } else {
        indexCmp
      }
    }
  }

  data class SR2LocatorChapterEnd private constructor(
    override val chapterHref: Href,
  ) : SR2Locator() {

    init {
      require(!this.chapterHref.toString().startsWith("/")) {
        "Chapter ${this.chapterHref} is not permitted to start with '/'"
      }
    }

    companion object {

      fun create(
        chapterHref: Href,
      ): SR2LocatorChapterEnd {
        return SR2LocatorChapterEnd(
          chapterHref = deslash(chapterHref),
        )
      }
    }

    override fun compareTo(other: SR2Locator): Int {
      val indexCmp = this.chapterHref.toString().compareTo(other.chapterHref.toString())
      return if (indexCmp == 0) {
        when (other) {
          is SR2LocatorPercent ->
            1.0.compareTo(other.chapterProgress)

          is SR2LocatorChapterEnd ->
            0
        }
      } else {
        indexCmp
      }
    }
  }
}
