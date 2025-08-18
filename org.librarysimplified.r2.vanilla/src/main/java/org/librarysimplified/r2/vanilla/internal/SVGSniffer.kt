package org.librarysimplified.r2.vanilla.internal

import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.FormatSniffer
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.mediatype.MediaType

class SVGSniffer : FormatSniffer {

  private object Svg : Specification

  private val svgFormat = Format(
    specification = FormatSpecification(Svg),
    fileExtension = FileExtension("svg"),
    mediaType = MediaType.SVG,
  )

  override fun sniffHints(
    hints: FormatHints,
  ): Format? {
    if (
      hints.hasFileExtension("svg") ||
      hints.hasMediaType(MediaType.SVG.toString())
    ) {
      return this.svgFormat
    }
    return null
  }
}
