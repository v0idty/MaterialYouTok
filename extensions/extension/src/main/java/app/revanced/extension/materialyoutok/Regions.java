package app.revanced.extension.materialyoutok;

final class Regions {
    static final class Region {
        final String iso, name, operator, mccmnc;
        Region(String iso, String name, String operator, String mccmnc) {
            this.iso = iso; this.name = name; this.operator = operator; this.mccmnc = mccmnc;
        }
        String flag() {
            int a = 0x1F1E6 + (Character.toUpperCase(iso.charAt(0)) - 'A');
            int b = 0x1F1E6 + (Character.toUpperCase(iso.charAt(1)) - 'A');
            return new String(Character.toChars(a)) + new String(Character.toChars(b));
        }
    }

    static final Region[] ALL = {
        new Region("by", "Belarus", "A1", "25701"),
        new Region("kz", "Kazakhstan", "Beeline", "40101"),
        new Region("ua", "Ukraine", "Kyivstar", "25503"),
        new Region("us", "United States", "AT&T", "310410"),
        new Region("gb", "United Kingdom", "EE", "23430"),
        new Region("de", "Germany", "Telekom", "26201"),
        new Region("fr", "France", "Orange", "20801"),
        new Region("es", "Spain", "Movistar", "21401"),
        new Region("it", "Italy", "TIM", "22201"),
        new Region("nl", "Netherlands", "KPN", "20408"),
        new Region("se", "Sweden", "Telia", "24001"),
        new Region("pl", "Poland", "Plus", "26001"),
        new Region("tr", "Turkey", "Turkcell", "28601"),
    };

    static Region byIso(String iso) {
        for (Region r : ALL) if (r.iso.equalsIgnoreCase(iso)) return r;
        return ALL[0];
    }
}
