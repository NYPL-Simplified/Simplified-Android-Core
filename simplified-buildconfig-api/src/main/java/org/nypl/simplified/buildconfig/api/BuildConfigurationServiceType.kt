package org.nypl.simplified.buildconfig.api

/**
 * A service to obtain access to various configuration values that are application-build-specific.
 */

interface BuildConfigurationServiceType :
  BuildConfigurationAccountsType,
  BuildConfigurationCatalogType,
  BuildConfigurationMetadataType,
  BuildConfigurationOAuthType,
  BuildConfigurationSettingsType
