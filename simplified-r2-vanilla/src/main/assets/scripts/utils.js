/* global Android */
"use strict";

// Notify native code that the page has loaded.
window.addEventListener("load", function() { // on page load
    // Notify native code that the page is loaded.
    if (isScrollModeEnabled()) {
        console.log("last_known_scrollY_position " + last_known_scrollY_position);
    } else {
        console.log("last_known_scrollX_position " + last_known_scrollX_position);
    }
    onReadingPositionChanged()
}, false);

var last_known_scrollX_position = 0;
var last_known_scrollY_position = 0;
var ticking = false;

// Returns the page currently displayed for this chapter as an int [1 - n].
var getCurrentPage = function() {
    return Math.abs(window.scrollX / window.innerWidth) + 1;
}

// Returns the page count for this chapter as an int.
var getPageCount = function() {
    return Math.abs(document.scrollingElement.scrollWidth / window.innerWidth);
}

// Position in range [0 - 1].
var onReadingPositionChanged = function() {
    var currentPage = getCurrentPage();
    var pageCount = getPageCount();
    console.log("current_page " + currentPage + ", page_count " + pageCount);
    Android.onReadingPositionChanged(currentPage, pageCount);
};

var isScrollModeEnabled = function() {
    return document.documentElement.style.getPropertyValue("--USER__scroll").toString().trim() == "readium-scroll-on";
}

window.addEventListener("scroll", function() {
    last_known_scrollY_position = window.scrollY / document.scrollingElement.scrollHeight;
    last_known_scrollX_position = Math.abs(window.scrollX / document.scrollingElement.scrollWidth);
    console.log("last_known_scrollX_position " + last_known_scrollX_position);
    console.log("last_known_scrollY_position " + last_known_scrollY_position);
    if (!ticking) {
        window.requestAnimationFrame(function() {
            onReadingPositionChanged();
            ticking = false;
        });
    }
    ticking = true;
});

var scrollToPage = function(page) {
    console.log("scrollToPage " + page);

    var offset = window.innerWidth * page;

    document.scrollingElement.scrollLeft = snapOffset(offset);
    last_known_scrollX_position = window.scrollX / document.scrollingElement.scrollWidth;
    onReadingPositionChanged();

    return document.scrollingElement.scrollLeft;
};

// Scroll to the given TagId in document and snap.
var scrollToId = function(id) {
    var element = document.getElementById(id);
    var elementOffset = element.scrollLeft; // element.getBoundingClientRect().left works for Gutenbergs books
    var offset = Math.round(window.scrollX + elementOffset);

    document.scrollingElement.scrollLeft = snapOffset(offset);
    onReadingPositionChanged();
};

// Position must be in the range [0 - 1], 0-100%.
var scrollToPosition = function(position) {
    console.log("ScrollToPosition " + position);
    if ((position < 0) || (position > 1)) {
        console.log("InvalidPosition");
        return;
    }
    var offset = document.scrollingElement.scrollWidth * position;

    document.scrollingElement.scrollLeft = snapOffset(offset);
    onReadingPositionChanged();
};

var scrollToEnd = function() {
    if (!isScrollModeEnabled()) {
        console.log("scrollToEnd " + document.scrollingElement.scrollWidth);
        document.scrollingElement.scrollLeft = document.scrollingElement.scrollWidth;
    } else {
        console.log("scrollToBottom " + document.body.scrollHeight);
        document.scrollingElement.scrollTop = document.body.scrollHeight;
        window.scrollTo(0, document.body.scrollHeight);
    }
    onReadingPositionChanged();
};

var scrollToStart = function() {
    if (!isScrollModeEnabled()) {
        console.log("scrollToStart " + 0);
        document.scrollingElement.scrollLeft = 0;
    } else {
        console.log("scrollToTop " + 0);
        document.scrollingElement.scrollTop = 0;
        window.scrollTo(0, 0);
    }
    onReadingPositionChanged();
};

