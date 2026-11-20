package dev.edtrick.nickhider.command;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.edtrick.nickhider.NickHiderMod;
import dev.edtrick.nickhider.mixin.NetworkPlayerInfoAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses and executes /nickhider sub-commands with interactive help and Tab autocompletion.
 */
public class NickHiderCommand {

    public static boolean handle(String message) {
        if (message == null) return false;

        String trimmed = message.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }

        String lower = trimmed.toLowerCase();
        String afterCmd = null;

        if (lower.startsWith("nickhider")) {
            afterCmd = trimmed.substring("nickhider".length()).trim();
        } else if (lower.startsWith("nick ") || lower.equals("nick")) {
            afterCmd = trimmed.substring("nick".length()).trim();
            if (!afterCmd.isEmpty()) {
                String firstWord = afterCmd.split("\\s+")[0].toLowerCase();
                if (!firstWord.equals("realskin") && !firstWord.equals("unnick") && !firstWord.equals("skin")) {
                    afterCmd = "nick " + afterCmd;
                }
            } else {
                afterCmd = "nick";
            }
        } else if (lower.startsWith("skin ") || lower.equals("skin")) {
            afterCmd = "skin " + trimmed.substring("skin".length()).trim();
        } else if (lower.startsWith("unnick")) {
            afterCmd = "unnick";
        } else if (lower.startsWith("realskin")) {
            afterCmd = "realskin";
        } else {
            return false;
        }

        if (afterCmd.isEmpty() || afterCmd.equalsIgnoreCase("help")) {
            printHelp();
            return true;
        }

