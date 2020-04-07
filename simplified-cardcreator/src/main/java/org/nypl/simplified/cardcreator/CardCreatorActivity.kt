package org.nypl.simplified.cardcreator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.nypl.simplified.cardcreator.databinding.ActivityCardCreatorBinding
import org.slf4j.LoggerFactory

/**
 * Main view responsible for patrons to create library cards
 * Should be with startActivityForResult
 */
class CardCreatorActivity : AppCompatActivity() {

  private val logger = LoggerFactory.getLogger(CardCreatorActivity::class.java)

  private lateinit var binding: ActivityCardCreatorBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCardCreatorBinding.inflate(layoutInflater)
    setContentView(binding.root)
  }

  override fun onResume() {
    super.onResume()
  }
}
