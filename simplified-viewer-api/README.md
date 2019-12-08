org.librarysimplified.viewer.api
===

The `org.librarysimplified.viewer.api` module provides a generic API
for instantiating _viewers_ for books.

A _viewer_ is a piece of code that can display specific types of books.
For example, _Readium_ is a _viewer_ of EPUB books. The NYPL's AudioBook
API is a _viewer_ of audio books. Viewers must register themselves by
implementing an [SPI](../simplified-viewer-spi/README.md) and registering 
the implementation via `ServiceLoader`.

#### See Also

* [AudioBook](https://github.com/NYPL-Simplified/audiobook-android)
* [org.librarysimplified.viewer.audiobook](../simplified-viewer-audiobook/README.md)
* [org.librarysimplified.viewer.epub.readium1](../simplified-viewer-epub-readium1/README.md)
* [org.librarysimplified.viewer.pdf](../simplified-viewer-pdf/README.md)
* [org.librarysimplified.viewer.spi](../simplified-viewer-spi/README.md)
* [Readium](https://www.readium.org)
