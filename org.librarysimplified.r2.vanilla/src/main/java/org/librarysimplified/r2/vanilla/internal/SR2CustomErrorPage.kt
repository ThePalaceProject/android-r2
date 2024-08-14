package org.librarysimplified.r2.vanilla.internal

import android.webkit.WebResourceResponse
import com.google.common.xml.XmlEscapers
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class SR2CustomErrorPage private constructor(
  private val errorAttributes: Map<String, String>,
  private val message: String,
) {

  companion object {
    fun create(
      errorAttributes: Map<String, String>,
      message: String,
    ): SR2CustomErrorPage {
      return SR2CustomErrorPage(errorAttributes.toMap(), message)
    }
  }

  fun toText(): String {
    val text = StringBuilder()
    text.append(
      """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html
  PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
  <head>
    <meta http-equiv="content-type" content="application/xhtml+xml; charset=utf-8"/>
    <title>An Error Occurred</title>
  </head>
  <body>
    <div>
      <h1>Error</h1>
      <h2>There appears to be a problem with this book.</h2>
      """.trimIndent(),
    )

    val escaper = XmlEscapers.xmlContentEscaper()
    text.append("<p>")
    text.append(escaper.escape(this.message))
    text.append("</p>\n")

    if (this.errorAttributes.isNotEmpty()) {
      text.append("<table>\n")
      for (entry in this.errorAttributes.entries) {
        text.append("<tr>\n")
        text.append("  <td>")
        text.append(escaper.escape(entry.key))
        text.append("</td>\n")
        text.append("  <td>")
        text.append(escaper.escape(entry.value))
        text.append("</td>\n")
        text.append("</tr>\n")
      }
      text.append("</table>")
    }

    text.append(
      """
    </div>
  </body>
</html>
      """.trimIndent(),
    )
    return text.toString()
  }

  fun toResourceResponse(): WebResourceResponse {
    return WebResourceResponse(
      "application/xhtml+xml",
      "UTF-8",
      ByteArrayInputStream(this.toText().toByteArray(charset = StandardCharsets.UTF_8)),
    )
  }
}
