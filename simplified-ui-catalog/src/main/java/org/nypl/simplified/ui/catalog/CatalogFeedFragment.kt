package org.nypl.simplified.ui.catalog

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.textfield.TextInputEditText
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
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups
import org.nypl.simplified.ui.catalog.databinding.FeedBinding
import org.nypl.simplified.ui.catalog.databinding.FeedHeaderBinding
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.slf4j.LoggerFactory

/**
 * A fragment displaying an OPDS feed.
 */

class CatalogFeedFragment : Fragment(), AgeGateDialog.BirthYearSelectedListener {

  companion object {

    const val PARAMETERS_ID =
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

  private var binding by autoCleared<FeedBinding>()
  private var withGroupsAdapter by autoCleared<CatalogFeedWithGroupsAdapter>()
  private var withoutGroupsAdapter by autoCleared<CatalogPagedAdapter>()

  private val logger = LoggerFactory.getLogger(CatalogFeedFragment::class.java)

  private val parameters: CatalogFeedArguments by lazy {
    requireArguments()[PARAMETERS_ID] as CatalogFeedArguments
  }

  private val listener: FragmentListenerType<CatalogFeedEvent> by fragmentListeners()

  private val borrowViewModel: CatalogBorrowViewModel by viewModels(
    factoryProducer = {
      CatalogBorrowViewModelFactory(services)
    }
  )

  private val viewModel: CatalogFeedViewModel by viewModels(
    factoryProducer = {
      CatalogFeedViewModelFactory(
        application = requireActivity().application,
        services = services,
        borrowViewModel = borrowViewModel,
        feedArguments = parameters,
        listener = listener
      )
    }
  )

  private val services = Services.serviceDirectory()
  private val bookCoverProvider = services.requireService(BookCoverProviderType::class.java)
  private val screenInformation = services.requireService(ScreenSizeInformationType::class.java)
  private val configService = services.requireService(BuildConfigurationServiceType::class.java)

  private lateinit var buttonCreator: CatalogButtons

  private lateinit var feedErrorDetails: Button
  private lateinit var feedErrorRetry: Button

  private var ageGateDialog: DialogFragment? = null
  private val feedWithGroupsData: MutableList<FeedGroup> = mutableListOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    ageGateDialog = childFragmentManager.findFragmentByTag(AGE_GATE_DIALOG_TAG) as? DialogFragment
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FeedBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = viewLifecycleOwner
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.feedStateLiveData.observe(viewLifecycleOwner) { reconfigureUI(it) }

    buttonCreator = CatalogButtons(requireContext(), screenInformation)

    configureFeedWithGroupsList()
    configureFeedWithoutGroupsList()

    feedErrorRetry = binding.feedError.feedErrorRetry
    feedErrorDetails = binding.feedError.feedErrorDetails
  }

  private fun configureFeedWithGroupsList() {
    val feedWithGroupsList = binding.feedWithGroups.feedWithGroupsList

    sharedListConfiguration(feedWithGroupsList)

    feedWithGroupsList.addItemDecoration(
      CatalogFeedWithGroupsDecorator(screenInformation.dpToPixels(16).toInt())
    )

    withGroupsAdapter = CatalogFeedWithGroupsAdapter(
      groups = feedWithGroupsData,
      coverLoader = bookCoverProvider,
      onFeedSelected = viewModel::openFeed,
      onBookSelected = viewModel::openBookDetail
    )

    feedWithGroupsList.adapter = withGroupsAdapter
  }

  private fun configureFeedWithoutGroupsList() {
    val feedWithoutGroupsList = binding.feedWithoutGroups.feedWithoutGroupsList

    sharedListConfiguration(feedWithoutGroupsList)

    withoutGroupsAdapter = CatalogPagedAdapter(
      context = requireActivity(),
      listener = viewModel,
      buttonCreator = buttonCreator,
      bookCovers = bookCoverProvider,
    )

    feedWithoutGroupsList.adapter = withoutGroupsAdapter
  }

