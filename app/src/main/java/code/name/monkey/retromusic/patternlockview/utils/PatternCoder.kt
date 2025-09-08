package code.name.monkey.retromusic.patternlockview.utils

import android.os.Build
import android.util.ArraySet
import androidx.annotation.RequiresApi
import code.name.monkey.retromusic.patternlockview.PatternLockView
import com.afollestad.viewpagerdots.DotsIndicator
import kotlin.random.Random

object PatternCoder {
    @RequiresApi(Build.VERSION_CODES.M)
    fun getNeighbours(gridSize: Int): Array<Array<ArraySet<Pair<Int, Int>>>> {
        val array = Array(gridSize) { Array<ArraySet<Pair<Int, Int>>>(gridSize) {
            _ -> ArraySet()
        } }

        for (y in 0..array.lastIndex) {
            for (x in 0..array[y].lastIndex) {
                val hasAbove = (y - 1) >= 0
                val hasBelow = (y + 1) < array.size
                val hasLeft = (x - 1) >= 0
                val hasFarLeft = (x - 2) >= 0
                val hasRight = (x + 1) < array[y].size
                val hasFarRight = (x + 2) < array[y].size

                with(array[y][x]) {
                    if (hasAbove) {
                        add(Pair(y - 1, x))
                        if (hasLeft)
                            add(Pair(y - 1, x - 1))
                        if (hasRight)
                            add(Pair(y - 1, x + 1))
                        if (hasFarLeft)
                            add(Pair(y - 1, x - 2))
                        if (hasFarRight)
                            add(Pair(y - 1, x + 2))
                    }

                    if (hasBelow) {
                        add(Pair(y + 1, x))
                        if (hasLeft)
                            add(Pair(y + 1, x - 1))
                        if (hasRight)
                            add(Pair(y + 1, x + 1))
                        if (hasFarLeft)
                            add(Pair(y + 1, x - 2))
                        if (hasFarRight)
                            add(Pair(y + 1, x + 2))
                    }

                    if (hasRight)
                        add(Pair(y, x + 1))
                    if (hasLeft)
                        add(Pair(y, x - 1))
                }
            }
        }

        return array
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun findRandomPattern(gridSize: Int, length: Int): ArrayList<Pair<Int, Int>> {
        val array = getNeighbours(gridSize)
        val pattern = ArrayList<Pair<Int, Int>>()
        pattern.add(Pair(
            Random.nextInt(gridSize), Random.nextInt(gridSize)
        ))

        while(pattern.size < length) {
            val point = pattern.last()
            val freshNeighbours = array[point.first][point.second].subtract(pattern)
            if (freshNeighbours.isEmpty()) {
                return findRandomPattern(gridSize, length)
            }

            val newPoint = freshNeighbours.toList().shuffled().first()
            pattern.add(newPoint)
        }

        return pattern
    }

    fun toDots(pattern: ArrayList<Pair<Int, Int>>): List<PatternLockView.Dot> {
        return pattern.map { PatternLockView.Dot.of(it.first, it.second) }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun findRandomDotPattern3x3(length: Int): List<PatternLockView.Dot> {
        val pattern = findRandomPattern(3, length)
        return toDots(pattern)
    }

    fun encodeDotsToString(pattern: List<PatternLockView.Dot>): String {
        return pattern.map { it.id }.joinToString(":")
    }

    fun decodeStringToDots(s: String): List<PatternLockView.Dot> {
        return s.split(":").map { PatternLockView.Dot.of(it.toInt()) }
    }
}