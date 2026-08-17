package org.librarysimplified.r2.views.internal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent
import org.librarysimplified.r2.api.SR2PrintPageEntry
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.SR2ReaderModel
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationTOCClose
import org.librarysimplified.r2.views.SR2ReaderViewEvent
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookLoadingFailed
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameAvailable
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameUnavailable

internal class SR2TOCPagesFragment : Fragment() {
  private lateinit var eventSubscriptions: CompositeDisposable
  private lateinit var pagesError: View
  private lateinit var pagesList: RecyclerView
  private lateinit var pagesAdapter: SR2TOCPageListAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.pagesAdapter =
      SR2TOCPageListAdapter(
        resources = this.resources,
        onPageEntrySelected = { this.onPageEntrySelected(it) },
      )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val layout =
      inflater.inflate(R.layout.sr2_toc_pages, container, false)
    this.pagesError =
      layout.findViewById(R.id.tocPagesError)
    this.pagesList =
      layout.findViewById(R.id.tocPagesList)

    this.pagesList.adapter = this.pagesAdapter
    this.pagesList.setHasFixedSize(true)
    this.pagesList.setItemViewCacheSize(32)
    (this.pagesList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    return layout
  }

  override fun onStart() {
    super.onStart()

    this.eventSubscriptions = CompositeDisposable()
    this.eventSubscriptions.add(SR2ReaderModel.viewEvents.subscribe(this::onViewEvent))
  }

  override fun onStop() {
    super.onStop()
    this.eventSubscriptions.dispose()
  }

  private fun onViewEvent(event: SR2ReaderViewEvent) {
    when (event) {
      is SR2BookLoadingFailed -> {
        // Nothing to do here.
      }

      is SR2ControllerBecameAvailable -> {
        val pages = event.controller.bookMetadata.printPages
        if (pages.isEmpty()) {
          this.pagesError.isVisible = true
          this.pagesList.isGone = true
          return
        }
        this.pagesAdapter.setPrintPageEntries(pages)
      }

      is SR2ControllerBecameUnavailable -> {
        // Nothing to do here.
      }
    }
  }

  private fun onPageEntrySelected(entry: SR2PrintPageEntry) {
    SR2ReaderModel.submitCommand(SR2Command.OpenPrintPage(entry.label))

    SR2UIThread.runOnUIThreadDelayed(
      { SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationTOCClose) },
      SR2TOC.tocSelectionDelay(),
    )
  }
}
