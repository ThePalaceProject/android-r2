package org.librarysimplified.r2.vanilla.internal

import android.webkit.WebView
import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.librarysimplified.r2.api.SR2BookMetadata
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Bookmark.Type.LAST_READ
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarksLoaded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionFailed
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionSucceeded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionRunningLong
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionStarted
import org.librarysimplified.r2.api.SR2Event.SR2OnCenterTapped
import org.librarysimplified.r2.api.SR2Event.SR2ReadingPositionChanged
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorChapterEnd
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent
import org.librarysimplified.r2.api.SR2Theme
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.streamer.server.Server
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.GuardedBy

/**
 * The default R2 controller implementation.
 */

internal class SR2Controller private constructor(
  private val configuration: SR2ControllerConfiguration,
  private val port: Int,
  private val server: Server,
  private val publication: Publication,
  private val epubFileName: String,
) : SR2ControllerType {

  companion object {

    private val logger =
      LoggerFactory.getLogger(SR2Controller::class.java)

    /**
     * Find a high-numbered port upon which to run the internal server. Tries up to ten
     * times to find a port and then gives up with an exception if it can't.
     */

    private fun fetchUnusedHTTPPort(): Int {
      for (i in 0 until 10) {
        try {
          val socket = ServerSocket(0)
          val port = socket.localPort
          socket.close()
          return port
        } catch (e: IOException) {
          this.logger.error("failed to open port: ", e)
        }

        try {
          Thread.sleep(1_000L)
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
        }
      }

      throw IOException("Unable to find an unused port for the server")
    }

    /**
     * Create a new controller based on the given configuration.
     */

    fun create(
      configuration: SR2ControllerConfiguration
    ): SR2ControllerType {
      val bookFile = configuration.bookFile
      this.logger.debug("creating controller for {}", bookFile)

      val publication = runBlocking {
        configuration.streamer.open(bookFile, allowUserInteraction = false)
      }.getOrElse {
        throw IOException("Failed to open EPUB", it)
      }

      if (publication.isRestricted) {
        throw IOException("Failed to unlock EPUB", publication.protectionError)
      }

      this.logger.debug("publication title: {}", publication.metadata.title)
      val port = this.fetchUnusedHTTPPort()
      this.logger.debug("server port: {}", port)

      val server = Server(port, configuration.context)
      this.logger.debug("starting server")
      server.start(5_000)

      this.logger.debug("loading epub into server")
      val epubName = "/${bookFile.name}"
      this.logger.debug("publication uri: {}", Publication.localBaseUrlOf(epubName, port))
      server.addEpub(
        publication = publication,
        container = null,
        fileName = epubName,
        userPropertiesPath = null
      )

      this.logger.debug("server ready")
      return SR2Controller(
        configuration = configuration,
        epubFileName = epubName,
        port = port,
        publication = publication,
        server = server
      )
    }
  }

  private val logger =
    LoggerFactory.getLogger(SR2Controller::class.java)

  /*
   * A single threaded command executor. The purpose of this executor is to accept
   * commands from multiple threads and ensure that the commands are executed serially.
   */

  private val queueExecutor =
    Executors.newSingleThreadExecutor { runnable ->
      val thread = Thread(runnable)
      thread.name = "org.librarysimplified.r2.vanilla.commandQueue"
      thread
    }

  private val closed = AtomicBoolean(false)

  @Volatile
  private var themeMostRecent: SR2Theme =
    this.configuration.theme

  private val eventSubject: Subject<SR2Event> =
    PublishSubject.create<SR2Event>()
      .toSerialized()

  private val webViewConnectionLock = Any()

  @GuardedBy("webViewConnectionLock")
  private var webViewConnection: SR2WebViewConnectionType? = null

  @Volatile
  private var currentChapterIndex = 0

  @Volatile
  private var currentChapterProgress = 0.0

  @Volatile
  private var currentBookProgress = 0.0

  @Volatile
  private var bookmarks = listOf<SR2Bookmark>()

  @Volatile
  private var uiVisible: Boolean = true

  init {
    this.eventSubject.subscribe { event -> this.logger.debug("event: {}", event) }
  }

  private fun locationOfSpineItem(
    index: Int
  ): String {
    require(index < this.publication.readingOrder.size) {
      "index must be in [0, ${this.publication.readingOrder.size}]; was $index"
    }

    return buildString {
      this.append(Publication.localBaseUrlOf(this@SR2Controller.epubFileName, this@SR2Controller.port))
      this.append(this@SR2Controller.publication.readingOrder[index].href)
    }
  }

  private fun setCurrentChapter(locator: SR2Locator) {
    require(locator.chapterIndex < this.publication.readingOrder.size) {
      "Chapter index ${locator.chapterIndex} must be in the range [0, ${this.publication.readingOrder.size})"
    }
    this.currentChapterIndex = locator.chapterIndex
    this.currentChapterProgress = when (locator) {
      is SR2LocatorPercent -> locator.chapterProgress
      is SR2LocatorChapterEnd -> 1.0
    }
  }

  private fun updateBookmarkLastRead(
    title: String,
    locator: SR2Locator
  ) {
    val newBookmark = SR2Bookmark(
      date = DateTime.now(),
      type = LAST_READ,
      title = title,
      locator = locator,
      bookProgress = this.currentBookProgress
    )
    val newBookmarks = this.bookmarks.toMutableList()
    newBookmarks.removeAll { bookmark -> bookmark.type == LAST_READ }
    newBookmarks.add(newBookmark)
    this.bookmarks = newBookmarks.toList()
    this.eventSubject.onNext(SR2BookmarkCreated(newBookmark))
  }

  private fun executeInternalCommand(
    command: SR2CommandSubmission
  ): ListenableFuture<*> {
    this.logger.debug("executing {}", command)

    if (this.closed.get()) {
      this.logger.debug("executor has been shut down")
      return Futures.immediateFuture(Unit)
    }

    return this.executeCommandSubmission(command)
  }

  private fun executeCommandSubmission(
    command: SR2CommandSubmission
  ): ListenableFuture<*> {
    return when (val apiCommand = command.command) {
      is SR2Command.OpenChapter ->
        this.executeCommandOpenChapter(command, apiCommand)
      SR2Command.OpenPageNext ->
        this.executeCommandOpenPageNext()
      SR2Command.OpenChapterNext ->
        this.executeCommandOpenChapterNext(command)
      SR2Command.OpenPagePrevious ->
        this.executeCommandOpenPagePrevious()
      is SR2Command.OpenChapterPrevious ->
        this.executeCommandOpenChapterPrevious(command)
      is SR2Command.BookmarksLoad ->
        this.executeCommandBookmarksLoad(apiCommand)
      SR2Command.Refresh ->
        this.executeCommandRefresh(command)
      SR2Command.BookmarkCreate ->
        this.executeCommandBookmarkCreate()
      is SR2Command.BookmarkDelete ->
        this.executeCommandBookmarkDelete(apiCommand)
      is SR2Command.ThemeSet ->
        this.executeCommandThemeSet(command, apiCommand)
    }
  }

  /**
   * Execute the [SR2Command.ThemeSet] command.
   */

  private fun executeCommandThemeSet(
    command: SR2CommandSubmission,
    apiCommand: SR2Command.ThemeSet
  ): ListenableFuture<*> {
    this.publishCommmandRunningLong(command)
    return this.executeThemeSet(waitForWebViewAvailability(), apiCommand.theme)
  }

  private fun executeThemeSet(
    viewConnection: SR2WebViewConnectionType,
    theme: SR2Theme
  ): ListenableFuture<*> {
    this.themeMostRecent = theme

    val f0 =
      viewConnection.executeJS { js -> js.setFontFamily(SR2Fonts.fontFamilyStringOf(theme.font)) }
    val f1 =
      viewConnection.executeJS { js -> js.setTheme(SR2ReadiumInternalTheme.from(theme.colorScheme)) }
    val f2 =
      viewConnection.executeJS { js -> js.setTypeScale(theme.textSize) }

    val allFutures = Futures.allAsList(f0, f1, f2)
    val setFuture = SettableFuture.create<Unit>()
    allFutures.addListener(
      {
        this.eventSubject.onNext(SR2Event.SR2ThemeChanged(theme))
        setFuture.set(Unit)
      },
      MoreExecutors.directExecutor()
    )
    return setFuture
  }

  private fun executeCommandThemeRefresh(): ListenableFuture<*> {
    return this.executeThemeSet(waitForWebViewAvailability(), this.themeMostRecent)
  }

  /**
   * Execute the [SR2Command.BookmarkDelete] command.
   */

  private fun executeCommandBookmarkDelete(
    apiCommand: SR2Command.BookmarkDelete
  ): ListenableFuture<*> {
    val newBookmarks = this.bookmarks.toMutableList()
    val removed = newBookmarks.remove(apiCommand.bookmark)
    if (removed) {
      this.bookmarks = newBookmarks.toList()
      this.eventSubject.onNext(SR2BookmarkDeleted(apiCommand.bookmark))
    }
    return Futures.immediateFuture(Unit)
  }

  /**
   * Execute the [SR2Command.BookmarkCreate] command.
   */

  private fun executeCommandBookmarkCreate(): ListenableFuture<*> {
    val bookmark =
      SR2Bookmark(
        date = DateTime.now(),
        type = SR2Bookmark.Type.EXPLICIT,
        title = SR2Books.makeChapterTitleOf(this.publication, this.currentChapterIndex),
        locator = SR2LocatorPercent(this.currentChapterIndex, this.currentChapterProgress),
        bookProgress = this.currentBookProgress
      )

    val newBookmarks = this.bookmarks.toMutableList()
    newBookmarks.add(bookmark)
    this.bookmarks = newBookmarks.toList()
    this.eventSubject.onNext(SR2BookmarkCreated(bookmark))
    return Futures.immediateFuture(Unit)
  }

  /**
   * Execute the [SR2Command.Refresh] command.
   */

  private fun executeCommandRefresh(
    command: SR2CommandSubmission
  ): ListenableFuture<*> {
    val openFuture =
      this.openChapterIndex(
        command, SR2LocatorPercent(this.currentChapterIndex, this.currentChapterProgress)
      )

    /*
     * If there was previously a theme set, then refresh the theme.
     */

    return Futures.transformAsync(
      openFuture,
      AsyncFunction { this.executeCommandThemeRefresh() },
      MoreExecutors.directExecutor()
    )
  }

  /**
   * Execute the [SR2Command.BookmarksLoad] command.
   */

  private fun executeCommandBookmarksLoad(
    apiCommand: SR2Command.BookmarksLoad
  ): ListenableFuture<*> {
    val newBookmarks = this.bookmarks.toMutableList()
    newBookmarks.addAll(apiCommand.bookmarks)
    this.bookmarks = newBookmarks.toList()
    this.eventSubject.onNext(SR2BookmarksLoaded)
    return Futures.immediateFuture(Unit)
  }

  /**
   * Execute the [SR2Command.OpenChapterPrevious] command.
   */

  private fun executeCommandOpenChapterPrevious(
    command: SR2CommandSubmission
  ): ListenableFuture<*> {
    val prevIndex =
      SR2Chapters.previousChapter(this.currentChapterIndex, this.bookMetadata.readingOrder)
        ?: return Futures.immediateFuture(Unit)

    return this.openChapterIndex(
      command,
      SR2LocatorChapterEnd(chapterIndex = prevIndex)
    )
  }

  /**
   * Execute the [SR2Command.OpenChapterNext] command.
   */

  private fun executeCommandOpenChapterNext(
    command: SR2CommandSubmission
  ): ListenableFuture<*> {
    val nextIndex =
      SR2Chapters.nextChapter(this.currentChapterIndex, this.bookMetadata.readingOrder)
        ?: return Futures.immediateFuture(Unit)

    return this.openChapterIndex(
      command,
      SR2LocatorPercent(
        chapterIndex = nextIndex,
        chapterProgress = 0.0
      )
    )
  }

  /**
   * Execute the [SR2Command.OpenPagePrevious] command.
   */

  private fun executeCommandOpenPagePrevious(): ListenableFuture<*> {
    return this.waitForWebViewAvailability().executeJS(SR2JavascriptAPIType::openPagePrevious)
  }

  /**
   * Execute the [SR2Command.OpenPageNext] command.
   */

  private fun executeCommandOpenPageNext(): ListenableFuture<*> {
    return this.waitForWebViewAvailability().executeJS(SR2JavascriptAPIType::openPageNext)
  }

  /**
   * Execute the [SR2Command.OpenChapter] command.
   */

  private fun executeCommandOpenChapter(
    command: SR2CommandSubmission,
    apiCommand: SR2Command.OpenChapter
  ): ListenableFuture<*> {
    return this.openChapterIndex(command, apiCommand.locator)
  }

  /**
   * Load the chapter for the given locator, and set the reading position appropriately.
   */

  private fun openChapterIndex(
    command: SR2CommandSubmission,
    locator: SR2Locator
  ): ListenableFuture<*> {
    val previousLocator =
      SR2LocatorPercent(this.currentChapterIndex, this.currentChapterProgress)

    try {
      this.publishCommmandRunningLong(command)

      val location = this.locationOfSpineItem(locator.chapterIndex)
      this.logger.debug("openChapterIndex: {}", location)
      this.setCurrentChapter(locator)

      val connection =
        this.waitForWebViewAvailability()
      val openFuture =
        connection.openURL(location)

      val themeFuture =
        Futures.transformAsync(
          openFuture,
          AsyncFunction { this.executeThemeSet(connection, this.themeMostRecent) },
          MoreExecutors.directExecutor()
        )

      val moveFuture =
        Futures.transformAsync(
          themeFuture,
          AsyncFunction { this.executeLocatorSet(connection, locator) },
          MoreExecutors.directExecutor()
        )

      return moveFuture
    } catch (e: Exception) {
      this.logger.error("unable to open chapter ${locator.chapterIndex}: ", e)
      this.setCurrentChapter(previousLocator)
      this.eventSubject.onNext(
        SR2Event.SR2Error.SR2ChapterNonexistent(
          chapterIndex = locator.chapterIndex,
          message = e.message ?: "Unable to open chapter ${locator.chapterIndex}"
        )
      )
      val future = SettableFuture.create<Unit>()
      future.setException(e)
      return future
    }
  }

  private fun executeLocatorSet(
    connection: SR2WebViewConnectionType,
    locator: SR2Locator
  ): ListenableFuture<*> =
    when (locator) {
      is SR2LocatorPercent ->
        connection.executeJS { js -> js.setProgression(locator.chapterProgress) }
      is SR2LocatorChapterEnd ->
        connection.executeJS { js -> js.openPageLast() }
    }

  private fun getBookProgress(chapterProgress: Double): Double {
    require(chapterProgress < 1 || chapterProgress > 0) {
      "progress must be in [0, 1]; was $chapterProgress"
    }

    val chapterCount = this.publication.readingOrder.size
    val currentIndex = this.currentChapterIndex
    val result = ((currentIndex + 1 * chapterProgress) / chapterCount)
    this.logger.debug("$result = ($currentIndex + 1 * $chapterProgress) / $chapterCount")
    return result
  }

  /**
   * A receiver that accepts calls from the Javascript code running inside the current
   * WebView.
   */

  private inner class JavascriptAPIReceiver(
    private val webView: WebView
  ) : SR2JavascriptAPIReceiverType {

    private val logger =
      LoggerFactory.getLogger(JavascriptAPIReceiver::class.java)

    @android.webkit.JavascriptInterface
    override fun onReadingPositionChanged(
      currentPage: Int,
      pageCount: Int
    ) {
      val chapterIndex =
        this@SR2Controller.currentChapterIndex

      val pageZeroBased =
        Math.max(0, currentPage - 1)
      val chapterProgress =
        pageZeroBased.toDouble() / pageCount.toDouble()
      val chapterTitle =
        SR2Books.makeChapterTitleOf(this@SR2Controller.publication, chapterIndex)

      this@SR2Controller.currentBookProgress =
        this@SR2Controller.getBookProgress(chapterProgress)
      this@SR2Controller.currentChapterProgress =
        chapterProgress

      this.logger.debug(
        """onReadingPositionChanged: chapterIndex=$chapterIndex, currentPage=$currentPage, pageCount=$pageCount, chapterProgress=$chapterProgress"""
      )

      /*
       * This is pure paranoia; we only update the last-read location if the new position
       * doesn't appear to point to the very start of the book. This is to defend against
       * any future bugs that might cause a "reading position change" event to be published
       * before the user's _real_ last-read position has been restored using a command or
       * bookmark. If this happened, we'd accidentally overwrite the user's reading position with
       * a pointer to the start of the book, so this check prevents that.
       */

      if (chapterIndex != 0 || chapterProgress > 0.000_001) {
        this@SR2Controller.queueExecutor.execute {
          this@SR2Controller.updateBookmarkLastRead(
            title = chapterTitle,
            locator = SR2LocatorPercent(
              chapterIndex = chapterIndex,
              chapterProgress = chapterProgress
            )
          )
        }
      }

      this@SR2Controller.eventSubject.onNext(
        SR2ReadingPositionChanged(
          chapterIndex = chapterIndex,
          chapterTitle = chapterTitle,
          chapterProgress = chapterProgress,
          currentPage = currentPage,
          pageCount = pageCount,
          bookProgress = this@SR2Controller.currentBookProgress
        )
      )
    }

    @android.webkit.JavascriptInterface
    override fun onCenterTapped() {
      this.logger.debug("onCenterTapped")
      this@SR2Controller.uiVisible = !this@SR2Controller.uiVisible
      this@SR2Controller.eventSubject.onNext(SR2OnCenterTapped(this@SR2Controller.uiVisible))
    }

    @android.webkit.JavascriptInterface
    override fun onClicked() {
      this.logger.debug("onClicked")
    }

    @android.webkit.JavascriptInterface
    override fun onLeftTapped() {
      this.logger.debug("onLeftTapped")
      this@SR2Controller.submitCommand(SR2Command.OpenPagePrevious)
    }

    @android.webkit.JavascriptInterface
    override fun onRightTapped() {
      this.logger.debug("onRightTapped")
      this@SR2Controller.submitCommand(SR2Command.OpenPageNext)
    }

    @android.webkit.JavascriptInterface
    override fun getViewportWidth(): Double {
      return this.webView.width.toDouble()
    }
  }

  private fun submitCommandActual(
    command: SR2CommandSubmission
  ) {
    this.logger.debug("submitCommand: {}", command)

    this.queueExecutor.execute {
      this.publishCommmandStart(command)
      val future = this.executeInternalCommand(command)
      try {
        try {
          future.get()
          this.publishCommmandSucceeded(command)
        } catch (e: ExecutionException) {
          throw e.cause!!
        }
      } catch (e: SR2WebViewDisconnectedException) {
        this.logger.debug("webview disconnected: could not execute {}", command)
        this.eventSubject.onNext(SR2Event.SR2Error.SR2WebViewInaccessible("No web view is connected"))
        this.publishCommmandFailed(command, e)
      } catch (e: Exception) {
        this.logger.error("{}: ", command, e)
        this.publishCommmandFailed(command, e)
      }
    }
  }

  /**
   * Publish an event to indicate that the current command is taking a long time to execute.
   */

  private fun publishCommmandRunningLong(command: SR2CommandSubmission) {
    this.eventSubject.onNext(SR2CommandExecutionRunningLong(command.command))
  }

  private fun publishCommmandSucceeded(command: SR2CommandSubmission) {
    this.eventSubject.onNext(SR2CommandExecutionSucceeded(command.command))
  }

  private fun publishCommmandFailed(
    command: SR2CommandSubmission,
    exception: Exception
  ) {
    this.eventSubject.onNext(SR2CommandExecutionFailed(command.command, exception))
  }

  private fun publishCommmandStart(command: SR2CommandSubmission) {
    this.eventSubject.onNext(SR2CommandExecutionStarted(command.command))
  }

  override val bookMetadata: SR2BookMetadata =
    SR2Books.makeMetadata(this.publication)

  override val events: Observable<SR2Event> =
    this.eventSubject

  override fun submitCommand(command: SR2Command) =
    this.submitCommandActual(SR2CommandSubmission(command = command))

  override fun bookmarksNow(): List<SR2Bookmark> =
    this.bookmarks

  override fun positionNow(): SR2Locator {
    return SR2LocatorPercent(
      this.currentChapterIndex,
      this.currentChapterProgress
    )
  }

  override fun themeNow(): SR2Theme {
    return this.themeMostRecent
  }

  override fun uiVisibleNow(): Boolean {
    return this.uiVisible
  }

  override fun viewConnect(webView: WebView) {
    this.logger.debug("viewConnect")

    val newConnection =
      SR2WebViewConnection.create(
        webView = webView,
        jsReceiver = this.JavascriptAPIReceiver(webView),
        commandQueue = this,
        uiExecutor = this.configuration.uiExecutor
      )

    synchronized(this.webViewConnectionLock) {
      this.webViewConnection = newConnection
    }
  }

  override fun viewDisconnect() {
    this.logger.debug("viewDisconnect")

    synchronized(this.webViewConnectionLock) {
      this.webViewConnection?.close()
      this.webViewConnection = null
    }
  }

  /**
   * Busy-wait for a web view connection.
   */

  private fun waitForWebViewAvailability(): SR2WebViewConnectionType {
    while (true) {
      synchronized(this.webViewConnectionLock) {
        val webView = this.webViewConnection
        if (webView != null) {
          return webView
        }
      }

      try {
        Thread.sleep(5_00L)
      } catch (e: InterruptedException) {
        throw SR2WebViewDisconnectedException("No web view connection is available")
      }
    }
  }

  override fun close() {
    if (this.closed.compareAndSet(false, true)) {
      try {
        this.viewDisconnect()
      } catch (e: Exception) {
        this.logger.error("could not disconnect view: ", e)
      }

      try {
        this.server.closeAllConnections()
      } catch (e: Exception) {
        this.logger.error("could not close connections: ", e)
      }

      try {
        this.server.stop()
      } catch (e: Exception) {
        this.logger.error("could not stop server: ", e)
      }

      try {
        this.publication.close()
      } catch (e: Exception) {
        this.logger.error("could not close publication: ", e)
      }

      try {
        this.queueExecutor.shutdown()
      } catch (e: Exception) {
        this.logger.error("could not stop command queue: ", e)
      }

      try {
        this.eventSubject.onComplete()
      } catch (e: Exception) {
        this.logger.error("could not complete event stream: ", e)
      }
    }
  }
}