        String[] parts = afterCmd.split("\\s+", 2);
        String sub = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1].trim() : "";

        switch (sub) {
            case "nick":
                if (!args.isEmpty()) {
                    handleSetNick(args);
                } else {
                    handleShowNick();
                }
                return true;

            case "skin":
                if (!args.isEmpty()) {
                    handleSetSkin(args);
                } else {
                    handleShowSkin();
                }
                return true;

            case "unnick":
                handleUnnick();
                return true;

            case "realskin":
                handleRealSkin();
                return true;

            case "help":
                printHelp();
                return true;

            default:
                NickHiderMod.sendMessage("§cUnknown sub-command: §f" + sub);
                printHelp();
                return true;
        }
    }

    public static void handleSetNick(String nickInput) {
        if (nickInput.isEmpty()) {
            handleShowNick();
            return;
        }
        String stripped = NickHiderMod.stripColorCodes(nickInput);
        if (stripped.length() > 30) {
            NickHiderMod.sendMessage(
                    "§cError: Nick is too long! Maximum 30 characters (visible length: " + stripped.length() + ")."
            );
            return;
        }
        String formatted = NickHiderMod.translateColorCodes(nickInput);
        NickHiderMod.setFakeNick(formatted);
        NickHiderMod.sendMessage("§aNick set to: " + formatted);
    }

    private static void handleShowNick() {
        String current = NickHiderMod.getFakeNick();
        if (current == null) {
            String realName = getRealName();
            NickHiderMod.sendMessage("§7No fake nick active. Using real IGN: §f§l" + realName);
        } else {
            NickHiderMod.sendMessage("§7Current fake nick: " + current);
        }
    }

    public static void handleSetSkin(final String ign) {
        if (ign.isEmpty()) {
            handleShowSkin();
            return;
        }
        NickHiderMod.sendMessage("§7Fetching skin for §f" + ign + "§7, please wait...");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final String[] textures = NickHiderMod.fetchSkinData(ign);
                if (textures == null) {
                    scheduleOnMain(new Runnable() {
                        @Override
                        public void run() {
                            NickHiderMod.sendMessage(
                                "§cError: Player \"§f" + ign + "§c\" was not found on Mojang servers."
                            );
                        }
                    });
                    return;
                }

                scheduleOnMain(new Runnable() {
                    @Override
                    public void run() {
                        NickHiderMod.setFakeSkin(textures[0], textures[1], ign);
                        NickHiderMod.sendMessage("§aSkin set to that of player: §f§l" + ign);
                        invalidateSkinCache();
                    }
                });
            }
        }, "NickHider-SkinFetch");
        thread.setDaemon(true);
        thread.start();
    }

    private static void handleShowSkin() {
        String owner = NickHiderMod.getFakeSkinOwner();
        if (owner == null) {
            NickHiderMod.sendMessage("§7No fake skin active. Using your real account skin.");
        } else {
            NickHiderMod.sendMessage("§7Current fake skin is from player: §f§l" + owner);
        }
    }

    public static void handleUnnick() {
        if (NickHiderMod.getFakeNick() == null) {
            NickHiderMod.sendMessage("§7No fake nick is currently active.");
        } else {
            NickHiderMod.clearFakeNick();
            NickHiderMod.sendMessage("§aNick reset to your real IGN: §f§l" + getRealName());
        }
    }

    public static void handleRealSkin() {
        if (NickHiderMod.getFakeSkinOwner() == null) {
            NickHiderMod.sendMessage("§7No fake skin is currently active.");
        } else {
            NickHiderMod.clearFakeSkin();
            NickHiderMod.sendMessage("§aSkin reset to your real account skin.");
            invalidateSkinCache();
        }
    }

    private static void scheduleOnMain(Runnable task) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            mc.addScheduledTask(task);
        }
    }

    // ─── Interactive Help Menu ────────────────────────────────────────────────

    private static void printHelp() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        mc.thePlayer.addChatMessage(new ChatComponentText(
                NickHiderMod.rainbowPrefix() + " " + EnumChatFormatting.GOLD + "Available commands:"
        ));

        sendCommandHelp("1", "/nickhider nick <NICK>", "/nickhider nick ",
                "Sets your fake nick client-side.\nSupports & color codes (e.g. &6&lEdTrick).\nMax 30 visible characters.");

        sendCommandHelp("2", "/nickhider skin <PLAYER_IGN>", "/nickhider skin ",
                "Fetches and applies another player's skin from Mojang\nclient-side and updates immediately.");

        sendCommandHelp("3", "/nickhider nick", "/nickhider nick",
                "Shows the currently active fake nick\nor tells you if you are using your real IGN.");

        sendCommandHelp("4", "/nickhider skin", "/nickhider skin",
                "Shows whose skin you are currently using\nor tells you if you are using your real account skin.");

        sendCommandHelp("5", "/nickhider unnick", "/nickhider unnick",
                "Resets your fake nick back to your real account IGN.");

        sendCommandHelp("6", "/nickhider realskin", "/nickhider realskin",
                "Resets your skin back to your real account skin\nimmediately without reconnecting.");
    }

    private static void sendCommandHelp(String num, String usage, String suggestCommand, String hoverDescription) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        ChatComponentText item = new ChatComponentText(
                EnumChatFormatting.GRAY + num + ". " + EnumChatFormatting.YELLOW + usage
        );

        ChatStyle style = new ChatStyle();
        style.setChatHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText(
                        EnumChatFormatting.AQUA + "Command: " + EnumChatFormatting.WHITE + usage + "\n\n"
                        + EnumChatFormatting.GREEN + "Description:\n" + EnumChatFormatting.GRAY + hoverDescription + "\n\n"
                        + EnumChatFormatting.YELLOW + "▶ Click to prepare command in chat"
                )
        ));
        style.setChatClickEvent(new ClickEvent(
                ClickEvent.Action.SUGGEST_COMMAND,
                suggestCommand
        ));
        item.setChatStyle(style);

        mc.thePlayer.addChatMessage(item);
    }

    // ─── Tab Autocompletion ───────────────────────────────────────────────────

    public static String[] getTabCompletions(String leftOfCursor) {
        if (leftOfCursor == null) return null;
        String lower = leftOfCursor.toLowerCase();

        // Autocomplete the root command
        if (lower.startsWith("/nick") && !leftOfCursor.contains(" ")) {
            if ("/nickhider".startsWith(lower)) {
                return new String[]{"/nickhider"};
            }
        }

        // Autocomplete subcommands: "/nickhider " or "/nick "
        if (lower.startsWith("/nickhider ") || lower.startsWith("/nick ")) {
            boolean isNickhider = lower.startsWith("/nickhider ");
            String afterCmd = leftOfCursor.substring(isNickhider ? "/nickhider ".length() : "/nick ".length());

            String[] subcommands = new String[]{"nick", "skin", "unnick", "realskin"};

            if (!afterCmd.contains(" ")) {
                String subLower = afterCmd.toLowerCase();
                List<String> list = new ArrayList<>();
                for (String sub : subcommands) {
                    if (sub.startsWith(subLower)) {
                        list.add(sub);
                    }
                }
                return list.toArray(new String[0]);
            } else {
                // If sub is skin, autocomplete online players
                String[] parts = afterCmd.split("\\s+", 2);
                String sub = parts[0].toLowerCase();
                String currentArg = parts.length > 1 ? parts[1].toLowerCase() : "";

                if (sub.equals("skin")) {
                    List<String> list = new ArrayList<>();
                    try {
                        Minecraft mc = Minecraft.getMinecraft();
                        if (mc != null && mc.getNetHandler() != null) {
                            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                                if (info.getGameProfile() != null && info.getGameProfile().getName() != null) {
                                    String name = info.getGameProfile().getName();
                                    if (name.toLowerCase().startsWith(currentArg)) {
                                        list.add(name);
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    return list.toArray(new String[0]);
                }
            }
        }

        return null;
    }

    public static String getRealName() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.getSession() != null) {
            return mc.getSession().getUsername();
        }
        return "Unknown";
    }

    public static void invalidateSkinCache() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return;

            NickHiderMod.reloadFakeSkinLocation();

            if (mc.thePlayer != null && mc.getNetHandler() != null && mc.thePlayer.getUniqueID() != null) {
                try {
                    NetworkPlayerInfo myInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                    if (myInfo != null) {
                        applySkinToInfo(myInfo);
                    }
                } catch (Throwable ignored) {}
            }

            if (mc.getNetHandler() != null && mc.getNetHandler().getPlayerInfoMap() != null) {
                for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                    if (isLocalInfo(info)) {
                        applySkinToInfo(info);
                    }
                }
            }
        } catch (Exception e) {
            NickHiderMod.LOGGER.warn("[NickHider] Could not refresh player list skin textures: {}", e.getMessage());
        }
    }

    private static void applySkinToInfo(NetworkPlayerInfo info) {
        try {
            GameProfile profile = info.getGameProfile();
            String fakeValue = NickHiderMod.getFakeSkinValue();
            String fakeSig   = NickHiderMod.getFakeSkinSignature();

            if (fakeValue != null) {
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", fakeValue, fakeSig != null ? fakeSig : ""));
            } else {
                profile.getProperties().removeAll("textures");
                Property realProp = NickHiderMod.getRealSkinProperty();
                if (realProp != null) {
                    profile.getProperties().put("textures", realProp);
                }
            }

            NetworkPlayerInfoAccessor accessor = (NetworkPlayerInfoAccessor) info;
            accessor.setLocationSkin(NickHiderMod.getFakeSkinLocation());
            accessor.setPlayerTexturesLoaded(false);
            accessor.invokeLoadPlayerTextures();
        } catch (Throwable ignored) {}
    }

    private static boolean isLocalInfo(NetworkPlayerInfo info) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || info == null || info.getGameProfile() == null) return false;

        if (mc.thePlayer != null) {
            if (mc.thePlayer.getUniqueID() != null && mc.thePlayer.getUniqueID().equals(info.getGameProfile().getId())) return true;
            if (mc.thePlayer.getName() != null && mc.thePlayer.getName().equalsIgnoreCase(info.getGameProfile().getName())) return true;
        }

        if (mc.getSession() != null) {
            if (mc.getSession().getProfile() != null && mc.getSession().getProfile().getId() != null) {
                if (mc.getSession().getProfile().getId().equals(info.getGameProfile().getId())) return true;
            }
            String realName = mc.getSession().getUsername();
            if (realName != null && realName.equalsIgnoreCase(info.getGameProfile().getName())) return true;
        }

        return false;
    }
}
