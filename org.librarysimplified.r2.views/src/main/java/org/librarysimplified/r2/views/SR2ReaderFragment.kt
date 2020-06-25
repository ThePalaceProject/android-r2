package org.librarysimplified.r2.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.common.base.Function
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.disposables.Disposable
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.slf4j.LoggerFactory

class SR2ReaderFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(SR2ReaderFragment::class.java)

  companion object {
    private const val PARAMETERS_ID =
      "org.librarysimplified.r2.views.SR2ReaderFragment.parameters"

    /**
     * Create a book detail fragment for the given parameters.
     */

    fun create(
      parameters: SR2ReaderFragmentParameters
    ): SR2ReaderFragment {
      val arguments = Bundle()
      arguments.putSerializable(this.PARAMETERS_ID, parameters)
      val fragment = SR2ReaderFragment()
      fragment.arguments = arguments
      return fragment
    }
  }

  private lateinit var controllerHost: SR2ControllerHostType
  private lateinit var menu: Menu
  private lateinit var parameters: SR2ReaderFragmentParameters
  private lateinit var positionPageView: TextView
  private lateinit var positionPercentView: TextView
  private lateinit var positionTitleView: TextView
  private lateinit var progressView: ProgressBar
  private lateinit var readerModel: SR2ReaderViewModel
  private lateinit var webView: WebView
  private var controller: SR2ControllerType? = null
  private var controllerSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.parameters = this.arguments!![PARAMETERS_ID] as SR2ReaderFragmentParameters
    this.setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view =
      inflater.inflate(R.layout.sr2_reader, container, false)

    this.webView = view.findViewById(R.id.readerWebView)
    this.progressView = view.findViewById(R.id.reader2_progress)
    this.positionPageView = view.findViewById(R.id.reader2_position_page)
    this.positionTitleView = view.findViewById(R.id.reader2_position_title)
    this.positionPercentView = view.findViewById(R.id.reader2_position_percent)

    return view
  }

  override fun onCreateOptionsMenu(
    menu: Menu,
    inflater: MenuInflater
  ) {
    inflater.inflate(R.menu.sr2_reader_menu, menu)
    this.menu = menu
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.readerMenuSettings -> {
        Toast.makeText(this.requireContext(), "Settings!", Toast.LENGTH_SHORT).show()
        true
      }
      R.id.readerMenuTOC -> {
        if (this.controller != null) {
          this.controllerHost.onNavigationOpenTableOfContents()
        }
        true
      }
      else ->
        super.onOptionsItemSelected(item)
    }
  }

  override fun onStart() {
    super.onStart()

    val activity = this.requireActivity()
    this.controllerHost = activity as SR2ControllerHostType
    this.readerModel =
      ViewModelProviders.of(activity)
        .get(SR2ReaderViewModel::class.java)

    /*
     * Fetch or create a controller with the given EPUB loaded into it.
     */

    val controllerFuture: ListenableFuture<SR2ControllerReference> =
      this.readerModel.createOrGet(
        configuration = SR2ControllerConfiguration(
          bookFile = this.parameters.bookFile,
          context = activity,
          ioExecutor = this.controllerHost.onControllerWantsIOExecutor(),
          uiExecutor = SR2UIThread::runOnUIThread
        ),
        controllers = this.controllerHost.onControllerRequired()
      )

    FluentFuture.from(controllerFuture)
      .transform(
        Function<SR2ControllerReference, Unit> { reference ->
          this.onBookOpenSucceeded(reference!!.controller, reference.isFirstStartup)
        },
        MoreExecutors.directExecutor()
      )
      .catching(
        Throwable::class.java,
        Function<Throwable, Unit> { e ->
          this.onBookOpenFailed(e!!)
        }, MoreExecutors.directExecutor()
      )
  }

  override fun onStop() {
    super.onStop()
    this.controllerSubscription?.dispose()
    this.controller?.viewDisconnect()
  }

  private fun onBookOpenSucceeded(
    controller: SR2ControllerType,
    isFirstStartup: Boolean
  ) {
    this.logger.debug("onBookOpenSucceeded: first startup {}", isFirstStartup)
    SR2UIThread.runOnUIThread { this.onBookOpenSucceededUI(controller, isFirstStartup) }
  }

  @UiThread
  private fun onBookOpenSucceededUI(
    controller: SR2ControllerType,
    isFirstStartup: Boolean
  ) {
    this.controller = controller
    controller.viewConnect(this.webView)
    this.controllerSubscription = controller.events.subscribe { this.onControllerEvent(it) }
    this.controllerHost.onControllerBecameAvailable(controller, isFirstStartup)
  }

  private fun onBookOpenFailed(e: Throwable) {
    this.logger.error("onBookOpenFailed: ", e)
    SR2UIThread.runOnUIThread { this.onBookOpenFailedUI(e) }
  }

  @UiThread
  private fun onBookOpenFailedUI(e: Throwable) {
    TODO()
  }

  @UiThread
  private fun onReadingPositionChanged(event: SR2Event.SR2ReadingPositionChanged) {
    this.logger.debug("chapterTitle=${event.chapterTitle}")
    this.progressView.apply { this.max = 100; this.progress = event.bookProgressPercent }
    this.positionPageView.text = this.getString(R.string.progress_page, event.currentPage, event.pageCount)
    this.positionTitleView.text = event.chapterTitle
    this.positionPercentView.text = this.getString(R.string.progress_percent, event.bookProgressPercent)
  }

  private fun onControllerEvent(event: SR2Event) {
    when (event) {
      is SR2Event.SR2ReadingPositionChanged -> {
        SR2UIThread.runOnUIThread { this.onReadingPositionChanged(event) }
      }
    }
  }
}
