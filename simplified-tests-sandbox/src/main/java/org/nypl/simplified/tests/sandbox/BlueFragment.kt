package org.nypl.simplified.tests.sandbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import org.slf4j.LoggerFactory

class BlueFragment : Fragment() {

  private lateinit var button: Button
  private val logger = LoggerFactory.getLogger(BlueFragment::class.java)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val layout = inflater.inflate(R.layout.blue, container, false)

    this.button = layout.findViewById<Button>(R.id.blueButton)
    this.button.setOnClickListener {
      val intent = Intent(Intent.ACTION_VIEW)
      intent.setData(Uri.parse("http://www.io7m.com"))
      startActivity(intent)
    }
    return layout
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    logger.debug("onCreate")
  }

  override fun onStop() {
    super.onStop()
    logger.debug("onStop")
  }

  override fun onStart() {
    super.onStart()
    logger.debug("onStart")
  }

  override fun onPause() {
    super.onPause()
    logger.debug("onPause")
  }

  override fun onDetach() {
    super.onDetach()
    logger.debug("onDetach")
  }
}
