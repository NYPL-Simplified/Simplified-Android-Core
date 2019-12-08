org.librarysimplified.accounts.source.spi
===

The `org.librarysimplified.accounts.source.spi` module specifies the
SPI used by implementations that want to contribute to the _accounts
registry_.

Implementations of the `AccountProviderSourceType` interface should
register themselves with `ServiceLoader` and will then be automatically
picked up and used by the accounts registry implementation.

#### See Also

* [org.librarysimplified.accounts.api](../simplified-accounts-api/README.md)
* [org.librarysimplified.accounts.registry.api](../simplified-accounts-registry-api/README.md)
* [org.librarysimplified.accounts.source.filebased](../simplified-accounts-source-filebased/README.md)
* [org.librarysimplified.accounts.source.nyplregistry](../simplified-accounts-source-nyplregistry/README.md)
