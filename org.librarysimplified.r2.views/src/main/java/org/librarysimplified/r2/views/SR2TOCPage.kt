package org.librarysimplified.r2.views

import androidx.fragment.app.Fragment

data class SR2TOCPage(
  val title: String,
  val fragmentConstructor: () -> Fragment)
