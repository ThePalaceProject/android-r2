package org.librarysimplified.r2.vanilla.internal

import android.app.Application
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
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
import org.librarysimplified.r2.api.SR2Bookmark.Type.LAST_READ
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreate
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkFailedToBeDeleted
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkTryToDelete
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarksLoaded
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
      return SR2Controller(
        configuration = configuration,
        publication = publication,
        assetRetriever = assetRetriever,
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

  private val navigationGraph: SR2NavigationGraph =
    SR2NavigationGraphs.create(this.publication)

  @Volatile
  private var currentTarget: SR2NavigationTarget =
    this.navigationGraph.start()

  @Volatile
  private var currentTargetProgress: Double = 0.0

  @Volatile
  private var currentBookProgress: Double? = 0.0

  @Volatile
  private var bookmarks = listOf<SR2Bookmark>()

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
  }

  private fun serverLocationOfTarget(
    target: SR2NavigationTarget,
  ): Url {
    return target.node.navigationPoint.locator.chapterHref.resolve(Url(PREFIX_PUBLICATION))
  }

  private fun setCurrentNode(target: SR2NavigationTarget) {
    this.logger.debug("currentNode: {}", target.node.javaClass)
    this.currentTarget = target
    this.currentTargetProgress = when (val locator = target.node.navigationPoint.locator) {
      is SR2LocatorPercent -> locator.chapterProgress
      is SR2LocatorChapterEnd -> 1.0
    }
  }

  private fun updateBookmarkLastRead(
    position: SR2ReadingPositionChanged,
  ) {
    /*
     * This is pure paranoia; we only update the last-read location if the new position
     * doesn't appear to point to the very start of the book. This is to defend against
     * any future bugs that might cause a "reading position change" event to be published
     * before the user's _real_ last-read position has been restored using a command or
     * bookmark. If this happened, we'd accidentally overwrite the user's reading position with
     * a pointer to the start of the book, so this check prevents that.
     */

    val currentNode = this.currentTarget.node
    if (currentNode !is SR2NavigationNode.SR2NavigationReadingOrderNode ||
      currentNode.index != 0 || position.chapterProgress > 0.000_001
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

        this.publishEvent(
          SR2BookmarkCreate(
            newBookmark,
            onBookmarkCreationCompleted = { createdBookmark ->
              if (createdBookmark != null) {
                val newBookmarks = this.bookmarks.toMutableList()
                newBookmarks.removeAll { bookmark -> bookmark.type == LAST_READ }
                newBookmarks.add(createdBookmark)
                this.bookmarks = newBookmarks.distinct().toList()
                this.publishEvent(SR2BookmarkCreated(createdBookmark))
              }
            },
          ),
        )
      }
    }
  }

  private fun executeInternalCommand(
    command: SR2CommandSubmission,
  ): ListenableFuture<*> {
    this.logger.debug("executing {}", command)

    if (this.closed.get()) {
      this.logger.debug("Executor has been shut down")
      return Futures.immediateFuture(Unit)
    }

    return this.executeCommandSubmission(command)
  }

  private fun executeCommandSubmission(
    command: SR2CommandSubmission,
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

  /**
   * Execute the [SR2Command.ThemeSet] command.
   */

  private fun executeCommandThemeSet(
    command: SR2CommandSubmission,
    apiCommand: SR2Command.ThemeSet,
  ): ListenableFuture<*> {
    this.publishCommandRunningLong(command)
    return this.executeThemeSet(this.waitForWebViewAvailability(), apiCommand.theme)
  }

  private fun executeThemeSet(
    viewConnection: SR2WebViewConnectionType,
    theme: SR2Theme,
  ): ListenableFuture<*> {
    this.themeMostRecent = theme

    val tasks = mutableListOf<ListenableFuture<Any>>()
    tasks.add(
      viewConnection.executeJS { js -> js.setFontFamily(SR2Fonts.fontFamilyStringOf(theme.font)) },
    )
    tasks.add(
      viewConnection.executeJS { js -> js.setTheme(SR2ReadiumInternalTheme.from(theme.colorScheme)) },
    )
    tasks.add(
      viewConnection.executeJS { js -> js.setFontSize(theme.textSize) },
    )
    tasks.add(
      viewConnection.executeJS { js -> js.setPublisherCSS(theme.publisherCSS) },
    )
    tasks.add(
      viewConnection.executeJS { js -> js.broadcastReadingPosition() },
    )

    val allFutures = Futures.allAsList(tasks)
    val setFuture = SettableFuture.create<Unit>()
    allFutures.addListener(
      {
        this.publishEvent(SR2Event.SR2ThemeChanged(theme))
        setFuture.set(Unit)
      },
      MoreExecutors.directExecutor(),
    )
    return setFuture
  }

  /**
   * Execute the [SR2Command.BookmarkDelete] command.
   */

  private fun executeCommandBookmarkDelete(
    apiCommand: SR2Command.BookmarkDelete,
  ): ListenableFuture<*> {
    this.bookmarks = this.bookmarks.map { bookmark ->
      bookmark.copy(
        isBeingDeleted = bookmark == apiCommand.bookmark,
      )
    }.distinct().toList()
    this.publishEvent(
      SR2BookmarkTryToDelete(
        bookmark = apiCommand.bookmark,
        onDeleteOperationFinished = { wasDeleted ->
          if (wasDeleted) {
            val newBookmarks = this.bookmarks.toMutableList()
            newBookmarks.remove(apiCommand.bookmark)
            this.bookmarks = newBookmarks.distinct().toList()
            this.publishEvent(SR2BookmarkDeleted(apiCommand.bookmark))
          } else {
            this.bookmarks = this.bookmarks.map { bookmark ->
              bookmark.copy(
                isBeingDeleted = if (bookmark == apiCommand.bookmark) {
                  false
                } else {
                  bookmark.isBeingDeleted
                },
              )
            }.distinct().toList()
            this.publishEvent(SR2BookmarkFailedToBeDeleted)
          }
        },
      ),
    )
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
        title = this.currentTarget.node.title,
        locator = SR2LocatorPercent(
          this.currentTarget.node.navigationPoint.locator.chapterHref,
          this.currentTargetProgress,
        ),
        bookProgress = this.currentBookProgress,
        uri = null,
      )

    this.publishEvent(
      SR2BookmarkCreate(
        bookmark = bookmark,
        onBookmarkCreationCompleted = { createdBookmark ->
          if (createdBookmark != null) {
            val newBookmarks = this.bookmarks.toMutableList()
            newBookmarks.add(createdBookmark)
            this.bookmarks = newBookmarks.distinct().toList()
            this.publishEvent(SR2BookmarkCreated(createdBookmark))
          }
        },
      ),
    )

    return Futures.immediateFuture(Unit)
  }

  /**
   * Execute the [SR2Command.Refresh] command.
   */

  private fun executeCommandRefresh(
    command: SR2CommandSubmission,
  ): ListenableFuture<*> {
    return this.openNodeForLocator(
      command,
      SR2LocatorPercent(
        chapterHref = this.currentTarget.node.navigationPoint.locator.chapterHref,
        chapterProgress = this.currentTargetProgress,
      ),
    )
  }

  /**
   * Execute the [SR2Command.BookmarksLoad] command.
   */

  private fun executeCommandBookmarksLoad(
    apiCommand: SR2Command.BookmarksLoad,
  ): ListenableFuture<*> {
    val newBookmarks = this.bookmarks.toMutableList()
    newBookmarks.addAll(apiCommand.bookmarks)
    this.bookmarks = newBookmarks.distinct().toList()
    this.publishEvent(SR2BookmarksLoaded)
    return Futures.immediateFuture(Unit)
  }

  /**
   * Execute the [SR2Command.OpenChapterPrevious] command.
   */

  private fun executeCommandOpenChapterPrevious(
    command: SR2CommandSubmission,
  ): ListenableFuture<*> {
    val previousNode =
      this.navigationGraph.findPreviousNode(this.currentTarget.node)
        ?: return Futures.immediateFuture(Unit)

    return this.openNodeForLocator(
      command,
      SR2LocatorChapterEnd(chapterHref = previousNode.navigationPoint.locator.chapterHref),
    )
  }

  /**
   * Execute the [SR2Command.OpenChapterNext] command.
   */

  private fun executeCommandOpenChapterNext(
    command: SR2CommandSubmission,
  ): ListenableFuture<*> {
    val nextNode =
      this.navigationGraph.findNextNode(this.currentTarget.node)
        ?: return Futures.immediateFuture(Unit)

    return this.openNodeForLocator(
      command,
      SR2LocatorPercent(
        chapterHref = nextNode.navigationPoint.locator.chapterHref,
        chapterProgress = 0.0,
      ),
    )
  }

  /**
   * Execute the [SR2Command.OpenPagePrevious] command.
   */

  private fun executeCommandOpenPagePrevious(): ListenableFuture<*> {
    return this.waitForWebViewAvailability().executeJS(SR2JavascriptAPIType::openPagePrevious)
  }

  /**
   * Execute the [SR2Command.HighlightTerms] command.
   */

  private fun executeCommandHighlightTerms(
    apiCommand: SR2Command.HighlightTerms,
  ): ListenableFuture<*> {
    if (this.searchingTerms.isBlank() && apiCommand.searchingTerms.isBlank()) {
      return Futures.immediateFuture(Unit)
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

  private fun executeCommandHighlightCurrentTerms(): ListenableFuture<*> {
    if (this.searchingTerms.isBlank()) {
      return Futures.immediateFuture(Unit)
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

  private fun executeCommandOpenPageNext(): ListenableFuture<*> {
    return this.waitForWebViewAvailability().executeJS(SR2JavascriptAPIType::openPageNext)
  }

  /**
   * Execute the [SR2Command.OpenChapter] command.
   */

  private fun executeCommandOpenChapter(
    command: SR2CommandSubmission,
    apiCommand: SR2Command.OpenChapter,
  ): ListenableFuture<*> {
    return this.openNodeForLocator(command, apiCommand.locator)
  }

  /**
   * Execute the [SR2Command.OpenLink] command.
   */

  private fun executeCommandOpenLink(
    apiCommand: SR2Command.OpenLink,
  ): ListenableFuture<*> {
    try {
      /*
       * Determine if the link is an internal EPUB link. If it is, translate it to an "open chapter"
       * command. This may not be completely precise if the link contains an optional '#' fragment.
       */

      val link = apiCommand.link
      if (apiCommand.link.startsWith(PREFIX_PUBLICATION)) {
        val target = Href(link.removePrefix(PREFIX_PUBLICATION))!!
        this.submitCommand(SR2Command.OpenChapter(SR2LocatorPercent(target, 0.0)))
        return Futures.immediateFuture(Unit)
      }

      this.publishEvent(SR2ExternalLinkSelected(apiCommand.link))
      return Futures.immediateFuture(Unit)
    } catch (e: Exception) {
      this.logger.error("Unable to open link ${apiCommand.link}: ", e)
      this.publishEvent(
        SR2Event.SR2Error.SR2ChapterNonexistent(
          chapterHref = apiCommand.link,
          message = e.message ?: "Unable to open chapter ${apiCommand.link}",
        ),
      )
      val future = SettableFuture.create<Unit>()
      future.setException(e)
      return future
    }
  }

  @OptIn(ExperimentalReadiumApi::class)
  private fun executeCommandSearch(
    command: SR2Command.Search,
  ): ListenableFuture<*> {
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
    return Futures.immediateFuture(Unit)
  }

  @OptIn(ExperimentalReadiumApi::class)
  private fun executeCommandCancelSearch(): ListenableFuture<*> {
    this.coroutineScope.launch {
      this@SR2Controller.searchIterator?.close()
      this@SR2Controller.searchIterator = null
    }

    return Futures.immediateFuture(Unit)
  }

  /**
   * Load the node for the given locator, and set the reading position appropriately.
   */

  private fun openNodeForLocator(
    command: SR2CommandSubmission,
    locator: SR2Locator,
  ): ListenableFuture<*> {
    val previousNode = this.currentTarget

    return try {
      this.publishCommandRunningLong(command)

      val target =
        this.navigationGraph.findNavigationNode(locator)
          ?: throw IllegalStateException("Unable to locate a chapter for locator $locator")

      val targetLocation = this.serverLocationOfTarget(target)
      this.logger.debug("openNodeForLocator: {}", targetLocation)
      this.setCurrentNode(target)

      val connection =
        this.waitForWebViewAvailability()
      val openFuture =
        connection.openURL(targetLocation.toString())

      val themeFuture =
        Futures.transformAsync(
          openFuture,
          { this.executeThemeSet(connection, this.themeMostRecent) },
          MoreExecutors.directExecutor(),
        )

      val scrollModeFuture =
        Futures.transformAsync(
          themeFuture,
          { connection.executeJS { js -> js.setScrollMode(this.configuration.scrollingMode) } },
          MoreExecutors.directExecutor(),
        )

      val moveFuture =
        Futures.transformAsync(
          scrollModeFuture,
          { this.executeLocatorSet(connection, locator) },
          MoreExecutors.directExecutor(),
        )

      /*
       * If there's a fragment, attempt to scroll to it.
       */

      when (val fragment = locator.chapterHref.toString().substringAfter('#', "")) {
        "" ->
          moveFuture

        else ->
          Futures.transformAsync(
            moveFuture,
            { connection.executeJS { js -> js.scrollToId(fragment) } },
            MoreExecutors.directExecutor(),
          )
      }
    } catch (e: Exception) {
      this.logger.error("Unable to open chapter ${locator.chapterHref}: ", e)
      this.setCurrentNode(previousNode)
      this.publishEvent(
        SR2Event.SR2Error.SR2ChapterNonexistent(
          chapterHref = locator.chapterHref.toString(),
          message = e.message ?: "Unable to open chapter ${locator.chapterHref}",
        ),
      )
      val future = SettableFuture.create<Unit>()
      future.setException(e)
      return future
    }
  }

  private fun executeLocatorSet(
    connection: SR2WebViewConnectionType,
    locator: SR2Locator,
  ): ListenableFuture<*> =
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

    val currentNode = this.currentTarget.node
    if (currentNode !is SR2NavigationNode.SR2NavigationReadingOrderNode) {
      return null
    }

    val currentIndex = currentNode.index
    val chapterCount = this.publication.readingOrder.size

    val result = ((currentIndex + 1 * chapterProgress) / chapterCount)
    this.logger.debug("$result = ($currentIndex + 1 * $chapterProgress) / $chapterCount")
    return result
  }

  private suspend fun getCurrentPage(chapterProgress: Double): Pair<Int?, Int?> {
    val currentNode = this.currentTarget.node
    if (currentNode !is SR2NavigationNode.SR2NavigationReadingOrderNode) {
      return null to null
    }

    val pageNumberingMode =
      when (this@SR2Controller.publication.metadata.presentation.layout) {
        FIXED -> SR2PageNumberingMode.WHOLE_BOOK
        else -> this.configuration.pageNumberingMode
      }

    val indexInReadingOrder = currentNode.index
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
      this@SR2Controller.coroutineScope.launch {
        this@SR2Controller.currentBookProgress =
          this@SR2Controller.getBookProgress(chapterProgress)
        this@SR2Controller.currentTargetProgress =
          chapterProgress

        val currentTarget =
          this@SR2Controller.currentTarget
        val targetHref =
          currentTarget.node.navigationPoint.locator.chapterHref
        val targetTitle =
          currentTarget.node.title
        val (currentPage, pageCount) =
          this@SR2Controller.getCurrentPage(chapterProgress)

        this@SR2Controller.publishEvent(
          SR2ReadingPositionChanged(
            chapterHref = targetHref,
            chapterTitle = targetTitle,
            chapterProgress = chapterProgress,
            currentPage = currentPage,
            pageCount = pageCount,
            bookProgress = this@SR2Controller.currentBookProgress,
          ),
        )
      }
    }

    @android.webkit.JavascriptInterface
    override fun onCenterTapped() {
      this.logger.debug("onCenterTapped")
      this@SR2Controller.uiVisible = !this@SR2Controller.uiVisible
      this@SR2Controller.publishEvent(SR2OnCenterTapped(this@SR2Controller.uiVisible))
    }

    @android.webkit.JavascriptInterface
    override fun onClicked() {
      this.logger.debug("onClicked")
    }

    @android.webkit.JavascriptInterface
    override fun onLeftTapped() {
      this.logger.debug("onLeftTapped")

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
      this.logger.error("logError: {}:{}: {}", file, line, message)
    }
  }

  private fun submitCommandActual(
    command: SR2CommandSubmission,
  ) {
    this.logger.debug("submitCommand: {}", command)

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
          this.logger.debug("Webview disconnected: could not execute {}", command)
          this.publishEvent(SR2Event.SR2Error.SR2WebViewInaccessible("No web view is connected"))
          this.publishCommandFailed(command, e)
        } catch (e: Exception) {
          this.logger.error("{}: ", command, e)
          this.publishCommandFailed(command, e)
        }
      }
    } catch (e: Exception) {
      this.logger.error("{}: ", command, e)
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
      this.logger.warn("Could not submit event: ", e)
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
    return SR2LocatorPercent(
      this.currentTarget.node.navigationPoint.locator.chapterHref,
      this.currentTargetProgress,
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
        uiExecutor = this.configuration.uiExecutor,
        scrollingMode = this.configuration.scrollingMode,
        layout = this.publication.metadata.presentation.layout ?: REFLOWABLE,
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
      this.coroutineScope.cancel()

      try {
        this.viewDisconnect()
      } catch (e: Exception) {
        this.logger.error("Could not disconnect view: ", e)
      }

      try {
        runBlocking {
          this@SR2Controller.publication.close()
        }
      } catch (e: Exception) {
        this.logger.error("Could not close publication: ", e)
      }

      try {
        this.queueExecutor.shutdown()
      } catch (e: Exception) {
        this.logger.error("Could not stop command queue: ", e)
      }

      try {
        this.eventSubject.onComplete()
      } catch (e: Exception) {
        this.logger.error("Could not complete event stream: ", e)
      }
    }
  }

  internal fun openAsset(path: String): WebResourceResponse {
    this.logger.debug("openAsset: {}", path)

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
        guessMimeType(path),
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
    this.logger.debug("openPublicationResource: {}", path)

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
          c.logger.error(
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
}
