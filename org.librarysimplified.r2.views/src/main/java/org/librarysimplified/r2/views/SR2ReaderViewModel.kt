package org.librarysimplified.r2.views

import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.views.internal.SR2ViewModelBookEvent
import org.librarysimplified.r2.views.internal.SR2ViewModelBookEvent.SR2ViewModelBookOpened
import org.librarysimplified.r2.views.internal.SR2ViewModelBookEvent.SR2ViewModelBookOpenFailed
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

  /*
   * An event subject that can receive events from any thread, and an event subscription that
   * publishes those events on the UI thread to a single unicast observable.
   */

  private val viewEventsSubject: Subject<SR2ReaderViewEvent> =
    PublishSubject.create<SR2ReaderViewEvent>()
      .toSerialized()
  private val viewEventsUnicast: UnicastWorkSubject<SR2ReaderViewEvent> =
    UnicastWorkSubject.create()
  private val viewEventSubscription =
    this.viewEventsSubject.observeOn(AndroidSchedulers.mainThread())
      .subscribe(this.viewEventsUnicast::onNext)

  /*
   * An event subject that can receive events from any thread, and an event subscription that
   * publishes those events on the UI thread to a single unicast observable.
   */

  private val bookEventsSubject: Subject<SR2ViewModelBookEvent> =
    PublishSubject.create<SR2ViewModelBookEvent>()
      .toSerialized()
  private val bookEventsUnicast: UnicastWorkSubject<SR2ViewModelBookEvent> =
    UnicastWorkSubject.create()
  private val bookEventSubscription =
    this.bookEventsSubject.observeOn(AndroidSchedulers.mainThread())
      .subscribe(this.bookEventsUnicast::onNext)

  private val controllerEventsUnicast: UnicastWorkSubject<SR2Event> =
    UnicastWorkSubject.create()

  private val subscriptions =
    CompositeDisposable()

  init {
    this.logger.debug("{}: initialized", this)
  }

  override fun toString(): String =
    "[SR2ReaderViewModel 0x${Integer.toHexString(this.hashCode())}]"

  /**
   * Events published by the views. This event stream is always observed upon the
   * Android UI thread.
   *
   * Note: This observable may only have a single observer at any given time, and is expected
   * to be used only by the activity hosting the reader fragment.
   */

  val viewEvents: Observable<SR2ReaderViewEvent> =
    this.viewEventsUnicast

  /**
   * Events published by the controller. This event stream is always observed upon the
   * Android UI thread.
   *
   * Note: This observable may only have a single observer at any given time, and is expected
   * to be used only by the reader fragment.
   */

  val controllerEvents: Observable<SR2Event> =
    this.controllerEventsUnicast

  /**
   * Events published by the controller's lifecycle. This event stream is always observed upon the
   * Android UI thread.
   *
   * Note: This observable may only have a single observer at any given time, and is expected
   * to be used only by the reader fragment.
   */

  val bookEvents: Observable<SR2ViewModelBookEvent> =
    this.bookEventsUnicast

  /**
   * Publish a view event.
   */

  fun publishViewEvent(event: SR2ReaderViewEvent) =
    this.viewEventsSubject.onNext(event)

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
      this.subscriptions.clear()
      this.viewEventSubscription.dispose()
      this.bookEventSubscription.dispose()
      this.controller?.close()
      this.controller = null
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
        val controllerReference =
          SR2ControllerReference(controller = existing, isFirstStartup = false)
        this.bookEventsSubject.onNext(SR2ViewModelBookOpened(controllerReference))
        return Futures.immediateFuture(controllerReference)
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
            this.subscriptions.clear()
            this.subscriptions.add(
              newController.events.observeOn(AndroidSchedulers.mainThread())
                .subscribe(this.controllerEventsUnicast::onNext)
            )
          }

          val controllerReference =
            SR2ControllerReference(controller = newController, isFirstStartup = true)

          this.bookEventsSubject.onNext(SR2ViewModelBookOpened(controllerReference))
          refFuture.set(controllerReference)
        } catch (e: Throwable) {
          this.logger.error("unable to create controller: ", e)
          this.bookEventsSubject.onNext(SR2ViewModelBookOpenFailed(e))
          refFuture.setException(e)
        }
      },
      MoreExecutors.directExecutor()
    )

    return refFuture
  }
}
