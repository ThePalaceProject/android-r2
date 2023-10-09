/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

var readium = (function() {
    // Catch JS errors to log them in the app.
    window.addEventListener("error", function(event) {
        Android.logError(event.message, event.filename, event.lineno);
    }, false);

    // Notify native code that the page has loaded.
    window.addEventListener("load", function(){ // on page load
        window.addEventListener("orientationchange", function() {
            onViewportWidthChanged();
            snapCurrentOffset();
        });
        onViewportWidthChanged();
    }, false);

    var pageWidth = 1;

    /*
     * Return the number of pages in paginated mode, or 1 for scrolling mode.
     */

    function getCurrentPageCount() {
        if (isScrollModeEnabled()) {
            return 1;
        }

        var scrollX       = window.scrollX;
        var documentWidth = document.scrollingElement.scrollWidth;
        var pageCountRaw  = Math.round(documentWidth / pageWidth);
        return Math.max(1, pageCountRaw);
    }

    function highlightSearchingTerms(searchingTerms, clearHighlight) {
      if (!document.body || typeof(document.body.innerHTML) == "undefined") {
        return false;
      }

      var bodyText = document.body.innerHTML;
      document.body.innerHTML = doHighlight(bodyText, searchingTerms, clearHighlight);

      return true;
    }

    function doHighlight(bodyText, searchingTerms, clearHighlight) {
      // find all occurrences of the searching terms in the given text and add some "highlight" tags
      // to them. We are not using a regular expression search because we want to filter out matches
      // that occur within HTML tags and script blocks, so we have to do a little extra validation
      var newText = "";
      var i = -1;
      var lcSearchingTerm = searchingTerms.toLowerCase();
      var lcBodyText = bodyText.toLowerCase();

      while (bodyText.length > 0) {
        i = lcBodyText.indexOf(lcSearchingTerm, i + 1);
        if (i < 0) {
          newText += bodyText;
          bodyText = "";
        } else {
          // skip anything inside an HTML tag
          if (bodyText.lastIndexOf(">", i) >= bodyText.lastIndexOf("<", i)) {
            // skip anything inside a <script> block
            if (lcBodyText.lastIndexOf("/script>", i) >= lcBodyText.lastIndexOf("<script", i)) {

              var highlightStartTag = "<font style=\"background-color:yellow;\">";
              var highlightEndTag = "</font>";

              if (clearHighlight) {
                indexHighlightStartTag = bodyText.indexOf(highlightStartTag, indexHighlightStartTag + 1);
                indexHighlightEndTag = bodyText.indexOf(highlightEndTag, indexHighlightEndTag + 1);
              } else {
                indexHighlightStartTag = -1
                indexHighlightEndTag = -1
              }

              if (indexHighlightStartTag !== -1 && indexHighlightEndTag !== -1) {
                newText += bodyText.substring(0, indexHighlightStartTag) + bodyText.substr(i, searchingTerms.length);
                bodyText = bodyText.substr(indexHighlightEndTag + highlightEndTag.length);
              } else {
                newText += bodyText.substring(0, i) + highlightStartTag + bodyText.substr(i, searchingTerms.length) + highlightEndTag;
                bodyText = bodyText.substr(i + searchingTerms.length);
              }
              lcBodyText = bodyText.toLowerCase();
              i = -1;
            }
          }
        }
      }

      return newText;
    }

    /*
     * Return the index of the current page (starting from 1) in paginated mode, or 1 for
     * scrolling mode.
     */

    function getCurrentPageIndex() {
        if (isScrollModeEnabled()) {
            return 1;
        }

        var scrollX       = window.scrollX;
        var pageIndexRaw  = Math.round(scrollX / pageWidth);
        var pageIndex1    = pageIndexRaw + 1;
        return Math.max(1, pageIndex1);
    }

    /*
     * Return `true` if the user is currently on the last page of the chapter. Always true
     * for scrolling mode.
     */

    function isOnLastPage() {
        var pageCount = getCurrentPageCount();
        var pageIndex = getCurrentPageIndex();
        return pageIndex == pageCount;
    }

    /*
     * Return `true` if the user is currently on the first page of the chapter. Always true
     * for scrolling mode.
     */

    function isOnFirstPage() {
        return getCurrentPageIndex() == 1;
    }

    /*
     * A function executed when the scroll position changes. This is used to calculate
     * page numbers, and will result in the last-read position being updated by the native
     * code.
     */

    function onScrollPositionChanged() {
      if (isScrollModeEnabled()) {
        var scrollY  = window.scrollY
        var height   = document.scrollingElement.scrollHeight;
        var progress = scrollY / height;

        Android.onReadingPositionChanged(progress, 1, 1);
        return;
      }

      var scrollX       = window.scrollX;
      var documentWidth = document.scrollingElement.scrollWidth;
      var progress      = scrollX / documentWidth;
      scrollToPosition(progress);

      var pageCount = getCurrentPageCount();
      var pageIndex = getCurrentPageIndex();
      Android.onReadingPositionChanged(progress, pageIndex, pageCount);
    }

    /*
     * A simple throttle used to prevent scroll events from being published too frequently.
     */

    var scrollThrottleTimeout = false;
    var scrollThrottle = (callback, time) => {
        if (scrollThrottleTimeout) {
            return;
        }

        scrollThrottleTimeout = true;
        setTimeout(() => {
            callback();
            scrollThrottleTimeout = false;
        }, time);
    }

    /*
     * Notify native code when the user scrolls. This is throttled in scrolling mode to prevent
     * updates from occurring too frequently, should the user fling the text somehow.
     */

    window.addEventListener("scroll", function() {
        if (isScrollModeEnabled()) {
          scrollThrottle(onScrollPositionChanged, 1000);
        } else {
          onScrollPositionChanged();
        }
    })

    function onViewportWidthChanged() {
        // We can't rely on window.innerWidth for the pageWidth on Android, because if the
        // device pixel ratio is not an integer, we get rounding issues offsetting the pages.
        //
        // See https://github.com/readium/readium-css/issues/97
        // and https://github.com/readium/r2-navigator-kotlin/issues/146
        var width = Android.getViewportWidth()
        pageWidth = width / window.devicePixelRatio;
        setProperty("--RS__viewportWidth", "calc(" + width + "px / " + window.devicePixelRatio + ")")
    }

    function isScrollModeEnabled() {
        return document.documentElement.style.getPropertyValue("--USER__scroll").toString().trim() == 'readium-scroll-on';
    }

    function isRTL() {
        return document.body.dir.toLowerCase() == 'rtl';
    }

    // Scroll to the element with the given tag ID
    function scrollToId(id) {
        var element = document.getElementById(id);
        if (!element) {
            console.log("no element with id " + id)
            return;
        }
        console.log("scrolling to element " + element + " with id " + id)
        var rect = element.getBoundingClientRect();
        scrollToRect(rect);
    }

    function scrollToRect(rect) {
      if (isScrollModeEnabled()) {
        document.scrollingElement.scrollTop =
          rect.top + window.scrollY - window.innerHeight / 2;
      } else {
        document.scrollingElement.scrollLeft = snapOffset(
          rect.left + window.scrollX
        );
      }
    }

    // Position must be in the range [0 - 1], 0-100%.
    function scrollToPosition(position) {
        if ((position < 0) || (position > 1)) {
            throw "scrollToPosition() must be given a position from 0.0 to 1.0";
        }

        if (isScrollModeEnabled()) {
            var offset = document.scrollingElement.scrollHeight * position;
            document.scrollingElement.scrollTop = offset;
        } else {
            var documentWidth = document.scrollingElement.scrollWidth;
            var factor = isRTL() ? -1 : 1;
            var offset = documentWidth * position * factor;
            var offsetSnapped = snapOffset(offset);
            document.scrollingElement.scrollLeft = offsetSnapped;
        }
    }

    function scrollToStart() {
        if (!isScrollModeEnabled()) {
            document.scrollingElement.scrollLeft = 0;
        } else {
            document.scrollingElement.scrollTop = 0;
            window.scrollTo(0, 0);
        }
    }

    function scrollToEnd() {
        if (!isScrollModeEnabled()) {
            var factor = isRTL() ? -1 : 1;
            document.scrollingElement.scrollLeft = snapOffset(document.scrollingElement.scrollWidth * factor);
        } else {
            document.scrollingElement.scrollTop = document.body.scrollHeight;
            window.scrollTo(0, document.body.scrollHeight);
        }
    }

    // Returns false if the page is already at the left-most scroll offset.
    function scrollLeft() {
        if (isRTL() && isOnLastPage()) {
          return false;
        }
        if (isOnFirstPage()) {
          return false;
        }

        var documentWidth = document.scrollingElement.scrollWidth;
        var offset = window.scrollX - pageWidth;
        var minOffset = isRTL() ? -(documentWidth - pageWidth) : 0;
        return scrollToOffset(Math.max(offset, minOffset));
    }

    // Returns false if the page is already at the right-most scroll offset.
    function scrollRight() {
        if (isRTL() && isOnFirstPage()) {
          return false;
        }
        if (isOnLastPage()) {
          return false
        }

        var documentWidth = document.scrollingElement.scrollWidth;
        var offset = window.scrollX + pageWidth;
        var maxOffset = Math.max(documentWidth - pageWidth, pageWidth);
        if (isRTL()) {
          maxOffset = 0;
        }
        return scrollToOffset(Math.min(offset, maxOffset));
    }

    // Scrolls to the given left offset.
    // Returns false if the page scroll position is already close enough to the given offset.
    function scrollToOffset(offset) {
        if (isScrollModeEnabled()) {
            throw "Called scrollToOffset() with scroll mode enabled. This can only be used in paginated mode.";
        }

        var offsetThen = window.scrollX;
        var offsetNow = snapOffset(offset);
        document.scrollingElement.scrollLeft = offsetNow;

        var difference = Math.abs(offsetNow - offsetThen)
        return difference > 0;
    }

    // Snap the offset to the nearest multiple of the page width.
    function snapOffset(offset) {
        return pageWidth * Math.round(offset / pageWidth)
    }

    // Snaps the current offset to the nearest multiple of the page width.
    function snapCurrentOffset() {
        if (isScrollModeEnabled()) {
            return;
        }
        document.scrollingElement.scrollLeft = snapOffset(window.scrollX);
    }

    /// User Settings.

    // For setting user setting.
    function setProperty(key, value) {
        var root = document.documentElement;

        root.style.setProperty(key, value);
    }

    // For removing user setting.
    function removeProperty(key) {
        var root = document.documentElement;

        root.style.removeProperty(key);
    }

    // Public API used by the navigator.
    return {
        'highlightSearchingTerms': highlightSearchingTerms,
        'scrollToId': scrollToId,
        'scrollToPosition': scrollToPosition,
        'scrollLeft': scrollLeft,
        'scrollRight': scrollRight,
        'scrollToStart': scrollToStart,
        'scrollToEnd': scrollToEnd,
        'setProperty': setProperty,
        'removeProperty': removeProperty,
        'broadcastReadingPosition': onScrollPositionChanged
    };

})();
