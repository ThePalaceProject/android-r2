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

}
