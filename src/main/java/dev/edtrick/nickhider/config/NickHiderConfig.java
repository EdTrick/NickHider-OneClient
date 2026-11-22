package dev.edtrick.nickhider.config;

import dev.edtrick.nickhider.NickHiderMod;
import dev.edtrick.nickhider.command.NickHiderCommand;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Button;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;

public class NickHiderConfig extends Config {

    public static NickHiderConfig INSTANCE;
    private static boolean syncing = false;

    // ─── Nickname Section ────────────────────────────────────────────────────

    @Switch(
            title = "Enable Nick Hider",
            description = "Toggle client-side nickname hiding on or off",
            subcategory = "Nickname"
    )
    public static boolean enableNickHider = false;

    @Text(
            title = "Fake Nick",
            description = "Client-side nickname to display (supports & color codes, max 30 chars)",
            placeholder = "e.g. &6&lEdTrick",
            subcategory = "Nickname"
    )
    public static String fakeNick = "";

    @Text(
            title = "Server Nick",
            description = "Server-side nick (auto-detected when chatting or via /nick, or set manually)",
            placeholder = "auto",
            subcategory = "Nickname"
    )
    public static String serverNick = "auto";

    @Button(
            title = "Apply Nick",
            text = "Apply",
            description = "Apply the fake nickname entered above",
            subcategory = "Nickname"
    )
    public void applyNick() {
        if (fakeNick == null || fakeNick.trim().isEmpty()) {
            enableNickHider = false;
            NickHiderCommand.handleUnnick();
        } else {
            enableNickHider = true;
            NickHiderCommand.handleSetNick(fakeNick.trim());
        }
        if (serverNick != null && !serverNick.trim().isEmpty() && !serverNick.equalsIgnoreCase("auto")) {
            NickHiderMod.setDetectedChatNick(serverNick.trim());
        }
        save();
    }

    // ─── Skin Section ────────────────────────────────────────────────────────

    @Switch(
            title = "Enable Skin Hider",
            description = "Toggle client-side custom skin on or off",
            subcategory = "Skin"
    )
    public static boolean enableSkinHider = false;

    @Text(
            title = "Fake Skin (IGN)",
            description = "Minecraft username whose skin will be applied client-side",
            placeholder = "e.g. Kolijn",
            subcategory = "Skin"
    )
    public static String fakeSkin = "";

    @Button(
            title = "Apply Skin",
            text = "Apply Skin",
            description = "Fetch and apply the skin of the player entered above",
            subcategory = "Skin"
    )
    public void applySkin() {
        if (fakeSkin == null || fakeSkin.trim().isEmpty()) {
            enableSkinHider = false;
            NickHiderCommand.handleRealSkin();
        } else {
            enableSkinHider = true;
            NickHiderCommand.handleSetSkin(fakeSkin.trim());
        }
        save();
    }

    // ─── Bedwars Level Section ───────────────────────────────────────────────

    @Switch(
            title = "Enable Bedwars Level",
            description = "Toggle client-side Bedwars stars & prestige spoofing on or off",
            subcategory = "Bedwars Level"
    )
    public static boolean enableBwLevel = false;

    @Text(
            title = "Bedwars Stars",
            description = "Numeric level (0 - 10000)",
            placeholder = "e.g. 1402",
            subcategory = "Bedwars Level"
    )
    public static String bwStars = "1402";

    @Text(
            title = "Color Scheme Preset",
            description = "Prestige level to borrow colors from (e.g. 1200, gold_prime) or 'auto'",
            placeholder = "auto",
            subcategory = "Bedwars Level"
    )
    public static String bwColorPreset = "auto";

    @Text(
            title = "Star Icon",
            description = "Star symbol (✫, ✪, ⚝, ✥, ✭) or prestige preset (or 'auto')",
            placeholder = "auto",
            subcategory = "Bedwars Level"
    )
    public static String bwIconPreset = "auto";

    @Button(
            title = "Apply Bedwars Level",
            text = "Apply",
            description = "Apply Bedwars level and prestige configuration",
            subcategory = "Bedwars Level"
    )
    public void applyBwLevel() {
        syncing = true;
        try {
            boolean inEnabled = enableBwLevel;
            String inStars = (bwStars != null) ? bwStars.trim() : "";
            String inColor = (bwColorPreset != null) ? bwColorPreset.trim() : "";
            String inIcon = (bwIconPreset != null) ? bwIconPreset.trim() : "";

            int stars = 1402;
            if (!inStars.isEmpty()) {
                try {
                    stars = Integer.parseInt(inStars);
                } catch (NumberFormatException ignored) {}
            }

            int colorLevel = -1;
            if (!inColor.isEmpty() && !inColor.equalsIgnoreCase("auto") && !inColor.equalsIgnoreCase("reset")) {
                dev.edtrick.nickhider.bedwars.BedwarsPrestige p = dev.edtrick.nickhider.bedwars.BedwarsPrestige.find(inColor);
                if (p != null) {
                    colorLevel = p.level;
                }
            }

            String icon = null;
            if (!inIcon.isEmpty() && !inIcon.equalsIgnoreCase("auto") && !inIcon.equalsIgnoreCase("reset")) {
                icon = dev.edtrick.nickhider.bedwars.BedwarsPrestige.resolveIcon(inIcon);
            }

            dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setAll(inEnabled, stars, colorLevel, icon);

            bwStars = String.valueOf(stars);
            bwColorPreset = (colorLevel >= 0) ? String.valueOf(colorLevel) : "auto";
            bwIconPreset = (icon != null) ? icon : "auto";
            enableBwLevel = inEnabled;

            save();
            NickHiderMod.sendMessage("§aBedwars level configuration applied: " + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag());
        } finally {
            syncing = false;
        }
    }

