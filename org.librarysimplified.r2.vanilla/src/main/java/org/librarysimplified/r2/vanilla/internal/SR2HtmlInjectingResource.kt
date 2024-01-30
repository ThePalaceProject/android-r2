package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.vanilla.internal.SR2Controller.Companion.PREFIX_ASSETS
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.flatMap
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource

class SR2HtmlInjectingResource(
  private val publication: Publication,
  private val mediaType: MediaType,
  resource: Resource,
) : TransformingResource(resource) {

  override suspend fun transform(
    data: Try<ByteArray, ReadError>,
  ): Try<ByteArray, ReadError> {
    if (!this.mediaType.isHtml) {
      return data
    }
    return data.flatMap { bytes ->
      transformBytes(bytes)
    }
  }

  private fun transformBytes(bytes: ByteArray): Try<ByteArray, ReadError> {
    val charset =
      this.mediaType.charset ?: Charsets.UTF_8
    val trimmedText =
      bytes.toString(charset).trim()

    val outputBytes = if (isReflowable()) {
      injectReflowableHtml(trimmedText)
    } else {
      injectFixedLayoutHtml(trimmedText)
    }
    return Try.success(outputBytes.toByteArray(charset))
  }

  private fun isReflowable(): Boolean {
    return this.publication.metadata.presentation.layout == EpubLayout.REFLOWABLE
  }

  private fun injectReflowableHtml(
    content: String,
  ): String {
    var resourceHtml = content
    // Inject links to css and js files
    val head = regexForOpeningHTMLTag("head").find(resourceHtml, 0)
    var beginHeadIndex = resourceHtml.indexOf("<head>", 0, false) + 6
    var endHeadIndex = resourceHtml.indexOf("</head>", 0, true)
    if (endHeadIndex == -1) {
      return content
    }

    val layout = SR2ReadiumCssLayout(this.publication.metadata)
    val endIncludes = mutableListOf<String>()
    val beginIncludes = mutableListOf<String>()
    beginIncludes.add("<meta name=\"viewport\" content=\"width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\" />")

    beginIncludes.add(linkToCSS("readium-css/${layout.readiumCSSPath}ReadiumCSS-before.css"))
    endIncludes.add(linkToCSS("readium-css/${layout.readiumCSSPath}ReadiumCSS-after.css"))
    endIncludes.add(linkToScript("scripts/gestures.js"))
    endIncludes.add(linkToScript("scripts/utils.js"))
    endIncludes.add(linkToScript("scripts/crypto-sha256.js"))
    endIncludes.add(linkToScript("scripts/highlight.js"))

    for (element in beginIncludes) {
      resourceHtml = StringBuilder(resourceHtml).insert(beginHeadIndex, element).toString()
      beginHeadIndex += element.length
      endHeadIndex += element.length
    }
    for (element in endIncludes) {
      resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
      endHeadIndex += element.length
    }
    resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, getHtmlFont(fontFamily = "OpenDyslexic", href = "/fonts/OpenDyslexic-Regular.otf")).toString()
    resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, "<style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>\n").toString()
    resourceHtml = applyDirectionAttribute(resourceHtml, this.publication.manifest)
    return resourceHtml
  }

  private fun applyDirectionAttribute(
    resourceHtml: String,
    manifest: Manifest,
  ): String {
    var resourceHtml1 = resourceHtml
    fun addRTLDir(tagName: String, html: String): String {
      return regexForOpeningHTMLTag(tagName).find(html, 0)?.let { result ->
        Regex("""dir=""").find(result.value, 0)?.let {
          html
        } ?: run {
          val beginHtmlIndex = html.indexOf("<$tagName", 0, true) + 5
          StringBuilder(html).insert(beginHtmlIndex, " dir=\"rtl\"").toString()
        }
      } ?: run {
        html
      }
    }

    val layout = SR2ReadiumCssLayout(manifest.metadata)

    if (layout.cssId == "rtl") {
      resourceHtml1 = addRTLDir("html", resourceHtml1)
      resourceHtml1 = addRTLDir("body", resourceHtml1)
    }

    return resourceHtml1
  }

  private fun regexForOpeningHTMLTag(name: String): Regex =
    Regex("""<$name.*?>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

  private fun injectFixedLayoutHtml(content: String): String {
    var resourceHtml = content
    val endHeadIndex = resourceHtml.indexOf("</head>", 0, true)
    if (endHeadIndex == -1) {
      return content
    }
    val includes = mutableListOf<String>()
    includes.add(linkToScript("scripts/gestures.js"))
    includes.add(linkToScript("scripts/utils.js"))
    for (element in includes) {
      resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
    }
    return resourceHtml
  }

  private fun getHtmlFont(
    fontFamily: String,
    href: String,
  ): String {
    val prefix = "<style type=\"text/css\"> @font-face{font-family: \"$fontFamily\"; src:url(\""
    val suffix = "\") format('truetype');}</style>\n"
    return prefix + href + suffix
  }

  private fun linkToCSS(resourceName: String): String {
    val prefix = "<link rel=\"stylesheet\" type=\"text/css\" href=\""
    val suffix = "\"/>\n"
    return prefix + PREFIX_ASSETS + resourceName + suffix
  }

  private fun linkToScript(resourceName: String): String {
    val prefix = "<script type=\"text/javascript\" src=\""
    val suffix = "\"></script>\n"
    return prefix + PREFIX_ASSETS + resourceName + suffix
  }
}
