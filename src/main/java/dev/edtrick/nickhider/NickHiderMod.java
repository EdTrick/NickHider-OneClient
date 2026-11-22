package dev.edtrick.nickhider;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import dev.edtrick.nickhider.config.OneConfigCompat;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickHiderMod implements ClientModInitializer {

    public static final String MOD_ID = "nickhider";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    // Current fake nick (null = use real name)
    private static String fakeNick = null;

    // Current fake skin textures: value + signature (null = use real skin)
    private static String fakeSkinValue = null;
    private static String fakeSkinSignature = null;
    private static String fakeSkinOwner = null;

    // Render cache for the fake skin
    private static ResourceLocation fakeSkinLocation = null;
    private static String fakeSkinModel = null;

    // Real account skin property captured from the server
    private static Property realSkinProperty = null;
    private static String realSkinUrl = null;
    private static boolean realSkinCaptured = false;

    private static Path configFile;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[NickHider] Initializing NickHider mod by EdTrick (1.8.9)");

        Path configDir = Minecraft.getMinecraft().mcDataDir.toPath()
                .resolve("config").resolve("nickhider");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("[NickHider] Failed to create config directory", e);
        }
        configFile = configDir.resolve("nickhider.properties");
        loadConfig();

        OneConfigCompat.init();

        LOGGER.info("[NickHider] Mod ready. fakeNick={}, fakeSkinOwner={}",
                fakeNick, fakeSkinOwner);
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public static String getFakeNick() { return fakeNick; }

    public static void setFakeNick(String nick) {
        fakeNick = nick;
        saveConfig();
        OneConfigCompat.sync();
    }

    public static void clearFakeNick() {
        fakeNick = null;
        saveConfig();
        OneConfigCompat.sync();
    }

    public static String getFakeSkinValue()     { return fakeSkinValue; }
    public static String getFakeSkinSignature() { return fakeSkinSignature; }
    public static String getFakeSkinOwner()     { return fakeSkinOwner; }

    public static ResourceLocation getFakeSkinLocation() {
        if (fakeSkinLocation == null && fakeSkinValue != null) {
            reloadFakeSkinLocation();
        }
        return fakeSkinLocation;
    }

    public static String getFakeSkinModel() {
        if (fakeSkinModel == null && fakeSkinValue != null) {
            reloadFakeSkinLocation();
        }
        return fakeSkinModel;
    }

    public static void reloadFakeSkinLocation() {
        if (fakeSkinValue == null) {
            fakeSkinLocation = null;
            fakeSkinModel = null;
            return;
        }

        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getSkinManager() == null) return;

            byte[] decoded = Base64.getDecoder().decode(fakeSkinValue);
            String json = new String(decoded, StandardCharsets.UTF_8);

            String url = extractJsonString(json, "url");
            if (url == null) return;

            String model = (json.contains("\"model\"") && json.contains("\"slim\"")) ? "slim" : "default";

            Map<String, String> metadata = new HashMap<String, String>();
            if ("slim".equals(model)) {
                metadata.put("model", "slim");
            }

            MinecraftProfileTexture texture = new MinecraftProfileTexture(url, metadata);
            fakeSkinLocation = mc.getSkinManager().loadSkin(texture, MinecraftProfileTexture.Type.SKIN);
            fakeSkinModel = model;
        } catch (Exception e) {
            LOGGER.error("[NickHider] Failed to load fake skin texture into TextureManager: {}", e.getMessage());
        }
    }

    public static void setFakeSkin(String value, String signature, String owner) {
        fakeSkinValue = value;
        fakeSkinSignature = signature;
        fakeSkinOwner = owner;
        reloadFakeSkinLocation();
        saveConfig();
        OneConfigCompat.sync();
    }

    public static void clearFakeSkin() {
        fakeSkinValue = null;
        fakeSkinSignature = null;
        fakeSkinOwner = null;
        fakeSkinLocation = null;
        fakeSkinModel = null;
        saveConfig();
        OneConfigCompat.sync();
    }

    public static Property getRealSkinProperty() { return realSkinProperty; }

    public static void setRealSkinProperty(Property property) {
        realSkinProperty = property;
        realSkinCaptured = true;
        if (property != null && property.getValue() != null) {
            try {
                byte[] decoded = Base64.getDecoder().decode(property.getValue());
                String json = new String(decoded, StandardCharsets.UTF_8);
                String url = extractJsonString(json, "url");
                if (url != null && !url.isEmpty()) {
                    realSkinUrl = url;
                }
            } catch (Exception ignored) {}
        }
        saveConfig();
    }

    public static String getRealSkinUrl() { return realSkinUrl; }

    public static boolean isRealSkinCaptured() { return realSkinCaptured; }

    // ─── Config persistence ──────────────────────────────────────────────────

    public static void saveConfig() {
        if (configFile == null) return;
        Properties prop = new Properties();
        if (fakeNick != null) prop.setProperty("fakeNick", fakeNick);
        if (fakeSkinValue != null) prop.setProperty("fakeSkinValue", fakeSkinValue);
        if (fakeSkinSignature != null) prop.setProperty("fakeSkinSignature", fakeSkinSignature);
        if (fakeSkinOwner != null) prop.setProperty("fakeSkinOwner", fakeSkinOwner);
        if (detectedChatNick != null) prop.setProperty("serverNick", detectedChatNick);
        if (realSkinUrl != null) prop.setProperty("realSkinUrl", realSkinUrl);
        if (realSkinProperty != null && realSkinProperty.getValue() != null) prop.setProperty("realSkinValue", realSkinProperty.getValue());

        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.getSession() != null && mc.getSession().getUsername() != null) {
            prop.setProperty("lastAccountIgn", mc.getSession().getUsername());
        }

        prop.setProperty("enableBwLevel", String.valueOf(dev.edtrick.nickhider.bedwars.BedwarsLevelManager.isEnabled()));
        prop.setProperty("bwStars", String.valueOf(dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getFakeStars()));
        prop.setProperty("bwColorPreset", String.valueOf(dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getColorPresetLevel()));
        if (dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getCustomIcon() != null) {
            prop.setProperty("bwIconPreset", dev.edtrick.nickhider.bedwars.BedwarsLevelManager.getCustomIcon());
        }

        prop.setProperty("enableNetworkLevel", String.valueOf(dev.edtrick.nickhider.network.NetworkLevelManager.isEnabled()));
        prop.setProperty("networkLevel", String.valueOf(dev.edtrick.nickhider.network.NetworkLevelManager.getFakeLevel()));
        prop.setProperty("detectedRealNetworkLevel", String.valueOf(dev.edtrick.nickhider.network.NetworkLevelManager.getDetectedRealLevel()));

        try (OutputStream os = Files.newOutputStream(configFile)) {
            prop.store(os, "NickHider Mod Configuration");
        } catch (IOException e) {
            LOGGER.error("[NickHider] Failed to save config", e);
        }
    }

    public static void loadConfig() {
        if (configFile == null || !Files.exists(configFile)) return;
        Properties prop = new Properties();
        try (InputStream is = Files.newInputStream(configFile)) {
            prop.load(is);
            fakeNick          = prop.getProperty("fakeNick", null);
            fakeSkinValue     = prop.getProperty("fakeSkinValue", null);
            fakeSkinSignature = prop.getProperty("fakeSkinSignature", null);
            fakeSkinOwner     = prop.getProperty("fakeSkinOwner", null);
            String lastAccountIgn = prop.getProperty("lastAccountIgn", null);
            Minecraft mc = Minecraft.getMinecraft();
            String currentAccountIgn = (mc != null && mc.getSession() != null) ? mc.getSession().getUsername() : null;
            if (lastAccountIgn != null && currentAccountIgn != null && !lastAccountIgn.equalsIgnoreCase(currentAccountIgn)) {
                // Different Minecraft account than last time! Reset server-side nick
                detectedChatNick = null;
            } else {
                detectedChatNick = prop.getProperty("serverNick", null);
            }
            realSkinUrl       = prop.getProperty("realSkinUrl", null);
            String savedRealSkinValue = prop.getProperty("realSkinValue", null);
            if (savedRealSkinValue != null && !savedRealSkinValue.isEmpty()) {
                realSkinProperty = new Property("textures", savedRealSkinValue, "");
                realSkinCaptured = true;
            }
            if (realSkinUrl != null) {
                realSkinCaptured = true;
            }

            String enBw = prop.getProperty("enableBwLevel", "false");
            boolean en = Boolean.parseBoolean(enBw);
            String starsStr = prop.getProperty("bwStars", "1402");
            int st = 1402;
            try {
                st = Integer.parseInt(starsStr);
            } catch (NumberFormatException ignored) {}
            String colorStr = prop.getProperty("bwColorPreset", "-1");
            int col = -1;
            try {
                col = Integer.parseInt(colorStr);
            } catch (NumberFormatException ignored) {}
            String ic = prop.getProperty("bwIconPreset", null);
            dev.edtrick.nickhider.bedwars.BedwarsLevelManager.loadValues(en, st, col, ic);

            String enNl = prop.getProperty("enableNetworkLevel", "false");
            boolean enN = Boolean.parseBoolean(enNl);
            String lvlStr = prop.getProperty("networkLevel", "250");
            int nl = 250;
            try {
                nl = Integer.parseInt(lvlStr);
            } catch (NumberFormatException ignored) {}
            dev.edtrick.nickhider.network.NetworkLevelManager.loadValues(enN, nl);

            String detectedRealNlvl = prop.getProperty("detectedRealNetworkLevel", "-1");
            try {
                int realNl = Integer.parseInt(detectedRealNlvl);
                if (realNl > 0) {
                    dev.edtrick.nickhider.network.NetworkLevelManager.setDetectedRealLevel(realNl);
                }
            } catch (NumberFormatException ignored) {}
        } catch (IOException e) {
            LOGGER.error("[NickHider] Failed to load config", e);
        }
    }

    // ─── String Replacement Engine ───────────────────────────────────────────

    private static String detectedChatNick = null;
    private static String lastSentChatMessage = null;
    private static long lastSentChatTime = 0;

    public static void recordSentChatMessage(String message) {
        lastSentChatMessage = message;
        lastSentChatTime = System.currentTimeMillis();
    }

    public static String getLastSentChatMessage() {
        return lastSentChatMessage;
    }

    public static long getLastSentChatTime() {
        return lastSentChatTime;
    }

    public static void setDetectedChatNick(String nick) {
        if (nick != null && nick.equalsIgnoreCase(fakeNick)) return;
        if (nick != null && nick.isEmpty()) nick = null;
        boolean changed = (detectedChatNick == null) ? (nick != null) : !detectedChatNick.equals(nick);
        detectedChatNick = nick;
        if (changed) {
            saveConfig();
            OneConfigCompat.sync();
            if (nick != null) {
                LOGGER.info("[NickHider] Server-side nickname updated: '{}'", nick);
            }
        }
        try {
            dev.edtrick.nickhider.command.NickHiderCommand.invalidateSkinCache();
        } catch (Throwable ignored) {}
    }

    public static String getDetectedChatNick() {
        return detectedChatNick;
    }

    public static java.util.List<String> getNamesToReplace() {
        java.util.List<String> names = new java.util.ArrayList<String>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return names;

        // 1. Mojang session username (real account IGN)
        if (mc.getSession() != null) {
            String realName = mc.getSession().getUsername();
            if (realName != null && !realName.isEmpty() && !names.contains(realName)) {
                names.add(realName);
            }
        }

        // 2. EntityPlayerSP name
        if (mc.thePlayer != null) {
            String playerName = mc.thePlayer.getName();
            if (playerName != null && !playerName.isEmpty() && !names.contains(playerName)) {
                names.add(playerName);
            }
        }

        // 3. Tablist entry for local player's UUID (catches server-side /nick on Hypixel/Minemen)
        if (mc.thePlayer != null && mc.getNetHandler() != null && mc.thePlayer.getUniqueID() != null) {
            try {
                net.minecraft.client.network.NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                if (info != null && info.getGameProfile() != null) {
                    String serverNick = info.getGameProfile().getName();
                    if (serverNick != null && !serverNick.isEmpty() && !names.contains(serverNick)) {
                        names.add(serverNick);
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 4. Session profile ID in tablist
        if (mc.getSession() != null && mc.getSession().getProfile() != null && mc.getNetHandler() != null) {
            try {
                if (mc.getSession().getProfile().getId() != null) {
                    net.minecraft.client.network.NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.getSession().getProfile().getId());
                    if (info != null && info.getGameProfile() != null) {
                        String sName = info.getGameProfile().getName();
                        if (sName != null && !sName.isEmpty() && !names.contains(sName)) {
                            names.add(sName);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 5. Detected chat nick (from server messages)
        if (detectedChatNick != null && !detectedChatNick.isEmpty() && !names.contains(detectedChatNick)) {
            names.add(detectedChatNick);
        }

        return names;
    }

    public static boolean isLocalPlayerProfile(com.mojang.authlib.GameProfile profile) {
        if (profile == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return false;

        // 1. Check UUID
        java.util.UUID localUuid = null;
        if (mc.thePlayer != null) {
            localUuid = mc.thePlayer.getUniqueID();
        } else if (mc.getSession() != null && mc.getSession().getProfile() != null) {
            localUuid = mc.getSession().getProfile().getId();
        }
        if (localUuid != null && localUuid.equals(profile.getId())) {
            return true;
        }

        // 2. Check Name against all names to replace (real IGN, thePlayer name, server nick, chat nick)
        String profileName = profile.getName();
        if (profileName != null && isLocalPlayerName(profileName)) {
            return true;
        }

        // 3. Check textures property against realSkinProperty or realSkinUrl if available
        if (checkSkinPropertyMatch(profile.getProperties())) {
            return true;
        }

        return false;
    }

    public static boolean checkSkinPropertyMatch(com.mojang.authlib.properties.PropertyMap properties) {
        if (properties == null || !properties.containsKey("textures")) return false;
        for (Property p : properties.get("textures")) {
            if (p == null || p.getValue() == null) continue;
            if (realSkinProperty != null && p.getValue().equals(realSkinProperty.getValue())) {
                return true;
            }
            if (realSkinUrl != null) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(p.getValue());
                    String json = new String(decoded, StandardCharsets.UTF_8);
                    if (json.contains(realSkinUrl)) {
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    public static boolean matchesRealSkin(net.minecraft.client.entity.AbstractClientPlayer player) {
        if (player == null) return false;
        if (realSkinProperty == null && realSkinUrl == null) return false;

        if (player.getGameProfile() != null && player.getGameProfile().getProperties() != null) {
            if (checkSkinPropertyMatch(player.getGameProfile().getProperties())) {
                return true;
            }
        }

        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.getNetHandler() != null && player.getUniqueID() != null) {
                net.minecraft.client.network.NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
                if (info != null && info.getGameProfile() != null && info.getGameProfile().getProperties() != null) {
                    if (checkSkinPropertyMatch(info.getGameProfile().getProperties())) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static boolean isLocalPlayerName(String name) {
        if (name == null || name.isEmpty()) return false;
        String clean = stripColorCodes(name).trim();
        if (clean.isEmpty()) return false;
        for (String n : getNamesToReplace()) {
            if (n != null && (n.equalsIgnoreCase(clean) || n.equalsIgnoreCase(name))) return true;
        }
        return false;
    }

    public static boolean isLocalPlayerEntity(net.minecraft.entity.Entity entity) {
        if (entity == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return false;
        if (entity == mc.thePlayer) return true;
        if (entity instanceof net.minecraft.entity.player.EntityPlayer) {
            net.minecraft.entity.player.EntityPlayer p = (net.minecraft.entity.player.EntityPlayer) entity;
            String name = p.getName();
            if (isLocalPlayerName(name)) return true;
            if (p.hasCustomName() && isLocalPlayerName(p.getCustomNameTag())) return true;
            if (isLocalPlayerProfile(p.getGameProfile())) return true;
            if (p instanceof net.minecraft.client.entity.AbstractClientPlayer) {
                if (matchesRealSkin((net.minecraft.client.entity.AbstractClientPlayer) p)) return true;
            }
        }
        return false;
    }

    public static boolean containsLocalPlayerName(String text) {
        if (text == null) return false;
        String stripped = stripColorCodes(text).toLowerCase();
        for (String name : getNamesToReplace()) {
            if (name != null && !name.isEmpty() && stripped.contains(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static String getActiveFormattingAt(String text, int index) {
        if (text == null || index <= 0) return "";
        int bound = Math.min(index, text.length());
        String sub = text.substring(0, bound);
        StringBuilder currentFormat = new StringBuilder();

        for (int i = 0; i < sub.length() - 1; i++) {
            if (sub.charAt(i) == '§') {
                char code = Character.toLowerCase(sub.charAt(i + 1));
                if ("0123456789abcdef".indexOf(code) >= 0) {
                    currentFormat.setLength(0);
                    currentFormat.append('§').append(code);
                } else if ("klmno".indexOf(code) >= 0) {
                    currentFormat.append('§').append(code);
                } else if (code == 'r') {
                    currentFormat.setLength(0);
                }
                i++;
            }
        }
        return currentFormat.toString();
    }

    public static List<String> extractCharFormats(String matched, String activeBefore) {
        List<String> formats = new ArrayList<String>();
        StringBuilder current = new StringBuilder(activeBefore != null ? activeBefore : "");
        for (int i = 0; i < matched.length(); i++) {
            char c = matched.charAt(i);
            if (c == '§' && i + 1 < matched.length()) {
                char code = Character.toLowerCase(matched.charAt(i + 1));
                if ("0123456789abcdef".indexOf(code) >= 0) {
                    current.setLength(0);
                    current.append('§').append(code);
                } else if ("klmno".indexOf(code) >= 0) {
                    current.append('§').append(code);
                } else if (code == 'r') {
                    current.setLength(0);
                }
                i++;
            } else {
                formats.add(current.toString());
            }
        }
        return formats;
    }

    public static boolean hasMultipleFormats(List<String> formats) {
        if (formats == null || formats.size() < 2) return false;
        String first = formats.get(0);
        for (int i = 1; i < formats.size(); i++) {
            if (!formats.get(i).equalsIgnoreCase(first)) return true;
        }
        return false;
    }

    public static String buildRainbowString(String text, List<String> formats, String activeAfter) {
        if (text == null || text.isEmpty()) return "";
        if (formats == null || formats.isEmpty()) return text;
        StringBuilder sb = new StringBuilder();
        int fCount = formats.size();
        for (int i = 0; i < text.length(); i++) {
            int fIdx = i * fCount / text.length();
            if (fIdx >= fCount) fIdx = fCount - 1;
            sb.append(formats.get(fIdx)).append(text.charAt(i));
        }
        if (activeAfter != null && !activeAfter.isEmpty()) {
            sb.append("§r").append(activeAfter);
        }
        return sb.toString();
    }

    public static String replaceNickInText(String text, String nameToReplace, String replacement) {
        if (text == null || nameToReplace == null || nameToReplace.isEmpty() || replacement == null) {
            return text;
        }

        // Build pattern that matches nameToReplace allowing any formatting codes between or immediately before characters
        StringBuilder regex = new StringBuilder("(?i)(?:§[0-9a-fk-orA-FK-OR])*");
        for (int i = 0; i < nameToReplace.length(); i++) {
            char c = nameToReplace.charAt(i);
            regex.append(Pattern.quote(String.valueOf(c)));
            if (i < nameToReplace.length() - 1) {
                regex.append("(?:§[0-9a-fk-orA-FK-OR])*");
            }
        }

        Pattern p = Pattern.compile(regex.toString());
        Matcher m = p.matcher(text);
        if (!m.find()) {
            return text;
        }

        StringBuffer sb = new StringBuffer();
        boolean repHasCustomColors = replacement.contains("§");
        do {
            int matchStart = m.start();
            int matchEnd = m.end();
            String matched = m.group(0);

            String activeBefore = getActiveFormattingAt(text, matchStart);
            String activeAfter = getActiveFormattingAt(text, matchEnd);
            List<String> formats = extractCharFormats(matched, activeBefore);

            String repToInsert;
            if (repHasCustomColors) {
                repToInsert = replacement;
                if (!activeAfter.isEmpty()) {
                    repToInsert = replacement + "§r" + activeAfter;
                }
            } else if (hasMultipleFormats(formats)) {
                repToInsert = buildRainbowString(replacement, formats, activeAfter);
            } else {
                String singleFmt = (!formats.isEmpty() && !formats.get(0).isEmpty()) ? formats.get(0) : activeBefore;
                if (!singleFmt.isEmpty()) {
                    repToInsert = singleFmt + replacement;
                    if (!activeAfter.isEmpty() && !activeAfter.equalsIgnoreCase(singleFmt)) {
                        repToInsert = singleFmt + replacement + "§r" + activeAfter;
                    }
                } else {
                    repToInsert = replacement;
                }
            }

            m.appendReplacement(sb, Matcher.quoteReplacement(repToInsert));
        } while (m.find());
        m.appendTail(sb);

        return sb.toString();
    }

    public static String filterString(String text) {
        if (text == null) return null;

        if (dev.edtrick.nickhider.bedwars.BedwarsLevelManager.isEnabled()) {
            text = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterGeneralString(text);
        }

        if (dev.edtrick.nickhider.network.NetworkLevelManager.isEnabled()) {
            text = dev.edtrick.nickhider.network.NetworkLevelManager.filterGeneralString(text);
        }

        if (fakeNick == null) return text;

        java.util.List<String> names = getNamesToReplace();
        for (String name : names) {
            if (name == null || name.isEmpty() || name.equalsIgnoreCase(fakeNick)) continue;
            text = replaceNickInText(text, name, fakeNick);
        }

        return text;
    }

    public static String translateColorCodes(String text) {
        if (text == null) return null;
        char[] b = text.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = '§';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    public static String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)[&§][0-9A-FK-OR]", "");
    }

    private static java.lang.reflect.Field chatTextField = null;
    static {
        try {
            chatTextField = ChatComponentText.class.getDeclaredField("text");
            chatTextField.setAccessible(true);
        } catch (Throwable t) {
            try {
                for (java.lang.reflect.Field f : ChatComponentText.class.getDeclaredFields()) {
                    if (f.getType() == String.class) {
                        chatTextField = f;
                        chatTextField.setAccessible(true);
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void setChatComponentTextValue(ChatComponentText component, String newText) {
        if (component == null) return;
        try {
            if (chatTextField != null) {
                chatTextField.set(component, newText);
            }
        } catch (Throwable ignored) {}
    }

    public static void collectTextLeaves(IChatComponent component, java.util.List<ChatComponentText> leaves) {
        if (component == null) return;
        if (component instanceof ChatComponentText) {
            leaves.add((ChatComponentText) component);
        }
        for (Object sibling : component.getSiblings()) {
            if (sibling instanceof IChatComponent) {
                collectTextLeaves((IChatComponent) sibling, leaves);
            }
        }
    }

    public static IChatComponent filterText(IChatComponent component) {
        return filterComponent(component);
    }

    public static IChatComponent filterComponent(IChatComponent component) {
        if (component == null) return null;
        if (fakeNick == null && !dev.edtrick.nickhider.bedwars.BedwarsLevelManager.isEnabled()
                && !dev.edtrick.nickhider.network.NetworkLevelManager.isEnabled()) return component;

        try {
            java.util.List<ChatComponentText> leaves = new java.util.ArrayList<ChatComponentText>();
            collectTextLeaves(component, leaves);

            if (!leaves.isEmpty()) {
                if (dev.edtrick.nickhider.bedwars.BedwarsLevelManager.isEnabled()) {
                    for (ChatComponentText leaf : leaves) {
                        String val = leaf.getChatComponentText_TextValue();
                        if (val != null && !val.isEmpty()) {
                            String filtered = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterGeneralString(val);
                            if (!val.equals(filtered)) {
                                setChatComponentTextValue(leaf, filtered);
                            }
                        }
                    }
                }

                if (dev.edtrick.nickhider.network.NetworkLevelManager.isEnabled()) {
                    for (ChatComponentText leaf : leaves) {
                        String val = leaf.getChatComponentText_TextValue();
                        if (val != null && !val.isEmpty()) {
                            String filtered = dev.edtrick.nickhider.network.NetworkLevelManager.filterGeneralString(val);
                            if (!val.equals(filtered)) {
                                setChatComponentTextValue(leaf, filtered);
                            }
                        }
                    }
                }

                if (fakeNick != null && !fakeNick.isEmpty()) {
                    filterLeavesForNick(leaves, fakeNick);
                }

                return component;
            } else {
                return filterComponentRecursive(component);
            }
        } catch (Exception e) {
            return component;
        }
    }

    private static void filterLeavesForNick(java.util.List<ChatComponentText> leaves, String fakeNick) {
        java.util.List<String> names = getNamesToReplace();
        for (String name : names) {
            if (name == null || name.isEmpty() || name.equalsIgnoreCase(fakeNick)) continue;

            // 1. Single-leaf replacement (handles names fully inside one leaf, preserving § color codes)
            for (ChatComponentText leaf : leaves) {
                String val = leaf.getChatComponentText_TextValue();
                if (val != null && !val.isEmpty() && stripColorCodes(val).toLowerCase().contains(name.toLowerCase())) {
                    String filtered = replaceNickInText(val, name, fakeNick);
                    if (!val.equals(filtered)) {
                        setChatComponentTextValue(leaf, filtered);
                    }
                }
            }

            // 2. Multi-leaf replacement (handles names split across multiple sibling leaves with styles)
            replaceNameAcrossLeaves(leaves, name, fakeNick);
        }
    }

    private static void replaceNameAcrossLeaves(java.util.List<ChatComponentText> leaves, String name, String fakeNick) {
        if (leaves == null || leaves.size() <= 1 || name == null || name.isEmpty() || fakeNick == null) return;

        StringBuilder combined = new StringBuilder();
        for (ChatComponentText leaf : leaves) {
            String text = leaf.getChatComponentText_TextValue();
            if (text != null) combined.append(text);
        }
        String fullText = combined.toString();
        if (!fullText.toLowerCase().contains(name.toLowerCase())) return;

        Pattern p = Pattern.compile("(?i)" + Pattern.quote(name));
        Matcher m = p.matcher(fullText);
        while (m.find()) {
            int matchStart = m.start();
            int matchEnd = m.end();

            int currentPos = 0;
            int firstLeafIdx = -1;
            int lastLeafIdx = -1;
            int firstLeafOffset = 0;
            int lastLeafEndOffset = 0;

            for (int i = 0; i < leaves.size(); i++) {
                ChatComponentText leaf = leaves.get(i);
                String text = leaf.getChatComponentText_TextValue();
                int len = (text != null) ? text.length() : 0;
                int leafStart = currentPos;
                int leafEnd = currentPos + len;
                currentPos = leafEnd;

                if (leafEnd > matchStart && leafStart < matchEnd) {
                    if (firstLeafIdx == -1) {
                        firstLeafIdx = i;
                        firstLeafOffset = matchStart - leafStart;
                    }
                    lastLeafIdx = i;
                    lastLeafEndOffset = matchEnd - leafStart;
                }
            }

            if (firstLeafIdx != -1 && firstLeafIdx < lastLeafIdx) {
                int numLeaves = lastLeafIdx - firstLeafIdx + 1;
                int fakeLen = fakeNick.length();

                for (int k = 0; k < numLeaves; k++) {
                    int leafIdx = firstLeafIdx + k;
                    ChatComponentText targetLeaf = leaves.get(leafIdx);
                    String origText = targetLeaf.getChatComponentText_TextValue();
                    if (origText == null) origText = "";

                    int segStart = k * fakeLen / numLeaves;
                    int segEnd = (k + 1) * fakeLen / numLeaves;
                    if (k == numLeaves - 1) segEnd = fakeLen;
                    String segment = (segStart < segEnd && segStart < fakeLen) ? fakeNick.substring(segStart, Math.min(segEnd, fakeLen)) : "";

                    if (k == 0) {
                        String prefix = (firstLeafOffset > 0 && firstLeafOffset <= origText.length()) ? origText.substring(0, firstLeafOffset) : "";
                        setChatComponentTextValue(targetLeaf, prefix + segment);
                    } else if (k == numLeaves - 1) {
                        String suffix = (lastLeafEndOffset >= 0 && lastLeafEndOffset < origText.length()) ? origText.substring(lastLeafEndOffset) : "";
                        setChatComponentTextValue(targetLeaf, segment + suffix);
                    } else {
                        setChatComponentTextValue(targetLeaf, segment);
                    }
                }

                // Re-evaluate combined text for any additional occurrences
                combined.setLength(0);
                for (ChatComponentText leaf : leaves) {
                    String text = leaf.getChatComponentText_TextValue();
                    if (text != null) combined.append(text);
                }
                fullText = combined.toString();
                m = p.matcher(fullText);
            }
        }
    }

    private static IChatComponent filterComponentRecursive(IChatComponent component) {
        IChatComponent result;

        if (component instanceof ChatComponentText) {
            String original = ((ChatComponentText) component).getChatComponentText_TextValue();
            String filtered = filterString(original);
            result = new ChatComponentText(filtered);
        } else {
            result = component.createCopy();
        }
        result.setChatStyle(component.getChatStyle().createShallowCopy());

        if (!component.getSiblings().isEmpty()) {
            result.getSiblings().clear();
            for (IChatComponent sibling : component.getSiblings()) {
                result.appendSibling(filterComponentRecursive(sibling));
            }
        }
        return result;
    }

    // ─── Rainbow prefix ──────────────────────────────────────────────────────

    public static String rainbowPrefix() {
        char[] text = "[NickHider]".toCharArray();
        EnumChatFormatting[] colors = {
                EnumChatFormatting.RED,
                EnumChatFormatting.GOLD,
                EnumChatFormatting.YELLOW,
                EnumChatFormatting.GREEN,
                EnumChatFormatting.AQUA,
                EnumChatFormatting.BLUE,
                EnumChatFormatting.LIGHT_PURPLE,
                EnumChatFormatting.RED,
                EnumChatFormatting.GOLD,
                EnumChatFormatting.YELLOW,
                EnumChatFormatting.GREEN
        };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length; i++) {
            sb.append(colors[i % colors.length].toString()).append(text[i]);
        }
        sb.append(EnumChatFormatting.RESET);
        return sb.toString();
    }

    public static void sendMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(
                    new ChatComponentText(rainbowPrefix() + " " + EnumChatFormatting.WHITE + message)
            );
        }
    }

    // ─── Multi-source Skin Data Fetcher ──────────────────────────────────────

    public static String[] fetchSkinData(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return null;
        String cleanName = playerName.trim();

        // 1. Try PlayerDB (Global Cloudflare CDN, no Mojang 429 rate limit, 1 call)
        try {
            String[] data = fetchFromPlayerDB(cleanName);
            if (data != null) {
                LOGGER.info("[NickHider] Fetched skin for '{}' from PlayerDB", cleanName);
                return data;
            }
        } catch (Exception e) {
            LOGGER.debug("[NickHider] PlayerDB fetch failed for '{}': {}", cleanName, e.getMessage());
        }

        // 2. Try Crafthead (Fast Cloudflare mirror)
        try {
            String[] data = fetchFromCrafthead(cleanName);
            if (data != null) {
                LOGGER.info("[NickHider] Fetched skin for '{}' from Crafthead", cleanName);
                return data;
            }
        } catch (Exception e) {
            LOGGER.debug("[NickHider] Crafthead fetch failed for '{}': {}", cleanName, e.getMessage());
        }

        // 3. Fallback to official Mojang API
        try {
            String[] data = fetchFromMojang(cleanName);
            if (data != null) {
                LOGGER.info("[NickHider] Fetched skin for '{}' from Mojang API", cleanName);
                return data;
            }
        } catch (Exception e) {
            LOGGER.debug("[NickHider] Mojang fetch failed for '{}': {}", cleanName, e.getMessage());
        }

        return null;
    }

    private static String[] fetchFromPlayerDB(String playerName) {
        try {
            URL url = new URL("https://playerdb.co/api/player/minecraft/" + playerName);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NickHider/1.0");

            if (conn.getResponseCode() == 200) {
                String json = readStream(conn.getInputStream());
                if (json.contains("\"success\":true") || json.contains("\"success\": true")) {
                    String value = extractJsonString(json, "value");
                    String signature = extractJsonString(json, "signature");
                    if (value != null) {
                        return new String[]{value, signature != null ? signature : ""};
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[NickHider] PlayerDB error: {}", e.getMessage());
        }
        return null;
    }

    private static String[] fetchFromCrafthead(String playerName) {
        try {
            URL url = new URL("https://crafthead.net/profile/" + playerName);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NickHider/1.0");

            if (conn.getResponseCode() == 200) {
                String json = readStream(conn.getInputStream());
                String value = extractJsonString(json, "value");
                String signature = extractJsonString(json, "signature");
                if (value != null) {
                    return new String[]{value, signature != null ? signature : ""};
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[NickHider] Crafthead error: {}", e.getMessage());
        }
        return null;
    }

    private static String[] fetchFromMojang(String playerName) {
        String uuid = fetchUUID(playerName);
        if (uuid == null) return null;
        return fetchSkinTextures(uuid);
    }

    public static String fetchUUID(String playerName) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NickHider/1.0");

            int code = conn.getResponseCode();
            if (code == 200) {
                String json = readStream(conn.getInputStream());
                String id = extractJsonString(json, "id");
                if (id != null) {
                    return id.replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                            "$1-$2-$3-$4-$5"
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.error("[NickHider] Error fetching UUID for '{}': {}", playerName, e.getMessage());
        }
        return null;
    }

    public static String[] fetchSkinTextures(String uuid) {
        try {
            String cleanUUID = uuid.replace("-", "");
            URL url = new URL(
                    "https://sessionserver.mojang.com/session/minecraft/profile/"
                    + cleanUUID + "?unsigned=false"
            );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NickHider/1.0");

            if (conn.getResponseCode() == 200) {
                String json = readStream(conn.getInputStream());
                String value     = extractJsonString(json, "value");
                String signature = extractJsonString(json, "signature");

                if (value != null) {
                    return new String[]{value, signature != null ? signature : ""};
                }
            }
        } catch (Exception e) {
            LOGGER.error("[NickHider] Error fetching skin textures for UUID '{}': {}", uuid, e.getMessage());
        }
        return null;
    }

    private static String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        is.close();
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon < 0) return null;
        int q1 = json.indexOf("\"", colon + 1);
        if (q1 < 0) return null;
        int q2 = q1 + 1;
        while (q2 < json.length()) {
            char c = json.charAt(q2);
            if (c == '\\') { q2 += 2; continue; }
            if (c == '"')  { break; }
            q2++;
        }
        return json.substring(q1 + 1, q2);
    }
}
