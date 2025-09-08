/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package code.name.monkey.retromusic.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.annotation.RequiresApi
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.activities.base.AbsThemeActivity
import code.name.monkey.retromusic.databinding.ActivityRuneBinding
import code.name.monkey.retromusic.patternlockview.PatternLockView
import code.name.monkey.retromusic.patternlockview.PatternLockView.AspectRatio
import code.name.monkey.retromusic.patternlockview.PatternLockView.PatternViewMode.AUTO_DRAW
import code.name.monkey.retromusic.patternlockview.listener.PatternLockViewListener
import code.name.monkey.retromusic.patternlockview.utils.PatternCoder
import code.name.monkey.retromusic.util.PreferenceUtil
import kotlin.random.Random


class RuneActivity : AbsThemeActivity() {
    private lateinit var binding: ActivityRuneBinding

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateNewRuneForAlbum(): List<PatternLockView.Dot> {
        while(true) {
            val rune = PatternCoder.findRandomDotPattern3x3(Random.nextInt(5, 8))
            val codedRune = PatternCoder.encodeDotsToString(rune)

            // check if rune unused
            if (PreferenceUtil.getAlbumForRune(codedRune) == 0L)
                return rune
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRuneBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        ToolbarContentTintHelper.colorBackButton(binding.toolbar)

        with(binding.patternInput) {
            aspectRatio = AspectRatio.ASPECT_RATIO_SQUARE

            val albumId = intent.extras?.getLong("albumId")
            if (albumId != null) {
                // album view mode
                // get the rune from sharedprefs if it exists; otherwise make a new one
                val oldEncodedRune = PreferenceUtil.getAlbumRune(albumId)
                var albumRune: List<PatternLockView.Dot>?
                if (oldEncodedRune == null) {
                    albumRune = generateNewRuneForAlbum()
                    val newEncodedRune = PatternCoder.encodeDotsToString(albumRune)
                    PreferenceUtil.setAlbumRune(albumId, newEncodedRune)
                } else {
                    Log.e("Runes", "got rune: \"$oldEncodedRune\"")
                    albumRune = PatternCoder.decodeStringToDots(oldEncodedRune)
                }

                isInputEnabled = false
                setPattern(AUTO_DRAW, albumRune)
            } else {
                // input mode by default
                isInputEnabled = true
            }

            addPatternLockListener(object : PatternLockViewListener {
                override fun onStarted() {
                }

                override fun onProgress(progressPattern: List<PatternLockView.Dot>) {
                }

                override fun onComplete(pattern: List<PatternLockView.Dot>) {
                    isInputEnabled = false
                    val encodedPattern = PatternCoder.encodeDotsToString(pattern)
                    val newAlbumId = PreferenceUtil.getAlbumForRune(encodedPattern)
                    if (newAlbumId != 0L) {
                        setViewMode(PatternLockView.PatternViewMode.CORRECT)
                        setResult(420, Intent().apply {
                            putExtra("albumId", newAlbumId)
                        })
                        finish()
                    } else {
                        setViewMode(PatternLockView.PatternViewMode.WRONG)
                        postDelayed({
                            clearPattern()
                            isInputEnabled = true
                        }, 650)
                    }
                }

                override fun onCleared() {
                }
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
