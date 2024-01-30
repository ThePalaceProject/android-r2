package org.librarysimplified.r2.api

import android.app.Application
import java.util.concurrent.CompletableFuture

/**
 * A provider of R2 controllers.
 */

interface SR2ControllerProviderType {

  /**
   * Create a new R2 controller on a thread provided by the given I/O executor.
   */

  fun create(
    context: Application,
    configuration: SR2ControllerConfiguration,
  ): CompletableFuture<SR2ControllerType> {
    val future = CompletableFuture<SR2ControllerType>()
    configuration.ioExecutor.execute {
      try {
        future.complete(this.createHere(context, configuration))
      } catch (e: Throwable) {
        future.completeExceptionally(e)
      }
    }
    return future
  }

  /**
   * Create a new controller on the current thread.
   *
   * Note that, as most implementations will perform I/O upon initialization, this method
   * should _not_ be called on the Android UI thread.
   */

  fun createHere(
    context: Application,
    configuration: SR2ControllerConfiguration,
  ): SR2ControllerType
}
