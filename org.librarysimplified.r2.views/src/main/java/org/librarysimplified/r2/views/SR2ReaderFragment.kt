package org.librarysimplified.r2.views

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.common.base.Function
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.slf4j.LoggerFactory

class SR2ReaderFragment : Fragment() {

  private lateinit var controllerHost: SR2ControllerHostType
  private lateinit var parameters: SR2ReaderFragmentParameters
  private lateinit var readerModel: SR2ReaderViewModel
  private lateinit var webView: WebView
  private var controller: SR2ControllerType? = null

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

  private val parametersId = PARAMETERS_ID
  private val logger = LoggerFactory.getLogger(SR2ReaderFragment::class.java)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.parameters =
      this.arguments!![this.parametersId] as SR2ReaderFragmentParameters
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.sr2_reader, container, false)

    this.webView =
      layout.findViewById(R.id.readerWebView)

    return layout
  }

  override fun onStart() {
    super.onStart()

    val activity =
      this.requireActivity()
    this.controllerHost =
      activity as SR2ControllerHostType

    this.readerModel =
      ViewModelProviders.of(activity)
        .get(SR2ReaderViewModel::class.java)

    /*
     * Fetch or create a controller with the given EPUB loaded into it.
     */

    val controllerFuture: ListenableFuture<SR2ControllerType> =
      this.readerModel.controllerFor(
        configuration = SR2ControllerConfiguration(
          bookFile = this.parameters.bookFile,
          context = activity,
          ioExecutor = controllerHost.onControllerWantsIOExecutor(),
          uiExecutor = this::runOnUIThread
        ),
        controllers = controllerHost.onControllerRequired()
      )

    FluentFuture.from(controllerFuture)
      .transform(
        Function<SR2ControllerType, Unit> { controller ->
          this.onBookOpenSucceeded(controller!!)
        },
        MoreExecutors.directExecutor()
      )
      .catching(
        Throwable::class.java,
        Function<Throwable, Unit> { exception ->
          this.onBookOpenFailed(exception!!)
        }, MoreExecutors.directExecutor()
      )
  }

  private fun onBookOpenSucceeded(controller: SR2ControllerType) {
    this.logger.debug("onBookOpenSucceeded")

    this.runOnUIThread {
      this.onBookOpenSucceededUI(controller)
    }
  }

  @UiThread
  private fun onBookOpenSucceededUI(controller: SR2ControllerType) {
    this.controller = controller
    controller.viewConnect(this.webView)
    this.controllerHost.onControllerBecameAvailable(controller)
  }

  private fun onBookOpenFailed(exception: Throwable) {
    this.logger.error("onBookOpenFailed: ", exception)

    this.runOnUIThread {
      this.onBookOpenFailedUI(exception)
    }
  }

  @UiThread
  private fun onBookOpenFailedUI(exception: Throwable) {

  }

  private fun runOnUIThread(f: () -> Unit) {
    val looper = Looper.getMainLooper()
    val h = Handler(looper)
    h.post {
      f.invoke()
    }
  }

  override fun onStop() {
    super.onStop()

    this.controller?.viewDisconnect()
  }
}