var scrollToPosition = function(position, dir) {
    console.log("ScrollToPosition " + position);
    if ((position < 0) || (position > 1)) {
        console.log("InvalidPosition");
        return;
    }

    if (!isScrollModeEnabled()) {
        var offset = 0;
        if (dir == "rtl") {
            offset = (-document.scrollingElement.scrollWidth + window.innerWidth) * (1.0 - position);
        } else {
            offset = document.scrollingElement.scrollWidth * position;
        }
        document.scrollingElement.scrollLeft = snapOffset(offset);
    } else {
        var offset = Math.round(document.body.scrollHeight * position);
        document.scrollingElement.scrollTop = offset;
        window.scrollTo(0, offset);
    }

    scrollLeft();
};

var scrollLeft = function() {
    console.log("scrollLeft");

    var offset = Math.round(window.scrollX - window.innerWidth);
    if (offset >= 0) {
        document.scrollingElement.scrollLeft = snapOffset(offset);
        last_known_scrollX_position = window.scrollX / document.scrollingElement.scrollWidth;
        onReadingPositionChanged();
        return "";
    } else {
        document.scrollingElement.scrollLeft = 0;
        return "edge"; // Need to previousDocument.
    }
};

var scrollLeftRTL = function() {
    console.log("scrollLeftRTL");

    var scrollWidth = document.scrollingElement.scrollWidth;
    var offset = Math.round(window.scrollX - window.innerWidth);
    var edge = -scrollWidth + window.innerWidth;

    if (window.innerWidth == scrollWidth) {
        // No scroll and default zoom
        return "edge";
    } else {
        // Scrolled and zoomed
        if (offset > edge) {
            document.scrollingElement.scrollLeft = snapOffset(offset);
            return 0;
        } else {
            var oldOffset = window.scrollX;
            document.scrollingElement.scrollLeft = edge;
            var diff = Math.abs(edge - oldOffset) / window.innerWidth;
            // In some case the scrollX cannot reach the position respecting to innerWidth
            if (diff > 0.01) {
                return 0;
            } else {
                return "edge";
            }
        }
    }
};

var scrollRight = function() {
    console.log("scrollRight");
    var offset = Math.round(window.scrollX + window.innerWidth);
    var scrollWidth = document.scrollingElement.scrollWidth;

    if (offset < scrollWidth) {
        console.log("offset < scrollWidth");

        document.scrollingElement.scrollLeft = snapOffset(offset);
        var newScrollPos = window.scrollX / document.scrollingElement.scrollWidth;
        if ((newScrollPos - last_known_scrollX_position) > 0.001) {
            last_known_scrollX_position = window.scrollX / document.scrollingElement.scrollWidth;
            onReadingPositionChanged();
        } else {
            var newoffset = Math.round(window.scrollX + window.innerWidth);
            document.scrollingElement.scrollLeft = snapOffset(newoffset);
            last_known_scrollX_position = window.scrollX / document.scrollingElement.scrollWidth;
            onReadingPositionChanged();
        }
        return "";
    } else {
        console.log("else");
        document.scrollingElement.scrollLeft = scrollWidth;
        last_known_scrollX_position = scrollWidth;
        return "edge"; // Need to nextDocument.
    }
};

var scrollRightRTL = function() {
    console.log("scrollRightRTL");

    var scrollWidth = document.scrollingElement.scrollWidth;
    var offset = Math.round(window.scrollX + window.innerWidth);
    var edge = 0;

    if (window.innerWidth == scrollWidth) {
        // No scroll and default zoom
        return "edge";
    } else {
        // Scrolled and zoomed
        if (offset < edge) {
            document.scrollingElement.scrollLeft = snapOffset(offset);
            return 0;
        } else {
            var oldOffset = window.scrollX;
            document.scrollingElement.scrollLeft = edge;
            var diff = Math.abs(edge - oldOffset) / window.innerWidth;
            // In some case the scrollX cannot reach the position respecting to innerWidth
            if (diff > 0.01) {
                return 0;
            } else {
                return "edge";
            }
        }
    }
};

// Snap the offset to the screen width (page width).
var snapOffset = function(offset) {
    var value = offset + 1;
    return value - (value % window.innerWidth);
};

/// User Settings.

// For setting user setting.
var setProperty = function(key, value) {
    var root = document.documentElement;
    root.style.setProperty(key, value);
    onReadingPositionChanged();  // Changes may impact reading position or page count.
};

// For removing user setting.
var removeProperty = function(key) {
    var root = document.documentElement;
    root.style.removeProperty(key);
    onReadingPositionChanged();  // Changes may impact reading position or page count.
};