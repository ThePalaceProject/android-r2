package org.librarysimplified.r2.vanilla.internal

import android.app.Application
import android.webkit.WebResourceResponse
import android.webkit.WebView
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.jcip.annotations.GuardedBy
import org.joda.time.DateTime
import org.librarysimplified.r2.api.SR2BookMetadata
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Bookmark.Type.EXPLICIT
import org.librarysimplified.r2.api.SR2Bookmark.Type.LAST_READ
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionFailed
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionSucceeded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionRunningLong
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionStarted
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandSearchResults
import org.librarysimplified.r2.api.SR2Event.SR2ExternalLinkSelected
import org.librarysimplified.r2.api.SR2Event.SR2OnCenterTapped
import org.librarysimplified.r2.api.SR2Event.SR2ReadingPositionChanged
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorChapterEnd
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent
import org.librarysimplified.r2.api.SR2PageNumberingMode
import org.librarysimplified.r2.api.SR2Theme
import org.librarysimplified.r2.vanilla.BuildConfig
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout.FIXED
import org.readium.r2.shared.publication.epub.EpubLayout.REFLOWABLE
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.data.asInputStream
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.round

/**
 * The default R2 controller implementation.
 */

internal class SR2Controller private constructor(
  private val configuration: SR2ControllerConfiguration,
  private val publication: Publication,
  private val assetRetriever: AssetRetriever,

  /*
   * All navigation changes are implemented by first having the code atomically set the
   * _navigation intent_, and then running a command that satisfies this intent by either
   * opening a new chapter in the webview, scrolling the webview, or both.
   */

  @Volatile private var currentNavigationIntent: SR2Locator,
  private val navigationGraph: SR2NavigationGraph,
) : SR2ControllerType {

  companion object {

    const val PREFIX_PUBLICATION =
      "https://org.librarysimplified.r2/publication/"
    const val PREFIX_ASSETS =
      "https://org.librarysimplified.r2/assets/"

    private val logger =
      LoggerFactory.getLogger(SR2Controller::class.java)

    /**
     * Create a new controller based on the given configuration.
     */

    fun create(
      context: Application,
      configuration: SR2ControllerConfiguration,
    ): SR2ControllerType {
      val bookFile = configuration.bookFile
      this.logger.debug("Creating controller for {}", bookFile)

      val httpClient =
        DefaultHttpClient(userAgent = "${BuildConfig.LIBRARY_PACKAGE_NAME}/${BuildConfig.R2_VERSION_NAME}")
      val assetRetriever =
        AssetRetriever(context.contentResolver, httpClient)

      val publicationParser =
        DefaultPublicationParser(
          context = context,
          httpClient = httpClient,
          assetRetriever = assetRetriever,
          pdfFactory = SR2NoPDFFactory,
        )
      val publicationOpener =
        PublicationOpener(
          publicationParser = publicationParser,
          contentProtections =
          configuration.contentProtections,
          onCreatePublication = {
          },
        )

      val publication =
        runBlocking {
          publicationOpener.open(
            asset = configuration.bookFile,
            credentials = null,
            allowUserInteraction = false,
          )
        }.getOrElse {
          throw IOException("Failed to open EPUB", ErrorException(it))
        }

      if (publication.isRestricted) {
        val protectionError = publication.protectionError
        if (protectionError != null) {
          throw IOException("Failed to unlock EPUB", ErrorException(protectionError))
        } else {
          throw IOException("Failed to unlock EPUB")
        }
      }

      if (publication.readingOrder.isEmpty()) {
        throw IOException("Publication has no chapters!")
      }

      this.logger.debug("Publication title: {}", publication.metadata.title)

      /*
       * If the list of bookmarks used to open the controller contains a "last read" position,
       * set this as the initial position for the controller.
       */

      val navigationGraph =
        SR2NavigationGraphs.create(publication)

      if (configuration.initialBookmarks.isNotEmpty()) {
        configuration.initialBookmarks.forEachIndexed { index, bookmark ->
          this.logger.debug("Bookmark [{}]: {}", index, bookmark)
        }
      } else {
        this.logger.debug("No bookmarks provided.")
      }

      val lastRead =
        configuration.initialBookmarks.find { bookmark -> bookmark.type == LAST_READ }

      val navigationIntent =
        if (lastRead != null) {
          this.logger.debug("LastRead: Attempting to start from {}", lastRead)
          lastRead.locator
        } else {
          this.logger.debug("LastRead: No last-read position, starting from start of book.")
          SR2LocatorPercent(
            chapterHref = navigationGraph.start().node.navigationPoint.locator.chapterHref,
            chapterProgress = 0.0,
          )
        }

      this.logger.debug("Navigation: Intent starting at {}", navigationIntent)

      return SR2Controller(
        configuration = configuration,
        publication = publication,
        assetRetriever = assetRetriever,
        navigationGraph = navigationGraph,
        currentNavigationIntent = navigationIntent,
      )
    }
  }

  private val subscriptions =
    CompositeDisposable()
  private val logger =
    LoggerFactory.getLogger(SR2Controller::class.java)
  private val coroutineScope: CoroutineScope =
    CoroutineScope(Dispatchers.Main)

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

  /*
   * Should the navigation intent be updated on the next chapter progress update?
   *
   * The web view will provide us with an endless stream of chapter progress updates, and not all
   * of them should be used to update the [currentNavigationIntent], because often the published
   * updates will be intermediate events while the web view loads and scrolls to locations.
   * Typically we only want to be told about a reading position update after we've explicitly turned
   * to the next (or previous) page. For all of the other explicit movements such as opening
   * chapters, we don't trust the WebView to safely update the navigation intent, and we already
   * know where we should be anyway!
   */

  private val updateNavigationIntentOnNextChapterProgressUpdate =
    AtomicBoolean(false)

  @OptIn(ExperimentalReadiumApi::class)
  private var searchIterator: SearchIterator? = null

  @Volatile
  private var searchingTerms = ""

  @Volatile
  private var themeMostRecent: SR2Theme =
    this.configuration.theme

  private val eventSubject: Subject<SR2Event> =
    PublishSubject.create<SR2Event>()
      .toSerialized()

  private val webViewConnectionLock = Any()

  @GuardedBy("webViewConnectionLock")
  private var webViewConnection: SR2WebViewConnectionType? = null

  override val bookMetadata: SR2BookMetadata =
    SR2Books.makeMetadata(
      publication = this.publication,
      bookId = this.configuration.bookId,
    )

  @Volatile
  private var currentBookProgress: Double? = 0.0

  @Volatile
  private var bookmarks = this.configuration.initialBookmarks.toList()

  @Volatile
  private var uiVisible: Boolean = true

  private var lastQuery = ""

  init {
    this.subscriptions.add(
      this.eventSubject.subscribe { event -> this.logger.trace("event: {}", event) },
    )

    this.subscriptions.add(
      this.eventSubject.ofType(SR2ReadingPositionChanged::class.java)
        .distinctUntilChanged()
        .throttleLast(1, TimeUnit.SECONDS)
        .subscribe(this::updateBookmarkLastRead),
    )

    // Pre-compute positions
    runBlocking { this@SR2Controller.publication.positionsByReadingOrder() }
    this.logger.debug(
      "[0x{}] Controller: Now Open {}",
      Integer.toUnsignedString(this.hashCode(), 16),
      this,
    )
  }

  private fun updateBookmarkLastRead(
    position: SR2ReadingPositionChanged,
  ) {
    this.queueExecutor.execute {
      val newBookmark = SR2Bookmark(
        date = DateTime.now(),
        type = LAST_READ,
        title = position.chapterTitle.orEmpty(),
        locator = position.locator,
        bookProgress = this.currentBookProgress,
        uri = null,
      )

      val newBookmarks = this.bookmarks.toMutableList()
      newBookmarks.removeAll { bookmark -> bookmark.type == LAST_READ }
      newBookmarks.add(newBookmark)
      this.bookmarks = newBookmarks.distinct().toList()
      this.publishEvent(SR2BookmarkCreated(newBookmark))
    }
  }

  private fun executeInternalCommand(
    command: SR2CommandSubmission,
  ): CompletableFuture<*> {
    this.debug("executing {}", command)

    if (this.closed.get()) {
      this.debug("Executor has been shut down")
      return CompletableFuture.completedFuture(Unit)
    }

    return this.executeCommandSubmission(command)
  }

  private fun executeCommandSubmission(
    command: SR2CommandSubmission,
  ): CompletableFuture<*> {
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

      SR2Command.Refresh ->
        this.executeCommandRefresh(command)

      SR2Command.BookmarkCreate ->
        this.executeCommandBookmarkCreate()

      is SR2Command.BookmarkDelete ->
        this.executeCommandBookmarkDelete(apiCommand)

      is SR2Command.ThemeSet ->
        this.executeCommandThemeSet(command, apiCommand)

      is SR2Command.OpenLink ->
        this.executeCommandOpenLink(apiCommand)

      is SR2Command.Search ->
        this.executeCommandSearch(apiCommand)

      is SR2Command.CancelSearch ->
        this.executeCommandCancelSearch()

      is SR2Command.HighlightTerms ->
        this.executeCommandHighlightTerms(apiCommand)

      SR2Command.HighlightCurrentTerms ->
        this.executeCommandHighlightCurrentTerms()
    }
  }

  private fun executeCommandBookmarkDelete(
    apiCommand: SR2Command.BookmarkDelete,
  ): CompletableFuture<*> {
    val target = apiCommand.bookmark
    return when (target.type) {
      EXPLICIT -> {
        this.bookmarks = this.bookmarks.filter { existing -> existing != apiCommand.bookmark }
        this.publishEvent(SR2BookmarkDeleted(apiCommand.bookmark))
        CompletableFuture.completedFuture(Unit)
      }

      LAST_READ -> {
        CompletableFuture.completedFuture(Unit)
      }
    }
  }

  private fun executeCommandBookmarkCreate(): CompletableFuture<*> {
    val node =
      this.navigationGraph.findNavigationNode(this.currentNavigationIntent)
        ?: return CompletableFuture.completedFuture(Unit)

    val newBookmark =
      SR2Bookmark(
        date = DateTime.now(),
        type = SR2Bookmark.Type.EXPLICIT,
        title = node.node.title,
        locator = this.positionNow(),
        bookProgress = this.currentBookProgress,
        uri = null,
      )

    this.bookmarks = this.bookmarks.plus(newBookmark).distinct().toList()
    this.publishEvent(SR2BookmarkCreated(newBookmark))
    return CompletableFuture.completedFuture(Unit)
  }

  /**
   * Execute the [SR2Command.ThemeSet] command.
   */

  private fun executeCommandThemeSet(
    command: SR2CommandSubmission,
    apiCommand: SR2Command.ThemeSet,
  ): CompletableFuture<*> {
    this.publishCommandRunningLong(command)
    return this.executeThemeSet(this.waitForWebViewAvailability(), apiCommand.theme)
  }

  private fun executeThemeSet(
    viewConnection: SR2WebViewConnectionType,
    theme: SR2Theme,
  ): CompletableFuture<*> {
    this.themeMostRecent = theme

    val allFutures = CompletableFuture.allOf(
      viewConnection.executeJS { js -> js.setFontFamily(SR2Fonts.fontFamilyStringOf(theme.font)) },
      viewConnection.executeJS { js -> js.setTheme(SR2ReadiumInternalTheme.from(theme.colorScheme)) },
      viewConnection.executeJS { js -> js.setFontSize(theme.textSize) },
      viewConnection.executeJS { js -> js.setPublisherCSS(theme.publisherCSS) },
      viewConnection.executeJS { js -> js.broadcastReadingPosition() },
    )

    allFutures.whenComplete { _, _ ->
      this.publishEvent(SR2Event.SR2ThemeChanged(theme))
    }
    return allFutures
  }

  /**
   * Execute the [SR2Command.Refresh] command.
   */

  private fun executeCommandRefresh(
    command: SR2CommandSubmission,
  ): CompletableFuture<*> {
    return this.moveToSatisfyNavigationIntent(command)
  }

  /**
   * Execute the [SR2Command.OpenChapterPrevious] command.
   */

  private fun executeCommandOpenChapterPrevious(
    command: SR2CommandSubmission,
  ): CompletableFuture<*> {
    val currentNode =
      this.navigationGraph.findNavigationNode(this.currentNavigationIntent)
        ?: return CompletableFuture.completedFuture(Unit)

    val previousNode =
      this.navigationGraph.findPreviousNode(currentNode.node)
        ?: return CompletableFuture.completedFuture(Unit)
    val locator = SR2LocatorChapterEnd(previousNode.navigationPoint.locator.chapterHref)
    this.setCurrentNavigationIntent(locator)
    return this.moveToSatisfyNavigationIntent(command)
  }

  /**
   * Execute the [SR2Command.OpenChapterNext] command.
   */

  private fun executeCommandOpenChapterNext(
    command: SR2CommandSubmission,
  ): CompletableFuture<*> {
    val currentNode =
      this.navigationGraph.findNavigationNode(this.currentNavigationIntent)
        ?: return CompletableFuture.completedFuture(Unit)

    val nextNode =
      this.navigationGraph.findNextNode(currentNode.node)
        ?: return CompletableFuture.completedFuture(Unit)

    this.setCurrentNavigationIntent(nextNode.navigationPoint.locator)
    return this.moveToSatisfyNavigationIntent(command)
  }

  /**
   * Execute the [SR2Command.OpenPagePrevious] command.
   */

  private fun executeCommandOpenPagePrevious(): CompletableFuture<*> {
    this.updateNavigationIntentOnNextChapterProgressUpdate.set(true)
    this.debug("Navigation: Page Previous")
    return this.waitForWebViewAvailability().executeJS(SR2JavascriptAPIType::openPagePrevious)
  }

  /**
   * Execute the [SR2Command.HighlightTerms] command.
   */

  private fun executeCommandHighlightTerms(
    apiCommand: SR2Command.HighlightTerms,
  ): CompletableFuture<*> {
    if (this.searchingTerms.isBlank() && apiCommand.searchingTerms.isBlank()) {
      return CompletableFuture.completedFuture(Unit)
    }

    val clearHighlight = apiCommand.clearHighlight

    // if the searching terms are blank, it means the user wants to clear the current highlighted
    // terms, so we need to pass those same highlighted terms
    val future = if (apiCommand.searchingTerms.isBlank()) {
      val currentTerms = this.searchingTerms
      this.waitForWebViewAvailability()
        .executeJS { js -> js.highlightSearchingTerms(currentTerms, clearHighlight) }
    } else {
      this.waitForWebViewAvailability()
        .executeJS { js -> js.highlightSearchingTerms(apiCommand.searchingTerms, clearHighlight) }
    }
    this.searchingTerms = apiCommand.searchingTerms

    return future
  }

  /**
   * Execute the [SR2Command.HighlightCurrentTerms] command.
   */

  private fun executeCommandHighlightCurrentTerms(): CompletableFuture<*> {
    if (this.searchingTerms.isBlank()) {
      return CompletableFuture.completedFuture(Unit)
    }
    return this.waitForWebViewAvailability()
      .executeJS { js ->
        js.highlightSearchingTerms(
          searchingTerms = this.searchingTerms,
          clearHighlight = false,
        )
      }
  }

  /**
   * Execute the [SR2Command.OpenPageNext] command.
   */

  private fun executeCommandOpenPageNext(): CompletableFuture<*> {
    this.updateNavigationIntentOnNextChapterProgressUpdate.set(true)
    this.debug("Navigation: Page Next")
    return this.waitForWebViewAvailability().executeJS(SR2JavascriptAPIType::openPageNext)
  }

  /**
   * Execute the [SR2Command.OpenChapter] command.
   */

  private fun executeCommandOpenChapter(
    command: SR2CommandSubmission,
    apiCommand: SR2Command.OpenChapter,
  ): CompletableFuture<*> {
    this.navigationGraph.findNavigationNode(apiCommand.locator)
      ?: return CompletableFuture.completedFuture(Unit)

    this.setCurrentNavigationIntent(apiCommand.locator)
    return this.moveToSatisfyNavigationIntent(command)
  }

  /**
   * Execute the [SR2Command.OpenLink] command.
   */

  private fun executeCommandOpenLink(
    apiCommand: SR2Command.OpenLink,
  ): CompletableFuture<*> {
    try {
      /*
       * Determine if the link is an internal EPUB link. If it is, translate it to an "open chapter"
       * command. This may not be completely precise if the link contains an optional '#' fragment.
       */

      val link = apiCommand.link
      if (apiCommand.link.startsWith(org.librarysimplified.r2.vanilla.internal.SR2Controller.Companion.PREFIX_PUBLICATION)) {
        val target = Href(link.removePrefix(org.librarysimplified.r2.vanilla.internal.SR2Controller.Companion.PREFIX_PUBLICATION))!!
        this.submitCommand(SR2Command.OpenChapter(SR2LocatorPercent(target, 0.0)))
        return CompletableFuture.completedFuture(Unit)
      }

      this.publishEvent(SR2ExternalLinkSelected(apiCommand.link))
      return CompletableFuture.completedFuture(Unit)
    } catch (e: Exception) {
      this.error("Unable to open link ${apiCommand.link}: ", e)
      this.publishEvent(
        SR2Event.SR2Error.SR2ChapterNonexistent(
          chapterHref = apiCommand.link,
          message = e.message ?: "Unable to open chapter ${apiCommand.link}",
        ),
      )
      val future = CompletableFuture<Unit>()
      future.completeExceptionally(e)
      return future
    }
  }

  @OptIn(ExperimentalReadiumApi::class)
  private fun executeCommandSearch(
    command: SR2Command.Search,
  ): CompletableFuture<*> {
    val searchQuery = command.searchQuery
    if (searchQuery != this.lastQuery) {
      this.coroutineScope.launch {
        this@SR2Controller.searchIterator =
          this@SR2Controller.publication.search(searchQuery)
        this@SR2Controller.publishEvent(
          SR2CommandSearchResults(command, this@SR2Controller.searchIterator),
        )
      }
    }

    this.lastQuery = searchQuery
    return CompletableFuture.completedFuture(Unit)
  }

  @OptIn(ExperimentalReadiumApi::class)
  private fun executeCommandCancelSearch(): CompletableFuture<*> {
    this.coroutineScope.launch {
      this@SR2Controller.searchIterator?.close()
      this@SR2Controller.searchIterator = null
    }
    return CompletableFuture.completedFuture(Unit)
  }

  /**
   * Load the node for the given locator, and set the reading position appropriately.
   */

  private fun moveToSatisfyNavigationIntent(
    command: SR2CommandSubmission,
  ): CompletableFuture<*> {
    return try {
      this.debug("Navigation: Move to {}", this.currentNavigationIntent)
      this.publishCommandRunningLong(command)

      val connection =
        this.waitForWebViewAvailability()
      val chapterURL =
        this.currentNavigationIntent.chapterHref
      val resolvedURL =
        chapterURL.resolve(Url(org.librarysimplified.r2.vanilla.internal.SR2Controller.Companion.PREFIX_PUBLICATION))

      val future =
        connection.openURL(resolvedURL.toString())
          .thenCompose {
            this.executeThemeSet(connection, this.themeMostRecent)
          }
          .thenCompose {
            connection.executeJS { js -> js.setScrollMode(this.configuration.scrollingMode) }
          }
          .thenCompose {
            this.executeLocatorSet(connection, this.currentNavigationIntent)
          }

      /*
       * If there's a fragment, attempt to scroll to it.
       */

      when (val fragment = chapterURL.toString().substringAfter('#', "")) {
        "" -> future

        else ->
          future.thenCompose {
            connection.executeJS { js -> js.scrollToId(fragment) }
          }
      }
    } catch (e: Exception) {
      this.error("Unable to open chapter ${this.currentNavigationIntent.chapterHref}: ", e)
      this.publishEvent(
        SR2Event.SR2Error.SR2ChapterNonexistent(
          chapterHref = this.currentNavigationIntent.chapterHref.toString(),
          message = e.message
            ?: "Unable to open chapter ${this.currentNavigationIntent.chapterHref}",
        ),
      )
      val future = CompletableFuture<Any>()
      future.completeExceptionally(e)
      return future
    }
  }

  private fun executeLocatorSet(
    connection: SR2WebViewConnectionType,
    locator: SR2Locator,
  ): CompletableFuture<*> =
    when (locator) {
      is SR2LocatorPercent -> {
        connection.executeJS { js -> js.setProgression(locator.chapterProgress) }
      }

      is SR2LocatorChapterEnd ->
        connection.executeJS { js -> js.openPageLast() }
    }

  private fun getBookProgress(chapterProgress: Double): Double? {
    require(chapterProgress < 1 || chapterProgress > 0) {
      "Progress must be in [0, 1]; was $chapterProgress"
    }

    val currentNode =
      this.navigationGraph.findNavigationNode(this.currentNavigationIntent)
        ?: return null

    if (currentNode.node !is SR2NavigationNode.SR2NavigationReadingOrderNode) {
      return null
    }

    val currentIndex = currentNode.node.index
    val chapterCount = this.publication.readingOrder.size

    val result = ((currentIndex + 1 * chapterProgress) / chapterCount)
    this.debug("BookProgress: $result = ($currentIndex + 1 * $chapterProgress) / $chapterCount")
    return result
  }

  private suspend fun getCurrentPage(chapterProgress: Double): Pair<Int?, Int?> {
    val currentNode =
      this.navigationGraph.findNavigationNode(this.currentNavigationIntent)
        ?: return null to null

    if (currentNode.node !is SR2NavigationNode.SR2NavigationReadingOrderNode) {
      return null to null
    }

    val pageNumberingMode =
      when (this@SR2Controller.publication.metadata.presentation.layout) {
        FIXED -> SR2PageNumberingMode.WHOLE_BOOK
        else -> this.configuration.pageNumberingMode
      }

    val indexInReadingOrder = currentNode.node.index
    val positionsByChapter = this.publication.positionsByReadingOrder()
    val currentChapterPositions = positionsByChapter[indexInReadingOrder]

    val positionIndex =
      round(chapterProgress * currentChapterPositions.size).toInt()
        .coerceAtMost(currentChapterPositions.size - 1)

    return when (pageNumberingMode) {
      SR2PageNumberingMode.PER_CHAPTER -> {
        val pageNumber = positionIndex + 1
        val pageCount = currentChapterPositions.size
        pageNumber to pageCount
      }

      SR2PageNumberingMode.WHOLE_BOOK -> {
        val pageNumber = currentChapterPositions[positionIndex].locations.position!!
        val pageCount = positionsByChapter.fold(0) { current, list -> current + list.size }
        pageNumber to pageCount
      }
    }
  }

  /**
   * A receiver that accepts calls from the Javascript code running inside the current
   * WebView.
   */

  private inner class JavascriptAPIReceiver(
    private val webView: WebView,
  ) : SR2JavascriptAPIReceiverType {

    private val logger =
      LoggerFactory.getLogger(JavascriptAPIReceiver::class.java)

    @android.webkit.JavascriptInterface
    override fun onReadingPositionChanged(
      chapterProgress: Double,
      currentPage: Int,
      pageCount: Int,
    ) {
      /*
       * If the controller is indicating that the user explicitly performed some kind of
       * navigation action, then this reading position update should be used to update the
       * navigation intent. Typically, this _only_ applies for page turns.
       */

      val controller = this@SR2Controller
      if (controller.updateNavigationIntentOnNextChapterProgressUpdate.compareAndSet(true, false)) {
        controller.debug("Navigation: Updating intent from reading position change.")
        controller.setCurrentNavigationIntent(
          when (val i = controller.currentNavigationIntent) {
            is SR2LocatorChapterEnd -> {
              i
            }

            is SR2LocatorPercent -> {
              SR2LocatorPercent(
                i.chapterHref,
                chapterProgress,
              )
            }
          },
        )
      }

      controller.coroutineScope.launch {
        controller.currentBookProgress =
          controller.getBookProgress(chapterProgress)

        val currentTarget =
          controller.navigationGraph.findNavigationNode(controller.currentNavigationIntent)

        if (currentTarget != null) {
          val targetHref =
            currentTarget.node.navigationPoint.locator.chapterHref
          val targetTitle =
            currentTarget.node.title
          val (resultCurrentPage, resultPageCount) =
            controller.getCurrentPage(chapterProgress)

          controller.publishEvent(
            SR2ReadingPositionChanged(
              chapterHref = targetHref,
              chapterTitle = targetTitle,
              chapterProgress = chapterProgress,
              currentPage = resultCurrentPage,
              pageCount = resultPageCount,
              bookProgress = controller.currentBookProgress,
            ),
          )
        } else {
          this@SR2Controller.warn("onReadingPositionChanged: currentTarget -> null")
        }
      }
    }

    @android.webkit.JavascriptInterface
    override fun onCenterTapped() {
      this@SR2Controller.debug("onCenterTapped")
      this@SR2Controller.uiVisible = !this@SR2Controller.uiVisible
      this@SR2Controller.publishEvent(SR2OnCenterTapped(this@SR2Controller.uiVisible))
    }

    @android.webkit.JavascriptInterface
    override fun onClicked() {
      this@SR2Controller.debug("onClicked")
    }

    @android.webkit.JavascriptInterface
    override fun onLeftTapped() {
      this@SR2Controller.debug("onLeftTapped")

      return when (this@SR2Controller.publication.metadata.presentation.layout) {
        FIXED ->
          this@SR2Controller.submitCommand(SR2Command.OpenChapterPrevious(atEnd = true))

        REFLOWABLE, null ->
          this@SR2Controller.submitCommand(SR2Command.OpenPagePrevious)
      }
    }

    @android.webkit.JavascriptInterface
    override fun onRightTapped() {
      this.logger.debug("onRightTapped")

      return when (this@SR2Controller.publication.metadata.presentation.layout) {
        FIXED ->
          this@SR2Controller.submitCommand(SR2Command.OpenChapterNext)

        REFLOWABLE, null ->
          this@SR2Controller.submitCommand(SR2Command.OpenPageNext)
      }
    }

    @android.webkit.JavascriptInterface
    override fun onLeftSwiped() {
      this.logger.debug("onLeftSwiped")

      return when (this@SR2Controller.publication.metadata.presentation.layout) {
        FIXED ->
          this@SR2Controller.submitCommand(SR2Command.OpenChapterNext)

        REFLOWABLE, null ->
          this@SR2Controller.submitCommand(SR2Command.OpenPageNext)
      }
    }

    @android.webkit.JavascriptInterface
    override fun onRightSwiped() {
      this.logger.debug("onRightSwiped")

      return when (this@SR2Controller.publication.metadata.presentation.layout) {
        FIXED ->
          this@SR2Controller.submitCommand(SR2Command.OpenChapterPrevious(atEnd = true))

        REFLOWABLE, null ->
          this@SR2Controller.submitCommand(SR2Command.OpenPagePrevious)
      }
    }

    @android.webkit.JavascriptInterface
    override fun getViewportWidth(): Double {
      return this.webView.width.toDouble()
    }

    @android.webkit.JavascriptInterface
    override fun logError(
      message: String?,
      file: String?,
      line: String?,
    ) {
      this@SR2Controller.error("logError: {}:{}: {}", file, line, message)
    }
  }

  private fun setCurrentNavigationIntent(locator: SR2Locator) {
    this.debug("Navigation: Intent is now {}", locator)
    this.currentNavigationIntent = locator
  }

  private fun submitCommandActual(
    command: SR2CommandSubmission,
  ) {
    this.debug("submitCommand: {}", command)

    try {
      this.queueExecutor.execute {
        this.publishCommandStart(command)
        val future = this.executeInternalCommand(command)
        try {
          try {
            future.get()
            this.publishCommandSucceeded(command)
          } catch (e: ExecutionException) {
            throw e.cause!!
          }
        } catch (e: SR2WebViewDisconnectedException) {
          this.warn("Webview disconnected: could not execute {}", command)
          this.publishEvent(SR2Event.SR2Error.SR2WebViewInaccessible("No web view is connected"))
          this.publishCommandFailed(command, e)
        } catch (e: Exception) {
          this.error("{}: ", command, e)
          this.publishCommandFailed(command, e)
        }
      }
    } catch (e: Exception) {
      this.error("{}: ", command, e)
      this.publishCommandFailed(command, e)
    }
  }

  /**
   * Publish an event to indicate that the current command is taking a long time to execute.
   */

  private fun publishCommandRunningLong(command: SR2CommandSubmission) {
    this.publishEvent(SR2CommandExecutionRunningLong(command.command))
  }

  private fun publishEvent(event: SR2Event) {
    try {
      this.eventSubject.onNext(event)
    } catch (e: Exception) {
      this.warn("Could not submit event: ", e)
    }
  }

  private fun publishCommandSucceeded(command: SR2CommandSubmission) {
    this.publishEvent(SR2CommandExecutionSucceeded(command.command))
  }

  private fun publishCommandFailed(
    command: SR2CommandSubmission,
    exception: Exception,
  ) {
    this.publishEvent(SR2CommandExecutionFailed(command.command, exception))
  }

  private fun publishCommandStart(command: SR2CommandSubmission) {
    this.publishEvent(SR2CommandExecutionStarted(command.command))
  }

  override val events: Observable<SR2Event> =
    this.eventSubject

  override fun submitCommand(command: SR2Command) =
    this.submitCommandActual(SR2CommandSubmission(command = command))

  override fun bookmarksNow(): List<SR2Bookmark> =
    this.bookmarks

  override fun positionNow(): SR2Locator {
    return this.currentNavigationIntent
  }

  override fun themeNow(): SR2Theme {
    return this.themeMostRecent
  }

  override fun uiVisibleNow(): Boolean {
    return this.uiVisible
  }

  override fun isBookmarkHere(): Boolean {
    return this.findExplicitBookmarkForCurrentPosition() != null
  }

  private fun findExplicitBookmarkForCurrentPosition(): SR2Bookmark? {
    return this.bookmarks.find { bookmark ->
      bookmark.locator == this.currentNavigationIntent && bookmark.type == EXPLICIT
    }
  }

  override fun bookmarkToggle() {
    val existing = this.findExplicitBookmarkForCurrentPosition()
    if (existing != null) {
      this.submitCommand(SR2Command.BookmarkDelete(existing))
    } else {
      this.submitCommand(SR2Command.BookmarkCreate)
    }
  }

  override fun viewConnect(webView: WebView) {
    this.debug("viewConnect")

    val newConnection =
      SR2WebViewConnection.create(
        webView = webView,
        jsReceiver = this.JavascriptAPIReceiver(webView),
        commandQueue = this,
        uiExecutor = this.configuration.uiExecutor,
        scrollingMode = this.configuration.scrollingMode,
        layout = this.publication.metadata.presentation.layout ?: REFLOWABLE,
      )

    synchronized(this.webViewConnectionLock) {
      this.webViewConnection = newConnection
    }

    this.submitCommand(SR2Command.Refresh)
  }

  override fun viewDisconnect() {
    this.debug("viewDisconnect")

    synchronized(this.webViewConnectionLock) {
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
      this.debug("Closing")
      this.coroutineScope.cancel()

      try {
        this.subscriptions.dispose()
      } catch (e: Exception) {
        this.error("Could not dispose of subscriptions: ", e)
      }

      try {
        this.viewDisconnect()
      } catch (e: Exception) {
        this.error("Could not disconnect view: ", e)
      }

      try {
        runBlocking {
          this@SR2Controller.publication.close()
        }
      } catch (e: Exception) {
        this.error("Could not close publication: ", e)
      }

      try {
        this.queueExecutor.shutdown()
      } catch (e: Exception) {
        this.error("Could not stop command queue: ", e)
      }

      try {
        this.eventSubject.onComplete()
      } catch (e: Exception) {
        this.error("Could not complete event stream: ", e)
      }
    }
  }

  internal fun openAsset(path: String): WebResourceResponse {
    this.debug("openAsset: {}", path)

    val resourcePath =
      "/org/librarysimplified/r2/vanilla/readium/$path"
    val resourceURL =
      SR2Controller::class.java.getResource(resourcePath)
        ?: return WebResourceResponse(
          "text/plain",
          null,
          404,
          "not found",
          mapOf(),
          ByteArrayInputStream(ByteArray(0)),
        )

    return resourceURL.openStream().use { stream ->
      WebResourceResponse(
        this.guessMimeType(path),
        null,
        200,
        "OK",
        mapOf(),
        ByteArrayInputStream(stream.readBytes()),
      )
    }
  }

  private fun guessMimeType(path: String): String {
    val upper = path.uppercase()
    if (upper.endsWith(".CSS")) {
      return MediaType.CSS.toString()
    }
    if (upper.endsWith(".JS")) {
      return MediaType.JAVASCRIPT.toString()
    }
    if (upper.endsWith(".TTF")) {
      return MediaType.TTF.toString()
    }
    if (upper.endsWith(".OTF")) {
      return MediaType.OTF.toString()
    }
    if (upper.endsWith(".HTML")) {
      return MediaType.HTML.toString()
    }
    if (upper.endsWith(".XHTML")) {
      return MediaType.XHTML.toString()
    }
    return "application/octet-stream"
  }

  internal fun openPublicationResource(path: String): WebResourceResponse? {
    this.debug("openPublicationResource: {}", path)

    val urlPath = Url(path)
    if (urlPath == null) {
      this.logger.error("Could not decode publication resource path: {}", path)
      return null
    }

    val resourceValue = this.publication.get(urlPath)
    if (resourceValue == null) {
      this.logger.error("Could not retrieve publication resource for URL: {}", urlPath)
      return null
    }

    return runBlocking {
      val c = this@SR2Controller
      when (val result = c.assetRetriever.retrieve(resourceValue)) {
        is Try.Failure -> {
          c.error(
            "Failed to retrieve publication resource for {}: {}",
            urlPath,
            result.value.message,
          )
          WebResourceResponse(
            result.value.message,
            "text/plain",
            500,
            result.value.message,
            null,
            result.value.message.byteInputStream(),
          )
        }

        is Try.Success -> {
          val resourceInjected =
            SR2HtmlInjectingResource(
              publication = c.publication,
              mediaType = result.value.format.mediaType,
              resourceValue,
            )

          WebResourceResponse(
            result.value.format.mediaType.toString(),
            result.value.format.mediaType.charset?.name(),
            200,
            "OK",
            null,
            resourceInjected.asInputStream(),
          )
        }
      }
    }
  }

  private fun debug(
    message: String,
    vararg arguments: Any,
  ) {
    this.logger.debug(
      "[SR2Controller 0x{}] $message",
      Integer.toUnsignedString(this.hashCode(), 16),
      arguments,
    )
  }

  private fun error(
    message: String,
    vararg arguments: Any?,
  ) {
    this.logger.error(
      "[SR2Controller 0x{}] $message",
      Integer.toUnsignedString(this.hashCode(), 16),
      arguments,
    )
  }

  private fun warn(
    message: String,
    vararg arguments: Any,
  ) {
    this.logger.warn(
      "[SR2Controller 0x{}] $message",
      Integer.toUnsignedString(this.hashCode(), 16),
      arguments,
    )
  }

  override fun toString(): String {
    return "[SR2Controller 0x${
      Integer.toUnsignedString(
        this.hashCode(),
        16,
      )
    } \"${this.publication.metadata.title}\"]"
  }
}
