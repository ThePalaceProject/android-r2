package org.librarysimplified.r2.views

/**
 * The type of commands from the SR2 views to the application.
 */

sealed class SR2ReaderViewCommand {

  /**
   * The user performed an action that means the current view should be closed.
   */

  data object SR2ReaderViewNavigationReaderClose : SR2ReaderViewCommand()

  /**
   * The user performed an action that means the current view should be closed.
   */

  data object SR2ReaderViewNavigationTOCClose : SR2ReaderViewCommand()

  /**
   * The user performed an action that means the TOC should be opened.
   */

  data object SR2ReaderViewNavigationTOCOpen : SR2ReaderViewCommand()

  /**
   * The user performed an action that means the search view should be opened.
   */

  data object SR2ReaderViewNavigationSearchOpen : SR2ReaderViewCommand()

  /**
   * The user performed an action that means the search view should be closed.
   */

  data object SR2ReaderViewNavigationSearchClose : SR2ReaderViewCommand()
}
