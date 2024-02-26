package org.librarysimplified.r2.api

import android.app.Application

/**
 * A provider of R2 controllers.
 */

interface SR2ControllerProviderType {

  /**
   * Create a new controller on the current thread.
   *
   * Note that, as most implementations will perform I/O upon initialization, this method
   * should _not_ be called on the Android UI thread.
   */

  fun create(
    context: Application,
    configuration: SR2ControllerConfiguration,
  ): SR2ControllerType
}
