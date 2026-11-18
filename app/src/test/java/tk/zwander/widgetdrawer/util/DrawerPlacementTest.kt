package tk.zwander.widgetdrawer.util

import org.junit.Assert.assertEquals
import org.junit.Test
import tk.zwander.common.data.DrawerPlacement

class DrawerPlacementTest {
    @Test fun leftHandleKeepsTheRightEdgeFixed() {
        val task = DrawerPlacement(0, .25f, .125f, .5f, .5f)
        assertEquals(task.copy(x = .375f, w = .375f), task.resizeLeft(.125f, .125f))
        assertEquals(task.copy(x = 0f, w = .75f), task.resizeLeft(-1f, .125f))
        assertEquals(task.copy(x = .625f, w = .125f), task.resizeLeft(1f, .125f))
    }

    @Test fun topHandleKeepsTheBottomEdgeFixed() {
        val task = DrawerPlacement(0, .25f, .125f, .5f, .5f)
        assertEquals(task.copy(y = .25f, h = .375f), task.resizeTop(.125f, .125f))
        assertEquals(task.copy(y = 0f, h = .625f), task.resizeTop(-1f, .125f))
        assertEquals(task.copy(y = .5f, h = .125f), task.resizeTop(1f, .125f))
    }

    @Test fun resizingATaskListUsesBothAxesAndKeepsItsOrigin() {
        val task = DrawerPlacement(0, .25f, .5f, .25f, .25f)
        assertEquals(task.copy(w = .5f, h = .375f), task.resized(.25f, .125f, .05f, .05f))
        assertEquals(task.copy(w = .75f, h = .5f), task.resized(1f, 1f, .05f, .05f))
        assertEquals(task.copy(w = .05f, h = .05f), task.resized(-1f, -1f, .05f, .05f))
    }
    @Test fun newItemsFillHolesThenGetTheirOwnPage() {
        val right = DrawerPlacement(0, .5f, 0f, .5f, 1f)
        val left = DrawerPlacement.next(listOf(right), .5f, 1f)
        assertEquals(right.copy(x = 0f), left)
        assertEquals(DrawerPlacement(1, 0f, 0f, .75f, .5f), DrawerPlacement.next(listOf(left, right), .75f, .5f))
    }
}
