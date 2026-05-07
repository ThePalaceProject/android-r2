package org.librarysimplified.r2.views

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.r2.api.SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND
import org.librarysimplified.r2.api.SR2ColorScheme.DARK_TEXT_ON_SEPIA
import org.librarysimplified.r2.api.SR2ColorScheme.LIGHT_TEXT_DARK_BACKGROUND
import org.librarysimplified.r2.api.SR2Command
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
import org.librarysimplified.r2.views.internal.SR2ColorFilters
import org.librarysimplified.r2.views.internal.SR2LimitedWebView
import org.librarysimplified.r2.views.internal.SR2Ripples
import org.librarysimplified.r2.views.internal.SR2SettingsDialog
import org.slf4j.LoggerFactory

class SR2ReaderFragment : SR2Fragment() {

  private lateinit var pageNext: View
  private lateinit var pagePrevious: View
  private lateinit var pageNextIcon: ImageView
  private lateinit var pagePreviousIcon: ImageView

  private val logger =
    LoggerFactory.getLogger(SR2ReaderFragment::class.java)

  private lateinit var uiShow: View
  private lateinit var uiShowIcon: ImageView
  private lateinit var buttonHideUIIcon: ImageView
  private lateinit var buttonHideUI: View
  private lateinit var buttonBack: View
  private lateinit var buttonBackIcon: ImageView
  private lateinit var buttonBookmark: View
  private lateinit var buttonBookmarkIcon: ImageView
  private lateinit var buttonSearch: View
  private lateinit var buttonSearchIcon: ImageView
  private lateinit var buttonSettings: View
  private lateinit var buttonSettingsIcon: ImageView
  private lateinit var container: ViewGroup
  private lateinit var eventSubscriptions: CompositeDisposable
  private lateinit var loadingView: ProgressBar
  private lateinit var positionPageView: TextView
  private lateinit var positionPercentView: TextView
  private lateinit var positionTitleView: TextView
  private lateinit var progressContainer: ViewGroup
  private lateinit var progressViewContainer: ViewGroup
  private lateinit var progressViewBorder: View
  private lateinit var progressViewFill: View
  private lateinit var titleText: TextView
  private lateinit var titleTouch: View
  private lateinit var titleTouchIcon: ImageView
  private lateinit var toolbar: ViewGroup
  private lateinit var toolbarButtonIcons: List<ImageView>
  private lateinit var toolbarButtons: List<View>
  private lateinit var webView: SR2LimitedWebView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val view =
      inflater.inflate(R.layout.sr2_reader, container, false)

    this.container =
      view.findViewById(R.id.readerContainer)
    this.progressContainer =
      view.findViewById(R.id.readerProgressContainer)
    this.webView =
      view.findViewById(R.id.readerWebView)
    this.progressViewContainer =
      view.findViewById(R.id.reader2_progress_bar_container)
    this.progressViewBorder =
      view.findViewById(R.id.reader2_progress_bar_border)
    this.progressViewFill =
      view.findViewById(R.id.reader2_progress_bar_fill)
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
    this.titleTouch =
      view.findViewById(R.id.reader2_position_title_touch)
    this.titleTouchIcon =
      view.findViewById(R.id.reader2_position_title_icon)
    this.uiShow =
      view.findViewById(R.id.readerShowTouch)
    this.uiShowIcon =
      view.findViewById(R.id.readerShow)

    this.uiShow.setOnClickListener {
      this.uiShow.postDelayed({
        this.showOrHideReadingUI(uiVisible = true)
      }, 250L)
    }

    this.titleTouch.setOnClickListener {
      this.titleTouch.postDelayed({
        this.onReaderMenuTOCSelected()
      }, 250L)
    }

    this.toolbar =
      view.findViewById(R.id.readerToolbar2)

    this.buttonBack =
      this.toolbar.findViewById(R.id.readerToolbarBackTouch)
    this.buttonBackIcon =
      this.toolbar.findViewById(R.id.readerToolbarBack)

    this.buttonSearch =
      this.toolbar.findViewById(R.id.readerToolbarSearchTouch)
    this.buttonSearchIcon =
      this.toolbar.findViewById(R.id.readerToolbarSearch)

    this.buttonSettings =
      this.toolbar.findViewById(R.id.readerToolbarSettingsTouch)
    this.buttonSettingsIcon =
      this.toolbar.findViewById(R.id.readerToolbarSettings)

