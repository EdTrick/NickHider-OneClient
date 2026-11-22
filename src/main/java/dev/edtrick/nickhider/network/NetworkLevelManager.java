package dev.edtrick.nickhider.network;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Hypixel Network Level spoofing across nametags, profile menus,
 * scoreboards, lobby experience bar, and chat.
 */
public class NetworkLevelManager {

    private static boolean enabled = false;
    private static int fakeLevel = 250;
    private static int detectedRealLevel = -1;

    // Nametag line above player e.g. "§9Level§f: §e95", "[Level: 95]", "Level: 95"
    public static final Pattern NAMETAG_LEVEL_REGEX = Pattern.compile(
            "(?i)((?:§[0-9a-fk-orA-FK-OR]|\\s|\\[)*(?:hypixel|network)?(?:§[0-9a-fk-orA-FK-OR]|\\s)*level(?:§[0-9a-fk-orA-FK-OR]|\\s)*:(?:§[0-9a-fk-orA-FK-OR]|\\s)*)[0-9]{1,5}"
    );

    // Scoreboard line e.g. "Hypixel Level: 95", "§fHypixel §fLevel: §b96"
    public static final Pattern SCOREBOARD_LEVEL_REGEX = Pattern.compile(
            "(?i)((?:§[0-9a-fk-orA-FK-OR]|\\s)*(?:hypixel|network)(?:§[0-9a-fk-orA-FK-OR]|\\s)+level(?:§[0-9a-fk-orA-FK-OR]|\\s)*:(?:§[0-9a-fk-orA-FK-OR]|\\s)*)[0-9]{1,5}"
    );

    // General string / GUI pattern 1: Explicit "Hypixel Level" or "Network Level" with optional colon
    // e.g. "§bHypixel Level §a96 §8|||||||||||||||||||||||| 0%" (Profile Leveling menu)
    public static final Pattern GENERAL_HYPIXEL_LEVEL_REGEX = Pattern.compile(
            "(?i)((?:§[0-9a-fk-orA-FK-OR]|\\s)*(?:hypixel|network)(?:§[0-9a-fk-orA-FK-OR]|\\s)+level(?:§[0-9a-fk-orA-FK-OR]|\\s)*:?(?:§[0-9a-fk-orA-FK-OR]|\\s)*)[0-9]{1,5}"
    );

    // General string / GUI pattern 2: Standalone "Level: <num>" in profile skull tooltips (Character Information)
    public static final Pattern GENERAL_STANDALONE_LEVEL_REGEX = Pattern.compile(
            "(?i)^((?:§[0-9a-fk-orA-FK-OR]|\\s)*level(?:§[0-9a-fk-orA-FK-OR]|\\s)*:(?:§[0-9a-fk-orA-FK-OR]|\\s)*)[0-9]{1,5}"
    );

    // Chat / title level-up announcements from the server (e.g. "You are now Level 96!", "You reached Hypixel Level 96!")
    public static final Pattern CHAT_SERVER_LEVEL_REGEX = Pattern.compile(
            "(?i)((?:you\\s+are\\s+now\\s+(?:hypixel\\s+)?level|you\\s+(?:have\\s+)?reached\\s+(?:hypixel\\s+)?level|your\\s+hypixel\\s+level\\s*(?:is|:)\\s*(?:now\\s+)?)(?:§[0-9a-fk-orA-FK-OR]|\\s)*)[0-9]{1,5}"
    );

    // ─── Symbols Check ───────────────────────────────────────────────────────

