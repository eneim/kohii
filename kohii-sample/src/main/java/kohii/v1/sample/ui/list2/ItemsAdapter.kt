package kohii.v1.sample.ui.list2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kohii.v1.exoplayer.Kohii
import kohii.v1.sample.R

class ItemsAdapter(
  private val kohii: Kohii,
) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return ViewHolder(inflater.inflate(R.layout.holder_player_view, parent, false))
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val container = holder.itemView.findViewById<AspectRatioFrameLayout>(R.id.playerContainer)
    val playerView = holder.itemView.findViewById<PlayerView>(R.id.playerView)

    container.setAspectRatio(1f)

    val mediaLink = VIDEOS[position]
    val itemTag = "$position-$mediaLink"

    kohii.setUp(mediaLink) {
      tag = itemTag
      preload = true
      repeatMode = Player.REPEAT_MODE_ONE
    }
        .bind(playerView)
  }

  override fun getItemCount(): Int = VIDEOS.size

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

  companion object {

    val VIDEOS = arrayOf(
        "https://leonardo.osnova.io/03acd28c-beb5-50cd-95b2-885914ab7b5e/-/format/mp4/",
        "https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_480_1_5MG.mp4",
        "https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_640_3MG.mp4",
        "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4",
        "https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4",
        "https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_2mb.mp4",
        "https://sample-videos.com/video123/mp4/480/big_buck_bunny_480p_1mb.mp4",
    )
  }
}
