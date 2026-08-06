package org.librarysimplified.r2.vanilla.internal

import org.librarysimplified.r2.vanilla.internal.SR2Controller.Companion.PREFIX_ASSETS
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
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
  override suspend fun transform(data: Try<ByteArray, ReadError>): Try<ByteArray, ReadError> {
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

    val outputText =
      if (isReflowable()) {
        injectReflowableHtml(trimmedText)
      } else {
        injectFixedLayoutHtml(trimmedText)
      }
    return Try.success(outputText.toByteArray(charset))
  }

  private fun isReflowable(): Boolean = this.publication.metadata.layout == Layout.REFLOWABLE

  private fun injectReflowableHtml(content: String): String {
    val headEndIndex = content.indexOf("</head>", 0, true)
    if (headEndIndex == -1) {
      return content
    }
    val layout =
      SR2ReadiumCssLayout(this.publication.metadata)
    val headStartIndex =
      content.indexOf("<head>", 0, false) + 6
    val beforeHeadContent =
      content.substring(0, headStartIndex)
    val headContent =
      content.substring(headStartIndex, headEndIndex)
    val afterHeadContent =
      content.substring(headEndIndex)

    val metaViewport =
      buildString {
        append("<meta ")
        append("name=\"viewport\" ")
        append("content=\"")
        append("width=device-width, ")
        append("height=device-height, ")
        append("initial-scale=1.0, ")
        append("maximum-scale=1.0, ")
        append("user-scalable=0")
        append("\"")
        append("/>")
      }

    val injectedHeadContent =
      buildString {
        append(headContent)
        append(metaViewport)
        append(linkToCSS("readium-css/${layout.readiumCSSPath}ReadiumCSS-before.css"))
        append(linkToCSS("readium-css/${layout.readiumCSSPath}ReadiumCSS-after.css"))
        append("<script>epubLayout=\"SR2_REFLOWABLE\";</script>")
        append(linkToScript("scripts/sr2.js"))
        append(getHtmlFont(fontFamily = "OpenDyslexic", href = "fonts/OpenDyslexic-Regular.otf"))
        append("<style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>")
        append("\n")
      }

    val resourceHtml =
      buildString {
        append(beforeHeadContent)
        append(injectedHeadContent)
        append(afterHeadContent)
      }

    return applyDirectionAttribute(
      resourceHtml,
      this.publication.manifest,
    )
  }

  private fun applyDirectionAttribute(
    resourceHtml: String,
    manifest: Manifest,
  ): String {
    var resourceHtml1 = resourceHtml

    fun addRTLDir(
      tagName: String,
      html: String,
    ): String =
      regexForOpeningHTMLTag(tagName).find(html, 0)?.let { result ->
        Regex("""dir=""").find(result.value, 0)?.let {
          html
        } ?: run {
          val beginHtmlIndex = html.indexOf("<$tagName", 0, true) + 5
          StringBuilder(html).insert(beginHtmlIndex, " dir=\"rtl\"").toString()
        }
      } ?: run {
        html
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
    val headEndIndex = content.indexOf("</head>", 0, true)
    if (headEndIndex == -1) {
      return content
    }

    val beforeHeadContent =
      content.substring(0, headEndIndex)
    val afterHeadContent =
      content.substring(headEndIndex)

    return buildString {
      append(beforeHeadContent)
      append("<script>epubLayout=\"SR2_FIXED\";</script>")
      append(linkToScript("scripts/sr2.js"))
      append(afterHeadContent)
    }
  }

  private fun getHtmlFont(
    fontFamily: String,
    href: String,
  ): String {
    val prefix = "<style type=\"text/css\"> @font-face{font-family: \"$fontFamily\"; src:url(\""
    val suffix = "\") format('truetype');}</style>\n"
    return buildString {
      append(prefix)
      append(PREFIX_ASSETS)
      append(href)
      append(suffix)
    }
  }

  private fun linkToCSS(resourceName: String): String {
    val prefix = "<link rel=\"stylesheet\" type=\"text/css\" href=\""
    val suffix = "\"/>\n"
    return buildString {
      append(prefix)
      append(PREFIX_ASSETS)
      append(resourceName)
      append(suffix)
    }
  }

  private fun linkToScript(resourceName: String): String {
    val prefix = "<script type=\"text/javascript\" src=\""
    val suffix = "\"></script>\n"
    return buildString {
      append(prefix)
      append(PREFIX_ASSETS)
      append(resourceName)
      append(suffix)
    }
  }
}
