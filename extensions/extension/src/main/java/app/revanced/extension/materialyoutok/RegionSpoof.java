package app.revanced.extension.materialyoutok;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

public final class RegionSpoof {
    static final String PREFS = "materialyoutok";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_ISO = "iso";

    private static Regions.Region current() {
        try {
            Application app = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            if (app != null) {
                SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                if (!p.getBoolean(KEY_ENABLED, true)) return null;
                return Regions.byIso(p.getString(KEY_ISO, "by"));
            }
        } catch (Throwable ignored) { }
        return Regions.ALL[0];
    }

    public static String simCountryIso(TelephonyManager tm) {
        Regions.Region r = current();
        return r == null ? tm.getSimCountryIso() : r.iso;
    }
    public static String networkCountryIso(TelephonyManager tm) {
        Regions.Region r = current();
        return r == null ? tm.getNetworkCountryIso() : r.iso;
    }
    public static String simOperator(TelephonyManager tm) {
        Regions.Region r = current();
        return r == null ? tm.getSimOperator() : r.mccmnc;
    }
    public static String networkOperator(TelephonyManager tm) {
        Regions.Region r = current();
        return r == null ? tm.getNetworkOperator() : r.mccmnc;
    }
}
