package org.librarysimplified.r2.demo

import java.io.File

/**
 * A mindlessly simple navigation controller.
 */

interface DemoNavigationControllerType {

  /**
   * Open the reader.
   */

  fun openReader(file: File)

  /**
   * Pop whatever is onscreen now.
   */

  fun popBackStack()

}
