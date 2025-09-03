/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.*
import code.name.monkey.retromusic.adapter.base.MediaEntryViewHolder
import code.name.monkey.retromusic.databinding.NumberRollViewBinding
import code.name.monkey.retromusic.db.PlaylistWithSongs
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.albumCoverOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.artistImageOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.menu.SongMenuHelper
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Genre
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.views.NumberRollView
import com.bumptech.glide.Glide
import java.util.*

class SearchAdapter(
    private val activity: FragmentActivity,
    private var dataSet: List<Any>,
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>(), ActionMode.Callback {

    // same as in AbsMultiSelectAdapter
    var actionMode: ActionMode? = null
    private val checked: MutableList<Song> = ArrayList()

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val inflater = mode?.menuInflater
        inflater?.inflate(R.menu.menu_media_selection, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_multi_select_adapter_check_all) {
            checkAll()
        } else {
            onMultipleItemAction(item!!, ArrayList(checked))
            maybeFinishActionMode()
            clearChecked()
        }
        return true
    }

    @Suppress("DEPRECATION")
    override fun onDestroyActionMode(mode: ActionMode?) {
        clearChecked()
        activity.window.statusBarColor = when {
            VersionUtils.hasMarshmallow() -> Color.TRANSPARENT
            else -> Color.BLACK
        }
        actionMode = null
        maybeFinishActionMode()
        onBackPressedCallback.remove()
    }

    private fun checkAll() {
        if (actionMode != null) {
            checked.clear()
            for (i in 0 until itemCount) {
                val identifier = getIdentifier(i)
                checked.add(identifier)
            }
            notifyDataSetChanged()
            updateCab()
        }
    }

    private fun getIdentifier(position: Int): Song {
        return (dataSet[position] as Song)
    }

    private fun getName(model: Song) = model.title

    private fun isChecked(identifier: Song): Boolean {
        return checked.contains(identifier)
    }

    private val isInQuickSelectMode: Boolean
        get() = actionMode != null

    private fun onMultipleItemAction(menuItem: MenuItem, selection: ArrayList<Song>) {
        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.itemId)
    }

    private fun toggleChecked(position: Int): Boolean {
        val identifier = getIdentifier(position)
        if (!checked.remove(identifier)) {
            checked.add(identifier)
        }
        notifyItemChanged(position)
        updateCab()
        return true
    }

    private fun clearChecked() {
        checked.clear()
        notifyDataSetChanged()
    }

    private fun updateCab() {
        if (actionMode == null) {
            actionMode = activity.startActionMode(this)?.apply {
                customView = NumberRollViewBinding.inflate(activity.layoutInflater).root
            }
            activity.onBackPressedDispatcher.addCallback(onBackPressedCallback)
            activity.supportFragmentManager.findFragmentById(R.id.miniPlayerFragment)?.view?.isClickable = false
        }
        val size = checked.size
        when {
            size <= 0 -> {
                maybeFinishActionMode()
            }
            else -> {
                actionMode?.customView?.findViewById<NumberRollView>(R.id.selection_mode_number)
                    ?.setNumber(size, true)
            }
        }
    }

    private fun maybeFinishActionMode(): Boolean {
        activity.supportFragmentManager.findFragmentById(R.id.miniPlayerFragment)?.view?.isClickable = true
        if (actionMode != null) {
            actionMode?.finish()
            return true
        }
        return false
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (maybeFinishActionMode())
                remove()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapDataSet(dataSet: List<Any>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (dataSet[position] is Album) return ALBUM
        if (dataSet[position] is Artist) return if ((dataSet[position] as Artist).isAlbumArtist) ALBUM_ARTIST else ARTIST
        if (dataSet[position] is Genre) return GENRE
        if (dataSet[position] is PlaylistWithSongs) return PLAYLIST
        return if (dataSet[position] is Song) SONG else HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            HEADER -> ViewHolder(
                LayoutInflater.from(activity).inflate(
                    R.layout.sub_header,
                    parent,
                    false
                ), viewType
            )

            ALBUM, ARTIST, ALBUM_ARTIST -> ViewHolder(
                LayoutInflater.from(activity).inflate(
                    R.layout.item_list_big,
                    parent,
                    false
                ), viewType
            )

            else -> ViewHolder(
                LayoutInflater.from(activity).inflate(R.layout.item_list, parent, false),
                viewType
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ALBUM -> {
                holder.imageTextContainer?.isVisible = true
                val album = dataSet[position] as Album
                holder.title?.text = album.title
                holder.text?.text = album.artistName
                Glide.with(activity).asDrawable().albumCoverOptions(album.safeGetFirstSong())
                    .load(RetroGlideExtension.getSongModel(album.safeGetFirstSong()))
                    .into(holder.image!!)
            }

            ARTIST -> {
                holder.imageTextContainer?.isVisible = true
                val artist = dataSet[position] as Artist
                holder.title?.text = artist.name
                holder.text?.text = MusicUtil.getArtistInfoString(activity, artist)
                Glide.with(activity).asDrawable().artistImageOptions(artist).load(
                    RetroGlideExtension.getArtistModel(artist)
                ).into(holder.image!!)
            }

            SONG -> {
                holder.itemView.isActivated = isChecked(dataSet[position] as Song)
                holder.imageTextContainer?.isVisible = true
                val song = dataSet[position] as Song
                holder.title?.text = song.title
                holder.text?.text = song.albumName
                Glide.with(activity).asDrawable().songCoverOptions(song)
                    .load(RetroGlideExtension.getSongModel(song)).into(holder.image!!)
            }

            GENRE -> {
                val genre = dataSet[position] as Genre
                holder.title?.text = genre.name
                holder.text?.text = String.format(
                    Locale.getDefault(),
                    "%d %s",
                    genre.songCount,
                    if (genre.songCount > 1) activity.getString(R.string.songs) else activity.getString(
                        R.string.song
                    )
                )
            }

            PLAYLIST -> {
                val playlist = dataSet[position] as PlaylistWithSongs
                holder.title?.text = playlist.playlistEntity.playlistName
                //holder.text?.text = MusicUtil.playlistInfoString(activity, playlist.songs)
            }

            ALBUM_ARTIST -> {
                holder.imageTextContainer?.isVisible = true
                val artist = dataSet[position] as Artist
                holder.title?.text = artist.name
                holder.text?.text = MusicUtil.getArtistInfoString(activity, artist)
                Glide.with(activity).asDrawable().artistImageOptions(artist).load(
                    RetroGlideExtension.getArtistModel(artist)
                ).into(holder.image!!)
            }

            else -> {
                holder.title?.text = dataSet[position].toString()
                holder.title?.setTextColor(ThemeStore.accentColor(activity))
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    inner class ViewHolder(itemView: View, itemViewType: Int) : MediaEntryViewHolder(itemView) {
        init {
            itemView.setOnLongClickListener(null)

            imageTextContainer?.isInvisible = true
            if (itemViewType == SONG) {
                itemView.setOnLongClickListener { _ ->
                    if (isChecked(getIdentifier(layoutPosition))) {
                        onActionItemClicked(actionMode, actionMode!!.menu!!.findItem(R.id.action_play_next))
                    } else {
                        if (MusicPlayerRemote.isPlaying) {
                            MusicPlayerRemote.playNext(dataSet[layoutPosition] as Song, quiet = true)
                            MusicPlayerRemote.playNextSong()
                        } else {
                            toggleChecked(layoutPosition)
                        }
                    }

                    true
                }

                imageTextContainer?.isGone = true
                menu?.isVisible = true
                menu?.setOnClickListener(object : SongMenuHelper.OnClickSongMenu(activity) {
                    override val song: Song
                        get() = dataSet[layoutPosition] as Song
                })
            } else {
                menu?.isVisible = false
            }

            when (itemViewType) {
                ALBUM -> setImageTransitionName(activity.getString(R.string.transition_album_art))
                ARTIST -> setImageTransitionName(activity.getString(R.string.transition_artist_image))
                else -> {
                    val container = itemView.findViewById<View>(R.id.imageContainer)
                    container?.isVisible = false
                }
            }
        }

        override fun onClick(v: View?) {
            val item = dataSet[layoutPosition]
            if (actionMode != null && itemViewType != SONG)
                return

            when (itemViewType) {
                ALBUM -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.albumDetailsFragment,
                        bundleOf(EXTRA_ALBUM_ID to (item as Album).id)
                    )
                }

                ARTIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.artistDetailsFragment,
                        bundleOf(EXTRA_ARTIST_ID to (item as Artist).id)
                    )
                }

                ALBUM_ARTIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.albumArtistDetailsFragment,
                        bundleOf(EXTRA_ARTIST_NAME to (item as Artist).name)
                    )
                }

                GENRE -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.genreDetailsFragment,
                        bundleOf(EXTRA_GENRE to (item as Genre))
                    )
                }

                PLAYLIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.playlistDetailsFragment,
                        bundleOf(EXTRA_PLAYLIST_ID to (item as PlaylistWithSongs).playlistEntity.playListId)
                    )
                }

                SONG -> {
                    if (MusicPlayerRemote.isPlaying) {
                        toggleChecked(layoutPosition)
                    } else {
                        MusicPlayerRemote.playNext(item as Song, quiet = true)
                        MusicPlayerRemote.playNextSong()
                    }
                }
            }
        }
    }

    companion object {
        private const val HEADER = 0
        private const val ALBUM = 1
        private const val ARTIST = 2
        private const val SONG = 3
        private const val GENRE = 4
        private const val PLAYLIST = 5
        private const val ALBUM_ARTIST = 6
    }
}
