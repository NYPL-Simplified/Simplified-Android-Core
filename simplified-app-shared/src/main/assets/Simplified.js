function Simplified() {

  // Swipe and edge tap gesture detection for page turning was handled here before,
  // but was moved into a readium plugin.

  this.pageDidChange = function() {
    // Do something when there's a page change.
  };
  
}

simplified = new Simplified();