package app.revanced.extension.materialyoutok;

import android.telephony.TelephonyManager;

public final class RegionSpoof {
    private static final String COUNTRY = "by";
    private static final String OPERATOR = "25701";

    public static String simCountryIso(TelephonyManager tm) { return COUNTRY; }
    public static String networkCountryIso(TelephonyManager tm) { return COUNTRY; }
    public static String simOperator(TelephonyManager tm) { return OPERATOR; }
    public static String networkOperator(TelephonyManager tm) { return OPERATOR; }
}
