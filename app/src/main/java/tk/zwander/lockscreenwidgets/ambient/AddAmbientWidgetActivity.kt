package tk.zwander.lockscreenwidgets.ambient

import tk.zwander.common.activities.add.AddWidgetActivity
import tk.zwander.common.activities.add.IAmbientConfigureActivity
import tk.zwander.common.data.WidgetData
import tk.zwander.common.util.idManager
import tk.zwander.common.util.prefManager

internal class AddAmbientWidgetActivity : AddWidgetActivity(), IAmbientConfigureActivity {
    override val showShortcuts = false
    override val colCount = 4
    override val rowCount = 8
    override val width: Float
        get() = resources.configuration.screenWidthDp.toFloat()
    override val height: Float
        get() = resources.configuration.screenHeightDp.toFloat()

    override var currentWidgets: Set<WidgetData>
        get() = setOfNotNull(prefManager.ambientWidget)
        set(value) {
            val previous = prefManager.ambientWidget?.id
            val selected = value.lastOrNull()
            prefManager.ambientWidget = selected
            if (previous != null && previous != selected?.id) idManager.removeId(previous)
        }
}