    this.buttonBookmark =
      this.toolbar.findViewById(R.id.readerToolbarBookmarkTouch)
    this.buttonBookmarkIcon =
      this.toolbar.findViewById(R.id.readerToolbarBookmark)

    this.buttonHideUI =
      view.findViewById(R.id.readerToolbarHideTouch)
    this.buttonHideUIIcon =
      view.findViewById(R.id.readerToolbarHide)

    this.buttonBack.setOnClickListener {
      this.onToolbarNavigationSelected()
    }
    this.buttonBookmark.setOnClickListener {
      this.onReaderMenuAddBookmarkSelected()
    }
    this.buttonSettings.setOnClickListener {
      this.onReaderMenuSettingsSelected()
    }
    this.buttonSearch.setOnClickListener {
      this.onReaderMenuSearchSelected()
    }
    this.buttonHideUI.setOnClickListener {
      this.buttonHideUI.postDelayed({
        this.showOrHideReadingUI(uiVisible = false)
      }, 250L)
    }

    this.pagePrevious =
      view.findViewById(R.id.readerPagePreviousTouch)
    this.pageNext =
      view.findViewById(R.id.readerPageNextTouch)
    this.pagePreviousIcon =
      view.findViewById(R.id.readerPagePrevious)
    this.pageNextIcon =
      view.findViewById(R.id.readerPageNext)

    this.pagePrevious.setOnClickListener {
      SR2ReaderModel.submitCommand(SR2Command.OpenPagePrevious)
    }
    this.pageNext.setOnClickListener {
      SR2ReaderModel.submitCommand(SR2Command.OpenPageNext)
    }

    this.toolbarButtonIcons =
      listOf(
        this.buttonBackIcon,
        this.buttonBookmarkIcon,
        this.buttonSearchIcon,
        this.buttonSettingsIcon,
        this.buttonHideUIIcon,
      )

    this.toolbarButtons =
      listOf(
        this.buttonBack,
        this.buttonBookmark,
        this.buttonSearch,
        this.buttonSettings,
        this.buttonHideUI,
      )

