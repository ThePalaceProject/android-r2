package org.librarysimplified.r2.vanilla.internal

import org.readium.r2.shared.fetcher.LazyResource
import org.readium.r2.shared.fetcher.Resource
import org.readium.r2.shared.fetcher.ResourceTry
import org.readium.r2.shared.fetcher.TransformingResource
import org.readium.r2.shared.fetcher.mapCatching
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.epub.layoutOf
import org.readium.r2.shared.publication.presentation.presentation

internal class SR2HtmlInjector(private val manifest: Manifest) {

  fun transform(resource: Resource): Resource = LazyResource {

    val link = resource.link()
    if (link.mediaType.isHtml)
      inject(resource)
    else
      resource
  }

  private suspend fun inject(resource: Resource): Resource = object : TransformingResource(resource) {

    override suspend fun transform(data: ResourceTry<ByteArray>): ResourceTry<ByteArray> =
      resource.read().mapCatching {
        val trimmedText = it.toString(link().mediaType.charset ?: Charsets.UTF_8).trim()
        val res = if (manifest.metadata.presentation.layoutOf(link()) == EpubLayout.REFLOWABLE)
          injectReflowableHtml(trimmedText)
        else
          injectFixedLayoutHtml(trimmedText)
        res.toByteArray()
      }

  }

  private fun injectReflowableHtml(content: String): String {
    var resourceHtml = content
    // Inject links to css and js files
    val head = regexForOpeningHTMLTag("head").find(resourceHtml, 0)
    var beginHeadIndex = resourceHtml.indexOf("<head>", 0, false) + 6
    var endHeadIndex = resourceHtml.indexOf("</head>", 0, true)
    if (endHeadIndex == -1)
      return content

    val layout = ReadiumCssLayout(manifest.metadata)

    val endIncludes = mutableListOf<String>()
    val beginIncludes = mutableListOf<String>()
    beginIncludes.add("<meta name=\"viewport\" content=\"width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=0\" />")

    beginIncludes.add(getHtmlLink("/assets/readium-css/${layout.readiumCSSPath}ReadiumCSS-before.css"))
    endIncludes.add(getHtmlLink("/assets/readium-css/${layout.readiumCSSPath}ReadiumCSS-after.css"))
    endIncludes.add(getHtmlScript("/assets/scripts/gestures.js"))
    endIncludes.add(getHtmlScript("/assets/scripts/utils.js"))
    endIncludes.add(getHtmlScript("/assets/scripts/crypto-sha256.js"))
    endIncludes.add(getHtmlScript("/assets/scripts/highlight.js"))

    for (element in beginIncludes) {
      resourceHtml = StringBuilder(resourceHtml).insert(beginHeadIndex, element).toString()
      beginHeadIndex += element.length
      endHeadIndex += element.length
    }
    for (element in endIncludes) {
      resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
      endHeadIndex += element.length
    }
    resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, getHtmlFont(fontFamily = "OpenDyslexic", href = "/assets/fonts/OpenDyslexic-Regular.otf")).toString()
    resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, "<style>@import url('https://fonts.googleapis.com/css?family=PT+Serif|Roboto|Source+Sans+Pro|Vollkorn');</style>\n").toString()

    resourceHtml = applyDirectionAttribute(resourceHtml, manifest)

    return resourceHtml
  }

  private fun applyDirectionAttribute(resourceHtml: String, manifest: Manifest): String {
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

    val layout = ReadiumCssLayout(manifest.metadata)

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
    if (endHeadIndex == -1)
      return content
    val includes = mutableListOf<String>()
    includes.add(getHtmlScript("/assets/scripts/gestures.js"))
    includes.add(getHtmlScript("/assets/scripts/utils.js"))
    for (element in includes) {
      resourceHtml = StringBuilder(resourceHtml).insert(endHeadIndex, element).toString()
    }
    return resourceHtml
  }

  private fun getHtmlFont(fontFamily: String, href: String): String {
    val prefix = "<style type=\"text/css\"> @font-face{font-family: \"$fontFamily\"; src:url(\""
    val suffix = "\") format('truetype');}</style>\n"
    return prefix + href + suffix
  }

  private fun getHtmlLink(resourceName: String): String {
    val prefix = "<link rel=\"stylesheet\" type=\"text/css\" href=\""
    val suffix = "\"/>\n"
    return prefix + resourceName + suffix
  }

  private fun getHtmlScript(resourceName: String): String {
    val prefix = "<script type=\"text/javascript\" src=\""
    val suffix = "\"></script>\n"

    return prefix + resourceName + suffix
  }
}
