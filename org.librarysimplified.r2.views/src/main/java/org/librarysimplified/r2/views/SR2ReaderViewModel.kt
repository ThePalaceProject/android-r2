package org.librarysimplified.r2.views

import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ControllerType
import org.slf4j.LoggerFactory

internal class SR2ReaderViewModel : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(SR2ReaderViewModel::class.java)

  private val controllerLock = Any()
  private var controller: SR2ControllerType? = null

  override fun onCleared() {
    super.onCleared()

    synchronized(this.controllerLock) {
      this.controller?.close()
      this.controller = null
    }
  }

  fun controllerFor(
    configuration: SR2ControllerConfiguration,
    controllers: SR2ControllerProviderType
  ): ListenableFuture<SR2ControllerType> {

    /*
     * If there's an existing controller, then return it.
     */

    synchronized(this.controllerLock) {
      val existing = this.controller
      if (existing != null) {
        return Futures.immediateFuture(existing)
      }
    }

    /*
     * Otherwise, asynchronously create a new controller.
     */

    val future =
      controllers.create(configuration)

    future.addListener(
      Runnable {
        try {
          val newController = future.get()
          synchronized(this.controllerLock) {
            this.controller = newController
          }
        } catch (e: Throwable) {
          this.logger.error("unable to create controller: ", e)
        }
      },
      MoreExecutors.directExecutor())

    return future
  }
}
