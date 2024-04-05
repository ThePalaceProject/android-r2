package org.librarysimplified.r2.views

import org.librarysimplified.r2.api.SR2ControllerType

/**
 * The type of events published by the reader view.
 */

sealed class SR2ReaderViewEvent {

  abstract val eventId: Int

  sealed class SR2ReaderViewControllerEvent : SR2ReaderViewEvent() {
    data class SR2ControllerBecameAvailable(
      override val eventId: Int,
      val controller: SR2ControllerType,
    ) : SR2ReaderViewControllerEvent()

    data class SR2ControllerBecameUnavailable(
      override val eventId: Int,
      val controller: SR2ControllerType,
    ) : SR2ReaderViewControllerEvent()
  }

  sealed class SR2ReaderViewBookEvent : SR2ReaderViewEvent() {
    data class SR2BookLoadingFailed(
      override val eventId: Int,
      val exception: Throwable,
    ) : SR2ReaderViewBookEvent()
  }
}
