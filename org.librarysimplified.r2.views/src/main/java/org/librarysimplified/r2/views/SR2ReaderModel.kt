package org.librarysimplified.r2.views

import android.app.Application
import androidx.annotation.UiThread
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandSearchResults
import org.librarysimplified.r2.api.SR2Executors
import org.librarysimplified.r2.api.SR2PageNumberingMode
import org.librarysimplified.r2.api.SR2ScrollingMode
import org.librarysimplified.r2.api.SR2Theme
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookLoadingFailed
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameAvailable
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameUnavailable
import org.librarysimplified.r2.views.search.SR2SearchPagingSource
import org.librarysimplified.r2.views.search.SR2SearchPagingSourceListener
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.protection.ContentProtection
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.SearchTry
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

object SR2ReaderModel {

  private val logger =
    LoggerFactory.getLogger(SR2ReaderModel::class.java)

  var isPreview: Boolean =
    false

  var searchTerm: String =
    ""

  @Volatile
  var scrollMode: SR2ScrollingMode =
    SR2ScrollingMode.SCROLLING_MODE_PAGINATED

  @Volatile
  var perChapterNumbering: SR2PageNumberingMode =
    SR2PageNumberingMode.WHOLE_BOOK

  private val viewCommandSource =
    PublishSubject.create<SR2ReaderViewCommand>()
      .toSerialized()

  private val viewEventSource =
    BehaviorSubject.create<SR2ReaderViewEvent>()
      .toSerialized()

  private val viewEventIds =
    AtomicInteger(0)

  private val controllerEventSource =
    PublishSubject.create<SR2Event>()
      .toSerialized()

  val viewCommands: Observable<SR2ReaderViewCommand> =
    this.viewCommandSource.observeOn(AndroidSchedulers.mainThread())

  val viewEvents: Observable<SR2ReaderViewEvent> =
    this.viewEventSource.observeOn(AndroidSchedulers.mainThread())

  val controllerEvents: Observable<SR2Event> =
    this.controllerEventSource.observeOn(AndroidSchedulers.mainThread())

  @OptIn(ExperimentalReadiumApi::class)
  private var searchIterator: SearchIterator? = null

  private val mutableSearchLocators =
    MutableStateFlow<List<Locator>>(emptyList())

  @OptIn(ExperimentalReadiumApi::class)
  private val pagingSourceFactory = InvalidatingPagingSourceFactory {
    SR2SearchPagingSource(object : SR2SearchPagingSourceListener {
      override suspend fun getIteratorNext(): SearchTry<LocatorCollection?> {
        val iterator = this@SR2ReaderModel.searchIterator ?: return Try.success(null)
        return iterator.next().onSuccess {
          this@SR2ReaderModel.mutableSearchLocators.value += (it?.locators.orEmpty())
        }
      }
    })
  }

  val searchLocators: StateFlow<List<Locator>> =
    this.mutableSearchLocators

  val searchResult: Flow<PagingData<Locator>> =
    Pager(PagingConfig(pageSize = 20), pagingSourceFactory = this.pagingSourceFactory).flow

  @UiThread
  fun submitViewCommand(command: SR2ReaderViewCommand) {
    SR2UIThread.checkIsUIThread()
    this.viewCommandSource.onNext(command)
  }

  private var controllerField: SR2ControllerType? = null

  fun controllerCreate(
    context: Application,
    contentProtections: List<ContentProtection>,
    bookFile: Asset,
    bookId: String,
    theme: SR2Theme,
    controllers: SR2ControllerProviderType,
    bookmarks: List<SR2Bookmark>,
  ): CompletableFuture<SR2ControllerType> {
    val existingController = this.controllerField
    this.closeAndPublishUnavailability(existingController)
    this.controllerField = null

    val future = CompletableFuture<SR2ControllerType>()
    SR2Executors.ioExecutor.execute {
      try {
        future.complete(
          controllers.create(
            context,
            SR2ControllerConfiguration(
              bookFile = bookFile,
              bookId = bookId,
              theme = theme,
              context = context,
              contentProtections = contentProtections,
              uiExecutor = SR2UIThread::runOnUIThread,
              scrollingMode = this.scrollMode,
              pageNumberingMode = this.perChapterNumbering,
              initialBookmarks = bookmarks,
            ),
          ),
        )
      } catch (e: Throwable) {
        future.completeExceptionally(e)
      }
    }

    future.whenComplete { newController, exception ->
      this.logger.debug("Completed controller opening...")

      if (exception != null) {
        this.logger.error("Failed to open controller: ", exception)
        this.viewEventSource.onNext(
          SR2BookLoadingFailed(this.viewEventIds.getAndIncrement(), exception),
        )
        return@whenComplete
      }

      check(newController != null)

      this.controllerField = newController
      this.viewEventSource.onNext(
        SR2ControllerBecameAvailable(this.viewEventIds.getAndIncrement(), newController),
      )
      newController.events.subscribe(this.controllerEventSource::onNext)
    }
    return future
  }

  private fun closeAndPublishUnavailability(
    existingController: SR2ControllerType?,
  ) {
    if (existingController != null) {
      this.viewEventSource.onNext(
        SR2ControllerBecameUnavailable(this.viewEventIds.getAndIncrement(), existingController),
      )
      existingController.close()
    }
  }

  @OptIn(ExperimentalReadiumApi::class)
  fun consumeSearchResults(event: SR2CommandSearchResults) {
    this.searchIterator = event.searchIterator
    this.pagingSourceFactory.invalidate()
  }

  fun isBookmarkHere(): Boolean {
    return this.controllerField?.isBookmarkHere() ?: false
  }

  fun bookmarkToggle() {
    this.controllerField?.bookmarkToggle()
  }

  fun viewDisconnect() {
    this.controllerField?.viewDisconnect()
  }

  fun submitCommand(command: SR2Command) {
    this.controllerField?.submitCommand(command)
  }

  fun theme(): SR2Theme {
    return this.controllerField?.themeNow() ?: SR2Theme()
  }

  fun bookmarks(): List<SR2Bookmark> {
    return this.controllerField?.bookmarksNow() ?: listOf()
  }
}
