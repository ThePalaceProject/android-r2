package org.librarysimplified.r2.views.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.readium.r2.shared.Search
import org.readium.r2.shared.publication.Locator

@OptIn(Search::class)
class SR2SearchPagingSource(
  private val pagingSourceListener: SR2SearchPagingSourceListener,
) : PagingSource<Unit, Locator>() {

  override val keyReuseSupported: Boolean get() = true

  override fun getRefreshKey(state: PagingState<Unit, Locator>): Unit? = null

  override suspend fun load(params: LoadParams<Unit>): LoadResult<Unit, Locator> {
    return try {
      val page = pagingSourceListener.getIteratorNext().getOrNull()
      LoadResult.Page(
        data = page?.locators.orEmpty(),
        prevKey = null,
        nextKey = if (page == null) {
          null
        } else {
          Unit
        },
      )
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }
}
