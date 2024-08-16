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
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
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

    private val LEADING_SLASHES =
      Pattern.compile("^/+")

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

  private val errorAttributes =
    ConcurrentHashMap<String, String>()
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
    this.publication.metadata.identifier?.let { x -> this.errorAttributes.put("Book ID", x) }
    this.publication.metadata.title?.let { x -> this.errorAttributes.put("Book Title", x) }

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
    this.logger.debug("{} Executing {}", this.name(), command)

    if (this.closed.get()) {
      this.logger.debug("{} Executor has been shut down", this.name())
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
    this.logger.debug("{} Navigation: Page Previous", this.name())
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
    this.logger.debug("{} Navigation: Page Next", this.name())
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
      if (apiCommand.link.startsWith(PREFIX_PUBLICATION)) {
        val target = Href(link.removePrefix(PREFIX_PUBLICATION))!!
        this.submitCommand(SR2Command.OpenChapter(SR2LocatorPercent(target, 0.0)))
        return CompletableFuture.completedFuture(Unit)
      }

      this.publishEvent(SR2ExternalLinkSelected(apiCommand.link))
      return CompletableFuture.completedFuture(Unit)
    } catch (e: Exception) {
      this.logger.error("{} executeCommandOpenLink: {}: ", this.name(), apiCommand.link, e)
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
      this.logger.debug(
        "{} Navigation: Move to {}",
        this.name(),
        this.currentNavigationIntent,
      )
      this.publishCommandRunningLong(command)

      val connection =
        this.waitForWebViewAvailability()
      val chapterURL =
        this.currentNavigationIntent.chapterHref.toString()

      /*
       * If we pass a URL with a fragment into the web view, this will result in
       * a URL with a fragment going into the underlying DRM engine, and this causes
       * Adobe DRM to go insane and fail to decrypt.
       *
       * Additionally, leading slashes are removed as this can cause an "absolute" URL
       * to be resolved against the absolute URL [PREFIX_PUBLICATION]. This tends to result
       * in URLs missing the "publication" path element.
       */

      val chapterURLWithoutFragment =
        chapterURL.substringBefore('#')
      val chapterWithoutSlashes =
        LEADING_SLASHES.matcher(chapterURLWithoutFragment)
          .replaceFirst("")
      val resolvedURL =
        URI(PREFIX_PUBLICATION).resolve(chapterWithoutSlashes)

      this.logger.debug("Translated {} to {}", chapterURL, resolvedURL)

      val future =
        connection.openURL(resolvedURL.toString())
          .handle { _, exception ->
            if (exception != null) {
              this.logger.debug("{} Failed to completely open URL: ", this.name(), exception)
            }
          }.thenCompose {
            connection.executeJS { js -> js.setScrollMode(this.configuration.scrollingMode) }
          }.handle { _, exception ->
            if (exception != null) {
              this.logger.debug("{} Failed to set scroll mode: ", this.name(), exception)
            }
          }.thenCompose {
            this.executeLocatorSet(connection, this.currentNavigationIntent)
          }.handle { _, exception ->
            if (exception != null) {
              this.logger.debug(
                "{} Failed to scroll to navigation intent: ",
                this.name(),
                exception,
              )
            }
          }

      /*
       * If there's a fragment, attempt to scroll to it.
       */

      when (val fragment = chapterURL.substringAfter('#', "")) {
        "" -> future

        else ->
          future.thenCompose {
            connection.executeJS { js -> js.scrollToId(fragment) }
          }.handle { _, exception ->
            if (exception != null) {
              this.logger.debug("{} Failed to scroll to fragment ID: ", this.name(), exception)
            }
          }
      }
    } catch (e: Exception) {
      this.logger.error(
        "{} moveToSatisfyNavigationIntent: {}: ",
        this.name(),
        this.currentNavigationIntent.chapterHref,
        e,
      )
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
  ): CompletableFuture<*> {
    this.logger.debug("{} executeLocatorSet: {}", this.name(), locator)

    return when (locator) {
      is SR2LocatorPercent -> {
        connection.executeJS { js -> js.setProgression(locator.chapterProgress) }
      }

      is SR2LocatorChapterEnd ->
        connection.executeJS { js -> js.openPageLast() }
    }
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
    this.logger.debug(
      "{} BookProgress: $result = ($currentIndex + 1 * $chapterProgress) / $chapterCount",
      this.name(),
    )
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

    @android.webkit.JavascriptInterface
    override fun onReadingPositionChanged(
      chapterProgress: Double,
      currentPage: Int,
      pageCount: Int,
    ) {
      val controller = this@SR2Controller
      controller.logger.debug(
        "{} onReadingPositionChanged: {} {} {}",
        controller.name(),
        chapterProgress,
        currentPage,
        pageCount,
      )

      /*
       * If the controller is indicating that the user explicitly performed some kind of
       * navigation action, then this reading position update should be used to update the
       * navigation intent. Typically, this _only_ applies for page turns.
       */

      if (controller.updateNavigationIntentOnNextChapterProgressUpdate.compareAndSet(true, false)) {
        controller.logger.debug(
          "{} Navigation: Updating intent from reading position change.",
          controller.name(),
        )
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
          controller.logger.warn(
            "{} onReadingPositionChanged: currentTarget -> null",
            controller.name(),
          )
        }
      }
    }

    @android.webkit.JavascriptInterface
    override fun onCenterTapped() {
      this@SR2Controller.logger.debug(
        "{} onCenterTapped",
        this@SR2Controller.name(),
      )
      this@SR2Controller.uiVisible = !this@SR2Controller.uiVisible
      this@SR2Controller.publishEvent(SR2OnCenterTapped(this@SR2Controller.uiVisible))
    }

    @android.webkit.JavascriptInterface
    override fun onClicked() {
      this@SR2Controller.logger.debug(
        "{} onClicked",
        this@SR2Controller.name(),
      )
    }

    @android.webkit.JavascriptInterface
    override fun onLeftTapped() {
      this@SR2Controller.logger.debug(
        "{} onLeftTapped",
        this@SR2Controller.name(),
      )

      return when (this@SR2Controller.publication.metadata.presentation.layout) {
        FIXED ->
          this@SR2Controller.submitCommand(SR2Command.OpenChapterPrevious(atEnd = true))

        REFLOWABLE, null ->
          this@SR2Controller.submitCommand(SR2Command.OpenPagePrevious)
      }
    }

    @android.webkit.JavascriptInterface
    override fun onRightTapped() {
      this@SR2Controller.logger.debug(
        "{} onRightTapped",
        this@SR2Controller.name(),
      )

      return when (this@SR2Controller.publication.metadata.presentation.layout) {
        FIXED ->
          this@SR2Controller.submitCommand(SR2Command.OpenChapterNext)

        REFLOWABLE, null ->
          this@SR2Controller.submitCommand(SR2Command.OpenPageNext)
      }
    }

    @android.webkit.JavascriptInterface
    override fun onLeftSwiped() {
      this@SR2Controller.logger.debug(
        "{} onLeftSwiped",
        this@SR2Controller.name(),
      )

      return when (this@SR2Controller.publication.metadata.presentation.layout) {
        FIXED ->
          this@SR2Controller.submitCommand(SR2Command.OpenChapterNext)

        REFLOWABLE, null ->
          this@SR2Controller.submitCommand(SR2Command.OpenPageNext)
      }
    }

    @android.webkit.JavascriptInterface
    override fun onRightSwiped() {
      this@SR2Controller.logger.debug(
        "{} onRightSwiped",
        this@SR2Controller.name(),
      )

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
      this@SR2Controller.logger.error(
        "{} logError: {}:{}: {}",
        this@SR2Controller.name(),
        file,
        line,
        message,
      )
    }
  }

  private fun setCurrentNavigationIntent(locator: SR2Locator) {
    this.logger.debug(
      "{} Navigation: Intent is now {}",
      this.name(),
      locator,
    )
    this.currentNavigationIntent = locator
  }

  private fun submitCommandActual(
    command: SR2CommandSubmission,
  ) {
    this.logger.debug(
      "{} submitCommand: {}",
      this.name(),
      command,
    )

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
          this.logger.warn("{} Webview disconnected: could not execute {}", this.name(), command)
          this.publishEvent(SR2Event.SR2Error.SR2WebViewInaccessible("No web view is connected"))
          this.publishCommandFailed(command, e)
        } catch (e: Exception) {
          this.logger.error("{}: {}: ", this.name(), command, e)
          this.publishCommandFailed(command, e)
        }
      }
    } catch (e: Exception) {
      this.logger.error("{}: {}: ", this.name(), command, e)
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
      this.logger.error("{}: Could not submit event {}: ", this.name(), event, e)
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
    this.logger.debug("{} viewConnect", this.name())

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
    this.logger.debug("{} viewDisconnect", this.name())

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
      this.logger.debug("{} close", this.name())
      this.coroutineScope.cancel()

      try {
        this.subscriptions.dispose()
      } catch (e: Exception) {
        this.logger.error("{} Could not dispose of subscriptions: ", this.name(), e)
      }

      try {
        this.viewDisconnect()
      } catch (e: Exception) {
        this.logger.error("{} Could not disconnect view: ", this.name(), e)
      }

      try {
        runBlocking {
          this@SR2Controller.publication.close()
        }
      } catch (e: Exception) {
        this.logger.error("{} Could not close publication: ", this.name(), e)
      }

      try {
        this.queueExecutor.shutdown()
      } catch (e: Exception) {
        this.logger.error("{} Could not stop command queue: ", this.name(), e)
      }

      try {
        this.eventSubject.onComplete()
      } catch (e: Exception) {
        this.logger.error("{} Could not complete event stream: ", this.name(), e)
      }
    }
  }

  internal fun openAsset(path: String): WebResourceResponse {
    this.logger.debug("{} openAsset: {}", this.name(), path)

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
    this.logger.debug("{} openPublicationResource: {}", this.name(), path)

    this.errorAttributes.put("Resource Path", path)

    val urlPath = Url(path)
    if (urlPath == null) {
      val error = SR2CustomErrorPage.create(
        this.errorAttributes.toMap(),
        "We could not decode the given publication resource path.",
      )
      this.errorAttributes.forEach { (k, v) -> MDC.put(k, v) }
      this.logger.error("Could not decode publication resource path: {}", path)
      return error.toResourceResponse()
    }

    /*
     * Actually read the resource. This can result in calls made to various DRM systems
     * for decryption, and so can fail catastrophically.
     */

    val resourceValue: Resource = try {
      val r = this.publication.get(urlPath)
      if (r == null) {
        val error = SR2CustomErrorPage.create(
          this.errorAttributes.toMap(),
          "The book appears to be missing the requested resource.",
        )
        this.errorAttributes.forEach { (k, v) -> MDC.put(k, v) }
        this.logger.error("Could not retrieve publication resource for URL: {}", urlPath)
        return error.toResourceResponse()
      }
      r
    } catch (e: Throwable) {
      val error = SR2CustomErrorPage.create(
        this.errorAttributes.toMap(),
        "There was a problem retrieving a resource from the book; the book may be damaged.",
      )
      this.errorAttributes.forEach { (k, v) -> MDC.put(k, v) }
      this.logger.error("Could not read/decrypt publication resource.", e)
      return error.toResourceResponse()
    }

    return runBlocking {
      val c = this@SR2Controller
      when (val result = c.assetRetriever.retrieve(resourceValue)) {
        is Try.Failure -> {
          c.errorAttributes.put("Message", result.value.message)
          val error = SR2CustomErrorPage.create(
            c.errorAttributes.toMap(),
            "There was a problem reading the requested resource: ${result.value.message}",
          )
          c.errorAttributes.forEach { (k, v) -> MDC.put(k, v) }
          c.logger.error(
            "{} Failed to retrieve publication resource for {}: {}",
            c.name(),
            urlPath,
            result.value.message,
          )
          MDC.remove("Message")
          c.errorAttributes.remove("Message")
          error.toResourceResponse()
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

  private fun name(): String {
    return "[SR2Controller 0x${Integer.toUnsignedString(this.hashCode(), 16)}]"
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
