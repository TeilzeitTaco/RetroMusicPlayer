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
package code.name.monkey.retromusic.adapter.song

import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.glide.RetroMusicColoredTarget
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicPlayerRemote.isPlaying
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.ViewUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import com.bumptech.glide.Glide
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem
import me.zhanghai.android.fastscroll.PopupTextProvider

class PlayingQueueAdapter(
    activity: FragmentActivity,
    itemLayoutRes: Int,
) : SongAdapter(activity, MusicPlayerRemote.playingQueue, itemLayoutRes),
    DraggableItemAdapter<PlayingQueueAdapter.ViewHolder>,
    SwipeableItemAdapter<PlayingQueueAdapter.ViewHolder>,
    PopupTextProvider {

    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val song = dataSet[position]
        holder.time?.text = MusicUtil.getReadableDurationString(song.duration)
        if (holder.itemViewType == HISTORY) {
            // this is the fade to black
            setAlpha(holder, 0.45f * (position.toFloat() / MusicPlayerRemote.position.toFloat()) + 0.165f)
        } else if (holder.itemViewType == CURRENT) {
            Glide.with(activity)
                .asBitmapPalette()
                .songCoverOptions(song)
                .load(RetroGlideExtension.getSongModel(song))
                .into(object : RetroMusicColoredTarget(holder.image!!) {
                    override fun onColorReady(colors: MediaNotificationProcessor) {
                        holder.title?.setTextColor(colors.secondaryTextColor)
                        holder.text?.setTextColor(colors.primaryTextColor)
                        holder.itemView.setBackgroundColor(colors.backgroundColor)
                    }
                })
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position < MusicPlayerRemote.position) {
            return HISTORY
        } else if (position > MusicPlayerRemote.position) {
            return UP_NEXT
        }
        return CURRENT
    }

    override fun loadAlbumCover(song: Song, holder: SongAdapter.ViewHolder) {
        if (holder.image == null) {
            return
        }
        Glide.with(activity)
            .load(RetroGlideExtension.getSongModel(song))
            .songCoverOptions(song)
            .into(holder.image!!)
    }

    private fun setAlpha(holder: SongAdapter.ViewHolder, alpha: Float) {
        with(holder) {
            image?.alpha = alpha
            title?.alpha = alpha
            text?.alpha = alpha
            paletteColorContainer?.alpha = alpha
            dragView?.alpha = alpha
            menu?.alpha = alpha
        }
    }

    override fun getPopupText(position: Int): String {
        return MusicUtil.getSectionName(dataSet[position].title)
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean {
        return ViewUtil.hitTest(holder.imageText!!, x, y) || ViewUtil.hitTest(
            holder.dragView!!,
            x,
            y
        )
    }

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange? {
        return null
    }

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        MusicPlayerRemote.moveSong(fromPosition, toPosition)
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean {
        return true
    }

    override fun onItemDragStarted(position: Int) {
        notifyDataSetChanged()
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : SongAdapter.ViewHolder(itemView) {
        @DraggableItemStateFlags
        private var mDragStateFlags: Int = 0

        override var songMenuRes: Int
            get() = R.menu.menu_item_playing_queue_song
            set(value) {
                super.songMenuRes = value
            }

        init {
            dragView?.isVisible = true
        }

        override fun onClick(v: View?) {
            if (!isPlaying)
                MusicPlayerRemote.playSongAt(layoutPosition)
            else {
                toggleChecked(layoutPosition)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            if (isChecked(getIdentifier(layoutPosition)!!)) {
                MusicPlayerRemote.removeFromQueue(checked)
                MusicPlayerRemote.playNext(checked, true)
                Toast.makeText(activity, "Moved ${checked.size} tracks to top of queue!",
                    Toast.LENGTH_SHORT).show()
                clearChecked()
                maybeFinishActionMode()
            } else {
                if (isPlaying) {
                    MusicPlayerRemote.playSongAt(layoutPosition)
                } else {
                    toggleChecked(layoutPosition)
                }
            }
            return true
        }

        override fun onSongMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_remove_from_playing_queue -> {
                    MusicPlayerRemote.removeFromQueue(layoutPosition)
                    notifyDataSetChanged()
                    return true
                }
            }
            return super.onSongMenuItemClick(item)
        }

        @DraggableItemStateFlags
        override fun getDragStateFlags(): Int {
            return mDragStateFlags
        }

        override fun setDragStateFlags(@DraggableItemStateFlags flags: Int) {
            mDragStateFlags = flags
        }

        override fun getSwipeableContainerView(): View {
            return dummyContainer!!
        }
    }

    companion object {
        private const val HISTORY = 0
        private const val CURRENT = 1
        private const val UP_NEXT = 2
    }

    override fun onSwipeItem(holder: ViewHolder, position: Int, result: Int): SwipeResultAction {
        return if (result == SwipeableItemConstants.RESULT_CANCELED) {
            SwipeResultActionDefault()
        } else {
            SwipedResultActionRemoveItem(this, position)
        }
    }

    override fun onGetSwipeReactionType(holder: ViewHolder, position: Int, x: Int, y: Int): Int {
        return if (onCheckCanStartDrag(holder, position, x, y)) {
            SwipeableItemConstants.REACTION_CAN_NOT_SWIPE_BOTH_H
        } else {
            SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
        }
    }

    override fun onSwipeItemStarted(holder: ViewHolder, p1: Int) {
    }

    override fun onSetSwipeBackground(holder: ViewHolder, position: Int, result: Int) {
    }

    inner class SwipedResultActionRemoveItem(
        private val adapter: PlayingQueueAdapter,
        private val position: Int,
    ) : SwipeResultActionRemoveItem() {

        override fun onPerformAction() {
            // currentlyShownSnackbar = null
        }

        override fun onSlideAnimationEnd() {
            // Swipe animation is much smoother when we do the heavy lifting after it's completed
            MusicPlayerRemote.removeFromQueue(adapter.dataSet[position])
            notifyDataSetChanged()
        }
    }
}
