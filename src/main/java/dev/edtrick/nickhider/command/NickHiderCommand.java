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
import net.minecraft.util.ResourceLocation;

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
        } else if (lower.startsWith("bwlvl ") || lower.equals("bwlvl")) {
            afterCmd = "bwlvl " + trimmed.substring("bwlvl".length()).trim();
        } else if (lower.startsWith("bedwars ") || lower.equals("bedwars")) {
            afterCmd = "bwlvl " + trimmed.substring("bedwars".length()).trim();
        } else if (lower.startsWith("nlvl ") || lower.equals("nlvl")) {
            afterCmd = "nlvl " + trimmed.substring("nlvl".length()).trim();
        } else if (lower.startsWith("networklevel ") || lower.equals("networklevel")) {
            afterCmd = "nlvl " + trimmed.substring("networklevel".length()).trim();
        } else if (lower.startsWith("nick ") || lower.equals("nick")) {
            afterCmd = trimmed.substring("nick".length()).trim();
            if (!afterCmd.isEmpty()) {
                String firstWord = afterCmd.split("\\s+")[0].toLowerCase();
                if (!firstWord.equals("realskin") && !firstWord.equals("unnick") && !firstWord.equals("skin")
                        && !firstWord.equals("bwlvl") && !firstWord.equals("bedwars") && !firstWord.equals("level")
                        && !firstWord.equals("nlvl") && !firstWord.equals("networklevel")) {
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

            case "servernick":
            case "snick":
                handleServerNick(args);
                return true;

            case "replay":
            case "replaynick":
                handleReplay(args);
                return true;

            case "bwlvl":
            case "bedwars":
                handleBwLvl(args);
                return true;

            case "level":
            case "nlvl":
            case "networklevel":
                handleNetworkLevel(args);
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

    public static void handleServerNick(String input) {
        if (input == null || input.isEmpty()) {
            String current = NickHiderMod.getDetectedChatNick();
            if (current != null) {
                NickHiderMod.sendMessage("§7Current server-side nick: §b" + current);
            } else {
                NickHiderMod.sendMessage("§7No server-side nick currently active. (Only your real IGN is replaced).");
            }
            return;
        }
        if (input.equalsIgnoreCase("reset") || input.equalsIgnoreCase("clear")
                || input.equalsIgnoreCase("none") || input.equalsIgnoreCase("auto")) {
            NickHiderMod.setDetectedChatNick(null);
            invalidateSkinCache();
            NickHiderMod.sendMessage("§aServer-side nick cleared. (Now only replacing your real IGN: §f" + getRealName() + "§a).");
            return;
        }
        String clean = NickHiderMod.stripColorCodes(input).trim();
        NickHiderMod.setDetectedChatNick(clean);
        invalidateSkinCache();
        NickHiderMod.sendMessage("§aServer-side nick set to: §b" + clean);
    }

    public static void handleReplay(String arg) {
        if (!arg.isEmpty()) {
            handleServerNick(arg);
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            NickHiderMod.sendMessage("§cError: You must be in a world to view replay players.");
            return;
        }

        List<String> playerNames = new ArrayList<String>();
        for (net.minecraft.entity.player.EntityPlayer p : mc.theWorld.playerEntities) {
            if (p != null && p != mc.thePlayer) {
                String pName = p.getName();
                if (pName != null && !pName.isEmpty() && !playerNames.contains(pName)) {
                    playerNames.add(pName);
                }
            }
        }

        if (playerNames.isEmpty()) {
            NickHiderMod.sendMessage("§7No other players found in the current world / replay.");
            return;
        }

        NickHiderMod.sendMessage("§6Players found in this world / replay (click to set as your nick):");
        ChatComponentText line = new ChatComponentText("");

        // Reset button
        ChatComponentText resetBtn = new ChatComponentText("§c[Reset / Real IGN] ");
        ChatStyle resetStyle = new ChatStyle();
        resetStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nickhider replay reset"));
        resetStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText("§eClick to clear server nick and only replace your real IGN (" + getRealName() + ")")));
        resetBtn.setChatStyle(resetStyle);
        line.appendSibling(resetBtn);

        for (String pName : playerNames) {
            ChatComponentText btn = new ChatComponentText("§b[" + pName + "] ");
            ChatStyle style = new ChatStyle();
            style.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nickhider replay " + pName));
            style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ChatComponentText("§eClick to set your server nick to §b" + pName)));
            btn.setChatStyle(style);
            line.appendSibling(btn);
        }
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(line);
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

    // ─── Hypixel Network Level Commands ─────────────────────────────────────

    public static void handleNetworkLevel(String args) {
        if (args.isEmpty() || args.equalsIgnoreCase("status")) {
            printNetworkLevelStatus();
            return;
        }

        if (args.equalsIgnoreCase("reset") || args.equalsIgnoreCase("clear")
                || args.equalsIgnoreCase("off") || args.equalsIgnoreCase("disable")) {
            dev.edtrick.nickhider.network.NetworkLevelManager.setEnabled(false);
            int real = dev.edtrick.nickhider.network.NetworkLevelManager.getDetectedRealLevel();
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null && real >= 0 && dev.edtrick.nickhider.network.NetworkLevelManager.isInHypixelLobby()) {
                mc.thePlayer.experienceLevel = real;
            }
            NickHiderMod.sendMessage("§eHypixel Network Level spoofing §cDISABLED§e. (Showing real level: §a" + (real >= 0 ? real : "detected in lobby") + "§e).");
            return;
        }

        if (args.equalsIgnoreCase("toggle")) {
            boolean newState = !dev.edtrick.nickhider.network.NetworkLevelManager.isEnabled();
            dev.edtrick.nickhider.network.NetworkLevelManager.setEnabled(newState);
            NickHiderMod.sendMessage(newState
                    ? "§aHypixel Network Level spoofing §2ENABLED §7(Level: §b" + dev.edtrick.nickhider.network.NetworkLevelManager.getFakeLevel() + "§7)"
                    : "§eHypixel Network Level spoofing §cDISABLED§e.");
            return;
        }

        if (args.equalsIgnoreCase("on") || args.equalsIgnoreCase("enable")) {
            dev.edtrick.nickhider.network.NetworkLevelManager.setEnabled(true);
            NickHiderMod.sendMessage("§aHypixel Network Level spoofing §2ENABLED §7(Level: §b" + dev.edtrick.nickhider.network.NetworkLevelManager.getFakeLevel() + "§7)");
            return;
        }

        try {
            int lvl = Integer.parseInt(args.trim());
            if (lvl < 0 || lvl > 10000) {
                NickHiderMod.sendMessage("§cError: Network level must be between 0 and 10000!");
                return;
            }
            dev.edtrick.nickhider.network.NetworkLevelManager.setFakeLevel(lvl);
            dev.edtrick.nickhider.network.NetworkLevelManager.setEnabled(true);
            NickHiderMod.sendMessage("§aHypixel Network Level set to: §b" + lvl + " §7(Spoofing §2ENABLED§7)");
        } catch (NumberFormatException e) {
            NickHiderMod.sendMessage("§cError: Invalid level \"" + args + "\". Must be an integer between 0 and 10000, or \"reset\".");
            printNetworkLevelStatus();
        }
    }

    public static void printNetworkLevelStatus() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        boolean enabled = dev.edtrick.nickhider.network.NetworkLevelManager.isEnabled();
        int fake = dev.edtrick.nickhider.network.NetworkLevelManager.getFakeLevel();
        int real = dev.edtrick.nickhider.network.NetworkLevelManager.getDetectedRealLevel();

        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GRAY + "-----------------------------------------------------"));
        mc.thePlayer.addChatMessage(new ChatComponentText(NickHiderMod.rainbowPrefix() + " " + EnumChatFormatting.AQUA + EnumChatFormatting.BOLD + "Hypixel Network Level Spoofing"));

        // Status row
        ChatComponentText statusRow = new ChatComponentText(EnumChatFormatting.GRAY + "Status: " + (enabled ? EnumChatFormatting.GREEN + "ENABLED " : EnumChatFormatting.RED + "DISABLED "));
        ChatComponentText toggleBtn = new ChatComponentText(EnumChatFormatting.YELLOW + "[TOGGLE]");
        ChatStyle toggleStyle = new ChatStyle();
        toggleStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Click to toggle ON/OFF")));
        toggleStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nickhider nlvl toggle"));
        toggleBtn.setChatStyle(toggleStyle);
        statusRow.appendSibling(toggleBtn);
        mc.thePlayer.addChatMessage(statusRow);

        // Fake Level row
        ChatComponentText levelRow = new ChatComponentText(EnumChatFormatting.GRAY + "Fake Level: " + EnumChatFormatting.AQUA + fake + " ");
        ChatComponentText editBtn = new ChatComponentText(EnumChatFormatting.AQUA + "[EDIT]");
        ChatStyle editStyle = new ChatStyle();
        editStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.AQUA + "Click to type network level")));
        editStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/nickhider nlvl "));
        editBtn.setChatStyle(editStyle);
        levelRow.appendSibling(editBtn);
        mc.thePlayer.addChatMessage(levelRow);

        // Real Level row
        String realStr = (real >= 0) ? String.valueOf(real) : "Not detected yet (enter a lobby)";
        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Detected Real Level: " + EnumChatFormatting.WHITE + realStr));

        // Quick Actions row
        ChatComponentText actionRow = new ChatComponentText(EnumChatFormatting.GRAY + "Quick Actions: ");
        ChatComponentText resetBtn = new ChatComponentText(EnumChatFormatting.RED + "[RESET / REAL LEVEL]");
        ChatStyle resetStyle = new ChatStyle();
        resetStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.RED + "Click to disable spoofing and restore real level")));
        resetStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nickhider nlvl reset"));
        resetBtn.setChatStyle(resetStyle);
        actionRow.appendSibling(resetBtn);
        mc.thePlayer.addChatMessage(actionRow);

        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GRAY + "-----------------------------------------------------"));
    }

    // ─── Hypixel Bedwars Level Commands ──────────────────────────────────────

    public static void handleBwLvl(String args) {
        if (args.isEmpty() || args.equalsIgnoreCase("status")) {
            printBwLvlStatus();
            return;
        }

        String[] parts = args.split("\\s+", 2);
        String sub = parts[0].toLowerCase();
        String subArg = parts.length > 1 ? parts[1].trim() : "";

        switch (sub) {
            case "stars":
            case "level":
            case "lvl":
                if (subArg.isEmpty()) {
                    printBwStarsHelp();
                } else {
                    try {
                        int lvl = Integer.parseInt(subArg);
                        if (lvl < 0 || lvl > 10000) {
                            NickHiderMod.sendMessage("§cError: Bedwars level must be between 0 and 10000!");
                            printBwStarsHelp();
                            return;
                        }
                        dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setFakeStars(lvl);
                        dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setEnabled(true);
                        NickHiderMod.sendMessage("§aBedwars stars set to: §f" + lvl + " §7(Preview: " + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag() + "§7)");
                    } catch (NumberFormatException e) {
                        NickHiderMod.sendMessage("§cError: Invalid number \"" + subArg + "\". Bedwars level must be an integer between 0 and 10000.");
                        printBwStarsHelp();
                    }
                }
                break;

            case "color":
            case "colors":
            case "prestige":
                if (subArg.isEmpty()) {
                    printBwColorMenu();
                } else if (subArg.equalsIgnoreCase("auto") || subArg.equalsIgnoreCase("reset")) {
                    dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setColorPresetLevel(-1);
                    dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setEnabled(true);
                    NickHiderMod.sendMessage("§aBedwars color scheme reset to §fAuto §7(follows fake stars: " + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag() + "§7)");
                } else {
                    dev.edtrick.nickhider.bedwars.BedwarsPrestige p = dev.edtrick.nickhider.bedwars.BedwarsPrestige.find(subArg);
                    if (p == null) {
                        NickHiderMod.sendMessage("§cError: Unknown Bedwars prestige \"" + subArg + "\". Click below or type §e/nickhider bwlvl color §cto view all 101 prestiges.");
                        printBwColorMenu();
                        return;
                    }
                    dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setColorPresetLevel(p.level);
                    dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setEnabled(true);
                    NickHiderMod.sendMessage("§aBedwars color scheme set to: §f" + p.name + " §7(Tier " + p.level + ") §7(Preview: " + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag() + "§7)");
                }
                break;

            case "icon":
            case "star":
                if (subArg.isEmpty()) {
                    printBwIconMenu();
                } else if (subArg.equalsIgnoreCase("auto") || subArg.equalsIgnoreCase("reset")) {
                    dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setCustomIcon(null);
                    dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setEnabled(true);
                    NickHiderMod.sendMessage("§aBedwars star icon reset to §fAuto §7(follows prestige default icon: " + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag() + "§7)");
                } else {
                    String icon = dev.edtrick.nickhider.bedwars.BedwarsPrestige.resolveIcon(subArg);
                    if (icon == null) {
                        NickHiderMod.sendMessage("§cError: Unknown star icon \"" + subArg + "\". Choose one from: ✫, ✪, ⚝, ✥, ✭ or a prestige preset.");
                        printBwIconMenu();
                        return;
                    }
                    dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setCustomIcon(icon);
                    dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setEnabled(true);
                    NickHiderMod.sendMessage("§aBedwars star icon set to: §f" + icon + " §7(Preview: " + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag() + "§7)");
                }
                break;

            case "toggle":
                boolean newState = !dev.edtrick.nickhider.bedwars.BedwarsLevelManager.isEnabled();
                dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setEnabled(newState);
                NickHiderMod.sendMessage(newState
                        ? "§aBedwars level spoofing §2ENABLED §7(Tag: " + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag() + "§7)"
                        : "§eBedwars level spoofing §cDISABLED§e.");
                break;

            case "on":
            case "enable":
                dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setEnabled(true);
                NickHiderMod.sendMessage("§aBedwars level spoofing §2ENABLED §7(Tag: " + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag() + "§7)");
                break;

            case "off":
            case "disable":
                dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setEnabled(false);
                NickHiderMod.sendMessage("§eBedwars level spoofing §cDISABLED§e.");
                break;

            case "reset":
                dev.edtrick.nickhider.bedwars.BedwarsLevelManager.setAll(false, 1402, -1, null);
                NickHiderMod.sendMessage("§aBedwars level spoofing reset to defaults (disabled, auto color, auto icon).");
                break;

            default:
                NickHiderMod.sendMessage("§cUnknown bwlvl option: §f" + sub);
                printBwLvlStatus();
                break;
        }
    }

    public static void printBwStarsHelp() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        mc.thePlayer.addChatMessage(new ChatComponentText(
                NickHiderMod.rainbowPrefix() + " " + EnumChatFormatting.RED + "Bedwars Stars Configuration:"
        ));
        mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GRAY + "You must specify a star level between " + EnumChatFormatting.WHITE + "0"
                + EnumChatFormatting.GRAY + " and " + EnumChatFormatting.WHITE + "10000" + EnumChatFormatting.GRAY + "."
        ));

        ChatComponentText example = new ChatComponentText(
                EnumChatFormatting.YELLOW + "▶ Click here to enter stars: " + EnumChatFormatting.GREEN + "/nickhider bwlvl stars <LVL>"
        );
        ChatStyle style = new ChatStyle();
        style.setChatHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText(EnumChatFormatting.YELLOW + "Click to suggest command in chat")
        ));
        style.setChatClickEvent(new ClickEvent(
                ClickEvent.Action.SUGGEST_COMMAND,
                "/nickhider bwlvl stars "
        ));
        example.setChatStyle(style);
        mc.thePlayer.addChatMessage(example);
    }

    public static void printBwColorMenu() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        mc.thePlayer.addChatMessage(new ChatComponentText(
                NickHiderMod.rainbowPrefix() + " " + EnumChatFormatting.GOLD + "Select Bedwars Prestige Color Scheme (5 per line):"
        ));

        // Auto option
        ChatComponentText autoBtn = new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GREEN + "Auto/Default" + EnumChatFormatting.GRAY + "]");
        ChatStyle autoStyle = new ChatStyle();
        autoStyle.setChatHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText(EnumChatFormatting.AQUA + "Follows your fake stars level automatically\n" + EnumChatFormatting.YELLOW + "▶ Click to select Auto")
        ));
        autoStyle.setChatClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/nickhider bwlvl color auto"
        ));
        autoBtn.setChatStyle(autoStyle);
        mc.thePlayer.addChatMessage(autoBtn);

        List<dev.edtrick.nickhider.bedwars.BedwarsPrestige> prestiges = dev.edtrick.nickhider.bedwars.BedwarsPrestige.getAllPrestiges();
        ChatComponentText line = null;
        int countInLine = 0;

        for (int i = 0; i < prestiges.size(); i++) {
            dev.edtrick.nickhider.bedwars.BedwarsPrestige p = prestiges.get(i);
            if (countInLine == 0) {
                line = new ChatComponentText("");
            } else {
                line.appendSibling(new ChatComponentText(" "));
            }

            ChatComponentText btn = new ChatComponentText(p.sample);
            ChatStyle btnStyle = new ChatStyle();
            btnStyle.setChatHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ChatComponentText(
                            EnumChatFormatting.GOLD + "Prestige: " + EnumChatFormatting.WHITE + p.name + "\n"
                            + EnumChatFormatting.YELLOW + "Tier Level: " + EnumChatFormatting.WHITE + p.level + "\n"
                            + EnumChatFormatting.AQUA + "Sample: " + p.sample + "\n\n"
                            + EnumChatFormatting.GREEN + "▶ Click to apply this color scheme"
                    )
            ));
            btnStyle.setChatClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/nickhider bwlvl color " + p.level
            ));
            btn.setChatStyle(btnStyle);
            line.appendSibling(btn);
            countInLine++;

            if (countInLine == 5 || i == prestiges.size() - 1) {
                mc.thePlayer.addChatMessage(line);
                countInLine = 0;
            }
        }
    }

    public static void printBwIconMenu() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        mc.thePlayer.addChatMessage(new ChatComponentText(
                NickHiderMod.rainbowPrefix() + " " + EnumChatFormatting.GOLD + "Select Bedwars Star Icon (5 per line):"
        ));

        // Auto option
        ChatComponentText autoBtn = new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GREEN + "Auto/Default" + EnumChatFormatting.GRAY + "]");
        ChatStyle autoStyle = new ChatStyle();
        autoStyle.setChatHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText(EnumChatFormatting.AQUA + "Follows the prestige default star icon\n" + EnumChatFormatting.YELLOW + "▶ Click to select Auto")
        ));
        autoStyle.setChatClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/nickhider bwlvl icon auto"
        ));
        autoBtn.setChatStyle(autoStyle);
        mc.thePlayer.addChatMessage(autoBtn);

        // 5 primary star symbols: ✫, ✪, ⚝, ✥, ✭
        String[] icons = new String[]{"✫", "✪", "⚝", "✥", "✭"};
        String[] names = new String[]{"Classic (0-1000)", "Prime (1100-1900)", "Cosmic (2000-2900)", "Crossed (3000-3900)", "Prestige (4000+)"};
        ChatComponentText symbolLine = new ChatComponentText("");
        for (int i = 0; i < icons.length; i++) {
            if (i > 0) symbolLine.appendSibling(new ChatComponentText("  "));
            String sym = icons[i];
            ChatComponentText btn = new ChatComponentText(EnumChatFormatting.YELLOW + "[" + EnumChatFormatting.WHITE + " " + sym + " " + EnumChatFormatting.YELLOW + "]");
            ChatStyle s = new ChatStyle();
            s.setChatHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ChatComponentText(
                            EnumChatFormatting.GOLD + "Star Icon: " + EnumChatFormatting.WHITE + sym + "\n"
                            + EnumChatFormatting.GRAY + "Style: " + names[i] + "\n\n"
                            + EnumChatFormatting.GREEN + "▶ Click to apply this icon"
                    )
            ));
            s.setChatClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/nickhider bwlvl icon " + sym
            ));
            btn.setChatStyle(s);
            symbolLine.appendSibling(btn);
        }
        mc.thePlayer.addChatMessage(symbolLine);

        // Also show major milestone prestige presets (5 per line)
        int[] milestoneLevels = new int[]{0, 500, 1000, 1100, 1500, 2000, 2500, 3000, 3500, 4000, 5000, 6000, 7000, 8000, 9000};
        ChatComponentText line = null;
        int countInLine = 0;
        for (int i = 0; i < milestoneLevels.length; i++) {
            int lvl = milestoneLevels[i];
            dev.edtrick.nickhider.bedwars.BedwarsPrestige p = dev.edtrick.nickhider.bedwars.BedwarsPrestige.getByLevel(lvl);
            if (countInLine == 0) {
                line = new ChatComponentText("");
            } else {
                line.appendSibling(new ChatComponentText(" "));
            }

            ChatComponentText btn = new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.WHITE + p.level + p.defaultIcon + EnumChatFormatting.GRAY + "]");
            ChatStyle s = new ChatStyle();
            s.setChatHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ChatComponentText(
                            EnumChatFormatting.GOLD + "Prestige Preset: " + EnumChatFormatting.WHITE + p.name + "\n"
                            + EnumChatFormatting.GRAY + "Icon: " + EnumChatFormatting.WHITE + p.defaultIcon + "\n\n"
                            + EnumChatFormatting.GREEN + "▶ Click to apply this icon preset"
                    )
            ));
            s.setChatClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/nickhider bwlvl icon " + p.level
            ));
            btn.setChatStyle(s);
            line.appendSibling(btn);
            countInLine++;

            if (countInLine == 5 || i == milestoneLevels.length - 1) {
                mc.thePlayer.addChatMessage(line);
                countInLine = 0;
            }
        }
    }

    public static void printBwLvlStatus() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        boolean enabled = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.isEnabled();
        int stars = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFakeStars();
        int colorTier = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getColorPresetLevel();
        String customIcon = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getCustomIcon();
        String formattedTag = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFormattedTag();

        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GRAY + "-----------------------------------------------------"));
        mc.thePlayer.addChatMessage(new ChatComponentText(NickHiderMod.rainbowPrefix() + " " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + "Bedwars Level Spoofing"));

        // Status row
        ChatComponentText statusRow = new ChatComponentText(EnumChatFormatting.GRAY + "Status: " + (enabled ? EnumChatFormatting.GREEN + "ENABLED " : EnumChatFormatting.RED + "DISABLED "));
        ChatComponentText toggleBtn = new ChatComponentText(EnumChatFormatting.YELLOW + "[TOGGLE]");
        ChatStyle toggleStyle = new ChatStyle();
        toggleStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Click to toggle ON/OFF")));
        toggleStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nickhider bwlvl toggle"));
        toggleBtn.setChatStyle(toggleStyle);
        statusRow.appendSibling(toggleBtn);
        mc.thePlayer.addChatMessage(statusRow);

        // Fake stars row
        ChatComponentText starsRow = new ChatComponentText(EnumChatFormatting.GRAY + "Fake Stars: " + EnumChatFormatting.WHITE + stars + " ");
        ChatComponentText editStarsBtn = new ChatComponentText(EnumChatFormatting.AQUA + "[EDIT]");
        ChatStyle editStarsStyle = new ChatStyle();
        editStarsStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.AQUA + "Click to type star level")));
        editStarsStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/nickhider bwlvl stars "));
        editStarsBtn.setChatStyle(editStarsStyle);
        starsRow.appendSibling(editStarsBtn);
        mc.thePlayer.addChatMessage(starsRow);

        // Color preset row
        String colorDesc = (colorTier >= 0) ? (dev.edtrick.nickhider.bedwars.BedwarsPrestige.getByLevel(colorTier).name + " (" + colorTier + ")") : "Auto";
        ChatComponentText colorRow = new ChatComponentText(EnumChatFormatting.GRAY + "Color Scheme: " + EnumChatFormatting.WHITE + colorDesc + " ");
        ChatComponentText editColorBtn = new ChatComponentText(EnumChatFormatting.GOLD + "[CHANGE]");
        ChatStyle editColorStyle = new ChatStyle();
        editColorStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GOLD + "Click to browse color schemes")));
        editColorStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nickhider bwlvl color"));
        editColorBtn.setChatStyle(editColorStyle);
        colorRow.appendSibling(editColorBtn);
        mc.thePlayer.addChatMessage(colorRow);

        // Star icon row
        String iconDesc = (customIcon != null) ? customIcon : ("Auto (" + dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getEffectiveIcon() + ")");
        ChatComponentText iconRow = new ChatComponentText(EnumChatFormatting.GRAY + "Star Icon: " + EnumChatFormatting.WHITE + iconDesc + " ");
        ChatComponentText editIconBtn = new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "[CHANGE]");
        ChatStyle editIconStyle = new ChatStyle();
        editIconStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "Click to browse star icons")));
        editIconStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nickhider bwlvl icon"));
        editIconBtn.setChatStyle(editIconStyle);
        iconRow.appendSibling(editIconBtn);
        mc.thePlayer.addChatMessage(iconRow);

        // Preview row
        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "Tag Preview: " + formattedTag));

        // Action row
        ChatComponentText actionRow = new ChatComponentText(EnumChatFormatting.GRAY + "Quick Actions: ");
        ChatComponentText resetBtn = new ChatComponentText(EnumChatFormatting.RED + "[RESET ALL]");
        ChatStyle resetStyle = new ChatStyle();
        resetStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.RED + "Click to reset Bedwars spoofing to defaults")));
        resetStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nickhider bwlvl reset"));
        resetBtn.setChatStyle(resetStyle);
        actionRow.appendSibling(resetBtn);
        mc.thePlayer.addChatMessage(actionRow);

        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GRAY + "-----------------------------------------------------"));
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

        sendCommandHelp("7", "/nickhider bwlvl", "/nickhider bwlvl",
                "Opens the Hypixel Bedwars level spoofing control panel\nwith clickable buttons for stars, colors and icons.");

        sendCommandHelp("8", "/nickhider bwlvl stars <LVL>", "/nickhider bwlvl stars ",
                "Sets your spoofed Bedwars stars (0 - 10000).\nLeave blank to show stars configuration help.");

        sendCommandHelp("9", "/nickhider bwlvl color", "/nickhider bwlvl color",
                "Opens a clickable chat menu with all 101 prestige color\nschemes formatted 5 per line to select from.");

        sendCommandHelp("10", "/nickhider bwlvl icon", "/nickhider bwlvl icon",
                "Opens a clickable chat menu to choose your star icon\n(✫, ✪, ⚝, ✥, ✭) or prestige preset.");

        sendCommandHelp("11", "/nickhider servernick [NICK]", "/nickhider servernick ",
                "Shows or sets your server-side nick (/nick on Hypixel).\nAuto-detected when chatting or via /nick notifications.");

        sendCommandHelp("12", "/nickhider replay [NICK]", "/nickhider replay",
                "Shows clickable list of replay players to select who you are,\nor sets your replay player name.");

        sendCommandHelp("13", "/nickhider level <LVL>", "/nickhider level ",
                "Sets your fake Hypixel Network Level (0 - 10000).\nSpoofs nametags, profile menu, scoreboard and XP bar.\nType \"reset\" to restore your real level.");
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
        if (lower.startsWith("/bw") && !leftOfCursor.contains(" ")) {
            if ("/bwlvl".startsWith(lower)) {
                return new String[]{"/bwlvl"};
            }
        }
        if (lower.startsWith("/nl") && !leftOfCursor.contains(" ")) {
            if ("/nlvl".startsWith(lower)) {
                return new String[]{"/nlvl"};
            }
        }

        // Autocomplete subcommands: "/nickhider ", "/nick ", "/bwlvl ", or "/nlvl "
        if (lower.startsWith("/nickhider ") || lower.startsWith("/nick ") || lower.startsWith("/bwlvl ")
                || lower.startsWith("/bedwars ") || lower.startsWith("/nlvl ") || lower.startsWith("/networklevel ")) {
            int prefixLen = "/nickhider ".length();
            if (lower.startsWith("/nick ")) prefixLen = "/nick ".length();
            else if (lower.startsWith("/bwlvl ")) prefixLen = "/bwlvl ".length();
            else if (lower.startsWith("/bedwars ")) prefixLen = "/bedwars ".length();
            else if (lower.startsWith("/nlvl ")) prefixLen = "/nlvl ".length();
            else if (lower.startsWith("/networklevel ")) prefixLen = "/networklevel ".length();

            String afterCmd = leftOfCursor.substring(prefixLen);

            String[] subcommands = new String[]{"nick", "skin", "unnick", "realskin", "bwlvl", "level", "nlvl", "servernick", "replay"};
            if (lower.startsWith("/bwlvl ") || lower.startsWith("/bedwars ")) {
                subcommands = new String[]{"stars", "color", "icon", "toggle", "on", "off", "reset", "status"};
            } else if (lower.startsWith("/nlvl ") || lower.startsWith("/networklevel ")) {
                subcommands = new String[]{"reset", "toggle", "on", "off", "status"};
            }

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
                } else if (sub.equals("bwlvl") || sub.equals("bedwars")) {
                    String[] bwSubcommands = new String[]{"stars", "color", "icon", "toggle", "on", "off", "reset", "status"};
                    if (!parts[1].contains(" ")) {
                        List<String> list = new ArrayList<>();
                        for (String bwSub : bwSubcommands) {
                            if (bwSub.startsWith(currentArg)) {
                                list.add(bwSub);
                            }
                        }
                        return list.toArray(new String[0]);
                    } else {
                        String[] bwParts = parts[1].split("\\s+", 2);
                        String bwAction = bwParts[0].toLowerCase();
                        String bwArg = bwParts.length > 1 ? bwParts[1].toLowerCase() : "";

                        if (bwAction.equals("color")) {
                            List<String> list = new ArrayList<>();
                            list.add("auto");
                            for (dev.edtrick.nickhider.bedwars.BedwarsPrestige p : dev.edtrick.nickhider.bedwars.BedwarsPrestige.getAllPrestiges()) {
                                String lvlStr = String.valueOf(p.level);
                                if (lvlStr.startsWith(bwArg)) list.add(lvlStr);
                                if (p.name.toLowerCase().startsWith(bwArg)) list.add(p.name);
                            }
                            return list.toArray(new String[0]);
                        } else if (bwAction.equals("icon")) {
                            List<String> list = new ArrayList<>();
                            String[] icons = new String[]{"auto", "✫", "✪", "⚝", "✥", "✭", "0", "1000", "2000", "3000", "4000"};
                            for (String ic : icons) {
                                if (ic.toLowerCase().startsWith(bwArg)) list.add(ic);
                            }
                            return list.toArray(new String[0]);
                        }
                    }
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

            // Also check all player entities in the world (e.g. EntityOtherPlayerMP in replays)
            if (mc.theWorld != null && mc.theWorld.playerEntities != null) {
                for (net.minecraft.entity.player.EntityPlayer p : mc.theWorld.playerEntities) {
                    if (p != null && NickHiderMod.isLocalPlayerEntity(p)) {
                        if (mc.getNetHandler() != null && p.getUniqueID() != null) {
                            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(p.getUniqueID());
                            if (info != null) {
                                applySkinToInfo(info);
                            }
                        }
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
            ResourceLocation fakeSkin = NickHiderMod.getFakeSkinLocation();
            if (fakeSkin != null) {
                accessor.setLocationSkin(fakeSkin);
            }
            accessor.setPlayerTexturesLoaded(false);
            accessor.invokeLoadPlayerTextures();
        } catch (Throwable ignored) {}
    }

    private static boolean isLocalInfo(NetworkPlayerInfo info) {
        if (info == null || info.getGameProfile() == null) return false;
        return NickHiderMod.isLocalPlayerProfile(info.getGameProfile());
    }
}
