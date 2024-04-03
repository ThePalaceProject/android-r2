package org.librarysimplified.r2.api

/**
 * Commands that may be executed by an R2 controller.
 *
 * @see [SR2ControllerType]
 */

sealed class SR2Command {

  /**
   * Reload whatever the controller opened last.
   */

  object Refresh : SR2Command() {
    override fun toString(): String =
      "[Refresh]"
  }

  /**
   * Open the chapter using the given locator.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  data class OpenChapter(
    val locator: SR2Locator,
  ) : SR2Command()

  /**
   * Open the next page. If the current chapter does not contain any more pages, then
   * implementations are required to behave as if the [OpenChapterNext] command had been
   * submitted instead.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  object OpenPageNext : SR2Command() {
    override fun toString(): String =
      "[OpenPageNext]"
  }

  /**
   * Open the next chapter, moving to the first page in that chapter.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  object OpenChapterNext : SR2Command() {
    override fun toString(): String =
      "[OpenChapterNext]"
  }

  /**
   * Open the previous page. If the current reading position is the start of the chapter, then
   * implementations are required to behave as if the [OpenChapterPrevious] command had been
   * submitted instead.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  object OpenPagePrevious : SR2Command() {
    override fun toString(): String =
      "[OpenPagePrevious]"
  }

  /**
   * Open the previous chapter. If [atEnd] is `true`, then seek to the last page in the
   * chapter.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  data class OpenChapterPrevious(
    val atEnd: Boolean,
  ) : SR2Command()

  /**
   * Open an arbitrary link. This will either result a translation to the [OpenChapter]
   * command, or it will result in the controller publishing events indicating either
   * an error, or that the link needs to be opened in an external web view.
   */

  data class OpenLink(
    val link: String,
  ) : SR2Command()

  /**
   * Perform a search on an EPUB to find the occurrences of a given query string.
   */

  data class Search(
    val searchQuery: String,
  ) : SR2Command()

  /**
   * Cancel a possible ongoing search.
   */

  data object CancelSearch : SR2Command()

  /**
   * Create a new (explicit) bookmark at the current reading position.
   */

  object BookmarkCreate : SR2Command() {
    override fun toString(): String =
      "[BookmarkCreate]"
  }

  /**
   * Delete a specific bookmark.
   */

  data class BookmarkDelete(
    val bookmark: SR2Bookmark,
  ) : SR2Command()

  /**
   * Set the overall theme for the reader.
   */

  data class ThemeSet(
    val theme: SR2Theme,
  ) : SR2Command()

  /**
   * Highlight or clear the highlight for the given terms.
   */

  data class HighlightTerms(
    val searchingTerms: String,
    val clearHighlight: Boolean,
  ) : SR2Command()

  /**
   * Highlight the current searching terms if a new chapter is loaded and a search has already been
   * done.
   */

  object HighlightCurrentTerms : SR2Command()
}
