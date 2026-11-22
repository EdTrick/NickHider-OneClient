package dev.edtrick.nickhider.bedwars;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Hypixel Bedwars level and star spoofing.
 */
public class BedwarsLevelManager {

    private static boolean enabled = false;
    private static int fakeStars = 1402;
    private static int colorPresetLevel = -1; // -1 = auto
    private static String customIcon = null;   // null = auto

    private static int detectedRealStars = -1;
    private static String detectedRealBwTag = null;
    private static int realNonMatchXpLevel = -1;

    // Pattern matching any Hypixel Bedwars star tag e.g. "[142✫]", "[16☆]", "[40☆]", "§7[§f142§7✫]", "§7[§f1§f4§f2§7✫§7]", "§c[§61§e0§a0§b0§d✪§5]"
    // Strictly requires digits to prevent mistakenly matching guild tags with stars like "[★]" or "[✪]"
    public static final Pattern BW_TAG_REGEX = Pattern.compile(
            "(?i)((?:§[0-9a-fk-orA-FK-OR])*)\\[(?=[^]]*[0-9])([^]]*?[\\u2605\\u2606\\u269D\\u2725-\\u272F\\*][^]]*?)\\]"
    );

    // Pattern matching scoreboard, nametag, and actionbar "Stars: 142", "BedWars: 142" or "Level: 142✫" (requires star icon for 'level')
    public static final Pattern SCOREBOARD_STARS_REGEX = Pattern.compile(
            "(?i)((?:§[0-9a-fk-orA-FK-OR]|\\s)*(?:stars|bedwars|(?:level(?=.*[\\u2605\\u2606\\u269D\\u2725-\\u272F\\*])))(?:§[0-9a-fk-orA-FK-OR]|\\s)*:(?:§[0-9a-fk-orA-FK-OR]|\\s)*)(?:\\[?(?:(?:§[0-9a-fk-orA-FK-OR])*[0-9])+(?:§[0-9a-fk-orA-FK-OR]|\\s)*[\\u2605\\u2606\\u269D\\u2725-\\u272F\\*]\\]?|[0-9]{1,5}(?:\\s*(?:§[0-9a-fk-orA-FK-OR])*[\\u2605\\u2606\\u269D\\u2725-\\u272F\\*])?)"
    );

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public static void loadValues(boolean newEnabled, int newStars, int newColorLevel, String newIcon) {
        enabled = newEnabled;
        fakeStars = (newStars < 0) ? 0 : Math.min(newStars, 10000);
        colorPresetLevel = newColorLevel;
        customIcon = newIcon;
    }