    this.toolbarButtons.forEach { buttonView ->
      buttonView.setOnKeyListener { _, keyCode, event ->
        return@setOnKeyListener if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ESCAPE) {
          onUserPressedEscapeOnToolbarButton()
          true
        } else {
          false
        }
      }
    }

    this.webView.setKeyboardControlListener { event ->
      this.onUserPressedKeyOnWebView(event)
    }

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

  private fun onUserPressedEscapeOnToolbarButton() {
    this.logger.debug("onUserPressedEscapeOnToolbarButton")

    this.showOrHideReadingUI(uiVisible = false)
  }

  private fun onUserPressedKeyOnWebView(
    event: KeyEvent,
  ) {
    this.logger.debug("onUserPressedKeyOnWebView: {}", event)

    when (event.keyCode) {
      KeyEvent.KEYCODE_ESCAPE -> {
        this.showOrHideReadingUI(uiVisible = true)
      }

      KeyEvent.KEYCODE_DPAD_RIGHT -> {
        SR2ReaderModel.submitCommand(SR2Command.OpenPageNext)
      }

      KeyEvent.KEYCODE_DPAD_LEFT -> {
        SR2ReaderModel.submitCommand(SR2Command.OpenPagePrevious)
      }
    }
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

  private fun onReaderMenuSearchSelected(): Boolean {
    SR2UIThread.checkIsUIThread()

    SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationSearchOpen)
    return true
  }

  private fun configureForTheme(theme: SR2Theme) {
    SR2UIThread.checkIsUIThread()

    val background = theme.colorScheme.background()
    val foreground = theme.colorScheme.foreground()

    when (theme.colorScheme) {
      DARK_TEXT_LIGHT_BACKGROUND -> {
        this.toolbarButtonIcons.forEach { v -> v.colorFilter = null }
        this.toolbarButtons.forEach { v ->
          v.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        }

        this.titleTouch.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        this.titleTouchIcon.colorFilter = null
        this.pageNext.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        this.pageNextIcon.colorFilter = null
        this.pagePrevious.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        this.pagePreviousIcon.colorFilter = null
        this.uiShow.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        this.uiShowIcon.colorFilter = null
      }

      LIGHT_TEXT_DARK_BACKGROUND -> {
        this.toolbarButtonIcons.forEach { v ->
          v.colorFilter = SR2ColorFilters.inversionFilter
        }
        this.toolbarButtons.forEach { v ->
          v.foreground = SR2Ripples.createRippleDrawableForDarkBackground()
        }

        this.titleTouch.foreground = SR2Ripples.createRippleDrawableForDarkBackground()
        this.titleTouchIcon.colorFilter = SR2ColorFilters.inversionFilter
        this.pageNext.foreground = SR2Ripples.createRippleDrawableForDarkBackground()
        this.pageNextIcon.colorFilter = SR2ColorFilters.inversionFilter
        this.pagePrevious.foreground = SR2Ripples.createRippleDrawableForDarkBackground()
        this.pagePreviousIcon.colorFilter = SR2ColorFilters.inversionFilter
        this.uiShow.foreground = SR2Ripples.createRippleDrawableForDarkBackground()
        this.uiShowIcon.colorFilter = SR2ColorFilters.inversionFilter
      }

      DARK_TEXT_ON_SEPIA -> {
        this.toolbarButtonIcons.forEach { v -> v.colorFilter = null }
        this.toolbarButtons.forEach { v ->
          v.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        }

        this.titleTouch.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        this.titleTouchIcon.colorFilter = null
        this.pageNext.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        this.pageNextIcon.colorFilter = null
        this.pagePrevious.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        this.pagePreviousIcon.colorFilter = null
        this.uiShow.foreground = SR2Ripples.createRippleDrawableForLightBackground()
        this.uiShowIcon.colorFilter = null
      }
    }

    this.container.setBackgroundColor(background)
    this.positionPageView.setTextColor(foreground)
    this.positionPercentView.setTextColor(foreground)
    this.positionTitleView.setTextColor(foreground)
    this.titleText.setTextColor(foreground)
    this.toolbar.setBackgroundColor(background)
    this.setProgressColors(foreground)
  }

  private fun setProgressColors(
    foreground: Int,
  ) {
    val backgroundView = this.progressViewBorder.background.mutate() as GradientDrawable
    backgroundView.setStroke(1, foreground)
    this.progressViewFill.setBackgroundColor(foreground)
  }

  private fun openSettings() {
    val activity = this.requireActivity()

    if (!SR2SettingsDialog.isOpen()) {
      SR2SettingsDialog.create(
        brightness = SR2BrightnessService(activity),
        context = activity,
      )
    }
  }

  /**
   * A small focus change listener for debugging.
   */

  private val onFocusChanged = OnGlobalFocusChangeListener { _, newFocus ->
    this.logger.debug("Focus changed: {}", newFocus)
  }

  override fun onStart() {
    this.logger.debug("onStart")
    super.onStart()

    this.eventSubscriptions = CompositeDisposable()
    this.eventSubscriptions.add(SR2ReaderModel.viewEvents.subscribe(this::onViewEvent))
    this.eventSubscriptions.add(SR2ReaderModel.controllerEvents.subscribe(this::onControllerEvent))

    try {
      val activity = this.requireActivity()
      val rootView = activity.window.decorView
      val viewTreeObserver = rootView.getViewTreeObserver()
      viewTreeObserver.addOnGlobalFocusChangeListener(this.onFocusChanged)
    } catch (e: Throwable) {
      this.logger.debug("Failed to register focus change listener: ", e)
    }
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
        this.titleText.text = event.controller.bookMetadata.title
        this.configureForTheme(event.controller.themeNow())
        this.showOrHideReadingUI(true)
        event.controller.viewConnect(this.webView)

        if (event.controller.configuration.allowCopyPaste) {
          this.webView.isLongClickable = true
          this.webView.setOnLongClickListener(null)
        } else {
          this.webView.isLongClickable = false
          this.webView.setOnLongClickListener {
            this.logger.debug("Ignoring long click.")
            true
          }
        }
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
      val activity = this.requireActivity()
      val rootView = activity.window.decorView
      val viewTreeObserver = rootView.getViewTreeObserver()
      viewTreeObserver.removeOnGlobalFocusChangeListener(this.onFocusChanged)
    } catch (e: Throwable) {
      this.logger.error("Failed to remove focus change listener: ", e)
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
      this.titleTouchIcon.visibility = View.VISIBLE
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
      this.progressViewContainer.visibility = View.GONE
    } else {
      this.positionPercentView.text = this.getString(R.string.progress_percent, bookProgressPercent)
      this.setProgress(bookProgressPercent / 100.0)
      this.positionPercentView.visibility = View.VISIBLE
      this.progressViewContainer.visibility = View.VISIBLE
    }
    this.reconfigureBookmarkMenuItem()
  }

  private fun setProgress(
    progress: Double,
  ) {
    val parentWidth = (this.progressViewFill.parent as View).width
    this.progressViewFill.layoutParams.width = (parentWidth * progress).toInt()
    this.progressViewFill.requestLayout()
  }

  private fun reconfigureBookmarkMenuItem() {
    SR2UIThread.checkIsUIThread()

    if (SR2ReaderModel.isBookmarkHere()) {
      this.buttonBookmarkIcon.setImageResource(R.drawable.sr2_bookmark_active)
      this.buttonBookmark.contentDescription =
        this.resources.getString(R.string.readerAccessDeleteBookmark)
    } else {
      this.buttonBookmarkIcon.setImageResource(R.drawable.sr2_bookmark_inactive)
      this.buttonBookmark.contentDescription =
        this.resources.getString(R.string.readerAccessAddBookmark)
    }
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

      is SR2ChapterNonexistent -> {
        // Nothing
      }

      is SR2WebViewInaccessible -> {
        // Nothing
      }

      is SR2OnCenterTapped -> {
        this.showOrHideReadingUI(event.uiVisible)
      }

      is SR2CommandSearchResults -> {
        // Nothing
      }

      is SR2CommandExecutionStarted -> {
        // Nothing
      }

      is SR2CommandExecutionRunningLong -> {
        this.viewsHandleLoadingState(showLoading = true)
      }

      is SR2CommandExecutionSucceeded -> {
        this.viewsHandleLoadingState(showLoading = false)
      }

      is SR2CommandExecutionFailed -> {
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

  private fun showOrHideReadingUI(
    uiVisible: Boolean,
  ) {
    SR2UIThread.checkIsUIThread()

    if (uiVisible) {
      this.enableReadingUI()
    } else {
      this.disableReadingUI()
    }
  }

  private fun disableReadingUI() {
    this.toolbar.visibility = View.INVISIBLE
    this.pageNextIcon.visibility = View.INVISIBLE
    this.pagePreviousIcon.visibility = View.INVISIBLE

    /*
     * When the reading UI is disabled, we change the behavior of the remaining buttons such
     * that the keyboard essentially controls everything.
     */

    this.pagePrevious.isFocusable = false
    this.pagePrevious.isClickable = true
    this.pageNext.isFocusable = false
    this.pageNext.isClickable = true
    this.uiShow.visibility = View.VISIBLE
    this.uiShow.isFocusable = false
    this.uiShow.isClickable = true
    this.buttonHideUI.visibility = View.INVISIBLE
    this.titleTouch.isFocusable = false
    this.titleTouch.isClickable = false

    this.webView.isFocusable = true
    this.webView.postDelayed({ this.webView.requestFocus() }, 250L)

    if (this.isUsingKeyboard()) {
      val toast =
        Toast.makeText(
          this.requireContext(),
          R.string.accessibilityPressESCToShow,
          Toast.LENGTH_SHORT,
        )
      toast.show()
    }
  }

  private fun isUsingKeyboard(): Boolean {
    val config: Configuration =
      resources.configuration
    val hasHardwareKeyboard =
      config.keyboard != Configuration.KEYBOARD_NOKEYS
    val isHardwareKeyboardPresent =
      config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

    return hasHardwareKeyboard && isHardwareKeyboardPresent
  }

  private fun enableReadingUI() {
    this.toolbar.visibility = View.VISIBLE
    this.pageNextIcon.visibility = View.VISIBLE
    this.pagePreviousIcon.visibility = View.VISIBLE

    /*
     * When the reading UI is enabled, we configure all views to give the standard Android
     * keyboard handling (arrow keys switch between views, pressing return selects a view).
     */

    this.pagePrevious.isFocusable = true
    this.pagePrevious.isClickable = true
    this.pageNext.isFocusable = true
    this.pageNext.isClickable = true
    this.buttonHideUI.visibility = View.VISIBLE

    this.uiShow.visibility = View.INVISIBLE
    this.uiShow.isFocusable = false
    this.uiShow.isClickable = false
    this.titleTouch.isFocusable = true
    this.titleTouch.isClickable = true

    this.webView.isFocusable = false
    this.buttonBack.postDelayed({ this.buttonBack.requestFocus() }, 250L)
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
