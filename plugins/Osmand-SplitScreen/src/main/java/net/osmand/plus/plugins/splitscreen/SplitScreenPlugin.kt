package net.osmand.plus.plugins.splitscreen

import android.app.Activity
import android.content.Context
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.views.mapwidgets.MapWidgetInfo
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.MapWidget

class SplitScreenPlugin(app: OsmandApplication) : OsmandPlugin(app) {

    override fun getId(): String {
        return "splitscreen"
    }

    override fun getName(): String {
        return app.getString(R.string.app_name) + " Split Screen"
    }

    override fun getDescription(linksEnabled: Boolean): CharSequence {
        return "Split screen layout with map and widgets"
    }

    override fun getLogoResourceId(): Int {
        return R.drawable.ic_extension_dark
    }

    override fun createWidgets(
        mapActivity: MapActivity,
        widgetsInfos: MutableList<MapWidgetInfo?>,
        appMode: ApplicationMode
    ) {
        // Add split screen widgets here
        // For now, just add existing widgets as example

        // You can create custom widgets here
        // Example: Add a custom text widget

        // This is where you would add your split screen specific widgets
        // The actual split screen layout would need to be implemented
        // by modifying the MapActivity layout or using fragments
    }

    override fun createMapWidgetForParams(
        mapActivity: MapActivity,
        widgetType: WidgetType,
        customId: String?,
        widgetsPanel: WidgetsPanel?
    ): MapWidget? {
        // Handle custom widget types for split screen
        return when (widgetType) {
            // Add custom widget types here
            else -> null
        }
    }
}
