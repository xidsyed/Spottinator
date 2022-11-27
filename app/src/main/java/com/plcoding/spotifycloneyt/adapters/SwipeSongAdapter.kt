package com.plcoding.spotifycloneyt.adapters

import androidx.recyclerview.widget.AsyncListDiffer
import com.plcoding.spotifycloneyt.R
import kotlinx.android.synthetic.main.list_item.view.*

class SwipeSongAdapter : BaseSongAdapter(R.layout.swipe_item) {
    override var differ = AsyncListDiffer(this, diffCallback)
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply{
            val text = "${song.title} - ${song.artist}"
            tvPrimary.text = text
            setOnClickListener{
                onItemClickListener?.let { callback ->
                    callback(song)
                }
            }

        }
    }

}