package org.librarysimplified.r2.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.slf4j.LoggerFactory

class SR2TOCBookmarksFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(SR2TOCBookmarksFragment::class.java)

  private lateinit var bookmarkAdapter: SR2TOCBookmarkAdapter
  private lateinit var controller: SR2ControllerType
  private lateinit var controllerHost: SR2ControllerHostType
  private lateinit var emptyText: TextView
  private lateinit var readerModel: SR2ReaderViewModel
  private val bookmarkSubscriptions = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.bookmarkAdapter =
      SR2TOCBookmarkAdapter(
        resources = this.resources,
        onBookmarkSelected = { this.onBookmarkSelected(it) })
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout =
      inflater.inflate(R.layout.sr2_toc_bookmarks, container, false)
    val recyclerView =
      layout.findViewById<RecyclerView>(R.id.tocBookmarksList)
    this.emptyText =
      layout.findViewById(R.id.tocBookmarksEmptyText)

    recyclerView.adapter = this.bookmarkAdapter
    recyclerView.setHasFixedSize(true)
    recyclerView.setItemViewCacheSize(32)
    recyclerView.layoutManager = LinearLayoutManager(this.context)
    (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    return layout
  }

  override fun onStart() {
    super.onStart()

    val activity = this.requireActivity()
    this.controllerHost = activity as SR2ControllerHostType
    this.readerModel =
      ViewModelProviders.of(activity)
        .get(SR2ReaderViewModel::class.java)

    this.controller =
      this.readerModel.get()!!

    this.bookmarkSubscriptions.add(
      this.controller.events.ofType(SR2BookmarkEvent::class.java)
        .subscribe { this.reloadBookmarks() })

    this.reloadBookmarks()
  }

  private fun reloadBookmarks() {
    SR2UIThread.runOnUIThread {
      this.reloadBookmarksUI()
    }
  }

  @UiThread
  private fun reloadBookmarksUI() {
    SR2UIThread.checkIsUIThread()

    val bookmarksNow = this.controller.bookmarksNow()
    this.logger.debug("received {} bookmarks", bookmarksNow.size)

    if (bookmarksNow.isEmpty()) {
      this.emptyText.visibility = View.VISIBLE
    } else {
      this.emptyText.visibility = View.INVISIBLE
    }

    this.bookmarkAdapter.setBookmarks(bookmarksNow)
  }

  private fun onBookmarkSelected(bookmark: SR2Bookmark) {
    this.controller.submitCommand(SR2Command.OpenChapter(bookmark.locator))

    SR2UIThread.runOnUIThreadDelayed(Runnable {
      this.controllerHost.onNavigationClose()
    }, 1_000L)
  }

  override fun onStop() {
    super.onStop()
    this.bookmarkSubscriptions.dispose()
  }
}
