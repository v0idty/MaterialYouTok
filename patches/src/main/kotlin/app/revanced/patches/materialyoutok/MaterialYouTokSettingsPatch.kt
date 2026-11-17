package app.revanced.patches.materialyoutok

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element

@Suppress("unused")
val materialYouTokSettingsPatch = resourcePatch(
    name = "MaterialYouTok settings",
    description = "Adds the MaterialYouTok region screen with its own launcher icon.",
) {
    compatibleWith("com.zhiliaoapp.musically")
    dependsOn(materialYouTokRegionPatch)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", "app.revanced.extension.materialyoutok.RegionSettingsActivity")
            activity.setAttribute("android:exported", "true")
            activity.setAttribute("android:label", "MaterialYouTok")
            activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault.NoActionBar")

            val filter = document.createElement("intent-filter")
            val action = document.createElement("action")
            action.setAttribute("android:name", "android.intent.action.MAIN")
            val category = document.createElement("category")
            category.setAttribute("android:name", "android.intent.category.LAUNCHER")
            filter.appendChild(action)
            filter.appendChild(category)
            activity.appendChild(filter)

            application.appendChild(activity)
        }
    }
}
