package org.nypl.simplified.cardcreator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.nypl.simplified.cardcreator.databinding.ActivityCardCreatorBinding

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
  }
}
