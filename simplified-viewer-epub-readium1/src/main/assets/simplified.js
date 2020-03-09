function Simplified() {

  var singleTouchGesture = false;
  var startX = 0;
  var startY = 0;

  var handleTouchStart = function(event) {
    singleTouchGesture = event.touches.length == 1;

    var touch = event.changedTouches[0];

    if(touch.target.nodeName.toUpperCase() === "A") {
      return;
    }

    startX = touch.screenX;
    startY = touch.screenY;

    // We must call this or else `ontouchcancel` can fire instead of the
    // desired `ontouchend` event on Android 4.X. See this for more info:
    // https://developer.android.com/guide/webapps/migrating.html#TouchCancel
    event.preventDefault();
  };

  var handleTouchEnd = function(event) {
    if(!singleTouchGesture) return;

    var touch = event.changedTouches[0];

    if(touch.target.nodeName.toUpperCase() === "A") {
      return;
    }

    var maxScreenX = window.orientation === 0 || window.orientation == 180
                   ? screen.width
                   : screen.height;

    var relativeDistanceX = (touch.screenX - startX) / maxScreenX;

    // Tap to turn.
    if(Math.abs(relativeDistanceX) < 0.1) {
      var position = touch.screenX / maxScreenX;
      if(position <= 0.2) {
        ReadiumSDK.reader.openPageLeft();
      } else if(position >= 0.8) {
        ReadiumSDK.reader.openPageRight();
      }
      event.stopPropagation();
      event.preventDefault();
      return;
    } else {
      var slope = (touch.screenY - startY) / (touch.screenX - startX);
      // Swipe to turn.
      if(Math.abs(slope) <= 0.5) {
        if(relativeDistanceX > 0) {
          ReadiumSDK.reader.openPageLeft();
        } else {
          ReadiumSDK.reader.openPageRight();
        }
        event.stopPropagation();
        event.preventDefault();
        return;
      }
    }
  };

  // Handle gestures between the inner content and the edge of the screen.
  document.addEventListener("touchstart", handleTouchStart, false);
  document.addEventListener("touchend", handleTouchEnd, false);

  /**
   * FIXME: The following two functions are only here to provide slightly better
   * support for returning to a particular spot in the book. This is due to the
   * inconsistencies and the unreliability of Readium's CFI returning us to the same
   * spot after certain UI actions take place. Previously, changing the font size (for
   * example) constantly returned the user to the first page of the current chapter.
   * This update is not perfect but stays within 2 to 3 pages of the current
   * bookmark CFI captured before the UI update.
   */

  /**
   * getReadiumCFI
   * When there's a font size or font family change in the UI,
   * keep track of the CFI of the current page.
   */
  this.getReadiumCFI = function () {
    var currentView = ReadiumSDK.reader.getCurrentView();
    var bookMark = currentView.bookmarkCurrentPage();

    this.currentViewCFI = bookMark;
  };

  /**
   * updateCFI
   * If there's an existing CFI that we are tracking, open the reader to that CFI.
   * TODO: the CFI works well for the previous font size or font family. When switching to
   * a new font size or font family, the CFI is no longer exactly the same as before.
   */
  this.setReadiumCFI = function () {
    var currentViewCFI = this.currentViewCFI || undefined;
    if (currentViewCFI) {
      setTimeout(function () {
        ReadiumSDK.reader.openSpineItemElementCfi(currentViewCFI.idref, currentViewCFI.contentCFI);
      }, 250);
    }
  };

  // This should be called by the host whenever the page changes. This is
  // because a change in the page can mean a change in the iframe and thus
  // requires resetting properties.
  this.pageDidChange = function() {
    var contentDocument = window.frames["epubContentIframe"].contentDocument;
    if(contentDocument === undefined) {
      // Support Android 4.X.
      contentDocument = window.frames["epubContentIframe"].document;
    }
    // Handle gestures for the inner content.
    contentDocument.removeEventListener("touchstart", handleTouchStart);
    contentDocument.addEventListener("touchstart", handleTouchStart, false);
    contentDocument.removeEventListener("touchend", handleTouchEnd);
    contentDocument.addEventListener("touchend", handleTouchEnd, false);
    // Set up the page turning animation. This works because Readium adjusts the
    // `left` property of its iframe document to advance pages.
    contentDocument.documentElement.style["transition"] = "left 0.2s";
    // Tell the browser to expect the `left` property to change. This allows the
    // the browser to set things up in advance to achieve better performance
    // whenever the property is altered.
    contentDocument.documentElement.style["will-change"] = "left";

    // Load the font family if it's not already there when the new
    // page gets rendered.
    this.linkOpenDyslexicFonts(contentDocument);
  };

  /**
   * TODO:
   * Even though the OpenDyslexic3 font is now added to the renderer,
   * there are still underlying issues with rendering this font. Specifically,
   * pages at the end of chapters get lost when using this
   * (and only this) font.
   */

  /**
   * linkOpenDyslexicFonts
   * If the current Open Dyslexic font is loaded on the page, do not
   * load it again. If there's a currently selected font by the user,
   * update the style to reflect it. This is needed when going from
   * chapter to chapter.
   */
  this.linkOpenDyslexicFonts = function(innerDocument) {
    var id = 'simplified-opendyslexic';
    if (innerDocument.getElementById(id)) {
      return;
    }
    var styleElement = document.createElement('style');
    styleElement.id = id;
    styleElement.textContent =
      "@font-face { \
        font-family: 'OpenDyslexic3'; \
        src: url('OpenDyslexic3-Regular.ttf'); \
        font-weight: normal; \
      }";
    innerDocument.head.appendChild(styleElement);
  }
}

simplified = new Simplified();
