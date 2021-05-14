package org.nypl.simplified.cardcreator

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.navigation.findNavController
import org.nypl.simplified.cardcreator.databinding.ActivityCardCreatorBinding
import org.nypl.simplified.cardcreator.utils.Cache

/**
 * Main view responsible for patrons to create library cards
 * Should be with startActivityForResult
 */
class CardCreatorActivity : AppCompatActivity() {

  private lateinit var binding: ActivityCardCreatorBinding

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

  override fun onDestroy() {
    super.onDestroy()
    Cache(this).clear()
  }
}
