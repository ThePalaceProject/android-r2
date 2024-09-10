package org.librarysimplified.r2.views

import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionFailed
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionSucceeded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionRunningLong
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionStarted
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandSearchResults
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2ChapterNonexistent
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2WebViewInaccessible
import org.librarysimplified.r2.api.SR2Event.SR2OnCenterTapped
import org.librarysimplified.r2.api.SR2Event.SR2ReadingPositionChanged
import org.librarysimplified.r2.api.SR2Event.SR2ThemeChanged
import org.librarysimplified.r2.api.SR2ScrollingMode.SCROLLING_MODE_CONTINUOUS
import org.librarysimplified.r2.api.SR2ScrollingMode.SCROLLING_MODE_PAGINATED
import org.librarysimplified.r2.api.SR2Theme
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationReaderClose
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationSearchOpen
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationTOCOpen
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookLoadingFailed
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameAvailable
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameUnavailable
import org.librarysimplified.r2.views.internal.SR2BrightnessService
import org.librarysimplified.r2.views.internal.SR2SettingsDialog
import org.slf4j.LoggerFactory

class SR2ReaderFragment : SR2Fragment() {

  private val logger =
    LoggerFactory.getLogger(SR2ReaderFragment::class.java)

  private lateinit var container: ViewGroup
  private lateinit var loadingView: ProgressBar
  private lateinit var menuBookmarkItem: MenuItem
  private lateinit var positionPageView: TextView
  private lateinit var positionPercentView: TextView
  private lateinit var positionTitleView: TextView
  private lateinit var progressContainer: ViewGroup
  private lateinit var progressView: ProgressBar
  private lateinit var titleText: TextView
  private lateinit var toolbar: Toolbar
  private lateinit var webView: WebView
  private lateinit var eventSubscriptions: CompositeDisposable

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val view =
      inflater.inflate(R.layout.sr2_reader, container, false)

    this.container =
      view.findViewById(R.id.readerContainer)
    this.toolbar =
      view.findViewById(R.id.readerToolbar)
    this.progressContainer =
      view.findViewById(R.id.readerProgressContainer)
    this.webView =
      view.findViewById(R.id.readerWebView)
    this.progressView =
      view.findViewById(R.id.reader2_progress)
    this.positionPageView =
      view.findViewById(R.id.reader2_position_page)
    this.positionTitleView =
      view.findViewById(R.id.reader2_position_title)
    this.positionPercentView =
      view.findViewById(R.id.reader2_position_percent)
    this.loadingView =
      view.findViewById(R.id.readerLoading)
    this.titleText =
      view.findViewById(R.id.titleText)

    this.toolbar.inflateMenu(R.menu.sr2_reader_menu)
    this.menuBookmarkItem = this.toolbar.menu.findItem(R.id.readerMenuAddBookmark)
    this.toolbar.menu.findItem(R.id.readerMenuSettings)
      .setOnMenuItemClickListener { this.onReaderMenuSettingsSelected() }
    this.toolbar.menu.findItem(R.id.readerMenuTOC)
      .setOnMenuItemClickListener { this.onReaderMenuTOCSelected() }

    val addBookmarkOption = this.toolbar.menu.findItem(R.id.readerMenuAddBookmark)
    addBookmarkOption.setOnMenuItemClickListener { this.onReaderMenuAddBookmarkSelected() }
    addBookmarkOption.isVisible = !SR2ReaderModel.isPreview

