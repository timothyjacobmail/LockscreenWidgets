package tk.zwander.widgetdrawer.util

import org.junit.Assert.assertEquals
import org.junit.Test
import tk.zwander.common.data.DrawerPlacement

class DrawerPlacementTest {
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
