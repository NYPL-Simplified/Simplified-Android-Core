Note
====

This document is out-of-date but may still be useful as a starting point. Please
consult with NYPL for further assistance with branding.

Implementation
==============

The application code is divided up into a series of clearly
defined modules:

  * [simplified-assert](../simplified-assert/README.md)
  * [simplified-books-core](../simplified-books-core/README.md)
  * [simplified-checkstyle](../simplified-checkstyle/README.md)
  * [simplified-downloader-core](../simplified-downloader-core/README.md)
  * [simplified-files](../simplified-files/README.md)
  * [simplified-http-core](../simplified-http-core/README.md)
  * [simplified-json-core](../simplified-json-core/README.md)
  * [simplified-opds-core](../simplified-opds-core/README.md)
  * [simplified-rfc3339-core](../simplified-rfc3339-core/README.md)
  * [simplified-stack](../simplified-stack/README.md)
  * [simplified-tenprint](../simplified-tenprint/README.md)
  * [simplified-test-utilities](../simplified-test-utilities/README.md)

The current module, `simplified-app-shared` implements the Android
user interface code, and various Android-specific aspects not covered
by the other modules.

The application is roughly partitioned into `catalog` and `reader` packages
(and the code in these separate parts of the application actually run
in separate processes to better isolate the native code portion of
[Readium](http://github.com/nypl/readium-sdk)).

Catalog
=======

The `catalog` package implements the book management and browsing interface.

The application primarily consumes and displays [OPDS](http://opds-spec.org/)
Catalog feeds (via the OPDS package). If the feed contains any OPDS
_groups_, the feed is rendered as a series of horizontally scrolling
lanes:

![With groups](src/main/png/groups.png)

Otherwise, the feed is rendered as an indefinitely scrolling list of individual books:

![No groups](src/main/png/no_groups.png)

On startup, the application fetches and displays an initial feed from
a configurable URI. For feeds that contain _groups_, the title of the
resulting lanes are clickable and lead to further feeds. Currently,
the implementation creates one Android `Activity` per feed, with each
`Activity` storing an immutable stack of the feed URIs that led to the
currently displayed feed. This allows the user to navigate back to the
parent feed using the Android _up_ button: A new `Activity` is created
that points to the feed URI at the top of the current stack. The
creation of new `Activity` instances for all navigation is necessary
for Android to correctly populate the _back stack_ so that use of the
Android back button results in the user stepping backwards through
the _history_ of feeds that they have viewed, as opposed to simply
travelling back up through the catalog's hierarchy of feeds.

When the current stack of feeds is empty, the _action bar_ displays a
button that shows and hides the _navigation drawer_. When the current
stack of feeds is non-empty, the button instead acts as if the user
had pressed the Android _up_ button. The button displays a _caret_
if up navigation is possible, and three horizontal lines otherwise
(as per the Android design guidelines).

![Up button](src/main/png/up_button.png)

In order to avoid the user having to constantly reload feeds from the
network when travelling back through the catalog, feeds are cached
in a simple in-memory LRU cache. Finally, to give the illusion that
the catalog is a single `Activity`, the default animation used when
Android Activities are created and destroyed are supressed.

### Searching

Searching of feeds is provided by a server-side mechanism. A feed
may contain zero or more links that can respond to
[OpenSearch](http://www.opensearch.org/Specifications/OpenSearch/1.1)
queries. Responses are received in the form of standard feeds and
therefore no extra logic is required on the part of the application
to display search results.

If the current displayed feed contains any usable search links, an
entry is added to the Android _action bar_ that allows the user to
enter search queries.

![Searching](src/main/png/search.png)

### Book Covers

Book covers are loaded and cached from URIs contained
within the consumed feeds. The application uses the excellent
[Picasso](https://square.github.io/picasso/) image library from Square
for asynchronous image loading and both in-memory and on-disk image
caching. For some books, covers are not actually provided and instead
these are generated algorithmically by the
[simplified-tenprint](../simplified-tenprint/README.md)
package.

### My Books

The _My Books_ section of the application shows the books that users
have borrowed and/or downloaded. The list of books is displayed in
exactly the same manner as an ordinary feed that does not contain any
_groups_. In practical terms, the list of books is displayed using
an ordinary catalog feed activity but with an OPDS feed produced at
run-time from the on-disk database of books.

### Navigation Drawer

The _navigation drawer_ provides navigation between the main parts
of the application. The drawer can be opened by swiping rightwards on
the left edge of the screen, or by pressing the action bar _up button_
when at the root of the catalog.

![Navigation Drawer](src/main/png/drawer.png)

The navigation drawer is conditionally opened on application
startup. As per the Android design guidelines, the application stores
a boolean flag indicating whether or not the user has ever opened
the navigation drawer manually. If the user has ever done this, it
is assumed that the user understands how the navigation drawer works
and the drawer is therefore not opened automatically on application
startup.

Reader
======

The _reader_ package uses the [Readium SDK](http://github.com/nypl/readium-sdk)
to render EPUB documents. The Readium SDK consists of a native library
written in C++, with a Javascript API. Applications using Readium
essentially communicate with it by evaluating the exposed Javascript
functions in a web view.

The main entry point of the reader is the `ReaderActivity` class,
which receives two arguments: `path` representing the on-disk path
to the EPUB that will be rendered, and `book_id` representing the
unique ID of the book. All operations performed by the activity are
performed asynchronously, with the activity listening for results by
implementing various listener interfaces.

The `ReaderActivity` performs the following steps to start displaying
an EPUB:

1. A web view `w` is created. Execution of javascript is allowed,
   but all caching and network access is disabled. The web view is
   configured such that any URI that has a scheme equal to `readium`
   or `simplified` is handled by the reader application rather than
   being opened as a normal URI by the web view. These URIs are used by
   `Readium` to essentially send events to the application that the
   application can then handle manually. The mapping from Javascript
   functions to these URIs is given by the Javascript code in the included
   `host_app_feedback.js` file.

2. The EPUB file at `path` is loaded. If the file does not exist or
   cannot be loaded, the activity terminates with an error message.

3. If the EPUB was loaded successfully, the activity instructs a local
   web server to bind to `localhost`. The server is passed the loaded EPUB
   so that it can serve files from the EPUB when asked. If the server
   fails to start, the activity terminates with an error message. The
   server is also responsible for serving other files required by the
   `Readium SDK` that are not part of the loaded EPUB. These files are
   included as Android assets.

4. If the server started successfully, the web view `w` is told
   to request a file named `reader.html`. This file is responsible
   for loading the Javascript code used by `Readium`. When all of the
   Javascript code has finished initializing, it tries to load a URI
   `readium:initialize`. This URI is intercepted by the application as
   described in the first step above.

5. If `Readium` successfully initialized, the application then
   instructs `Readium` to start displaying the book by using the
   Javascript function `ReadiumSDK.reader.openBook(...)`.

### Media Overlay

For _media_ elements such as audio and video, `Readium` allows some
degree of control via a media overlay. In practical terms, this is
a small piece of user interface that is shown iff the current book
page contains a media element, and evaluates Javascript functions to
start/stop playback and switch between elements.

![Media](src/main/png/media_overlay.png)

When a page contains a _media_ element, the `Readium` package
Javascript function `ReadiumSDK.reader.isMediaOverlayAvailable()`
returns `true`. This function is evaluated every time a new page is
opened in order to decide whether or not to show the media overlay
interface. The various interface buttons evaluate functions exposed
by the `ReaderReadiumJavaScriptAPIType` type in order to control the
media elements.

### Processes

Because `Readium` is written in a memory-unsafe language, any bugs in
the package have the potential to corrupt memory and cause crashes that
cannot easily be debugged. The SDK has to process a large amount of
untrusted and often malformed input, and therefore bugs are considered
to be a likely occurence! To better isolate the code, the _reader_ is
placed into a separate process. The idea is that if the code is going
to crash, it is desirable that the crash should occur in the _reader_,
as opposed to a subtle memory-corruption bug causing the application
to crash long after the user has already moved back to the _catalog_ or
into a different book. The design of the `simplified` package is such
that only a single book can be open at a time, the current _reader_
process is closed when the user closes the book, and therefore the
cause of any crashes in the reader can be tracked back to individual
books if necessary.

This is the same approach as taken by the
[Chromium](http://chromium.org/) browser, where each tab is executed
in a separate process and if that process crashes, it is immediately
obvious who/what is to blame for the crash.

