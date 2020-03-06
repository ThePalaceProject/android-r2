package org.librarysimplified.r2.views

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
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
import org.slf4j.LoggerFactory

class SR2ReaderFragment : Fragment() {

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
      arguments.putSerializable(PARAMETERS_ID, parameters)
      val fragment = SR2ReaderFragment()
      fragment.arguments = arguments
      return fragment
    }
  }

  private val logger = LoggerFactory.getLogger(SR2ReaderFragment::class.java)

  private lateinit var webView: WebView
  private lateinit var progressView: ProgressBar
  private lateinit var positionPageView: TextView
  private lateinit var positionTitleView: TextView
  private lateinit var positionPercentView: TextView

  private lateinit var controllerHost: SR2ControllerHostType
  private lateinit var parameters: SR2ReaderFragmentParameters
  private lateinit var readerModel: SR2ReaderViewModel

  private var controller: SR2ControllerType? = null
  private var controllerSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    parameters = arguments!![PARAMETERS_ID] as SR2ReaderFragmentParameters
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view =
      inflater.inflate(R.layout.sr2_reader, container, false)

    webView = view.findViewById(R.id.readerWebView)
    progressView = view.findViewById(R.id.reader2_progress)
    positionPageView = view.findViewById(R.id.reader2_position_page)
    positionTitleView = view.findViewById(R.id.reader2_position_title)
    positionPercentView = view.findViewById(R.id.reader2_position_percent)

    return view
  }

  override fun onStart() {
    super.onStart()

    val activity = requireActivity()
    controllerHost = activity as SR2ControllerHostType
    readerModel =
      ViewModelProviders.of(activity)
        .get(SR2ReaderViewModel::class.java)

    /*
     * Fetch or create a controller with the given EPUB loaded into it.
     */

    val controllerFuture: ListenableFuture<SR2ControllerType> =
      readerModel.controllerFor(
        configuration = SR2ControllerConfiguration(
          bookFile = parameters.bookFile,
          context = activity,
          ioExecutor = controllerHost.onControllerWantsIOExecutor(),
          uiExecutor = this::runOnUIThread
        ),
        controllers = controllerHost.onControllerRequired()
      )

    FluentFuture.from(controllerFuture)
      .transform(
        Function<SR2ControllerType, Unit> { controller ->
          onBookOpenSucceeded(controller!!)
        },
        MoreExecutors.directExecutor()
      )
      .catching(
        Throwable::class.java,
        Function<Throwable, Unit> { e ->
          onBookOpenFailed(e!!)
        }, MoreExecutors.directExecutor()
      )
  }

  override fun onStop() {
    super.onStop()
    controllerSubscription?.dispose()
    controller?.viewDisconnect()
  }

  private fun onBookOpenSucceeded(controller: SR2ControllerType) {
    logger.debug("onBookOpenSucceeded")
    runOnUIThread { onBookOpenSucceededUI(controller) }
  }

  @UiThread
  private fun onBookOpenSucceededUI(controller: SR2ControllerType) {
    this.controller = controller
    controller.viewConnect(webView)

    controllerSubscription =
      controller.events.subscribe { onControllerEvent(it) }

    controllerHost.onControllerBecameAvailable(controller)
  }

  private fun onBookOpenFailed(e: Throwable) {
    logger.error("onBookOpenFailed: ", e)
    runOnUIThread { onBookOpenFailedUI(e) }
  }

  @UiThread
  private fun onBookOpenFailedUI(e: Throwable) {
    TODO()
  }

  @UiThread
  private fun onReadingPositionChanged(event: SR2Event.SR2ReadingPositionChanged) {
    logger.debug("chapterTitle=${event.chapterTitle}")
    progressView.apply { max = 100; progress = event.percent }
    positionPageView.text = getString(R.string.progress_page, event.currentPage, event.pageCount)
    positionTitleView.text = event.chapterTitle
    positionPercentView.text = getString(R.string.progress_percent, event.percent)
  }

  private fun onControllerEvent(event: SR2Event) {
    when (event) {
      is SR2Event.SR2ReadingPositionChanged -> {
        runOnUIThread { onReadingPositionChanged(event) }
      }
    }
  }

  private fun runOnUIThread(f: () -> Unit) {
    val looper = Looper.getMainLooper()
    val h = Handler(looper)
    h.post { f.invoke() }
  }
}
