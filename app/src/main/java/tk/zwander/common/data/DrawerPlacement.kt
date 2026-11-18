package tk.zwander.common.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Coordinates are fractions of the usable drawer page, independent of screen density. */
@Parcelize
data class DrawerPlacement(val page: Int, val x: Float, val y: Float, val w: Float, val h: Float) : Parcelable {
    fun moved(dx: Float, dy: Float) = copy(x = (x + dx).coerceIn(0f, 1f - w), y = (y + dy).coerceIn(0f, 1f - h))

    fun resized(dw: Float, dh: Float, minW: Float, minH: Float): DrawerPlacement {
        val roomW = (1f - x).coerceAtLeast(0f)
        val roomH = (1f - y).coerceAtLeast(0f)
        return copy(w = (w + dw).coerceIn(minW.coerceAtMost(roomW), roomW),
            h = (h + dh).coerceIn(minH.coerceAtMost(roomH), roomH))
    }

    fun resizeLeft(dx: Float, minWidth: Float): DrawerPlacement {
        val right = (x + w).coerceAtMost(1f)
        val left = (x + dx).coerceIn(0f, (right - minWidth).coerceAtLeast(0f))
        return copy(x = left, w = right - left)
    }

    fun resizeTop(dy: Float, minHeight: Float): DrawerPlacement {
        val bottom = (y + h).coerceAtMost(1f)
        val top = (y + dy).coerceIn(0f, (bottom - minHeight).coerceAtLeast(0f))
        return copy(y = top, h = bottom - top)
    }

    companion object {
        fun next(used: List<DrawerPlacement>, width: Float, height: Float): DrawerPlacement {
            for (page in 0..((used.maxOfOrNull { it.page } ?: -1) + 1)) {
                val items = used.filter { it.page == page }
                val xs = (listOf(0f) + items.map { it.x + it.w }).distinct().sorted()
                val ys = (listOf(0f) + items.map { it.y + it.h }).distinct().sorted()
                for (y in ys) for (x in xs) {
                    if (x + width > 1f || y + height > 1f) continue
                    if (items.none { x < it.x + it.w && x + width > it.x && y < it.y + it.h && y + height > it.y }) {
                        return DrawerPlacement(page, x, y, width, height)
                    }
                }
            }
            error("Widget dimensions must fit a drawer page")
        }
    }
}
