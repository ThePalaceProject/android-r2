package org.librarysimplified.r2.views.internal

import org.librarysimplified.r2.views.SR2ControllerReference

/**
 * An internal set of events published by the reader's view model. These events indicate
 * that a book was opened or failed to open.
 */

sealed class SR2ViewModelBookEvent {

  data class SR2ViewModelBookOpened(
    val reference: SR2ControllerReference
  ) : SR2ViewModelBookEvent()

  data class SR2ViewModelBookOpenFailed(
    val exception: Throwable
  ) : SR2ViewModelBookEvent()
}
