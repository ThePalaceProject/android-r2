package org.librarysimplified.r2.views.search

interface SR2SearchResultSectionListener {
  fun isStartOfSection(index: Int): Boolean

  fun sectionTitle(index: Int): String
}