    // ─── Network Level Section ───────────────────────────────────────────────

    @Switch(
            title = "Enable Network Level",
            description = "Toggle client-side Hypixel Network Level spoofing on or off",
            subcategory = "Network Level"
    )
    public static boolean enableNetworkLevel = false;

    @Text(
            title = "Network Level",
            description = "Enter spoofed Hypixel Network Level (0 - 10000)",
            placeholder = "250",
            subcategory = "Network Level"
    )
    public static String networkLevel = "250";

    @Button(
            title = "Apply Network Level",
            text = "Apply",
            description = "Apply Hypixel Network Level configuration",
            subcategory = "Network Level"
    )
    public void applyNetworkLevel() {
        syncing = true;
        try {
            boolean inEnabled = enableNetworkLevel;
            String inLevel = (networkLevel != null) ? networkLevel.trim() : "";
            int lvl = 250;
            if (!inLevel.isEmpty()) {
                try {
                    lvl = Integer.parseInt(inLevel);
                } catch (NumberFormatException ignored) {}
            }
            dev.edtrick.nickhider.network.NetworkLevelManager.setAll(inEnabled, lvl);
            networkLevel = String.valueOf(lvl);
            enableNetworkLevel = inEnabled;
            save();
            NickHiderMod.sendMessage("§aNetwork Level configuration applied: §b" + lvl);
        } finally {
            syncing = false;
        }
    }

    // ─── Constructor & Callbacks ─────────────────────────────────────────────

    public NickHiderConfig() {
        super("nickhider.json", "/assets/nickhider/icon.png", "NickHider", Config.Category.VISUALS);
        addAliases(new String[]{"nick", "skin", "nickhider"});
        INSTANCE = this;

        addCallback("enableNickHider", () -> {
            if (syncing) return;
            if (enableNickHider) {
                if (fakeNick != null && !fakeNick.trim().isEmpty()) {
                    NickHiderCommand.handleSetNick(fakeNick.trim());
                } else {
                    NickHiderMod.sendMessage("§cError: No fake nick entered. Please type a nickname in the box below.");
                }
            } else {
                NickHiderCommand.handleUnnick();
            }
            save();
        });

        addCallback("enableSkinHider", () -> {
            if (syncing) return;
            if (enableSkinHider) {
                if (fakeSkin != null && !fakeSkin.trim().isEmpty()) {
                    NickHiderCommand.handleSetSkin(fakeSkin.trim());
                } else {
                    NickHiderMod.sendMessage("§cError: No skin IGN entered. Please type a player username in the box below.");
                }
            } else {
                NickHiderCommand.handleRealSkin();
            }
            save();
        });

        addCallback("enableBwLevel", () -> {
            if (syncing) return;
            dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setEnabled(enableBwLevel);
            save();
            NickHiderMod.sendMessage(enableBwLevel
                    ? "§aBedwars Level spoofing enabled: " + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag()
                    : "§eBedwars Level spoofing disabled.");
        });

        addCallback("enableNetworkLevel", () -> {
            if (syncing) return;
            dev.edtrick.nickhider.network.NetworkLevelManager.setEnabled(enableNetworkLevel);
            save();
            NickHiderMod.sendMessage(enableNetworkLevel
                    ? "§aHypixel Network Level spoofing enabled: §b" + dev.edtrick.nickhider.network.NetworkLevelManager.getFakeLevel()
                    : "§eHypixel Network Level spoofing disabled.");
        });
    }

    public void syncFromMod() {
        if (syncing) return;
        syncing = true;
        try {
            enableNickHider = (NickHiderMod.getFakeNick() != null);
            if (NickHiderMod.getFakeNick() != null) {
                fakeNick = NickHiderMod.getFakeNick();
            }
            String sn = NickHiderMod.getDetectedChatNick();
            serverNick = (sn != null) ? sn : "auto";
            enableSkinHider = (NickHiderMod.getFakeSkinOwner() != null);
            if (NickHiderMod.getFakeSkinOwner() != null) {
                fakeSkin = NickHiderMod.getFakeSkinOwner();
            }

            enableBwLevel = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.isEnabled();
            bwStars = String.valueOf(dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFakeStars());
            int c = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getColorPresetLevel();
            bwColorPreset = (c >= 0) ? String.valueOf(c) : "auto";
            String ic = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getCustomIcon();
            bwIconPreset = (ic != null) ? ic : "auto";

            enableNetworkLevel = dev.edtrick.nickhider.network.NetworkLevelManager.isEnabled();
            networkLevel = String.valueOf(dev.edtrick.nickhider.network.NetworkLevelManager.getFakeLevel());
        } finally {
            syncing = false;
        }
    }
}
