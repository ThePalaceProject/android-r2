package org.librarysimplified.r2.views

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Dimension
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposables
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
import org.librarysimplified.r2.api.SR2Event.SR2PageSetRecalculating
import org.librarysimplified.r2.api.SR2Event.SR2PageSetRecalculationFinished
import org.librarysimplified.r2.api.SR2Event.SR2ThemeChanged
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
  private val logger =
    LoggerFactory.getLogger(SR2ReaderFragment::class.java)

  private lateinit var root: View
  private lateinit var centerTouch: View
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

    this.root = view
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
      this.uiShow.postDelayed({ SR2ReaderModel.uiToggle() }, 250L)
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
        this.uiShow.postDelayed({ SR2ReaderModel.uiToggle() }, 250L)
      }, 250L)
    }

    this.centerTouch =
      view.findViewById(R.id.readerCenterTouch)

    this.centerTouch.setOnClickListener {
      this.centerTouch.postDelayed({ SR2ReaderModel.uiToggle() }, 250L)
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
          this.onUserPressedEscapeOnToolbarButton()
          true
        } else {
          false
        }
      }
    }

    this.webView.setKeyboardControlListener { event ->
      this.onUserPressedKeyOnWebView(event)
    }

    this.viewsHandleLoadingState(showLoading = true)

    /*
     * Apply window insets as the bottom of the reader may overlap navigation controls on some
     * devices.
     */

    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
      val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
      v.updatePadding(bottom = bottom)
      insets
    }

    ViewCompat.requestApplyInsets(view)
    return view
  }

  private fun onUserPressedEscapeOnToolbarButton() {
    this.logger.debug("onUserPressedEscapeOnToolbarButton")

    SR2ReaderModel.uiSetVisible(false)
  }

  private fun onUserPressedKeyOnWebView(event: KeyEvent) {
    this.logger.debug("onUserPressedKeyOnWebView: {}", event)

    when (event.keyCode) {
      KeyEvent.KEYCODE_SPACE -> {
        SR2ReaderModel.uiSetVisible(true)
      }

      KeyEvent.KEYCODE_ESCAPE -> {
        SR2ReaderModel.uiSetVisible(true)
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

  private fun setProgressColors(foreground: Int) {
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

  private val onFocusChanged =
    OnGlobalFocusChangeListener { _, newFocus ->
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

    val uiVisibleSubscription =
      SR2ReaderModel.uiIsVisible.subscribe { _, isVisible ->
        if (isVisible) {
          this.enableReadingUI()
        } else {
          this.disableReadingUI()
        }
      }

    this.eventSubscriptions.add(Disposables.fromAction { uiVisibleSubscription.close() })
    SR2ReaderModel.wakeLockAcquire(this.requireActivity())
  }

  private fun onViewEvent(event: SR2ReaderViewEvent) {
    this.logger.debug("onViewEvent: {}", event)

    when (event) {
      is SR2BookLoadingFailed -> {
        // Nothing to do here.
      }

      is SR2ControllerBecameAvailable -> {
        this.titleText.text = event.controller.bookMetadata.title
        this.configureForTheme(event.controller.themeNow())
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

    SR2ReaderModel.wakeLockRelease()
    super.onStop()
  }

  private fun onReadingPositionChanged(event: SR2ReadingPositionChanged) {
    val context = this.context ?: return
    this.logger.debug("chapterTitle=${event.chapterTitle}")
    if (event.chapterTitle == null) {
      this.positionTitleView.visibility = View.GONE
    } else {
      this.positionTitleView.text = event.chapterTitle
      this.positionTitleView.visibility = VISIBLE
      this.titleTouchIcon.visibility = VISIBLE
    }

    if (event.currentPage == null || event.pageCount == null) {
      this.positionPageView.visibility = View.GONE
    } else {
      this.positionPageView.text =
        context.getString(R.string.progress_page, event.currentPage, event.pageCount)
      this.positionPageView.visibility = VISIBLE
    }

    val bookProgressPercent = event.bookProgressPercent
    if (bookProgressPercent == null) {
      this.positionPercentView.visibility = View.GONE
      this.progressViewContainer.visibility = View.GONE
    } else {
      this.positionPercentView.text = this.getString(R.string.progress_percent, bookProgressPercent)
      this.setProgress(bookProgressPercent / 100.0)
      this.positionPercentView.visibility = VISIBLE
      this.progressViewContainer.visibility = VISIBLE
    }
    this.reconfigureBookmarkMenuItem()
  }

  private fun setProgress(progress: Double) {
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
        // Nothing
      }

      is SR2CommandSearchResults -> {
        // Nothing
      }

      is SR2CommandExecutionStarted -> {
        // Nothing
      }

      is SR2CommandExecutionRunningLong -> {
        this.loadingView.isIndeterminate = true
        this.loadingView.progress = 0
        this.loadingView.visibility = VISIBLE
        this.webView.visibility = INVISIBLE
      }

      is SR2CommandExecutionSucceeded -> {
        this.loadingView.visibility = INVISIBLE
        this.webView.visibility = VISIBLE
      }

      is SR2CommandExecutionFailed -> {
        this.loadingView.visibility = INVISIBLE
        this.webView.visibility = VISIBLE
      }

      is SR2Event.SR2ExternalLinkSelected -> {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(event.link))
        this.startActivity(browserIntent)
      }

      is SR2BookmarkDeleted -> {
        this.reconfigureBookmarkMenuItem()
      }

      is SR2PageSetRecalculating -> {
        this.loadingView.isIndeterminate = false
        this.loadingView.progress = (event.progress * 100).toInt()
        this.loadingView.visibility = VISIBLE
        this.webView.visibility = INVISIBLE
      }

      is SR2PageSetRecalculationFinished -> {
        this.loadingView.postDelayed({
          this.loadingView.visibility = INVISIBLE
          this.webView.visibility = VISIBLE
        }, 200L)
      }
    }
  }

  private fun dpToPixels(
    @Dimension(unit = Dimension.DP) d: Double
  ): Int = (d * this.resources.displayMetrics.density).toInt()

  private fun setWebViewMargins(
    @Dimension(unit = Dimension.DP) marginDp: Double
  ) {
    val marginPx = this.dpToPixels(marginDp)
    val params = this.webView.layoutParams as MarginLayoutParams
    params.leftMargin = marginPx
    params.rightMargin = marginPx
    this.webView.layoutParams = params
  }

  private fun disableReadingUI() {
    this.toolbar.visibility = INVISIBLE
    this.centerTouch.contentDescription = this.getString(R.string.accessibilityShowUIButton)

    /*
     * When the reading UI is disabled, we change the behavior of the remaining buttons such
     * that the keyboard essentially controls everything.
     */

    this.uiShow.visibility = VISIBLE
    this.uiShow.isFocusable = false
    this.uiShow.isClickable = true
    this.buttonHideUI.visibility = INVISIBLE
    this.titleTouch.isFocusable = false
    this.titleTouch.isClickable = true

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
      this.resources.configuration
    val hasHardwareKeyboard =
      config.keyboard != Configuration.KEYBOARD_NOKEYS
    val isHardwareKeyboardPresent =
      config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

    return hasHardwareKeyboard && isHardwareKeyboardPresent
  }

  private fun enableReadingUI() {
    this.toolbar.visibility = VISIBLE
    this.centerTouch.contentDescription = this.getString(R.string.accessibilityHideUIButton)

    /*
     * When the reading UI is enabled, we configure all views to give the standard Android
     * keyboard handling (arrow keys switch between views, pressing return selects a view).
     */

    this.buttonHideUI.visibility = VISIBLE

    this.uiShow.visibility = INVISIBLE
    this.uiShow.isFocusable = false
    this.uiShow.isClickable = false
    this.titleTouch.isFocusable = true
    this.titleTouch.isClickable = true

    this.webView.isFocusable = false
    this.buttonBack.postDelayed({ this.buttonBack.requestFocus() }, 250L)
  }

  private fun viewsHandleLoadingState(showLoading: Boolean) {
    SR2UIThread.checkIsUIThread()

    val (webViewVisibility, loadingVisibility) =
      if (showLoading) {
        INVISIBLE to VISIBLE
      } else {
        VISIBLE to INVISIBLE
      }

    if (this.webView.visibility != webViewVisibility) {
      this.webView.visibility = webViewVisibility
    }
    if (this.loadingView.visibility != loadingVisibility) {
      this.loadingView.visibility = loadingVisibility
    }
  }
}
