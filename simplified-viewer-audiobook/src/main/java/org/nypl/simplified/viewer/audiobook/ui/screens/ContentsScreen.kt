package org.nypl.simplified.viewer.audiobook.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.nypl.simplified.viewer.audiobook.R
import org.readium.r2.shared.publication.Link

internal interface ContentsScreenListener {

  fun onTocItemCLicked(link: Link)
}

@Composable
internal fun ContentsScreen(
  links: List<Link>,
  listener: ContentsScreenListener
) {
  Scaffold { paddingValues ->
    Box(
      modifier = Modifier
        .padding(paddingValues)
    ) {
      Contents(
        modifier = Modifier
          .fillMaxSize(),
        links = links,
        onItemClick = { listener.onTocItemCLicked(it) }
      )
    }
  }
}

@Composable
private fun Contents(
  modifier: Modifier = Modifier,
  links: List<Link>,
  onItemClick: (Link) -> Unit
) {
  LazyColumn(
    modifier = modifier
  ) {
    itemsIndexed(links, key = null) { index, link ->
      TocItem(
        modifier = Modifier.fillMaxWidth(),
        index = index,
        link = link,
        onClick = { onItemClick(link) }
      )
      Divider()
    }
  }
}

@Composable
private fun TocItem(
  modifier: Modifier,
  index: Int,
  link: Link,
  onClick: () -> Unit
) {
  Box(
     modifier = modifier
       .height(80.dp)
       .clickable { onClick() }
       .padding(15.dp),
     contentAlignment = Alignment.CenterStart
  ) {
    Text(
      modifier = Modifier,
      text = link.title
        ?: stringResource(R.string.audio_book_player_toc_chapter_n, index),
    )
  }
}
