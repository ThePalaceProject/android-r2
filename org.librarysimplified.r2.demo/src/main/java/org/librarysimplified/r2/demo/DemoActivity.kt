package org.librarysimplified.r2.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.vanilla.SR2Controllers
import org.librarysimplified.r2.views.SR2ControllerHostType
import org.librarysimplified.r2.views.SR2ReaderFragment
import org.librarysimplified.r2.views.SR2ReaderFragmentParameters
import java.io.File
import java.util.concurrent.Executors

class DemoActivity : AppCompatActivity(), DemoNavigationControllerType, SR2ControllerHostType {

  private lateinit var controller: SR2ControllerType

  private val ioExecutor =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1) { runnable ->
      val thread = Thread(runnable)
      thread.name = "org.librarysimplified.r2.demo.io"
      thread
    })

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.demo_host)

    if (savedInstanceState == null) {
      val fragment = DemoIntroFragment()
      this.supportFragmentManager.beginTransaction()
        .replace(R.id.mainFragmentHost, fragment, "MAIN")
        .commit()
    }
  }

  override fun openReader(file: File) {
    val fragment =
      SR2ReaderFragment.create(
        SR2ReaderFragmentParameters(
          bookFile = file
        )
      )

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHost, fragment, "MAIN")
      .addToBackStack(null)
      .commit()
  }

  override fun popBackStack() {
    this.supportFragmentManager.popBackStack()
  }

  override fun onControllerRequired(): SR2ControllerProviderType {
    return SR2Controllers()
  }

  override fun onControllerBecameAvailable(controller: SR2ControllerType) {
    this.controller = controller
  }

  override fun onControllerWantsIOExecutor(): ListeningExecutorService {
    return this.ioExecutor
  }

  override fun onNavigationClose() {
    this.supportFragmentManager.popBackStack()
  }

  override fun onNavigationOpenTableOfContents() {

  }
}
