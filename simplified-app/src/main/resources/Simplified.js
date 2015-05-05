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
  };
  
  var handleTouchEnd = function(event) {
    if(!singleTouchGesture) {
      return;
    }
    
    var touch = event.changedTouches[0];
    if(touch.target.nodeName.toUpperCase() === "A") {
      return;
    }
    
    var maxScreenX = screen.width;
    var relativeDistanceX = (touch.screenX - startX) / maxScreenX;

    // Tap to turn.
    if(Math.abs(relativeDistanceX) < 0.1) {
      var position = touch.screenX / maxScreenX;
      if(position <= 0.2) {
        window.location = "simplified:gesture-left";
      } else if(position >= 0.8) {
        window.location = "simplified:gesture-right";
      } else {
        window.location = "simplified:gesture-center";
      }
      event.stopPropagation();
      event.preventDefault();
      return;
    } else {
      var slope = (touch.screenY - startY) / (touch.screenX - startX);
      // Swipe to turn.
      if(Math.abs(slope) <= 0.5) {
        if(relativeDistanceX > 0) {
          window.location = "simplified:gesture-left";
        } else {
          window.location = "simplified:gesture-right";
        }
        event.stopPropagation();
        event.preventDefault();
        return;
      }
    }
  };
  
  // Handles gestures between inner content and edge of screen.
  document.addEventListener("touchstart", handleTouchStart, false);
  document.addEventListener("touchend", handleTouchEnd, false);
  document.documentElement.style.webkitTouchCallout = "none";
  document.documentElement.style.webkitUserSelect = "none";
  
  // This should be called by the host whenever the page changes. This is because a change in the
  // page can mean a change in the iframe and thus requires resetting properties.
  this.pageDidChange = function() {
    // Disable selection.
    window.frames["epubContentIframe"].document.documentElement.style.webkitTouchCallout = "none";
    window.frames["epubContentIframe"].document.documentElement.style.webkitUserSelect = "none";
    // Handles gestures for the inner content.
    window.frames["epubContentIframe"].removeEventListener("touchstart", handleTouchStart);
    window.frames["epubContentIframe"].addEventListener("touchstart", handleTouchStart, false);
    window.frames["epubContentIframe"].removeEventListener("touchend", handleTouchEnd);
    window.frames["epubContentIframe"].addEventListener("touchend", handleTouchEnd, false);
  };
  
}

simplified = new Simplified();