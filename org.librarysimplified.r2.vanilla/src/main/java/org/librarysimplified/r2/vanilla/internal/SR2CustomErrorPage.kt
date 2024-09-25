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
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta http-equiv="content-type" content="application/xhtml+xml; charset=utf-8"/>
    <title>An Error Occurred</title>
    <style type="text/css">
a {
  text-decoration: none;
}   
.error {
  margin: 3em;
}
.errorHeader svg {
  float: left;
}
.errorHeader h1 {
  margin-left: 1.5em;
}
.errorHR {
  background-color: #eeeeee;
  height:           1px;
  width:            100%;
}
.errorTable {
  margin-top:    2em;
  margin-bottom: 3em;
  line-height:   1.5em;
}
.errorKey {
  font-weight: bold;
  vertical-align: top;
}
.errorValue {
  padding-left: 1em;
  vertical-align: top;
}
.errorSupport {
  margin-top:    2em;
  margin-bottom: 2em;
}
.errorReturn {

}
    </style>
  </head>
  <body>
    <div class="error">
      <div class="errorHeader">
        <svg xmlns="http://www.w3.org/2000/svg"
          fill="#b0220c"
          viewBox="0 0 512 512"
          width="32"
          height="32">
          <!--! Font Awesome Pro 6.5.2 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license (Commercial License) Copyright 2024 Fonticons, Inc. -->
          <path d="M27.4 432L0 480H55.3 456.7 512l-27.4-48L283.6 80.4 256 32 228.4 80.4 27.4 432zm401.9 0H82.7L256 128.7 429.3 432zM232 296v24h48V296 208H232v88zm48 104V352H232v48h48z"/>
        </svg>
        <h1>Error</h1>
        <b>There appears to be a problem with this book.</b>
      </div>
      """.trimIndent(),
    )

    val escaper = XmlEscapers.xmlContentEscaper()
    text.append("<p>")
    text.append(escaper.escape(this.message))
    text.append("</p>\n")

    if (this.errorAttributes.isNotEmpty()) {
      text.append("<table class=\"errorTable\">\n")
      for (entry in this.errorAttributes.entries) {
        text.append("<tr>\n")
        text.append("  <td class=\"errorKey\">")
        text.append(escaper.escape(entry.key))
        text.append("</td>\n")
        text.append("  <td class=\"errorValue\">")
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
