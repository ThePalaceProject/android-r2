package org.librarysimplified.r2.vanilla.internal

import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.format.FormatSniffer
import org.readium.r2.shared.util.format.FormatSpecification
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.mediatype.MediaType

/**
 * A sniffer for OTF fonts. Without this sniffer in place, fonts will often fail to load in
 * the web view but not before causing the web view to wait a couple of seconds before failing.
 * This can result in an extra couple of seconds wait for each font before chapter content
 * is considered "loaded" and therefore visible.
 */

class OTFSniffer : FormatSniffer {
  private object Otf : Specification

  private val otfFormat =
    Format(
      specification = FormatSpecification(Otf),
      fileExtension = FileExtension("otf"),
      mediaType = MediaType.OTF,
    )

  override fun sniffHints(hints: FormatHints): Format? {
    if (
      hints.hasFileExtension("otf") ||
      hints.hasMediaType(MediaType.OTF.toString())
    ) {
      return this.otfFormat
    }
    return null
  }
}