    public static void setAll(boolean newEnabled, int newStars, int newColorLevel, String newIcon) {
        enabled = newEnabled;
        fakeStars = (newStars < 0) ? 0 : Math.min(newStars, 10000);
        colorPresetLevel = newColorLevel;
        customIcon = newIcon;
        NickHiderMod.saveConfig();
        dev.edtrick.nickhider.config.OneConfigCompat.sync();
    }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) {
        enabled = value;
        NickHiderMod.saveConfig();
        dev.edtrick.nickhider.config.OneConfigCompat.sync();
    }

    public static int getFakeStars() { return fakeStars; }
    public static void setFakeStars(int stars) {
        if (stars < 0) stars = 0;
        if (stars > 10000) stars = 10000;
        fakeStars = stars;
        NickHiderMod.saveConfig();
        dev.edtrick.nickhider.config.OneConfigCompat.sync();
    }

    public static int getColorPresetLevel() { return colorPresetLevel; }
    public static void setColorPresetLevel(int level) {
        colorPresetLevel = level;
        NickHiderMod.saveConfig();
        dev.edtrick.nickhider.config.OneConfigCompat.sync();
    }

    public static String getCustomIcon() { return customIcon; }
    public static void setCustomIcon(String icon) {
        customIcon = icon;
        NickHiderMod.saveConfig();
        dev.edtrick.nickhider.config.OneConfigCompat.sync();
    }

    public static int getDetectedRealStars() { return detectedRealStars; }
    public static void setDetectedRealStars(int stars) { detectedRealStars = stars; }

    public static int getRealNonMatchXpLevel() { return realNonMatchXpLevel; }
    public static void setRealNonMatchXpLevel(int level) { realNonMatchXpLevel = level; }

    public static String getDetectedRealBwTag() { return detectedRealBwTag; }
    public static void setDetectedRealBwTag(String tag) { detectedRealBwTag = tag; }

    // ─── Tag Generation ──────────────────────────────────────────────────────

    public static BedwarsPrestige getEffectiveColorPrestige() {
        if (colorPresetLevel >= 0) {
            return BedwarsPrestige.getByLevel(colorPresetLevel);
        }
        return BedwarsPrestige.getByLevel(fakeStars);
    }

    public static String getEffectiveIcon() {
        if (customIcon != null && !customIcon.isEmpty()) {
            return customIcon;
        }
        return getEffectiveColorPrestige().defaultIcon;
    }

    public static String getFormattedTag() {
        return getEffectiveColorPrestige().format(fakeStars, getEffectiveIcon());
    }

    public static String getFormattedWithoutBrackets() {
        return getEffectiveColorPrestige().formatWithoutBrackets(fakeStars, getEffectiveIcon());
    }

    public static String getUnformattedTag() {
        return "[" + fakeStars + getEffectiveIcon() + "]";
    }

    private static final ThreadLocal<Boolean> checkingActive = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private static boolean cachedBedwarsActive = false;
    private static long lastActiveCheckTime = 0;

    private static boolean cachedMatchActive = false;
    private static long lastMatchCheckTime = 0;

    public static boolean isBedwarsActive() {
        if (!enabled) return false;
        long now = System.currentTimeMillis();
        if (now - lastActiveCheckTime < 250) {
            return cachedBedwarsActive;
        }
        lastActiveCheckTime = now;
        cachedBedwarsActive = computeIsBedwarsActive();
        return cachedBedwarsActive;
    }

    private static boolean computeIsBedwarsActive() {
        if (checkingActive.get()) {
            return false;
        }
        checkingActive.set(true);
        try {
            // 1. Check HyBedWars if present
            try {
                Class<?> hpClass = Class.forName("tomeko.hybedwars.location.HypixelPackets");
                Object instance = hpClass.getField("INSTANCE").get(null);
                java.lang.reflect.Method m = hpClass.getMethod("getInBedwars");
                Object res = m.invoke(instance);
                if (res instanceof Boolean && ((Boolean) res)) {
                    return true;
                }
            } catch (Throwable ignored) {}

            // 2. Check HyInfo if present
            try {
                Class<?> hpClass = Class.forName("tomeko.hyinfo.location.HypixelPackets");
                Object instance = hpClass.getField("INSTANCE").get(null);
                java.lang.reflect.Method m = hpClass.getMethod("getInBedwars");
                Object res = m.invoke(instance);
                if (res instanceof Boolean && ((Boolean) res)) {
                    return true;
                }
            } catch (Throwable ignored) {}

            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) return false;
            net.minecraft.scoreboard.Scoreboard sb = mc.theWorld.getScoreboard();
            if (sb != null) {
                net.minecraft.scoreboard.ScoreObjective sidebar = sb.getObjectiveInDisplaySlot(1);
                if (sidebar != null && sidebar.getDisplayName() != null) {
                    String clean = NickHiderMod.stripColorCodes(sidebar.getDisplayName()).replaceAll("\\s+", "").toUpperCase();
                    if (clean.contains("BEDWARS")) {
                        return true;
                    }
                }
                if (sidebar != null) {
                    java.util.Collection<net.minecraft.scoreboard.Score> scores = sb.getSortedScores(sidebar);
                    for (net.minecraft.scoreboard.Score score : scores) {
                        String pName = score.getPlayerName();
                        net.minecraft.scoreboard.ScorePlayerTeam team = sb.getPlayersTeam(pName);
                        String line = (team != null) ? (team.getColorPrefix() + pName + team.getColorSuffix()) : pName;
                        String cleanLine = NickHiderMod.stripColorCodes(line).replaceAll("\\s+", "").toUpperCase();
                        if (cleanLine.contains("BEDWARS") || cleanLine.contains("BEDSDESTROYED") || cleanLine.contains("BEDSBROKEN")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        } finally {
            checkingActive.set(false);
        }
    }

    public static boolean isBedwarsMatchActive() {
        if (!enabled) return false;
        long now = System.currentTimeMillis();
        if (now - lastMatchCheckTime < 250) {
            return cachedMatchActive;
        }
        lastMatchCheckTime = now;
        cachedMatchActive = computeIsBedwarsMatchActive();
        return cachedMatchActive;
    }

    private static boolean computeIsBedwarsMatchActive() {
        if (checkingActive.get()) {
            return false;
        }
        checkingActive.set(true);
        try {
            // 1. Check HyBedWars mod if present (uses official HypixelModAPI ClientboundLocationPacket)
            try {
                Class<?> hpClass = Class.forName("tomeko.hybedwars.location.HypixelPackets");
                Object instance = hpClass.getField("INSTANCE").get(null);
                java.lang.reflect.Method inBwMethod = hpClass.getMethod("getInBedwars");
                java.lang.reflect.Method inGameMethod = hpClass.getMethod("getInGame");
                java.lang.reflect.Method inLobbyMethod = hpClass.getMethod("getInLobby");

                boolean inBw = (Boolean) inBwMethod.invoke(instance);
                boolean inGame = (Boolean) inGameMethod.invoke(instance);
                boolean inLobby = (Boolean) inLobbyMethod.invoke(instance);

                if (inBw && inGame && !inLobby) {
                    return true;
                }
                if (inLobby || !inBw) {
                    return false;
                }
            } catch (Throwable ignored) {}

            // 2. Fallback to Scoreboard inspection
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) return false;
            net.minecraft.scoreboard.Scoreboard sb = mc.theWorld.getScoreboard();
            if (sb != null) {
                net.minecraft.scoreboard.ScoreObjective sidebar = sb.getObjectiveInDisplaySlot(1);
                if (sidebar == null || sidebar.getDisplayName() == null) {
                    return false;
                }

                String cleanTitle = NickHiderMod.stripColorCodes(sidebar.getDisplayName()).replaceAll("\\s+", "").toUpperCase();
                if (!cleanTitle.contains("BEDWARS")) {
                    return false;
                }

                java.util.Collection<net.minecraft.scoreboard.Score> scores = sb.getSortedScores(sidebar);
                boolean hasLobbyIndicator = false;
                boolean hasMatchIndicator = false;

                for (net.minecraft.scoreboard.Score score : scores) {
                    String pName = score.getPlayerName();
                    net.minecraft.scoreboard.ScorePlayerTeam team = sb.getPlayersTeam(pName);
                    String line = (team != null) ? (team.getColorPrefix() + pName + team.getColorSuffix()) : pName;
                    String cleanLine = NickHiderMod.stripColorCodes(line).replaceAll("\\s+", "").toUpperCase();

                    if (cleanLine.contains("LOBBY:") || cleanLine.contains("LOBBY#")
                            || cleanLine.contains("MYSTERYVAULT") || cleanLine.contains("LOOTCHESTS:")
                            || cleanLine.contains("COINS:") || cleanLine.contains("TOKENS:")
                            || cleanLine.contains("REWARDS:")) {
                        hasLobbyIndicator = true;
                    }

                    if (cleanLine.contains("BEDSBROKEN") || cleanLine.contains("BEDSDESTROYED")
                            || cleanLine.contains("FINALKILLS") || cleanLine.contains("DIAMOND")
                            || cleanLine.contains("EMERALD") || cleanLine.contains("WAITING")
                            || cleanLine.contains("STARTING") || cleanLine.contains("PLAYERS:")
                            || cleanLine.contains("RED:") || cleanLine.contains("BLUE:")
                            || cleanLine.contains("GREEN:") || cleanLine.contains("YELLOW:")
                            || cleanLine.contains("AQUA:") || cleanLine.contains("WHITE:")
                            || cleanLine.contains("PINK:") || cleanLine.contains("GRAY:") || cleanLine.contains("GREY:")) {
                        hasMatchIndicator = true;
                    }
                }

                if (hasLobbyIndicator) {
                    return false;
                }
                return hasMatchIndicator;
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        } finally {
            checkingActive.set(false);
        }
    }

    private static void recordRealStars(String rawTag) {
        if (rawTag == null) return;
        String digitsOnly = NickHiderMod.stripColorCodes(rawTag).replaceAll("[^0-9]", "");
        if (!digitsOnly.isEmpty()) {
            try {
                int parsed = Integer.parseInt(digitsOnly);
                if (parsed != fakeStars && parsed > 0 && parsed <= 10000) {
                    detectedRealStars = parsed;
                    detectedRealBwTag = rawTag;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    public static String replaceTagInString(String text) {
        if (text == null) return null;
        Matcher m = BW_TAG_REGEX.matcher(text);
        if (m.find()) {
            recordRealStars(m.group());
            return m.replaceAll(Matcher.quoteReplacement(getFormattedTag()));
        }
        return text;
    }

    // ─── Filtering Logic ─────────────────────────────────────────────────────

    private static java.lang.reflect.Field textField = null;
    static {
        try {
            textField = ChatComponentText.class.getDeclaredField("text");
            textField.setAccessible(true);
        } catch (Throwable t) {
            try {
                for (java.lang.reflect.Field f : ChatComponentText.class.getDeclaredFields()) {
                    if (f.getType() == String.class) {
                        textField = f;
                        textField.setAccessible(true);
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private static void setLeafText(ChatComponentText component, String newText) {
        if (component == null) return;
        try {
            if (textField != null) {
                textField.set(component, newText);
            }
        } catch (Throwable ignored) {}
    }

    private static void collectLeaves(IChatComponent component, List<ChatComponentText> leaves) {
        if (component == null) return;
        if (component instanceof ChatComponentText) {
            leaves.add((ChatComponentText) component);
        }
        for (Object sibling : component.getSiblings()) {
            if (sibling instanceof IChatComponent) {
                collectLeaves((IChatComponent) sibling, leaves);
            }
        }
    }

    /**
     * Filters chat messages: if the message was sent by the local player (or is a pre-game join/quit
     * by the local player), replace their Bedwars star tag with the fake Bedwars tag.
     * Strictly verifies that the local player is the speaker before the colon, ensuring that other players
     * mentioning the local player's name NEVER have their star tags altered.
     */
    public static IChatComponent filterChat(IChatComponent component) {
        if (component == null || !enabled || !isBedwarsActive()) return component;
        try {
            String fullUnformatted = NickHiderMod.stripColorCodes(component.getUnformattedText());

            // 1. Quick check: does the message contain any Bedwars star tag at all?
            if (!BW_TAG_REGEX.matcher(component.getFormattedText()).find()
                    && !BW_TAG_REGEX.matcher(fullUnformatted).find()) {
                return component;
            }

            // Strip any leading timestamp e.g. "[23:23] ", "[23:23:01] ", "[11:23 PM] "
            String clean = NickHiderMod.stripColorCodes(fullUnformatted);
            String noTimestamp = clean.replaceFirst("(?i)^(?:\\[[0-9]{1,2}:[0-9]{1,2}(?::[0-9]{1,2})?(?:\\s*[AP]M)?\\]|[0-9]{1,2}:[0-9]{1,2}(?::[0-9]{1,2})?)\\s*", "");

            // 2. Identify if the local player is the owner/speaker of this message:
            int colonIdx = noTimestamp.indexOf(": ");
            if (colonIdx == -1) {
                colonIdx = noTimestamp.indexOf(':');
            }
            boolean isLocalMessage = false;

            if (colonIdx != -1) {
                // In chat messages, the speaker prefix is everything before the speaker colon
                String speakerPrefix = noTimestamp.substring(0, colonIdx);
                isLocalMessage = NickHiderMod.containsLocalPlayerName(speakerPrefix);
                if (!isLocalMessage && NickHiderMod.getFakeNick() != null && !NickHiderMod.getFakeNick().isEmpty()) {
                    isLocalMessage = speakerPrefix.toLowerCase().contains(NickHiderMod.getFakeNick().toLowerCase());
                }
            } else {
                // If there is no colon, check for pre-game lobby join/quit messages
                String lower = noTimestamp.toLowerCase();
                int actionIdx = lower.indexOf("has joined");
                if (actionIdx == -1) actionIdx = lower.indexOf("has quit");
                if (actionIdx != -1) {
                    String playerPart = noTimestamp.substring(0, actionIdx);
                    isLocalMessage = NickHiderMod.containsLocalPlayerName(playerPart);
                    if (!isLocalMessage && NickHiderMod.getFakeNick() != null && !NickHiderMod.getFakeNick().isEmpty()) {
                        isLocalMessage = playerPart.toLowerCase().contains(NickHiderMod.getFakeNick().toLowerCase());
                    }
                }
            }

            // If the message is NOT from/about the local player, leave star tags completely untouched!
            if (!isLocalMessage) {
                return component;
            }

            // 3. The message is confirmed from the local player: replace their star tag
            List<ChatComponentText> leaves = new ArrayList<>();
            collectLeaves(component, leaves);
            if (leaves.isEmpty()) return component;

            int speakerLeafIdx = -1;
            List<String> names = NickHiderMod.getNamesToReplace();
            for (int i = 0; i < leaves.size(); i++) {
                String leafText = NickHiderMod.stripColorCodes(leaves.get(i).getChatComponentText_TextValue()).toLowerCase();
                for (String name : names) {
                    if (name != null && !name.isEmpty() && leafText.contains(name.toLowerCase())) {
                        speakerLeafIdx = i;
                        break;
                    }
                }
                if (speakerLeafIdx != -1) break;
            }
            if (speakerLeafIdx == -1) speakerLeafIdx = leaves.size() - 1;

            boolean replaced = false;
            for (int i = speakerLeafIdx; i >= 0; i--) {
                ChatComponentText leaf = leaves.get(i);
                String val = leaf.getChatComponentText_TextValue();
                Matcher m = BW_TAG_REGEX.matcher(val);
                if (m.find()) {
                    recordRealStars(m.group());
                    String rep = m.replaceAll(Matcher.quoteReplacement(getFormattedTag()));
                    setLeafText(leaf, rep);
                    replaced = true;
                    break; // Only the speaker's star tag in the header
                }
            }

            if (!replaced) {
                for (ChatComponentText leaf : leaves) {
                    String val = leaf.getChatComponentText_TextValue();
                    Matcher m = BW_TAG_REGEX.matcher(val);
                    if (m.find()) {
                        recordRealStars(m.group());
                        String rep = m.replaceAll(Matcher.quoteReplacement(getFormattedTag()));
                        setLeafText(leaf, rep);
                        break;
                    }
                }
            }

            return component;
        } catch (Exception e) {
            return component;
        }
    }

    public static String filterChatString(String text) {
        if (text == null || !enabled || !isBedwarsActive()) return text;

        String clean = NickHiderMod.stripColorCodes(text);
        String noTimestamp = clean.replaceFirst("(?i)^(?:\\[[0-9]{1,2}:[0-9]{1,2}(?::[0-9]{1,2})?(?:\\s*[AP]M)?\\]|[0-9]{1,2}:[0-9]{1,2}(?::[0-9]{1,2})?)\\s*", "");
        int colon = noTimestamp.indexOf(": ");
        if (colon == -1) colon = noTimestamp.indexOf(':');

        String speaker = (colon != -1) ? noTimestamp.substring(0, colon) : noTimestamp;
        List<String> names = NickHiderMod.getNamesToReplace();
        boolean hasLocalPlayer = false;
        for (String name : names) {
            if (name != null && !name.isEmpty() && speaker.toLowerCase().contains(name.toLowerCase())) {
                hasLocalPlayer = true;
                break;
            }
        }
        if (!hasLocalPlayer && NickHiderMod.getFakeNick() != null && !NickHiderMod.getFakeNick().isEmpty()) {
            if (NickHiderMod.stripColorCodes(speaker).toLowerCase().contains(NickHiderMod.getFakeNick().toLowerCase())) {
                hasLocalPlayer = true;
            }
        }

        if (!hasLocalPlayer) return text;

        Matcher matcher = BW_TAG_REGEX.matcher(text);
        if (matcher.find()) {
            recordRealStars(matcher.group());
            text = matcher.replaceFirst(Matcher.quoteReplacement(getFormattedTag()));
        }

        return text;
    }

    /**
     * Filters player name strings from ScorePlayerTeam.formatPlayerName.
     * When isLocalPlayer is true, replaces the player's Bedwars tag with the fake tag.
     * Also replaces sidebar scoreboard lines matching "Stars: \d+" or "Level: \d+✫".
     */
    public static String filterTeamPlayerName(String text, boolean isLocalPlayer) {
        if (text == null || !enabled || !isBedwarsActive()) return text;
        if (text.contains(getFormattedWithoutBrackets()) || text.contains(getFormattedTag())) return text;

        // 1. If it's the local player (tablist, nametag, scoreboard player line)
        if (isLocalPlayer) {
            Matcher m = BW_TAG_REGEX.matcher(text);
            if (m.find()) {
                recordRealStars(m.group());
                text = m.replaceAll(Matcher.quoteReplacement(getFormattedTag()));
            }
            return text;
        }

        // 2. Scoreboard lines like "Stars: 142" or "Level: 142✫" with prestige colors & icon
        Matcher sbMatcher = SCOREBOARD_STARS_REGEX.matcher(text);
        if (sbMatcher.find()) {
            text = sbMatcher.replaceFirst(Matcher.quoteReplacement(sbMatcher.group(1) + getFormattedWithoutBrackets()));
            return text;
        }

        return text;
    }

    /**
     * Filters actionbar ("record playing") and title/subtitle text.
     */
    public static String filterActionBarOrTitle(String text) {
        if (text == null || !enabled || !isBedwarsActive()) return text;

        Matcher tagMatcher = BW_TAG_REGEX.matcher(text);
        if (tagMatcher.find()) {
            text = tagMatcher.replaceAll(Matcher.quoteReplacement(getFormattedTag()));
        }

        Matcher sbMatcher = SCOREBOARD_STARS_REGEX.matcher(text);
        if (sbMatcher.find()) {
            text = sbMatcher.replaceFirst(Matcher.quoteReplacement(sbMatcher.group(1) + getFormattedWithoutBrackets()));
        }

        return text;
    }

    /**
     * General string replacement (e.g. for holograms or tooltips with player name and stars).
     */
    public static String filterGeneralString(String text) {
        if (text == null || !enabled || !isBedwarsActive()) return text;
        if (text.contains(getFormattedWithoutBrackets()) || text.contains(getFormattedTag())) return text;

        // If the string contains a colon (like a chat message "Speaker: Message"),
        // the local player MUST be in the speaker portion (before the colon).
        String clean = NickHiderMod.stripColorCodes(text);
        String noTimestamp = clean.replaceFirst("(?i)^(?:\\[[0-9]{1,2}:[0-9]{1,2}(?::[0-9]{1,2})?(?:\\s*[AP]M)?\\]|[0-9]{1,2}:[0-9]{1,2}(?::[0-9]{1,2})?)\\s*", "");
        int colon = noTimestamp.indexOf(": ");
        if (colon == -1) colon = noTimestamp.indexOf(':');
        String targetPrefix = (colon != -1) ? noTimestamp.substring(0, colon) : noTimestamp;

        List<String> names = NickHiderMod.getNamesToReplace();
        boolean hasLocalPlayer = false;
        for (String name : names) {
            if (name != null && !name.isEmpty() && targetPrefix.toLowerCase().contains(name.toLowerCase())) {
                hasLocalPlayer = true;
                break;
            }
        }
        if (!hasLocalPlayer && NickHiderMod.getFakeNick() != null && !NickHiderMod.getFakeNick().isEmpty()) {
            if (NickHiderMod.stripColorCodes(targetPrefix).toLowerCase().contains(NickHiderMod.getFakeNick().toLowerCase())) {
                hasLocalPlayer = true;
            }
        }

        if (hasLocalPlayer) {
            Matcher m = BW_TAG_REGEX.matcher(text);
            if (m.find()) {
                recordRealStars(m.group());
                text = m.replaceAll(Matcher.quoteReplacement(getFormattedTag()));
            }
        }

        return text;
    }
}
