package org.nypl.simplified.cardcreator

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import org.nypl.simplified.cardcreator.databinding.ActivityCardCreatorBinding
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.viewmodel.CardCreatorViewModelFactory

/**
 * Main view responsible for patrons to create library cards
 * Should be with startActivityForResult
 */
class CardCreatorActivity : AppCompatActivity() {

  companion object {
    const val CARD_CREATED = Activity.RESULT_OK
    const val CARD_CREATION_CANCELED = Activity.RESULT_CANCELED
    const val CHILD_CARD_CREATED = Activity.RESULT_FIRST_USER
    const val CARD_CREATION_ERROR = Activity.RESULT_FIRST_USER + 1
  }

  private lateinit var binding: ActivityCardCreatorBinding

  private val cardCreatorService: CardCreatorService by lazy {
    CardCreatorService(
      this.intent.getStringExtra("clientId")!!,
      this.intent.getStringExtra("clientSecret")!!
    )
  }

  private val defaultViewModelFactory by lazy {
    CardCreatorViewModelFactory(cardCreatorService)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCardCreatorBinding.inflate(layoutInflater)
    setContentView(binding.root)
    if (intent.getBooleanExtra("isLoggedIn", false)) {
      Cache(this).isJuvenileCard = true
      binding.toolbar.setTitle(R.string.create_child_card)
    }
    window.statusBarColor = Color.BLACK
    binding.toolbar.apply {
      navigationIcon = navigationIcon?.also { DrawableCompat.setTint(it, Color.WHITE) }
      setTitleTextColor(Color.WHITE)
      setNavigationOnClickListener {
        if (!findNavController(binding.cardCreatorNavHostFragment.id).popBackStack()) {
          onBackPressed()
        }
      }
    }
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return defaultViewModelFactory
  }

  override fun onDestroy() {
    super.onDestroy()
    Cache(this).clear()
  }
}
