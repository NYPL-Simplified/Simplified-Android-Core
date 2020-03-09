org.librarysimplified.migration.spi
===

The `org.librarysimplified.migration.spi` module specifies an SPI for
data migrations. Implementations are expected to register instances of
the `MigrationProviderType` interface with `ServiceLoader`, and these
instances will be picked up automatically by the migration API on
application startup.

#### See Also

* [org.librarysimplified.migration.api](../simplified-migration-api/README.md)
