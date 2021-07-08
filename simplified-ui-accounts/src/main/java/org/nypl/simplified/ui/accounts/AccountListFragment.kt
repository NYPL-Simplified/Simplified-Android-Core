package org.nypl.simplified.ui.accounts

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.ui.accounts.databinding.AccountListRegistryBinding
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageLoaderType
import org.slf4j.LoggerFactory

/**
 * A fragment that shows the account registry and allows for account creation.
 */
class AccountListFragment : Fragment(R.layout.account_list_registry) {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.accounts.AccountListFragment.parameters"

    /**
     * Create a new accounts fragment for the given parameters.
     */
    fun create(parameters: AccountListFragmentParameters): AccountListFragment {
      val fragment = AccountListFragment()
      fragment.arguments = bundleOf(PARAMETERS_ID to parameters)
      return fragment
    }
  }


  private val parameters: AccountListFragmentParameters by lazy {
    this.requireArguments()[PARAMETERS_ID] as AccountListFragmentParameters
  }

  private val services by lazy { Services.serviceDirectory() }
  private val logger by lazy { LoggerFactory.getLogger(AccountListFragment::class.java) }
  private val imageLoader: ImageLoaderType by lazy { services.requireService(ImageLoaderType::class.java) }
  private val listener: FragmentListenerType<AccountListEvent> by fragmentListeners()
  private val viewModel: AccountListViewModel by assistedViewModels {
    val locationManager =
      requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    AccountListViewModel(locationManager)
  }
  private val requestLocationPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission(),
    ::getLocation
  )

  private val subscriptions = CompositeDisposable()
  private val runningAnimations = mutableListOf<Animator>()

  private val registeredAccountListAdapter =
    AccountListAdapter(imageLoader, ::onRegisteredAccountClicked, ::onRegisteredAccountDeleted)

  private val accountListAdapter = FilterableAccountListAdapter(imageLoader, ::onAccountClicked)

  private lateinit var binding: AccountListRegistryBinding
  private val registeredAccountsHeader: TextView
    get() = binding.accountMyLibrariesHeader
  private val registeredAccountsHeaderIcon: ImageView
    get() = binding.accountMyLibrariesExpandIcon
  private val registeredAccountsHeaderDivider: View
    get() = binding.accountMyLibrariesExpandDivider
  private val registeredAccountsList: RecyclerView
    get() = binding.accountMyLibrariesList
  private val accountList: RecyclerView
    get() = binding.accountRegistryList
  private val noLocation: TextView
    get() = binding.accountsNoLocation
  private val progress: ContentLoadingProgressBar
    get() = binding.accountRegistryProgress

  private var reload: MenuItem? = null
  private var errorDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.account_list_registry, menu)
    this.reload = menu.findItem(R.id.accountMenuActionReload)

    val search = menu.findItem(R.id.accountMenuActionSearch)
    val searchView = search.actionView as SearchView

    searchView.imeOptions = EditorInfo.IME_ACTION_DONE
    searchView.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    searchView.queryHint = getString(R.string.accountSearchHint)

    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String): Boolean {
        search.collapseActionView()
        return true
      }

      override fun onQueryTextChange(newText: String): Boolean {
        viewModel.query(newText)
        return true
      }
    })

    search.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
      override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        // Do nothing
        return true
      }

      override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        viewModel.clearQuery()
        return true
      }
    })
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding = AccountListRegistryBinding.bind(view)
    with(registeredAccountsList) {
      adapter = registeredAccountListAdapter
      addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
    }
    with(accountList) {
      adapter = accountListAdapter
      addItemDecoration(SpaceItemDecoration(requireContext(), RecyclerView.VERTICAL))
    }
    registeredAccountsHeader.setOnClickListener { toggleRegisteredLibraries() }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.accountMenuActionReload -> {
        this.reload()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onStart() {
    super.onStart()

    this.configureToolbar()

    this.viewModel.registeredAccounts
      .subscribe(this::updateRegisteredAccounts)
      .let(subscriptions::add)
    this.viewModel.accountCreationEvents
      .subscribe(this::onAccountEvent)
      .let(subscriptions::add)
    Observable.combineLatest(this.viewModel.isLoading, this.viewModel.accounts, ::Pair)
      .subscribe { displayAccounts(it.first, it.second) }
      .let(subscriptions::add)

    requestLocationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.clear()
    runningAnimations.forEach(Animator::end)
  }

  private fun displayAccounts(isLoading: Boolean, accounts: List<AccountProviderDescription>) {
    accountListAdapter.submitList(accounts)
    this.reload?.isEnabled = !isLoading
    val (showNoLocationText, showList) = when {
      isLoading -> {
        this.progress.show()
        false to false
      }
      else -> {
        this.progress.hide()
        val isEmpty = accounts.isEmpty()
        isEmpty to !isEmpty
      }
    }
    noLocation.isVisible = showNoLocationText
    accountList.isVisible = showList
  }

  private fun onRegisteredAccountClicked(account: AccountType) {
    this.listener.post(AccountListEvent.AccountSelected(account.id))
  }

  private fun onRegisteredAccountDeleted(account: AccountType) {
    val context = this.requireContext()
    AlertDialog.Builder(context)
      .setTitle(R.string.accountsDeleteConfirmTitle)
      .setMessage(
        context.getString(
          R.string.accountsDeleteConfirm,
          account.provider.displayName
        )
      )
      .setNegativeButton(R.string.cancel) { dialog, _ ->
        dialog.dismiss()
      }
      .setPositiveButton(R.string.accountsDelete) { dialog, _ ->
        this.viewModel.deleteAccountByProvider(account.provider.id)
        dialog.dismiss()
      }
      .create()
      .show()
  }

  private fun onAccountClicked(account: AccountProviderDescription) {
    this.logger.debug("selected account: {} ({})", account.id, account.title)
    this.viewModel.createAccount(account.id)
  }

  private fun configureToolbar() {
    this.supportActionBar?.apply {
      title = getString(R.string.accountAdd)
      subtitle = null
    }
  }

  private fun reload() {
    this.reload?.isEnabled = false
    this.viewModel.refreshAccountRegistry()
  }

  private fun toggleRegisteredLibraries() {
    val animators = if (registeredAccountsList.isGone) {
      registeredAccountsHeaderDivider.isGone = false
      listOf(
        registeredAccountsList.verticalExpand(),
        registeredAccountsHeaderIcon.animateRotate(180f)
      )
    } else {
      registeredAccountsHeaderDivider.isGone = true
      listOf(
        registeredAccountsList.verticalShrink(),
        registeredAccountsHeaderIcon.animateRotate(0f)
      )
    }
    runningAnimations += AnimatorSet().apply {
      playTogether(animators)
      start()
    }
  }

  private fun View.animateRotate(to: Float): Animator {
    return ValueAnimator.ofFloat(rotation, to).apply {
      addUpdateListener { rotation = it.animatedValue as Float }
    }
  }

  private fun View.verticalShrink(): Animator {
    val sizeAnimator = ValueAnimator.ofInt(height, 0).apply {
      addUpdateListener {
        val animatedValue = it.animatedValue as Int
        duration = 250L
        updateLayoutParams { height = animatedValue }
      }
      doOnEnd { isGone = true }
    }
    val visibilityAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
      duration = 175L
      startDelay = 75L
      addUpdateListener {
        alpha = it.animatedValue as Float
      }
    }
    return AnimatorSet().apply {
      playTogether(sizeAnimator, visibilityAnimator)
    }
  }

  private fun View.verticalExpand(): Animator {
    val matchParentMeasureSpec =
      View.MeasureSpec.makeMeasureSpec((parent as View).width, View.MeasureSpec.EXACTLY)
    val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    measure(matchParentMeasureSpec, wrapContentMeasureSpec)
    val targetHeight: Int = measuredHeight
    val sizeAnimator = ValueAnimator.ofInt(height, targetHeight).apply {
      addUpdateListener {
        val animatedValue = it.animatedValue as Int
        duration = 250L
        updateLayoutParams { height = animatedValue }
      }
      doOnStart { isGone = false }
    }
    val visibilityAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = 175L
      startDelay = 75L
      addUpdateListener {
        alpha = it.animatedValue as Float
      }
    }
    return AnimatorSet().apply {
      playTogether(sizeAnimator, visibilityAnimator)
    }
  }

  private fun updateRegisteredAccounts(accounts: List<AccountType>) {
    registeredAccountListAdapter.submitList(accounts)
    if (registeredAccountsList.isVisible)
      runningAnimations += registeredAccountsList.verticalExpand().apply {
        start()
      }
  }

  private fun onAccountEvent(event: AccountEvent) {
    when (event) {
      is AccountEventCreation.AccountEventCreationSucceeded -> {
        if (!parameters.addMultipleAccounts)
          this.listener.post(AccountListEvent.AccountCreated)
      }
      is AccountEventCreation.AccountEventCreationFailed -> {
        this.showAccountCreationFailedDialog(event)
      }
    }
  }

  private fun showAccountCreationFailedDialog(accountEvent: AccountEventCreation.AccountEventCreationFailed) {
    this.logger.debug("showAccountCreationFailedDialog")

    if (this.errorDialog == null) {
      val newDialog =
        AlertDialog.Builder(this.requireActivity())
          .setTitle(R.string.accountCreationFailed)
          .setMessage(R.string.accountCreationFailedMessage)
          .setPositiveButton(R.string.accountsDetails) { dialog, _ ->
            this.errorDialog = null
            this.showErrorPage(accountEvent)
            dialog.dismiss()
          }.create()
      this.errorDialog = newDialog
      newDialog.show()
    }
  }

  private fun showErrorPage(accountEvent: AccountEventCreation.AccountEventCreationFailed) {
    val parameters =
      ErrorPageParameters(
        emailAddress = this.viewModel.supportEmailAddress,
        body = "",
        subject = "[simplye-error-report]",
        attributes = accountEvent.attributes.toSortedMap(),
        taskSteps = accountEvent.taskResult.steps
      )
    this.listener.post(AccountListEvent.OpenErrorPage(parameters))
  }

  @SuppressLint("MissingPermission")
  private fun getLocation(isPermissionGranted: Boolean) = viewModel.getLocation(isPermissionGranted)
}
