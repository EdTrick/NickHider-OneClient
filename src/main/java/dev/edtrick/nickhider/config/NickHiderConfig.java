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
    }

    public void syncFromMod() {
        syncing = true;
        try {
            enableNickHider = (NickHiderMod.getFakeNick() != null);
            if (NickHiderMod.getFakeNick() != null) {
                fakeNick = NickHiderMod.getFakeNick();
            }
            enableSkinHider = (NickHiderMod.getFakeSkinOwner() != null);
            if (NickHiderMod.getFakeSkinOwner() != null) {
                fakeSkin = NickHiderMod.getFakeSkinOwner();
            }
        } finally {
            syncing = false;
        }
    }
}
