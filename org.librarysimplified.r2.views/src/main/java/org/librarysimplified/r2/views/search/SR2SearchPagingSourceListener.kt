package org.librarysimplified.r2.views.search

import org.readium.r2.shared.Search
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.services.search.SearchTry

@OptIn(Search::class)
interface SR2SearchPagingSourceListener {
  suspend fun getIteratorNext(): SearchTry<LocatorCollection?>
}
