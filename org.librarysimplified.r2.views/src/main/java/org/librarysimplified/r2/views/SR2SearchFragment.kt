package org.librarysimplified.r2.views

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionFailed
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionSucceeded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionRunningLong
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionStarted
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandSearchResults
import org.librarysimplified.r2.api.SR2Event.SR2Error
import org.librarysimplified.r2.api.SR2Event.SR2ExternalLinkSelected
import org.librarysimplified.r2.api.SR2Event.SR2OnCenterTapped
import org.librarysimplified.r2.api.SR2Event.SR2ReadingPositionChanged
import org.librarysimplified.r2.api.SR2Event.SR2ThemeChanged
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.views.SR2ReaderViewCommand.SR2ReaderViewNavigationSearchClose
import org.librarysimplified.r2.views.search.SR2SearchResultAdapter
import org.librarysimplified.r2.views.search.SR2SearchResultSectionItemDecoration
import org.librarysimplified.r2.views.search.SR2SearchResultSectionListener
import org.readium.r2.shared.publication.Href

class SR2SearchFragment : SR2Fragment() {

  private lateinit var subscriptions: CompositeDisposable
  private lateinit var noResultLabel: TextView
  private lateinit var searchAdapter: SR2SearchResultAdapter
  private lateinit var searchResultsList: RecyclerView
  private lateinit var searchView: SearchView
  private lateinit var toolbar: Toolbar

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R.layout.sr2_search, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.toolbar =
      view.findViewById(R.id.searchToolbar)
    this.searchResultsList =
      view.findViewById(R.id.searchResultsList)
    this.noResultLabel =
      view.findViewById(R.id.noResultLabel)

    this.toolbar.inflateMenu(R.menu.sr2_search_menu)
    this.toolbar.setNavigationOnClickListener { this.close() }
    this.toolbar.setNavigationContentDescription(R.string.settingsAccessibilityBack)

    this.searchAdapter = SR2SearchResultAdapter(
      onItemClicked = { locator ->
        val controller = SR2ReaderModel.controller()

        controller.submitCommand(
          SR2Command.HighlightTerms(
            searchingTerms = this.searchView.query.toString(),
            clearHighlight = false,
          ),
        )

        controller.submitCommand(
          SR2Command.OpenChapter(
            SR2Locator.SR2LocatorPercent(
              chapterHref = Href(locator.href),
              chapterProgress = locator.locations.progression ?: 0.0,
            ),
          ),
        )
        this.close()
      },
    )

    this.searchResultsList.apply {
      this.adapter = this@SR2SearchFragment.searchAdapter
      this.layoutManager = LinearLayoutManager(
        this@SR2SearchFragment.requireContext(),
        LinearLayoutManager.VERTICAL,
        false,
      )
      this.addItemDecoration(
        SR2SearchResultSectionItemDecoration(
          this.context,
          object : SR2SearchResultSectionListener {
            override fun isStartOfSection(index: Int): Boolean =
              SR2ReaderModel.searchLocators.value.run {
                when {
                  index == 0 -> {
                    true
                  }

                  index < 0 || index >= size -> {
                    false
                  }

                  else -> {
                    getOrNull(index)?.title != getOrNull(index - 1)?.title
                  }
                }
              }

            override fun sectionTitle(index: Int): String {
              return SR2ReaderModel.searchLocators.value.getOrNull(index)?.title.orEmpty()
            }
          },
        ),
      )
      this.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
    }

    this.configureSearch()
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (!hidden) {
      this.showKeyboard()
    }
  }

  @UiThread
  private fun close() {
    SR2UIThread.checkIsUIThread()
    SR2ReaderModel.submitViewCommand(SR2ReaderViewNavigationSearchClose)
  }

  private fun configureSearch() {
    val search = this.toolbar.menu.findItem(R.id.readerMenuSearch)
    this.searchView = search.actionView as SearchView

    this.searchView.inputType = InputType.TYPE_CLASS_TEXT
    this.searchView.isIconified = false
    this.searchView.setOnCloseListener {
      SR2ReaderModel.controller().submitCommand(SR2Command.CancelSearch)

      if (this.searchView.query.isNotBlank()) {
        this.searchView.setQuery("", false)
      } else {
        SR2ReaderModel.controller().submitCommand(
          SR2Command.HighlightTerms(
            searchingTerms = SR2ReaderModel.searchTerm,
            clearHighlight = true,
          ),
        )

        SR2ReaderModel.searchTerm = ""
        this.close()
      }
      true
    }

    this.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String): Boolean {
        SR2ReaderModel.controller().submitCommand(SR2Command.Search(query))
        this@SR2SearchFragment.searchView.clearFocus()
        return true
      }

      override fun onQueryTextChange(newText: String): Boolean {
        return true
      }
    })

    this.showKeyboard()

    SR2ReaderModel.searchResult
      .onEach { this.searchAdapter.submitData(it) }
      .launchIn(this.viewLifecycleOwner.lifecycleScope)

    SR2ReaderModel.searchLocators
      .onEach { this.noResultLabel.isVisible = it.isEmpty() }
      .launchIn(this.viewLifecycleOwner.lifecycleScope)
  }

  override fun onStart() {
    super.onStart()

    this.subscriptions =
      CompositeDisposable()
    this.subscriptions.add(
      SR2ReaderModel.controllerEvents.subscribe(this::onControllerEvent),
    )
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.dispose()
  }

  @UiThread
  private fun onControllerEvent(event: SR2Event) {
    SR2UIThread.checkIsUIThread()

    when (event) {
      is SR2ReadingPositionChanged,
      is SR2BookmarkDeleted,
      is SR2BookmarkCreated,
      is SR2ThemeChanged,
      is SR2Error.SR2ChapterNonexistent,
      is SR2Error.SR2WebViewInaccessible,
      is SR2OnCenterTapped,
      is SR2CommandExecutionRunningLong,
      is SR2CommandExecutionStarted,
      is SR2CommandExecutionSucceeded,
      is SR2CommandExecutionFailed,
      is SR2ExternalLinkSelected,
      -> {
        // Nothing
      }

      is SR2CommandSearchResults -> {
        SR2ReaderModel.consumeSearchResults(event)
      }
    }
  }

  private fun showKeyboard() {
    this.searchView.post {
      this.searchView.requestFocus()
      (this.requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
        ?.showSoftInput(this.requireView().findFocus(), 0)
    }
  }
}
