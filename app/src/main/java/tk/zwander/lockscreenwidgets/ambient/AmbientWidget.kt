package tk.zwander.lockscreenwidgets.ambient

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import tk.zwander.common.util.appWidgetManager
import tk.zwander.common.util.idManager
import tk.zwander.common.util.prefManager

internal data class AmbientWidget(val id: Int, val info: AppWidgetProviderInfo, val isSelected: Boolean) {
    val isBadge: Boolean
        get() = info.provider.className == "co.adityarajput.notifilter.AppBadgeWidget" &&
                info.provider.packageName in setOf("co.adityarajput.notifilter", "co.adityarajput.notifilter.debug")
}

internal fun Context.findAmbientWidgets(): List<AmbientWidget> {
    val selected = prefManager.ambientWidgets.mapNotNull { item ->
        appWidgetManager.getAppWidgetInfo(item.id)?.let { AmbientWidget(item.id, it, isSelected = true) }
    }
    if (selected.isNotEmpty()) return selected
    return listOfNotNull(idManager.collectAllIds().firstNotNullOfOrNull { id ->
        val info = appWidgetManager.getAppWidgetInfo(id) ?: return@firstNotNullOfOrNull null
        AmbientWidget(id, info, isSelected = false).takeIf { it.isBadge }
    })
}
