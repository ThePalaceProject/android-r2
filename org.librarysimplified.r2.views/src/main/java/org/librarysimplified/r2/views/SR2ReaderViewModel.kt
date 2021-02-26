package org.librarysimplified.r2.views

import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * The view model shared between all SR2 fragments and the hosting activity.
 */

class SR2ReaderViewModel(
  private val parameters: SR2ReaderParameters
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(SR2ReaderViewModel::class.java)

  private val controllerLock = Any()
  private var controller: SR2ControllerType? = null

  private val eventSubject =
    PublishSubject.create<SR2ReaderViewEvent>()
      .toSerialized()

  init {
    this.logger.debug("{}: initialized", this)
  }

  override fun toString(): String =
    "[SR2ReaderViewModel 0x${Integer.toHexString(this.hashCode())}]"

  /**
   * Events published by the views.
   */

  val viewEvents: Observable<SR2ReaderViewEvent> =
    this.eventSubject

  /**
   * Publish a view event.
   */

  fun publishViewEvent(event: SR2ReaderViewEvent) =
    this.eventSubject.onNext(event)

  /**
   * An I/O executor used for executing commands on the controller.
   */

  val ioExecutor: ListeningExecutorService =
    MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(1) { runnable ->
        val thread = Thread(runnable)
        thread.name = "org.librarysimplified.r2.io"
        thread
      }
    )

  override fun onCleared() {
    super.onCleared()

    this.logger.debug("{}: onCleared", this)
    synchronized(this.controllerLock) {
      this.controller?.close()
      this.controller = null
      this.eventSubject.onComplete()
      this.ioExecutor.shutdown()
    }
  }

  fun get(): SR2ControllerType? {
    return synchronized(this.controllerLock) {
      this.controller
    }
  }

  fun createOrGet(
    configuration: SR2ControllerConfiguration
  ): ListenableFuture<SR2ControllerReference> {

    /*
     * If there's an existing controller, then return it.
     */

    synchronized(this.controllerLock) {
      val existing = this.controller
      if (existing != null) {
        return Futures.immediateFuture(
          SR2ControllerReference(
            controller = existing,
            isFirstStartup = false
          )
        )
      }
    }

    /*
     * Otherwise, asynchronously create a new controller.
     */

    val refFuture =
      SettableFuture.create<SR2ControllerReference>()
    val future =
      this.parameters.controllers.create(configuration)

    future.addListener(
      Runnable {
        try {
          val newController = future.get()
          synchronized(this.controllerLock) {
            this.controller = newController
          }
          refFuture.set(
            SR2ControllerReference(
              controller = newController,
              isFirstStartup = true
            )
          )
        } catch (e: Throwable) {
          this.logger.error("unable to create controller: ", e)
          refFuture.setException(e)
        }
      },
      MoreExecutors.directExecutor()
    )

    return refFuture
  }
}