    val searchOption = this.toolbar.menu.findItem(R.id.readerMenuSearch)
    searchOption.setOnMenuItemClickListener {
      SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationSearchOpen)
      true
    }

    this.toolbar.setNavigationOnClickListener { this.onToolbarNavigationSelected() }
    this.toolbar.setNavigationContentDescription(R.string.settingsAccessibilityBack)

    /*
     * We don't show page numbers in continuous scroll mode.
     */

    when (SR2ReaderModel.scrollMode) {
      SCROLLING_MODE_PAGINATED -> {
        // The defaults are fine
      }

      SCROLLING_MODE_CONTINUOUS -> {
        this.positionPageView.visibility = View.INVISIBLE
      }
    }

    this.viewsHandleLoadingState(showLoading = true)
    return view
  }

  private fun onReaderMenuAddBookmarkSelected(): Boolean {
    SR2UIThread.checkIsUIThread()

    SR2ReaderModel.bookmarkToggle()
    return true
  }

  private fun onReaderMenuTOCSelected(): Boolean {
    SR2UIThread.checkIsUIThread()

    SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationTOCOpen)
    return true
  }

  private fun onReaderMenuSettingsSelected(): Boolean {
    SR2UIThread.checkIsUIThread()

    this.openSettings()
    return true
  }

  private fun onToolbarNavigationSelected(): Boolean {
    SR2UIThread.checkIsUIThread()

    SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationReaderClose)
    return true
  }

  private fun configureForTheme(theme: SR2Theme) {
    SR2UIThread.checkIsUIThread()

    val background = theme.colorScheme.background()
    val foreground = theme.colorScheme.foreground()

    this.toolbar.setBackgroundColor(background)
    this.toolbar.setTitleTextColor(foreground)
    this.toolbar.navigationIcon?.setColorFilter(foreground, PorterDuff.Mode.SRC_ATOP)
    this.toolbar.menu.forEach { item ->
      item.icon?.setColorFilter(foreground, PorterDuff.Mode.SRC_ATOP)
    }
    this.container.setBackgroundColor(background)
    this.titleText.setTextColor(foreground)
    this.positionPageView.setTextColor(foreground)
    this.positionTitleView.setTextColor(foreground)
    this.positionPercentView.setTextColor(foreground)
  }

  private fun openSettings() {
    val activity = requireActivity()
    SR2SettingsDialog.create(
      brightness = SR2BrightnessService(activity),
      context = activity,
    )
  }

  override fun onStart() {
    this.logger.debug("onStart")
    super.onStart()

    this.eventSubscriptions = CompositeDisposable()
    this.eventSubscriptions.add(SR2ReaderModel.viewEvents.subscribe(this::onViewEvent))
    this.eventSubscriptions.add(SR2ReaderModel.controllerEvents.subscribe(this::onControllerEvent))
  }

  private fun onViewEvent(
    event: SR2ReaderViewEvent,
  ) {
    this.logger.debug("onViewEvent: {}", event)

    when (event) {
      is SR2BookLoadingFailed -> {
        // Nothing to do here.
      }

      is SR2ControllerBecameAvailable -> {
        this.toolbar.title = event.controller.bookMetadata.title
        this.titleText.text = event.controller.bookMetadata.title
        this.configureForTheme(event.controller.themeNow())
        this.showOrHideReadingUI(true)
        event.controller.viewConnect(this.webView)
      }

      is SR2ControllerBecameUnavailable -> {
        // Nothing to do here.
      }
    }
  }

  override fun onStop() {
    this.logger.debug("onStop")

    try {
      this.eventSubscriptions.dispose()
    } catch (e: Throwable) {
      this.logger.error("Error closing subscriptions: ", e)
    }

    try {
      SR2ReaderModel.viewDisconnect()
    } catch (e: Throwable) {
      // Nothing we can do about this.
    }

    super.onStop()
  }

  private fun onReadingPositionChanged(event: SR2ReadingPositionChanged) {
    val context = this.context ?: return
    this.logger.debug("chapterTitle=${event.chapterTitle}")
    if (event.chapterTitle == null) {
      this.positionTitleView.visibility = View.GONE
    } else {
      this.positionTitleView.text = event.chapterTitle
      this.positionTitleView.visibility = View.VISIBLE
    }

    if (event.currentPage == null || event.pageCount == null) {
      this.positionPageView.visibility = View.GONE
    } else {
      this.positionPageView.text =
        context.getString(R.string.progress_page, event.currentPage, event.pageCount)
      this.positionPageView.visibility = View.VISIBLE
    }

    val bookProgressPercent = event.bookProgressPercent
    if (bookProgressPercent == null) {
      this.positionPercentView.visibility = View.GONE
      this.progressView.visibility = View.GONE
    } else {
      this.positionPercentView.text = this.getString(R.string.progress_percent, bookProgressPercent)
      this.progressView.apply {
        this.max = 100
        this.progress = bookProgressPercent
      }
      this.positionPercentView.visibility = View.VISIBLE
      this.progressView.visibility = View.VISIBLE
    }
    this.reconfigureBookmarkMenuItem()
  }

  private fun reconfigureBookmarkMenuItem() {
    SR2UIThread.checkIsUIThread()

    val currentColorFilter = this.menuBookmarkItem.icon?.colorFilter
    if (SR2ReaderModel.isBookmarkHere()) {
      this.menuBookmarkItem.setIcon(R.drawable.sr2_bookmark_active)
      MenuItemCompat.setContentDescription(
        this.menuBookmarkItem,
        this.resources.getString(R.string.readerAccessDeleteBookmark),
      )
    } else {
      this.menuBookmarkItem.setIcon(R.drawable.sr2_bookmark_inactive)
      MenuItemCompat.setContentDescription(
        this.menuBookmarkItem,
        this.resources.getString(R.string.readerAccessAddBookmark),
      )
    }
    this.menuBookmarkItem.icon?.colorFilter = currentColorFilter
  }

  private fun onControllerEvent(event: SR2Event) {
    SR2UIThread.checkIsUIThread()

    when (event) {
      is SR2ReadingPositionChanged -> {
        this.onReadingPositionChanged(event)
      }

      is SR2BookmarkCreated -> {
        this.reconfigureBookmarkMenuItem()
      }

      is SR2ThemeChanged -> {
        this.configureForTheme(event.theme)
      }

      is SR2ChapterNonexistent,
      is SR2WebViewInaccessible,
      -> {
        // Nothing
      }

      is SR2OnCenterTapped -> {
        this.showOrHideReadingUI(event.uiVisible)
      }

      is SR2CommandSearchResults,
      is SR2CommandExecutionStarted,
      -> {
        // Nothing
      }

      is SR2CommandExecutionRunningLong -> {
        this.viewsHandleLoadingState(showLoading = true)
      }

      is SR2CommandExecutionSucceeded,
      is SR2CommandExecutionFailed,
      -> {
        this.viewsHandleLoadingState(showLoading = false)
      }

      is SR2Event.SR2ExternalLinkSelected -> {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(event.link))
        this.startActivity(browserIntent)
      }

      is SR2BookmarkDeleted -> {
        this.reconfigureBookmarkMenuItem()
      }
    }
  }

  private fun showOrHideReadingUI(uiVisible: Boolean) {
    SR2UIThread.checkIsUIThread()

    this.toolbar.isVisible = uiVisible
  }

  private fun viewsHandleLoadingState(showLoading: Boolean) {
    SR2UIThread.checkIsUIThread()

    val (webViewVisibility, loadingVisibility) = if (showLoading) {
      View.INVISIBLE to View.VISIBLE
    } else {
      View.VISIBLE to View.INVISIBLE
    }

    if (this.webView.visibility != webViewVisibility) {
      this.webView.visibility = webViewVisibility
    }
    if (this.loadingView.visibility != loadingVisibility) {
      this.loadingView.visibility = loadingVisibility
    }
  }
}
