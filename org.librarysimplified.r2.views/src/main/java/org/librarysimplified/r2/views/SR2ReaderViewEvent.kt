package org.librarysimplified.r2.views

/**
 * The type of events published by the reader view.
 */

sealed class SR2ReaderViewEvent {

  sealed class SR2ReaderViewControllerEvent : SR2ReaderViewEvent() {
    data class SR2ControllerBecameAvailable(
      val reference: SR2ControllerReference,
    ) : SR2ReaderViewControllerEvent()
  }

  sealed class SR2ReaderViewBookEvent : SR2ReaderViewEvent() {
    data class SR2BookLoadingFailed(
      val exception: Throwable,
    ) : SR2ReaderViewBookEvent()
  }
}
