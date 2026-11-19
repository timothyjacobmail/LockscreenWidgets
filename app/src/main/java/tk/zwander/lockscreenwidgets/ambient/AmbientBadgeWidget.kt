package tk.zwander.lockscreenwidgets.ambient

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import tk.zwander.common.util.appWidgetManager
import tk.zwander.common.util.idManager

internal data class AmbientBadgeWidget(val id: Int, val info: AppWidgetProviderInfo)

internal fun Context.findAmbientBadgeWidget(): AmbientBadgeWidget? {
    return idManager.collectAllIds().firstNotNullOfOrNull { id ->
        val info = appWidgetManager.getAppWidgetInfo(id) ?: return@firstNotNullOfOrNull null
        val provider = info.provider
        if ((provider.packageName == "co.adityarajput.notifilter" ||
                    provider.packageName == "co.adityarajput.notifilter.debug") &&
            provider.className == "co.adityarajput.notifilter.AppBadgeWidget"
        ) AmbientBadgeWidget(id, info) else null
    }
}
