package org.nypl.simplified.ui.catalog

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.TEXT_ALIGNMENT_TEXT_END
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import org.librarysimplified.services.api.Services
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedFacets
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.feeds.api.FeedSearch
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.ui.accounts.AccountPickerDialogFragment
import org.nypl.simplified.ui.catalog.CatalogFeedOwnership.CollectedFromAccounts
import org.nypl.simplified.ui.catalog.CatalogFeedOwnership.OwnedByAccount
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedAgeGate
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoadFailed
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedEmpty
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedNavigation
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoading
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.theme.ThemeControl
import org.slf4j.LoggerFactory

/**
 * A fragment displaying an OPDS feed.
 */

class CatalogFeedFragment : Fragment(R.layout.feed), AgeGateDialog.BirthYearSelectedListener {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.catalog.CatalogFragmentFeed.parameters"

    private val AGE_GATE_DIALOG_TAG =
      AgeGateDialog::class.java.simpleName

    /**
     * Create a catalog feed fragment for the given parameters.
     */

    fun create(parameters: CatalogFeedArguments): CatalogFeedFragment {
      val fragment = CatalogFeedFragment()
      fragment.arguments = bundleOf(PARAMETERS_ID to parameters)
      return fragment
    }
  }

  private val logger = LoggerFactory.getLogger(CatalogFeedFragment::class.java)

  private val parameters: CatalogFeedArguments by lazy {
    this.requireArguments()[PARAMETERS_ID] as CatalogFeedArguments
  }

  private val services =
    Services.serviceDirectory()

  private val listener: FragmentListenerType<CatalogFeedEvent> by fragmentListeners()

  private val borrowViewModel: CatalogBorrowViewModel by viewModels(
    factoryProducer = {
      CatalogBorrowViewModelFactory(services)
    }
  )

  private val viewModel: CatalogFeedViewModel by viewModels(
    factoryProducer = {
      CatalogFeedViewModelFactory(
        application = this.requireActivity().application,
        services = Services.serviceDirectory(),
        borrowViewModel = borrowViewModel,
        feedArguments = this.parameters,
        listener = this.listener
      )
    }
  )

  private val bookCovers =
    services.requireService(BookCoverProviderType::class.java)
  private val screenInformation =
    services.requireService(ScreenSizeInformationType::class.java)
  private val configurationService =
    services.requireService(BuildConfigurationServiceType::class.java)

  private lateinit var buttonCreator: CatalogButtons
  private lateinit var feedEmpty: ViewGroup
  private lateinit var feedError: ViewGroup
  private lateinit var feedErrorDetails: Button
  private lateinit var feedErrorRetry: Button
  private lateinit var feedLoading: ViewGroup
  private lateinit var feedNavigation: ViewGroup
  private lateinit var feedWithGroups: ViewGroup
  private lateinit var feedWithGroupsAdapter: CatalogFeedWithGroupsAdapter
  private lateinit var feedWithGroupsFacets: LinearLayout
  private lateinit var feedWithGroupsFacetsScroll: ViewGroup
  private lateinit var feedWithGroupsHeader: ViewGroup
  private lateinit var feedWithGroupsList: RecyclerView
  private lateinit var feedWithGroupsTabs: RadioGroup
  private lateinit var feedWithoutGroups: ViewGroup
  private lateinit var feedWithoutGroupsAdapter: CatalogPagedAdapter
  private lateinit var feedWithoutGroupsFacets: LinearLayout
  private lateinit var feedWithoutGroupsFacetsScroll: ViewGroup
  private lateinit var feedWithoutGroupsHeader: ViewGroup
  private lateinit var feedWithoutGroupsList: RecyclerView
  private lateinit var feedWithoutGroupsScrollListener: RecyclerView.OnScrollListener
  private lateinit var feedWithoutGroupsTabs: RadioGroup

  private var ageGateDialog: DialogFragment? = null
  private val feedWithGroupsData: MutableList<FeedGroup> = mutableListOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    this.ageGateDialog = childFragmentManager.findFragmentByTag(AGE_GATE_DIALOG_TAG) as? DialogFragment
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.viewModel.stateLive.observe(this.viewLifecycleOwner, this::reconfigureUI)

    this.buttonCreator =
      CatalogButtons(this.requireContext(), this.screenInformation)

    this.feedEmpty =
      view.findViewById(R.id.feedEmpty)
    this.feedError =
      view.findViewById(R.id.feedError)
    this.feedLoading =
      view.findViewById(R.id.feedLoading)
    this.feedNavigation =
      view.findViewById(R.id.feedNavigation)
    this.feedWithGroups =
      view.findViewById(R.id.feedWithGroups)
    this.feedWithoutGroups =
      view.findViewById(R.id.feedWithoutGroups)

    this.feedWithGroupsHeader =
      view.findViewById(R.id.feedWithGroupsHeader)
    this.feedWithGroupsFacetsScroll =
      this.feedWithGroupsHeader.findViewById(R.id.feedHeaderFacetsScroll)
    this.feedWithGroupsFacets =
      this.feedWithGroupsHeader.findViewById(R.id.feedHeaderFacets)
    this.feedWithGroupsTabs =
      this.feedWithGroupsHeader.findViewById(R.id.feedHeaderTabs)

    this.feedWithGroupsList = this.feedWithGroups.findViewById(R.id.feedWithGroupsList)
    this.feedWithGroupsList.setHasFixedSize(true)
    this.feedWithGroupsList.setItemViewCacheSize(32)
    this.feedWithGroupsList.layoutManager = LinearLayoutManager(this.context)
    (this.feedWithGroupsList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    this.feedWithGroupsList.addItemDecoration(
      CatalogFeedWithGroupsDecorator(this.screenInformation.dpToPixels(16).toInt())
    )

    this.feedWithGroupsAdapter =
      CatalogFeedWithGroupsAdapter(
        groups = this.feedWithGroupsData,
        coverLoader = this.bookCovers,
        onFeedSelected = this.viewModel::openFeed,
        onBookSelected = this.viewModel::openBookDetail
      )
    this.feedWithGroupsList.adapter = this.feedWithGroupsAdapter

    this.feedWithoutGroupsHeader =
      view.findViewById(R.id.feedWithoutGroupsHeader)
    this.feedWithoutGroupsFacetsScroll =
      this.feedWithoutGroupsHeader.findViewById(R.id.feedHeaderFacetsScroll)
    this.feedWithoutGroupsFacets =
      this.feedWithoutGroupsHeader.findViewById(R.id.feedHeaderFacets)
    this.feedWithoutGroupsTabs =
      this.feedWithoutGroupsHeader.findViewById(R.id.feedHeaderTabs)

    this.feedWithoutGroupsList = this.feedWithoutGroups.findViewById(R.id.feedWithoutGroupsList)
    this.feedWithoutGroupsList.setHasFixedSize(true)
    this.feedWithoutGroupsList.setItemViewCacheSize(32)
    this.feedWithoutGroupsList.layoutManager = LinearLayoutManager(this.context)
    (this.feedWithoutGroupsList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.feedErrorRetry =
      this.feedError.findViewById(R.id.feedErrorRetry)
    this.feedErrorDetails =
      this.feedError.findViewById(R.id.feedErrorDetails)

    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE
  }

  override fun onStart() {
    super.onStart()

    this.feedWithoutGroupsScrollListener = CatalogScrollListener(this.bookCovers)
    this.feedWithoutGroupsList.addOnScrollListener(this.feedWithoutGroupsScrollListener)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.catalog, menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    // Necessary to reconfigure the Toolbar here due to the "Switch Account" action.
    this.configureToolbar()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.catalogMenuActionSearch -> {
        this.viewModel.stateLive.value?.search?.let { search ->
          this.openSearchDialog(requireContext(), search)
        }
        true
      }
      R.id.catalogMenuActionReload -> {
        this.viewModel.syncAccounts()
        true
      }
      android.R.id.home -> {
        if (this.viewModel.isAccountCatalogRoot()) {
          this.openAccountPickerDialog()
          true
        } else {
          super.onOptionsItemSelected(item)
        }
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun reconfigureUI(feedState: CatalogFeedState) {
    return when (feedState) {
      is CatalogFeedAgeGate ->
        this.onCatalogFeedAgeGate(feedState)
      is CatalogFeedLoading ->
        this.onCatalogFeedLoading(feedState)
      is CatalogFeedWithGroups ->
        this.onCatalogFeedWithGroups(feedState)
      is CatalogFeedWithoutGroups ->
        this.onCatalogFeedWithoutGroups(feedState)
      is CatalogFeedNavigation ->
        this.onCatalogFeedNavigation(feedState)
      is CatalogFeedLoadFailed ->
        this.onCatalogFeedLoadFailed(feedState)
      is CatalogFeedEmpty ->
        this.onCatalogFeedEmpty(feedState)
    }
  }

  override fun onStop() {
    super.onStop()

    /*
     * We aggressively unset adapters here in order to try to encourage prompt unsubscription
     * of views from the book registry.
     */

    this.feedWithoutGroupsList.removeOnScrollListener(this.feedWithoutGroupsScrollListener)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    this.feedWithoutGroupsList.adapter = null
    this.feedWithGroupsList.adapter = null
  }

  private fun onCatalogFeedAgeGate(
    @Suppress("UNUSED_PARAMETER") feedState: CatalogFeedAgeGate
  ) {
    this.openAgeGateDialog()
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()
  }

  private fun onCatalogFeedEmpty(
    @Suppress("UNUSED_PARAMETER") feedState: CatalogFeedEmpty
  ) {
    this.dismissAgeGateDialog()
    this.feedEmpty.visibility = View.VISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()
  }

  private fun onCatalogFeedLoading(
    @Suppress("UNUSED_PARAMETER") feedState: CatalogFeedLoading
  ) {
    this.dismissAgeGateDialog()
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.VISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()
  }

  private fun onCatalogFeedNavigation(
    @Suppress("UNUSED_PARAMETER") feedState: CatalogFeedNavigation
  ) {
    this.dismissAgeGateDialog()
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.VISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()
  }

  private fun onCatalogFeedWithoutGroups(
    feedState: CatalogFeedWithoutGroups
  ) {
    this.dismissAgeGateDialog()
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.VISIBLE

    this.configureToolbar()

    this.configureFacets(
      facetHeader = this.feedWithoutGroupsHeader,
      facetTabs = this.feedWithoutGroupsTabs,
      facetLayoutScroller = this.feedWithoutGroupsFacetsScroll,
      facetLayout = this.feedWithoutGroupsFacets,
      facetsByGroup = feedState.facetsByGroup
    )

    this.feedWithoutGroupsAdapter =
      CatalogPagedAdapter(
        context = requireActivity(),
        listener = this.viewModel,
        buttonCreator = this.buttonCreator,
        bookCovers = this.bookCovers,
      )

    this.feedWithoutGroupsList.adapter = this.feedWithoutGroupsAdapter
    feedState.entries.observe(this) { newPagedList ->
      this.logger.debug("received paged list ({} elements)", newPagedList.size)
      this.feedWithoutGroupsAdapter.submitList(newPagedList)
    }
  }

  private fun onCatalogFeedWithGroups(
    feedState: CatalogFeedWithGroups
  ) {
    this.dismissAgeGateDialog()
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.VISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()

    this.configureFacets(
      facetHeader = this.feedWithGroupsHeader,
      facetTabs = this.feedWithGroupsTabs,
      facetLayoutScroller = this.feedWithGroupsFacetsScroll,
      facetLayout = this.feedWithGroupsFacets,
      facetsByGroup = feedState.feed.facetsByGroup
    )

    this.feedWithGroupsData.clear()
    this.feedWithGroupsData.addAll(feedState.feed.feedGroupsInOrder)
    this.feedWithGroupsAdapter.notifyDataSetChanged()
  }

  private fun onCatalogFeedLoadFailed(
    feedState: CatalogFeedLoadFailed
  ) {
    this.dismissAgeGateDialog()
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.VISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()

    this.feedErrorRetry.isEnabled = true
    this.feedErrorRetry.setOnClickListener { button ->
      button.isEnabled = false
      this.viewModel.reloadFeed()
    }

    this.feedErrorDetails.isEnabled = true
    this.feedErrorDetails.setOnClickListener {
      this.viewModel.showFeedErrorDetails(feedState.failure)
    }
  }

  private fun configureToolbar() {
    this.configureToolbarNavigation()
    this.configureToolbarTitles()
  }

  private fun configureToolbarNavigation() {
    fun showAccountPickerAction() {
      // Configure the 'Home Action' in the Toolbar to show the account picker when tapped.
      this.supportActionBar?.apply {
        // Configure whether or not the user should be able to change accounts
        if (configurationService.showChangeAccountsUi) {
          setHomeAsUpIndicator(R.drawable.accounts)
          setHomeActionContentDescription(R.string.catalogAccounts)
          setDisplayHomeAsUpEnabled(true)
        } else {
          setDisplayHomeAsUpEnabled(false)
        }
      }
    }

    try {
      if (this.viewModel.isAccountCatalogRoot()) {
        showAccountPickerAction()
      }
    } catch (e: Exception) {
      // Nothing to do
    }
  }

  private fun configureToolbarTitles() {
    this.supportActionBar?.let {
      it.title = this.parameters.title
      it.subtitle = this.viewModel.accountProvider?.displayName
    }
  }

  private fun openAccountPickerDialog() {
    return when (val ownership = this.parameters.ownership) {
      is OwnedByAccount -> {
        val dialog =
          AccountPickerDialogFragment.create(
            currentId = ownership.accountId,
            showAddAccount = this.configurationService.allowAccountsAccess
          )
        dialog.show(parentFragmentManager, dialog.tag)
      }
      CollectedFromAccounts -> {
        throw IllegalStateException("Can't switch account from collected feed!")
      }
    }
  }

  @SuppressLint("InflateParams")
  private fun openSearchDialog(
    context: Context,
    search: FeedSearch
  ) {
    val view = LayoutInflater.from(context).inflate(R.layout.search_dialog, null)
    val searchView = view.findViewById<TextView>(R.id.searchDialogText)!!

    val builder = AlertDialog.Builder(context).apply {
      setPositiveButton(R.string.catalogSearch) { dialog, _ ->
        val query = searchView.text.toString().trim()
        this@CatalogFeedFragment.viewModel.performSearch(search, query)
        dialog.dismiss()
      }
      setNegativeButton(R.string.cancel) { dialog, _ ->
        dialog.dismiss()
      }
      setView(view)
    }

    val dialog = builder.create()
    searchView.setOnEditorActionListener { _, actionId, _ ->
      return@setOnEditorActionListener when (actionId) {
        EditorInfo.IME_ACTION_SEARCH -> {
          val query = searchView.text.toString().trim()
          this@CatalogFeedFragment.viewModel.performSearch(search, query)
          dialog.dismiss()
          true
        }
        else -> false
      }
    }
    dialog.show()
  }

  private fun configureFacets(
    facetHeader: ViewGroup,
    facetTabs: RadioGroup,
    facetLayoutScroller: ViewGroup,
    facetLayout: LinearLayout,
    facetsByGroup: Map<String, List<FeedFacet>>
  ) {
    /*
     * If the facet groups are empty, hide the header entirely.
     */

    if (facetsByGroup.isEmpty()) {
      facetHeader.visibility = View.GONE
      return
    }

    /*
     * If one of the groups is an entry point, display it as a set of tabs. Otherwise, hide
     * the tab layout entirely.
     */

    this.configureFacetTabs(FeedFacets.findEntryPointFacetGroup(facetsByGroup), facetTabs)

    /*
     * Otherwise, for each remaining non-entrypoint facet group, show a drop-down menu allowing
     * the selection of individual facets. If there are no remaining groups, hide the button
     * bar.
     */

    val remainingGroups = facetsByGroup
      .filter { entry ->
        /*
         * SIMPLY-2923: Hide the 'Collection' Facet until approved by UX.
         */
        entry.key != "Collection"
      }
      .filter { entry ->
        !FeedFacets.facetGroupIsEntryPointTyped(entry.value)
      }

    if (remainingGroups.isEmpty()) {
      facetLayoutScroller.visibility = View.GONE
      return
    }

    val buttonLayoutParams =
      LinearLayout.LayoutParams(
        this.screenInformation.dpToPixels(96).toInt(),
        LinearLayout.LayoutParams.WRAP_CONTENT
      )

    val textLayoutParams =
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      )

    textLayoutParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL

    val spacerLayoutParams =
      LinearLayout.LayoutParams(
        this.screenInformation.dpToPixels(8).toInt(),
        LinearLayout.LayoutParams.MATCH_PARENT
      )

    val sortedNames = remainingGroups.keys.sorted()
    val context = this.requireContext()

    facetLayout.removeAllViews()
    sortedNames.forEach { groupName ->
      val group = remainingGroups.getValue(groupName)
      if (FeedFacets.facetGroupIsEntryPointTyped(group)) {
        return@forEach
      }

      val button = AppCompatButton(context)
      val buttonLabel = AppCompatTextView(context)
      val spaceStart = Space(context)
      val spaceMiddle = Space(context)
      val spaceEnd = Space(context)

      val active =
        group.find { facet -> facet.isActive }
          ?: group.firstOrNull()

      buttonLayoutParams.weight = 1.0f
      button.id = View.generateViewId()
      button.layoutParams = buttonLayoutParams
      button.text = active?.title
      button.ellipsize = TextUtils.TruncateAt.END
      button.setOnClickListener {
        this.showFacetSelectDialog(groupName, group)
      }

      spaceStart.layoutParams = spacerLayoutParams
      spaceMiddle.layoutParams = spacerLayoutParams
      spaceEnd.layoutParams = spacerLayoutParams

      buttonLabel.layoutParams = textLayoutParams
      buttonLabel.text = "$groupName: "
      buttonLabel.labelFor = button.id
      buttonLabel.maxLines = 1
      buttonLabel.ellipsize = TextUtils.TruncateAt.END
      buttonLabel.textAlignment = TEXT_ALIGNMENT_TEXT_END
      buttonLabel.gravity = Gravity.END or Gravity.CENTER_VERTICAL

      facetLayout.addView(spaceStart)
      facetLayout.addView(buttonLabel)
      facetLayout.addView(spaceMiddle)
      facetLayout.addView(button)
      facetLayout.addView(spaceEnd)
    }

    facetLayoutScroller.scrollTo(0, 0)
  }

  private fun configureFacetTabs(
    facetGroup: List<FeedFacet>?,
    facetTabs: RadioGroup
  ) {
    if (facetGroup == null) {
      facetTabs.visibility = View.GONE
      return
    }

    /*
     * Add a set of radio buttons to the view.
     */

    facetTabs.removeAllViews()
    val size = facetGroup.size
    for (index in 0 until size) {
      val facet = facetGroup[index]
      val button = RadioButton(this.requireContext())
      val buttonLayout =
        LinearLayout.LayoutParams(
          this.screenInformation.dpToPixels(160).toInt(),
          ViewGroup.LayoutParams.MATCH_PARENT,
          1.0f / size.toFloat()
        )

      button.layoutParams = buttonLayout
      button.gravity = Gravity.CENTER
      button.maxLines = 1
      button.ellipsize = TextUtils.TruncateAt.END

      /*
       * The buttons need unique IDs so that they can be addressed within the parent
       * radio group.
       */

      button.id = View.generateViewId()

      if (index == 0) {
        button.setBackgroundResource(R.drawable.catalog_facet_tab_button_background_left)
        button.setButtonDrawable(R.drawable.catalog_facet_tab_button_background_left)
      } else if (index == size - 1) {
        button.setBackgroundResource(R.drawable.catalog_facet_tab_button_background_right)
        button.setButtonDrawable(R.drawable.catalog_facet_tab_button_background_right)
      } else {
        button.setBackgroundResource(R.drawable.catalog_facet_tab_button_background_middle)
        button.setButtonDrawable(R.drawable.catalog_facet_tab_button_background_middle)
      }

      button.text = facet.title
      button.setTextColor(this.colorStateListForFacetTabs())
      button.setOnClickListener {
        this.logger.debug("selected entry point facet: {}", facet.title)
        this.viewModel.openFacet(facet)
      }
      facetTabs.addView(button)
    }

    /*
     * Uncheck all of the buttons, and then check the one that corresponds to the current
     * active facet.
     */

    facetTabs.clearCheck()

    for (index in 0 until size) {
      val facet = facetGroup[index]
      val button = facetTabs.getChildAt(index) as RadioButton

      if (facet.isActive) {
        this.logger.debug("active entry point facet: {}", facet.title)
        facetTabs.check(button.id)
      }
    }
  }

  private fun colorStateListForFacetTabs(): ColorStateList {
    val activity = this.requireActivity()

    val states =
      arrayOf(
        intArrayOf(android.R.attr.state_checked),
        intArrayOf(-android.R.attr.state_checked)
      )

    val colors =
      intArrayOf(
        ContextCompat.getColor(activity, R.color.simplifiedColorBackground),
        ThemeControl.resolveColorAttribute(activity.theme, R.attr.colorPrimary)
      )

    return ColorStateList(states, colors)
  }

  private fun showFacetSelectDialog(
    groupName: String,
    group: List<FeedFacet>
  ) {
    val choices = group.sortedBy { it.title }
    val names = choices.map { it.title }.toTypedArray()
    val checkedItem = choices.indexOfFirst { it.isActive }

    // Build the dialog
    val alertBuilder = AlertDialog.Builder(this.requireContext())
    alertBuilder.setTitle(groupName)
    alertBuilder.setSingleChoiceItems(names, checkedItem) { dialog, checked ->
      val selected = choices[checked]
      this.logger.debug("selected facet: {}", selected)
      this.viewModel.openFacet(selected)
      dialog.dismiss()
    }
    alertBuilder.create().show()
  }

  override fun onBirthYearSelected(isOver13: Boolean) {
    this.viewModel.updateBirthYear(isOver13)
  }

  private fun openAgeGateDialog() {
    if (this.ageGateDialog != null) {
      return
    }

    val ageGate = AgeGateDialog.create()
    ageGate.show(childFragmentManager, AGE_GATE_DIALOG_TAG)
    this.ageGateDialog = ageGate
  }

  private fun dismissAgeGateDialog() {
    this.ageGateDialog?.dismiss()
    this.ageGateDialog = null
  }
}
