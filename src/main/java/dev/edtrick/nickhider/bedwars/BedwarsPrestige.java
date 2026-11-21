package dev.edtrick.nickhider.bedwars;

import java.util.*;

/**
 * Represents a Hypixel Bedwars prestige level tier, its color scheme, and star icon.
 */
public class BedwarsPrestige {

    public final int level;
    public final String name;
    public final String sample;
    public final String defaultIcon;
    public final String openBracket;
    public final String[] digitColors;
    public final String starColor;
    public final String closeBracket;

    public BedwarsPrestige(int level, String name, String sample, String defaultIcon,
                           String openBracket, String[] digitColors, String starColor, String closeBracket) {
        this.level = level;
        this.name = name;
        this.sample = sample;
        this.defaultIcon = defaultIcon;
        this.openBracket = openBracket;
        this.digitColors = digitColors;
        this.starColor = starColor;
        this.closeBracket = closeBracket;
    }

    /**
     * Formats an arbitrary star level (e.g. 1402) using this prestige's color scheme
     * and a chosen star icon (or this prestige's default icon if null/empty).
     */
    public String format(int stars, String customIcon) {
        String icon = (customIcon != null && !customIcon.isEmpty()) ? customIcon : defaultIcon;
        String digits = String.valueOf(stars);
        StringBuilder sb = new StringBuilder();
        sb.append(openBracket);

        if (digitColors == null || digitColors.length == 0) {
            sb.append(digits);
        } else if (digitColors.length == 1) {
            sb.append(digitColors[0]).append(digits);
        } else {
            for (int i = 0; i < digits.length(); i++) {
                int cIdx;
                if (digits.length() == digitColors.length) {
                    cIdx = i;
                } else if (digits.length() < digitColors.length) {
                    cIdx = (int) Math.round((double) i * (digitColors.length - 1) / Math.max(1, digits.length() - 1));
                } else {
                    cIdx = Math.min(i, digitColors.length - 1);
                }
                sb.append(digitColors[cIdx]).append(digits.charAt(i));
            }
        }

        sb.append(starColor).append(icon);
        sb.append(closeBracket);
        return sb.toString();
    }

    /**
     * Formats an arbitrary star level without outer brackets, preserving individual digit colors,
     * star color, and the star icon. Perfect for scoreboard display.
     */
    public String formatWithoutBrackets(int stars, String customIcon) {
        String icon = (customIcon != null && !customIcon.isEmpty()) ? customIcon : defaultIcon;
        String digits = String.valueOf(stars);
        StringBuilder sb = new StringBuilder();

        if (digitColors == null || digitColors.length == 0) {
            sb.append(digits);
        } else if (digitColors.length == 1) {
            sb.append(digitColors[0]).append(digits);
        } else {
            for (int i = 0; i < digits.length(); i++) {
                int cIdx;
                if (digits.length() == digitColors.length) {
                    cIdx = i;
                } else if (digits.length() < digitColors.length) {
                    cIdx = (int) Math.round((double) i * (digitColors.length - 1) / Math.max(1, digits.length() - 1));
                } else {
                    cIdx = Math.min(i, digitColors.length - 1);
                }
                sb.append(digitColors[cIdx]).append(digits.charAt(i));
            }
        }

        sb.append(starColor).append(icon);
        return sb.toString();
    }

    // ─── Registry of all 101 Hypixel Bedwars Prestiges ──────────────────────

    private static final List<BedwarsPrestige> PRESTIGES = new ArrayList<BedwarsPrestige>();
    private static final Map<Integer, BedwarsPrestige> BY_LEVEL = new HashMap<Integer, BedwarsPrestige>();
    private static final Map<String, BedwarsPrestige> BY_NAME = new HashMap<String, BedwarsPrestige>();

