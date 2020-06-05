package org.nypl.simplified.ui.catalog

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * A login dialog used in catalogs.
 */

class CatalogFragmentLoginDialog : Fragment() {
  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.catalog.CatalogFragmentLoginDialog.parameters"

    /**
     * Create a login fragment for the given parameters.
     */

    fun create(parameters: CatalogFragmentLoginDialogParameters): CatalogFragmentLoginDialog {
      val arguments = Bundle()
      arguments.putSerializable(this.PARAMETERS_ID, parameters)
      val fragment = CatalogFragmentLoginDialog()
      fragment.arguments = arguments
      return fragment
    }
  }
}