    /**
     * Checks if a string contains any Bedwars star or special prestige symbols.
     * If true, it belongs to Bedwars/Skywars and must NEVER be treated as Network Level.
     */
    public static boolean hasStarSymbol(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '✫' || c == '✪' || c == '★' || c == '✰' || c == '⚡' || c == '❤' || c == '⚔'
                    || c == '✈' || c == '☣' || c == '⚚' || c == '✝' || c == '☠' || c == '♨' || c == '☄'
                    || c == '✦' || c == '✧' || c == '✥' || c == '❖' || c == '❂' || c == '❃' || c == '❄' || c == '❅') {
                return true;
            }
        }
        return false;
    }

    // ─── Hypixel Lobby Detection ─────────────────────────────────────────────

    private static boolean cachedInLobby = false;
    private static long lastLobbyCheckTime = 0;

    /**
     * Checks if the player is currently in a Hypixel Lobby.
     * In Hypixel lobbies, the scoreboard sidebar ALWAYS contains a "Lobby: <number/name>" line.
     * In-game matches (SkyWars, Bedwars, Duels, etc.) do NOT have a "Lobby:" line.
     */
    public static boolean isInHypixelLobby() {
        long now = System.currentTimeMillis();
        if (now - lastLobbyCheckTime < 250) {
            return cachedInLobby;
        }
        lastLobbyCheckTime = now;
        cachedInLobby = computeIsInHypixelLobby();
        return cachedInLobby;
    }

    private static boolean computeIsInHypixelLobby() {
        try {
            // 1. Check HyBedWars mod HypixelPackets.INSTANCE.getInLobby() if available
            try {
                Class<?> hpClass = Class.forName("tomeko.hybedwars.location.HypixelPackets");
                Object instance = hpClass.getField("INSTANCE").get(null);
                java.lang.reflect.Method inLobbyMethod = hpClass.getMethod("getInLobby");
                Object res = inLobbyMethod.invoke(instance);
                if (res instanceof Boolean && ((Boolean) res)) {
                    return true;
                }
            } catch (Throwable ignored) {}

            // 2. Check scoreboard sidebar lines for "Lobby: <num>"
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) return false;
            Scoreboard sb = mc.theWorld.getScoreboard();
            if (sb != null) {
                ScoreObjective sidebar = sb.getObjectiveInDisplaySlot(1);
                if (sidebar != null) {
                    java.util.Collection<Score> scores = sb.getSortedScores(sidebar);
                    for (Score score : scores) {
                        String pName = score.getPlayerName();
                        ScorePlayerTeam team = sb.getPlayersTeam(pName);
                        String line = (team != null) ? (team.getColorPrefix() + pName + team.getColorSuffix()) : pName;
                        String clean = NickHiderMod.stripColorCodes(line).trim();
                        // Hypixel lobbies always have "Lobby: <num>" (e.g. "Lobby: 2", "Lobby: 4", "Lobby: mega1")
                        if (clean.matches("(?i)^lobby\\s*:\\s*[a-zA-Z0-9_-]+.*")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        NickHiderMod.saveConfig();
        dev.edtrick.nickhider.config.OneConfigCompat.sync();
    }

    public static int getFakeLevel() {
        return fakeLevel;
    }

    public static void setFakeLevel(int level) {
        if (level < 0) level = 0;
        if (level > 10000) level = 10000;
        fakeLevel = level;
        NickHiderMod.saveConfig();
        dev.edtrick.nickhider.config.OneConfigCompat.sync();
    }

    public static int getDetectedRealLevel() {
        return detectedRealLevel;
    }

    public static void setDetectedRealLevel(int level) {
        if (level >= 0) {
            detectedRealLevel = level;
        }
    }

    public static void loadValues(boolean newEnabled, int newLevel) {
        enabled = newEnabled;
        fakeLevel = (newLevel < 0) ? 0 : Math.min(newLevel, 10000);
    }

    public static void setAll(boolean newEnabled, int newLevel) {
        enabled = newEnabled;
        fakeLevel = (newLevel < 0) ? 0 : Math.min(newLevel, 10000);
        NickHiderMod.saveConfig();
        dev.edtrick.nickhider.config.OneConfigCompat.sync();
    }

    // ─── String & Component Replacement ─────────────────────────────────────

    /**
     * Filters nametag labels rendered above player entities (e.g. HyInfo level tag "§9Level§f: §e95").
     */
    public static String replaceLevelInNametag(String text) {
        if (text == null || !enabled) return text;
        if (hasStarSymbol(text)) return text;
        String lower = NickHiderMod.stripColorCodes(text).toLowerCase();
        if (lower.contains("bedwars") || lower.contains("skywars")) return text;

        Matcher m = NAMETAG_LEVEL_REGEX.matcher(text);
        if (m.find()) {
            captureRealLevel(m.group());
            return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + fakeLevel));
        }
        return text;
    }

    /**
     * Filters scoreboard lines matching "Hypixel Level: <num>" or "Network Level: <num>".
     * Strictly avoids Bedwars scoreboard lines ("Level: 1317✫").
     */
    public static String filterScoreboardLine(String text) {
        if (text == null || !enabled) return text;
        if (hasStarSymbol(text)) return text;

        Matcher m = SCOREBOARD_LEVEL_REGEX.matcher(text);
        if (m.find()) {
            captureRealLevel(m.group());
            return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + fakeLevel));
        }
        return text;
    }

    /**
     * General string filter used for tooltips (e.g. Character Information GUI hover,
     * Hypixel Leveling brewing stand progress) and other GUI elements.
     */
    public static String filterGeneralString(String text) {
        if (text == null || !enabled) return text;
        if (hasStarSymbol(text)) return text;

        // 1. Explicit Hypixel / Network level (with optional colon and progress bar)
        Matcher mH = GENERAL_HYPIXEL_LEVEL_REGEX.matcher(text);
        if (mH.find()) {
            captureRealLevel(mH.group());
            return mH.replaceFirst(Matcher.quoteReplacement(mH.group(1) + fakeLevel));
        }

        // 2. Standalone "Level: <num>" in profile tooltips (e.g. Character Information skull tooltip)
        String lower = NickHiderMod.stripColorCodes(text).toLowerCase();
        if (!lower.contains("until") && !lower.contains("experience") && !lower.contains("exp")
                && !lower.contains("bedwars") && !lower.contains("skywars") && !lower.contains("tier")
                && !lower.contains("island") && !lower.contains("kill") && !lower.contains("coins")) {
            Matcher mS = GENERAL_STANDALONE_LEVEL_REGEX.matcher(text);
            if (mS.find()) {
                captureRealLevel(mS.group());
                return mS.replaceFirst(Matcher.quoteReplacement(mS.group(1) + fakeLevel));
            }
        }

        return text;
    }

    /**
     * Filters actionbar ("record playing") and title/subtitle text.
     */
    public static String filterActionBarOrTitle(String text) {
        if (text == null || !enabled) return text;
        if (hasStarSymbol(text)) return text;

        Matcher m = CHAT_SERVER_LEVEL_REGEX.matcher(text);
        if (m.find()) {
            captureRealLevel(m.group());
            return m.replaceAll(Matcher.quoteReplacement(m.group(1) + fakeLevel));
        }
        Matcher mH = GENERAL_HYPIXEL_LEVEL_REGEX.matcher(text);
        if (mH.find()) {
            captureRealLevel(mH.group());
            return mH.replaceAll(Matcher.quoteReplacement(mH.group(1) + fakeLevel));
        }
        return text;
    }

    /**
     * Filters chat messages mentioning network level for the local player.
     * Strictly targets native server announcements (e.g. "You are now Level 96!"),
     * NEVER modifying regular player chat messages.
     */
    public static IChatComponent filterChat(IChatComponent component) {
        if (component == null || !enabled) return component;
        try {
            String fullUnformatted = component.getUnformattedText();
            if (hasStarSymbol(fullUnformatted)) return component;

            // Check if this is a server level-up announcement or /level response
            Matcher fullMatcher = CHAT_SERVER_LEVEL_REGEX.matcher(fullUnformatted);
            if (!fullMatcher.find()) {
                return component; // Not a server level announcement! Leave player messages untouched!
            }

            // Capture the real level announced by the server (handles dynamic level-ups!)
            captureRealLevel(fullMatcher.group());

            List<ChatComponentText> leaves = new ArrayList<>();
            collectLeaves(component, leaves);
            for (ChatComponentText leaf : leaves) {
                String val = leaf.getChatComponentText_TextValue();
                Matcher m = CHAT_SERVER_LEVEL_REGEX.matcher(val);
                if (m.find()) {
                    String replaced = m.replaceAll(Matcher.quoteReplacement(m.group(1) + fakeLevel));
                    setLeafText(leaf, replaced);
                }
            }
            return component;
        } catch (Exception ignored) {
            return component;
        }
    }

    private static void captureRealLevel(String matchedText) {
        if (matchedText == null) return;
        String clean = NickHiderMod.stripColorCodes(matchedText);
        Matcher dm = Pattern.compile("(?i)level\\s*:?\\s*([0-9]{1,5})").matcher(clean);
        if (dm.find()) {
            try {
                int parsed = Integer.parseInt(dm.group(1));
                if (parsed != fakeLevel && parsed > 0 && parsed <= 5000) {
                    detectedRealLevel = parsed;
                }
            } catch (NumberFormatException ignored) {}
            return;
        }
        String digits = clean.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            try {
                int parsed = Integer.parseInt(digits);
                if (parsed != fakeLevel && parsed > 0 && parsed <= 5000) {
                    detectedRealLevel = parsed;
                }
            } catch (NumberFormatException ignored) {}
        }
    }

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
}