  private fun sharedListConfiguration(list: RecyclerView) {
    list.setHasFixedSize(true)
    list.setItemViewCacheSize(32)
    list.layoutManager = LinearLayoutManager(context)
    (list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
  }

  override fun onStart() {
    super.onStart()

    binding.feedWithoutGroups.feedWithoutGroupsList.addOnScrollListener(
      CatalogScrollListener(
        bookCoverProvider
      )
    )
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.catalog, menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    // Necessary to reconfigure the Toolbar here due to the "Switch Account" action.
    configureToolbar()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.catalogMenuActionSearch -> {
        viewModel.feedStateLiveData.value?.search?.let { search ->
          openSearchDialog(requireContext(), search)
        }
        true
      }
      R.id.catalogMenuActionReload -> {
        viewModel.syncAccounts()
        true
      }
      android.R.id.home -> {
        if (viewModel.isAccountCatalogRoot()) {
          openAccountPickerDialog()
          true
        } else {
          super.onOptionsItemSelected(item)
        }
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun reconfigureUI(feedState: CatalogFeedState) {
    if (feedState is CatalogFeedAgeGate) openAgeGateDialog() else dismissAgeGateDialog()
    configureToolbar()

    when (feedState) {
      is CatalogFeedWithGroups -> onCatalogFeedWithGroups(feedState)
      is CatalogFeedWithoutGroups -> onCatalogFeedWithoutGroups(feedState)
      is CatalogFeedLoadFailed -> onCatalogFeedLoadFailed(feedState)
      else -> {
      }
    }
  }

  override fun onStop() {
    super.onStop()
    binding.feedWithoutGroups.feedWithoutGroupsList.clearOnScrollListeners()
  }

  private fun onCatalogFeedWithoutGroups(
    feedState: CatalogFeedWithoutGroups
  ) {
    configureFacets(
      headerBinding = binding.feedWithoutGroups.feedWithoutGroupsHeader,
      facetsByGroup = feedState.facetsByGroup
    )

    feedState.entries.observe(viewLifecycleOwner) { newPagedList ->
      logger.debug("received paged list ({} elements)", newPagedList.size)
      withoutGroupsAdapter.submitList(newPagedList)
    }
  }

  private fun onCatalogFeedWithGroups(
    feedState: CatalogFeedWithGroups
  ) {
    configureFacets(
      headerBinding = binding.feedWithGroups.feedWithGroupsHeader,
      facetsByGroup = feedState.feed.facetsByGroup
    )

    feedWithGroupsData.clear()
    feedWithGroupsData.addAll(feedState.feed.feedGroupsInOrder)
    withGroupsAdapter.notifyDataSetChanged()
  }

  private fun onCatalogFeedLoadFailed(
    feedState: CatalogFeedLoadFailed
  ) {
    feedErrorRetry.isEnabled = true
    feedErrorRetry.setOnClickListener { button ->
      button.isEnabled = false
      viewModel.reloadFeed()
    }

    feedErrorDetails.isEnabled = true
    feedErrorDetails.setOnClickListener {
      viewModel.showFeedErrorDetails(feedState.failure)
    }
  }

  private fun configureToolbar() {
    configureToolbarNavigation()
    configureToolbarTitles()
  }

  private fun configureToolbarNavigation() {
    fun showAccountPickerAction() {
      // Configure the 'Home Action' in the Toolbar to show the account picker when tapped.
      supportActionBar?.apply {
        // Configure whether or not the user should be able to change accounts
        if (configService.showChangeAccountsUi) {
          setHomeAsUpIndicator(R.drawable.accounts)
          setHomeActionContentDescription(R.string.catalogAccounts)
          setDisplayHomeAsUpEnabled(true)
        } else {
          setDisplayHomeAsUpEnabled(false)
        }
      }
    }

    try {
      if (viewModel.isAccountCatalogRoot()) {
        showAccountPickerAction()
      }
    } catch (e: Exception) {
      // Nothing to do
    }
  }

  private fun configureToolbarTitles() {
    supportActionBar?.let {
      it.title = parameters.title
      it.subtitle = viewModel.accountProvider?.displayName
    }
  }

  private fun openAccountPickerDialog() {
    return when (val ownership = parameters.ownership) {
      is OwnedByAccount -> {
        val dialog =
          AccountPickerDialogFragment.create(
            currentId = ownership.accountId,
            showAddAccount = configService.allowAccountsAccess
          )
        dialog.show(parentFragmentManager, dialog.tag)
      }
      CollectedFromAccounts -> {
        throw IllegalStateException("Can't switch account from collected feed!")
      }
    }
  }

  @SuppressLint("InflateParams")
  fun openSearchDialog(
    context: Context,
    search: FeedSearch
  ) {
    val view = LayoutInflater.from(context).inflate(R.layout.search_dialog, null)
    val searchInput = view.findViewById<TextInputEditText>(R.id.searchDialogText)!!

    val builder = AlertDialog.Builder(context).apply {
      setPositiveButton(R.string.catalogSearch) { dialog, _ ->
        val query = searchInput.text.toString().trim()
        viewModel.performSearch(search, query)
        dialog.dismiss()
      }
      setNegativeButton(R.string.cancel) { dialog, _ ->
        dialog.dismiss()
      }
      setView(view)
    }

    val dialog = builder.create()
    searchInput.setOnEditorActionListener { _, actionId, _ ->
      when (actionId) {
        EditorInfo.IME_ACTION_SEARCH -> {
          val query = searchInput.text.toString().trim()
          viewModel.performSearch(search, query)
          dialog.dismiss()
          true
        }
        else -> false
      }
    }
    dialog.show()

    val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
    button.isEnabled = false
    searchInput.doAfterTextChanged { text ->
      text?.let { button.isEnabled = it.length >= 2 }
    }
  }

  private fun configureFacets(
    headerBinding: FeedHeaderBinding,
    facetsByGroup: Map<String, List<FeedFacet>>
  ) {
    if (facetsByGroup.isEmpty()) {
      headerBinding.root.visibility = View.GONE
    } else {
      configureFacets(
        headerBinding.feedHeaderTabs,
        headerBinding.feedHeaderFacetsScroll,
        headerBinding.feedHeaderFacets,
        facetsByGroup
      )
    }
  }

  private fun configureFacets(
    facetTabs: RadioGroup,
    facetLayoutScroller: ViewGroup,
    facetLayout: LinearLayout,
    facetsByGroup: Map<String, List<FeedFacet>>
  ) {
    /*
     * If one of the groups is an entry point, display it as a set of tabs. Otherwise, hide
     * the tab layout entirely.
     */

    configureFacetTabs(FeedFacets.findEntryPointFacetGroup(facetsByGroup), facetTabs)

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
        screenInformation.dpToPixels(96).toInt(),
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
        screenInformation.dpToPixels(8).toInt(),
        LinearLayout.LayoutParams.MATCH_PARENT
      )

    val sortedNames = remainingGroups.keys.sorted()
    val context = requireContext()

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
      button.setOnClickListener { showFacetSelectDialog(groupName, group) }

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
      val button = RadioButton(requireContext())
      val buttonLayout =
        LinearLayout.LayoutParams(
          screenInformation.dpToPixels(160).toInt(),
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

      val drawableId = when (index) {
        0 -> R.drawable.catalog_facet_tab_button_background_left
        size - 1 -> R.drawable.catalog_facet_tab_button_background_right
        else -> R.drawable.catalog_facet_tab_button_background_middle
      }

      button.setBackgroundResource(drawableId)
      button.setButtonDrawable(drawableId)

      button.text = facet.title
      button.setOnClickListener {
        logger.debug("selected entry point facet: {}", facet.title)
        viewModel.openFacet(facet)
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
        logger.debug("active entry point facet: {}", facet.title)
        facetTabs.check(button.id)
      }
    }
  }

  private fun showFacetSelectDialog(
    groupName: String,
    group: List<FeedFacet>
  ) {
    val choices = group.sortedBy { it.title }
    val names = choices.map { it.title }.toTypedArray()
    val checkedItem = choices.indexOfFirst { it.isActive }

    // Build the dialog
    val alertBuilder = AlertDialog.Builder(requireContext())
    alertBuilder.setTitle(groupName)
    alertBuilder.setSingleChoiceItems(names, checkedItem) { dialog, checked ->
      val selected = choices[checked]
      logger.debug("selected facet: {}", selected)
      viewModel.openFacet(selected)
      dialog.dismiss()
    }
    alertBuilder.create().show()
  }

  override fun onBirthYearSelected(isOver13: Boolean) = viewModel.updateBirthYear(isOver13)

  private fun openAgeGateDialog() {
    if (ageGateDialog != null) {
      return
    }

    val ageGate = AgeGateDialog.create()
    ageGate.show(childFragmentManager, AGE_GATE_DIALOG_TAG)
    ageGateDialog = ageGate
  }

  private fun dismissAgeGateDialog() {
    ageGateDialog?.dismiss()
    ageGateDialog = null
  }
}
