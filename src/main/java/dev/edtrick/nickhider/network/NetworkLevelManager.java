package dev.edtrick.nickhider.network;

import dev.edtrick.nickhider.NickHiderMod;
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

    // Nametag line above player e.g. "Level: 95", "§9Level: §e95"
    public static final Pattern NAMETAG_LEVEL_REGEX = Pattern.compile(
            "(?i)^((?:§[0-9a-fk-orA-FK-OR]|\\s)*(?:hypixel\\s+|network\\s+)?level\\s*:\\s*(?:§[0-9a-fk-orA-FK-OR])*)[0-9]{1,5}(?:\\.[0-9]+)?"
    );

    // Scoreboard line e.g. "Hypixel Level: 95", "Hypixel Level: §b95"
    public static final Pattern SCOREBOARD_LEVEL_REGEX = Pattern.compile(
            "(?i)((?:hypixel|network)\\s+level\\s*:\\s*(?:§[0-9a-fk-orA-FK-OR])*)[0-9]{1,5}(?:\\.[0-9]+)?"
    );

    // Tooltip line in profile GUI e.g. "§7Level: §695"
    public static final Pattern TOOLTIP_LEVEL_REGEX = Pattern.compile(
            "(?i)^((?:§[0-9a-fk-orA-FK-OR]|\\s)*level\\s*:\\s*(?:§[0-9a-fk-orA-FK-OR])*)[0-9]{1,5}(?:\\.[0-9]+)?"
    );

    // Chat notification e.g. "You are currently Hypixel Level 95!", "reached Hypixel Level 95!"
    public static final Pattern CHAT_LEVEL_REGEX = Pattern.compile(
            "(?i)((?:hypixel\\s+level|network\\s+level|reached\\s+(?:hypixel\\s+)?level|level\\s*(?:is|:))\\s*(?:§[0-9a-fk-orA-FK-OR])*)[0-9]{1,5}(?:\\.[0-9]+)?"
    );

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
     * Filters nametag labels rendered above player entities (e.g. HyInfo level tag).
     */
    public static String replaceLevelInNametag(String text) {
        if (text == null || !enabled) return text;
        Matcher m = NAMETAG_LEVEL_REGEX.matcher(text);
        if (m.find()) {
            captureRealLevel(m.group());
            return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + fakeLevel));
        }
        return text;
    }

    /**
     * Filters scoreboard lines matching "Hypixel Level: <num>" or "Network Level: <num>".
     */
    public static String filterScoreboardLine(String text) {
        if (text == null || !enabled) return text;
        Matcher m = SCOREBOARD_LEVEL_REGEX.matcher(text);
        if (m.find()) {
            captureRealLevel(m.group());
            return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + fakeLevel));
        }
        return text;
    }

    /**
     * General string filter used for tooltips (e.g. Character Information GUI hover)
     * and other GUI elements.
     */
    public static String filterGeneralString(String text) {
        if (text == null || !enabled) return text;

        // 1. Check profile hover line "Level: <num>"
        Matcher mTooltip = TOOLTIP_LEVEL_REGEX.matcher(text);
        if (mTooltip.find()) {
            captureRealLevel(mTooltip.group());
            return mTooltip.replaceFirst(Matcher.quoteReplacement(mTooltip.group(1) + fakeLevel));
        }

        // 2. Check scoreboard-style "Hypixel Level: <num>"
        Matcher mSb = SCOREBOARD_LEVEL_REGEX.matcher(text);
        if (mSb.find()) {
            captureRealLevel(mSb.group());
            return mSb.replaceFirst(Matcher.quoteReplacement(mSb.group(1) + fakeLevel));
        }

        return text;
    }

    /**
     * Filters actionbar ("record playing") and title/subtitle text.
     */
    public static String filterActionBarOrTitle(String text) {
        if (text == null || !enabled) return text;
        Matcher m = CHAT_LEVEL_REGEX.matcher(text);
        if (m.find()) {
            captureRealLevel(m.group());
            return m.replaceAll(Matcher.quoteReplacement(m.group(1) + fakeLevel));
        }
        return text;
    }

    /**
     * Filters chat messages mentioning network level for the local player.
     */
    public static IChatComponent filterChat(IChatComponent component) {
        if (component == null || !enabled) return component;
        try {
            String fullUnformatted = component.getUnformattedText();
            List<String> names = NickHiderMod.getNamesToReplace();
            boolean mentionsLocal = false;
            for (String name : names) {
                if (name != null && !name.isEmpty() && fullUnformatted.toLowerCase().contains(name.toLowerCase())) {
                    mentionsLocal = true;
                    break;
                }
            }
            if (!mentionsLocal && (fullUnformatted.contains("Hypixel Level") || fullUnformatted.contains("Network Level"))) {
                mentionsLocal = true;
            }
            if (!mentionsLocal) return component;

            List<ChatComponentText> leaves = new ArrayList<>();
            collectLeaves(component, leaves);
            for (ChatComponentText leaf : leaves) {
                String val = leaf.getChatComponentText_TextValue();
                Matcher m = CHAT_LEVEL_REGEX.matcher(val);
                if (m.find()) {
                    captureRealLevel(m.group());
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
        String digits = matchedText.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            try {
                int parsed = Integer.parseInt(digits);
                if (parsed != fakeLevel) {
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