    private static void register(int level, String name, String sample) {
        // Parse sample string into brackets, colors and star
        int openIdx = sample.indexOf('[');
        String openBracket = (openIdx >= 0) ? sample.substring(0, openIdx + 1) : "§7[";

        int closeIdx = sample.lastIndexOf(']');
        int lastCodeBeforeClose = sample.lastIndexOf('§', closeIdx);
        String closeBracket = "]";
        int innerEnd = closeIdx;
        if (lastCodeBeforeClose > openIdx && lastCodeBeforeClose == closeIdx - 2) {
            closeBracket = sample.substring(lastCodeBeforeClose, closeIdx + 1);
            innerEnd = lastCodeBeforeClose;
        }

        String inner = (openIdx >= 0 && innerEnd > openIdx) ? sample.substring(openIdx + 1, innerEnd) : sample;

        // Find star icon: ✫, ✪, ⚝, ✥, ✭
        String defaultIcon = "✫";
        int starPos = -1;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '✫' || c == '✪' || c == '⚝' || c == '✥' || c == '✭' || c == '*') {
                defaultIcon = String.valueOf(c);
                starPos = i;
                break;
            }
        }

        String starColor = "";
        int digitsEnd = (starPos >= 0) ? starPos : inner.length();
        if (starPos >= 2 && inner.charAt(starPos - 2) == '§') {
            starColor = inner.substring(starPos - 2, starPos);
            digitsEnd = starPos - 2;
        }

        String digitsPart = (digitsEnd >= 0) ? inner.substring(0, digitsEnd) : "";

        // Determine initial active color
        String curColor = "§7";
        for (int i = 0; i < openBracket.length() - 1; i++) {
            if (openBracket.charAt(i) == '§') {
                curColor = openBracket.substring(i, i + 2);
            }
        }

        List<String> colorsList = new ArrayList<String>();
        for (int i = 0; i < digitsPart.length(); i++) {
            if (digitsPart.charAt(i) == '§' && i + 1 < digitsPart.length()) {
                curColor = digitsPart.substring(i, i + 2);
                i++;
            } else if (Character.isDigit(digitsPart.charAt(i))) {
                colorsList.add(curColor);
            }
        }

        if (starColor.isEmpty()) {
            starColor = curColor;
        }
        if (colorsList.isEmpty()) {
            colorsList.add(curColor);
        }

        String[] digitColors = colorsList.toArray(new String[0]);
        BedwarsPrestige prestige = new BedwarsPrestige(level, name, sample, defaultIcon,
                openBracket, digitColors, starColor, closeBracket);

        PRESTIGES.add(prestige);
        BY_LEVEL.put(level, prestige);
        BY_NAME.put(name.toLowerCase().replaceAll("[^a-z0-9]", ""), prestige);
    }

    static {
        register(0, "Stone", "§7[0✫]");
        register(100, "Iron", "§f[100✫]");
        register(200, "Gold", "§6[200✫]");
        register(300, "Diamond", "§b[300✫]");
        register(400, "Emerald", "§2[400✫]");
        register(500, "Sapphire", "§3[500✫]");
        register(600, "Ruby", "§4[600✫]");
        register(700, "Crystal", "§d[700✫]");
        register(800, "Opal", "§9[800✫]");
        register(900, "Amethyst", "§5[900✫]");
        register(1000, "Rainbow", "§c[§61§e0§a0§b0§d✪§5]");
        register(1100, "Iron Prime", "§7[§f1100§7✪]");
        register(1200, "Gold Prime", "§7[§e1200§6✪§7]");
        register(1300, "Diamond Prime", "§7[§b1300§3✪§7]");
        register(1400, "Emerald Prime", "§7[§a1400§2✪§7]");
        register(1500, "Sapphire Prime", "§7[§31500§9✪§7]");
        register(1600, "Ruby Prime", "§7[§c1600§4✪§7]");
        register(1700, "Crystal Prime", "§7[§d1700§5✪§7]");
        register(1800, "Opal Prime", "§7[§91800§1✪§7]");
        register(1900, "Amethyst Prime", "§7[§51900§8✪§7]");
        register(2000, "Mirror", "§8[§72§f00§70⚝§8]");
        register(2100, "Light", "§f[2§e10§60⚝]");
        register(2200, "Dawn", "§6[2§f20§b0§3⚝]");
        register(2300, "Dusk", "§5[2§d30§60§e⚝]");
        register(2400, "Air", "§b[2§f40§70⚝§8]");
        register(2500, "Wind", "§f[2§a50§20⚝]");
        register(2600, "Nebula", "§4[2§c60§d0⚝§5]");
        register(2700, "Thunder", "§e[2§f70§80⚝]");
        register(2800, "Earth", "§a[2§280§60⚝§e]");
        register(2900, "Water", "§b[2§390§90⚝§1]");
        register(3000, "Fire", "§e[3§600§c0✥§4]");
        register(3100, "Sunrise", "§9[3§310§60✥§e]");
        register(3200, "Eclipse", "§c[§43§720§40§c✥]");
        register(3300, "Gamma", "§9[33§d0§c0✥§4]");
        register(3400, "Majestic", "§2[§a3§d40§50✥§2]");
        register(3500, "Andesine", "§c[3§450§20§a✥]");
        register(3600, "Marine", "§a[36§b0§90✥§1]");
        register(3700, "Element", "§4[3§c70§b0§3✥]");
        register(3800, "Galaxy", "§1[3§98§500§d✥§1]");
        register(3900, "Atomic", "§c[3§a90§30§9✥]");
        register(4000, "Sunset", "§5[4§c00§60✭§e]");
        register(4100, "Time", "§e[4§61§c0§d0✭§5]");
        register(4200, "Winter", "§1[§94§32§b0§f0§7✭]");
        register(4300, "Obsidian", "§0[§54§830§50✭§0]");
        register(4400, "Spring", "§2[4§a4§e0§60§5✭§d]");
        register(4500, "Ice", "§f[4§b50§30✭]");
        register(4600, "Summer", "§3[§b4§e6§600§d✭§5]");
        register(4700, "Spinel", "§f[§44§c70§90§1✭§9]");
        register(4800, "Autumn", "§5[4§c8§600§b✭§3]");
        register(4900, "Mystic", "§2[§a4§f900§a✭§2]");
        register(5000, "Eternal", "§4[5§50§900§1✭§0]");
        register(5100, "Burnout", "§4[§c51§60§e0§f✭§4]");
        register(5200, "Cooldown", "§1[§95§32§b0§f0§e✭§1]");
        register(5300, "Obliteration", "§5[§d5§e3§f0§e0§d✭§5]");
        register(5400, "Ender", "§3[§a5§24§80§20§a✭§3]");
        register(5500, "Brust", "§2[§a5§e5§f0§b0§d✭§5]");
        register(5600, "Comical", "§4[§c5§e6§f0§e0§c✭§4]");
        register(5700, "Lusterlost", "§4[§65§27§30§90§5✭§8]");
        register(5800, "Maelstrom", "§5[§c5§68§f0§b0§3✭§9]");
        register(5900, "Time Undone", "§7[§05§89§70§f0✭§7]");
        register(6000, "Umbrella", "§c[§f6000§c✭§f]");
        register(6100, "Luminous", "§6[§e6§f100§b✭§3]");
        register(6200, "Tortilla", "§e[§f6§e2§600§f✭§e]");
        register(6300, "Corn", "§a[§e6300§a✭§2]");
        register(6400, "Bittersweet", "§b[6§c400§a✭]");
        register(6500, "Sweetsour", "§3[6§a50§f0§a✭§3]");
        register(6600, "Pop", "§9[§d6600§b✭§9]");
        register(6700, "Bubblegum", "§5[§d6700§f✭§5]");
        register(6800, "Contrast", "§0[§668§e00§f✭]");
        register(6900, "Blended", "§a[690§20✭§8]");
        register(7000, "Allay", "§3[§b7000§f✭§3]");
        register(7100, "Blaze", "§4[§c7§61§e0§c0§6✭§e]");
        register(7200, "Creeper", "§2[§a7§f2§20§a0§f✭§8]");
        register(7300, "Drowned", "§2[§373§b00§a✭§2]");
        register(7400, "Enderman", "§8[7400§d✭§8]");
        register(7500, "Frog", "§6[7§250§f0✭]");
        register(7600, "Ghast", "§f[76§700§c✭§8]");
        register(7700, "Hoglin", "§d[§c7700§6✭§d]");
        register(7800, "Iron Golem", "§8[§77§f800§e✭§8]");
        register(7900, "Jerry", "§6[§f7§29§60§20§f✭§6]");
        register(8000, "Kringle", "§2[§a800§c0§4✭§2]");
        register(8100, "Liquid", "§8[§78§f1§b0§30§9✭§1]");
        register(8200, "Mint", "§f[8200§a✭§f]");
        register(8300, "Neglected", "§8[8§430§c0✭§8]");
        register(8400, "Onion", "§f[§d840§a0✭§f]");
        register(8500, "Poser", "§3[§68500§e✭§3]");
        register(8600, "Quartz", "§d[§f8600§e✭§d]");
        register(8700, "Rich", "§8[§68700✭§8]");
        register(8800, "Sanguine", "§4[88§c00§f✭]");
        register(8900, "Titanic", "§9[§b890§30✭§9]");
        register(9000, "Unorthodox", "§d[9000§5✭§8]");
        register(9100, "Volcanic", "§0[§c9§610§c0✭§4]");
        register(9200, "Weeping Cherry", "§2[§d9200§a✭§2]");
        register(9300, "X-ray", "§f[§89300§f✭]");
        register(9400, "Yearn", "§e[§69§44§800✭]");
        register(9500, "Zebra", "§0[9§850§70✭§f]");
        register(9600, "Caution", "§e[96§000§e✭§0]");
        register(9700, "Indescribable", "§d[97§e00§b✭§e]");
        register(9800, "Forgotten", "§0[§89800✭§0]");
        register(9900, "Fuse", "§8[§79§f900§e✭§f]");
        register(10000, "Prestigious", "§9[§b1§f0000§c✭§4]");
    }

    public static List<BedwarsPrestige> getAllPrestiges() {
        return Collections.unmodifiableList(PRESTIGES);
    }

    public static BedwarsPrestige getByLevel(int level) {
        if (level < 0) level = 0;
        if (level > 10000) level = 10000;

        BedwarsPrestige exact = BY_LEVEL.get(level);
        if (exact != null) return exact;

        // Find highest prestige with prestige.level <= level
        BedwarsPrestige best = PRESTIGES.get(0);
        for (BedwarsPrestige p : PRESTIGES) {
            if (p.level <= level) {
                best = p;
            } else {
                break;
            }
        }
        return best;
    }

    public static BedwarsPrestige find(String query) {
        if (query == null) return null;
        query = query.trim();
        if (query.isEmpty() || query.equalsIgnoreCase("auto") || query.equalsIgnoreCase("reset")) {
            return null;
        }

        try {
            int lvl = Integer.parseInt(query);
            return getByLevel(lvl);
        } catch (NumberFormatException ignored) {}

        String clean = query.toLowerCase().replaceAll("[^a-z0-9]", "");
        BedwarsPrestige named = BY_NAME.get(clean);
        if (named != null) return named;

        for (BedwarsPrestige p : PRESTIGES) {
            String pClean = p.name.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (pClean.contains(clean) || clean.contains(pClean)) {
                return p;
            }
        }

        return null;
    }

    public static String resolveIcon(String query) {
        if (query == null) return null;
        query = query.trim();
        if (query.isEmpty() || query.equalsIgnoreCase("auto") || query.equalsIgnoreCase("reset")) {
            return null;
        }

        if (query.equals("✫") || query.equals("✪") || query.equals("⚝") || query.equals("✥") || query.equals("✭") || query.equals("*")) {
            return query;
        }

        BedwarsPrestige p = find(query);
        if (p != null) {
            return p.defaultIcon;
        }

        return null;
    }
}
