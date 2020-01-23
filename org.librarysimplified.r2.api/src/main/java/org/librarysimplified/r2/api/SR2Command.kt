package org.librarysimplified.r2.api

/**
 * Commands that may be executed by an R2 controller.
 *
 * @see [SR2ControllerType]
 */

sealed class SR2Command {

  /**
   * Open the chapter with the given index.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  data class OpenChapter(
    val chapterIndex: Int
  ) : SR2Command()

  /**
   * Open the next page. If the current chapter does not contain any more pages, then
   * implementations are required to behave as if the [OpenChapterNext] command had been
   * submitted instead.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  object OpenPageNext : SR2Command()

  /**
   * Open the next chapter, moving to the first page in that chapter.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  object OpenChapterNext : SR2Command()

  /**
   * Open the previous page. If the current reading position is the start of the chapter, then
   * implementations are required to behave as if the [OpenChapterPrevious] command had been
   * submitted instead.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  object OpenPagePrevious : SR2Command()

  /**
   * Open the previous chapter. If [atEnd] is `true`, then seek to the last page in the
   * chapter.
   *
   * @see [SR2Event.SR2Error.SR2ChapterNonexistent]
   */

  data class OpenChapterPrevious(
    val atEnd: Boolean
  ) : SR2Command()

}
