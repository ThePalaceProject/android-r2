package org.librarysimplified.r2.api

/**
 * The type of events published by R2 controllers.
 */

sealed class SR2Event {

  /**
   * The subset of events that correspond to errors.
   */

  sealed class SR2Error : SR2Event() {

    /**
     * An attempt was made to open a chapter in a book that does not exist.
     */

    data class SR2ChapterNonexistent(
      val chapterIndex: Int,
      val message: String
    ) : SR2Error()

    /**
     * An attempt was made to execute a command that requires access to a [WebView],
     * but not web view is currently connected to the controller.
     */

    data class SR2WebViewInaccessible(
      val message: String
    ) : SR2Error()
  }

  /**
   * The center of the webview was tapped. Implementors may want to show reader
   * controls as a result of this event.
   */

  class SR2OnCenterTapped : SR2Event()

  /**
   * The current reading position changed.
   */

  data class SR2ReadingPositionChanged(
    val chapterIndex: Int,
    val progress: Double
  ) : SR2Event()
}
