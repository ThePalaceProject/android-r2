package org.librarysimplified.r2.views.search

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.ui_thread.SR2UIThread
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.SR2ReaderParameters
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationClose
import org.librarysimplified.r2.views.SR2ReaderViewModel
import org.librarysimplified.r2.views.SR2ReaderViewModelFactory

class SR2SearchFragment private constructor(
  private val parameters: SR2ReaderParameters,
) : Fragment() {

  companion object {
    fun create(parameters: SR2ReaderParameters): SR2SearchFragment {
      return SR2SearchFragment(parameters)
    }
  }

  private lateinit var controller: SR2ControllerType
  private lateinit var noResultLabel: TextView
  private lateinit var readerModel: SR2ReaderViewModel
  private lateinit var searchAdapter: SR2SearchResultAdapter
  private lateinit var searchResultsList: RecyclerView
  private lateinit var searchView: SearchView
  private lateinit var toolbar: Toolbar

  private var controllerEvents: Disposable? = null
  private var searchingTerms = ""

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R.layout.sr2_search, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.readerModel =
      ViewModelProvider(requireActivity(), SR2ReaderViewModelFactory(this.parameters))
        .get(SR2ReaderViewModel::class.java)

    this.controller =
      this.readerModel.get()!!

    this.toolbar =
      view.findViewById(R.id.searchToolbar)

    this.toolbar.inflateMenu(R.menu.sr2_search_menu)
    this.toolbar.setNavigationOnClickListener {
      close()
    }
    this.toolbar.setNavigationContentDescription(R.string.settingsAccessibilityBack)

    this.searchAdapter = SR2SearchResultAdapter(
      onItemClicked = { locator ->
        this.controller.submitCommand(
          SR2Command.HighlightTerms(
            searchingTerms = searchView.query.toString(),
            clearHighlight = false,
          ),
        )
        this.controller.submitCommand(
          SR2Command.OpenChapter(
            SR2Locator.SR2LocatorPercent(
              chapterHref = locator.href,
              chapterProgress = locator.locations.progression ?: 0.0,
            ),
          ),
        )
        close()
      },
    )

    this.searchResultsList = view.findViewById(R.id.searchResultsList)
    this.searchResultsList.apply {
      adapter = searchAdapter
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
      addItemDecoration(
        SR2SearchResultSectionItemDecoration(
          context,
          object : SR2SearchResultSectionListener {
            override fun isStartOfSection(index: Int): Boolean =
              readerModel.searchLocators.value.run {
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

            override fun sectionTitle(index: Int): String =
              readerModel.searchLocators.value.getOrNull(index)?.title.orEmpty()
          },
        ),
      )
      addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    this.noResultLabel = view.findViewById(R.id.noResultLabel)

    configureSearch()
  }

  override fun onStop() {
    this.controllerEvents?.dispose()
    if (::controller.isInitialized) {
      this.controller.viewDisconnect()
    }
    super.onStop()
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (!hidden) {
      showKeyboard()
    }
  }

  private fun close() {
    SR2UIThread.checkIsUIThread()

    this.readerModel.publishViewEvent(SR2ReaderViewNavigationClose)
  }

  private fun configureSearch() {
    val search = toolbar.menu.findItem(R.id.readerMenuSearch)
    searchView = search.actionView as SearchView

    searchView.inputType = InputType.TYPE_CLASS_TEXT
    searchView.isIconified = false

    searchView.setOnCloseListener {
      controller.submitCommand(SR2Command.CancelSearch)
      if (searchView.query.isNotBlank()) {
        searchView.setQuery("", false)
      } else {
        controller.submitCommand(
          SR2Command.HighlightTerms(
            searchingTerms = searchingTerms,
            clearHighlight = true,
          ),
        )

        searchingTerms = ""

        close()
      }
      true
    }
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String): Boolean {
        controller.submitCommand(SR2Command.Search(query))
        searchView.clearFocus()
        return true
      }

      override fun onQueryTextChange(newText: String): Boolean {
        return true
      }
    })

    showKeyboard()

    this.controllerEvents =
      this.readerModel.controllerEvents.subscribe(this::onControllerEvent)

    readerModel.searchResult
      .onEach { searchAdapter.submitData(it) }
      .launchIn(viewLifecycleOwner.lifecycleScope)

    readerModel.searchLocators
      .onEach { noResultLabel.isVisible = it.isEmpty() }
      .launchIn(viewLifecycleOwner.lifecycleScope)
  }

  private fun onControllerEvent(event: SR2Event) {
    SR2UIThread.checkIsUIThread()

    when (event) {
      is SR2Event.SR2ReadingPositionChanged,
      SR2Event.SR2BookmarkEvent.SR2BookmarksLoaded,
      is SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted,
      is SR2Event.SR2BookmarkEvent.SR2BookmarkTryToDelete,
      is SR2Event.SR2BookmarkEvent.SR2BookmarkCreated,
      SR2Event.SR2BookmarkEvent.SR2BookmarkFailedToBeDeleted,
      is SR2Event.SR2ThemeChanged,
      is SR2Event.SR2Error.SR2ChapterNonexistent,
      is SR2Event.SR2Error.SR2WebViewInaccessible,
      is SR2Event.SR2OnCenterTapped,
      is SR2Event.SR2BookmarkEvent.SR2BookmarkCreate,
      is SR2Event.SR2CommandEvent.SR2CommandExecutionRunningLong,
      is SR2Event.SR2CommandEvent.SR2CommandExecutionStarted,
      is SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionSucceeded,
      is SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionFailed,
      is SR2Event.SR2ExternalLinkSelected,
      -> {
        // Nothing
      }

      is SR2Event.SR2CommandEvent.SR2CommandSearchResults -> {
        this.readerModel.extractResults(event)
      }
    }
  }

  private fun showKeyboard() {
    searchView.post {
      searchView.requestFocus()
      (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
        ?.showSoftInput(requireView().findFocus(), 0)
    }
  }
}
