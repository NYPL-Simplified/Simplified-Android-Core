org.librarysimplified.accounts.source.filebased
===

The `org.librarysimplified.accounts.source.filebased` module provides
a file-based _account source_.

The implementation loads a set of _account providers_ from an
`Accounts.json` file in the `assets` directory of the current 
application. Application frontends can include their own versions of
this file in order to provide a set of default accounts.

An example of a simple `Accounts.json` file is as follows:

```
[
  {
    "@version": "20190708",
    "addAutomatically": true,
    "catalogURI": "https://lfabooksthattravel.cantookstation.com/catalog/featuredresources.atom",
    "displayName": "Books That Travel",
    "idUUID": "urn:provider:com.cantookstation.lfabooksthattravel",
    "logo": "simplified-asset:logos/btt.png",
    "mainColor": "#ec1c24",
    "subtitle": "Books That Travel"
  },
  {
    "@version": "20190708",
    "addAutomatically": true,
    "catalogURI": "http://openbookshelf.dp.la/OB/groups/3",
    "displayName": "Open Bookshelf",
    "idUUID": "urn:provider:la.dp.openbookshelf",
    "logo": "simplified-asset:logos/dpla.png",
    "mainColor": "#ec1c24",
    "subtitle": "Open Bookshelf"
  }
]
```

#### See Also

* [org.librarysimplified.accounts.source.spi](../simplified-accounts-source-spi/README.md)
