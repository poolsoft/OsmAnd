package net.osmand.plus.plugins.splitscreen

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget

class SplitScreenPlugin(app: net.osmand.plus.OsmandApplication) : OsmandPlugin(app) {
    companion object {
        const val PLUGIN_ID = "splitscreen"
        const val PLUGIN_NAME = "Split Screen"
        const val PLUGIN_DESCRIPTION = "Split screen layout with map and widgets"
    }

    override fun getId(): String = PLUGIN_ID
    override fun getName(): String = PLUGIN_NAME
    override fun getDescription(linksEnabled: Boolean): CharSequence = PLUGIN_DESCRIPTION

    override fun registerLayers(context: android.content.Context, mapActivity: MapActivity?) {
        super.registerLayers(context, mapActivity)
        if (mapActivity != null) {
            val rootView = mapActivity.findViewById<FrameLayout>(android.R.id.content)
            val inflater = LayoutInflater.from(mapActivity)
            val splitView = inflater.inflate(android.R.layout.simple_list_item_1, rootView, false)
            splitView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (rootView.height * 0.3).toInt()
            )
            // Burada kendi widget'ını veya özel view'unu ekleyebilirsin
            rootView.addView(splitView)
        }
    }
}
