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
import java.util.Base64;
import java.util.HashMap;
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
    }

    public static boolean isRealSkinCaptured() { return realSkinCaptured; }

    // ─── Config persistence ──────────────────────────────────────────────────

    public static void saveConfig() {
        if (configFile == null) return;
        Properties prop = new Properties();
        if (fakeNick != null) prop.setProperty("fakeNick", fakeNick);
        if (fakeSkinValue != null) prop.setProperty("fakeSkinValue", fakeSkinValue);
        if (fakeSkinSignature != null) prop.setProperty("fakeSkinSignature", fakeSkinSignature);
        if (fakeSkinOwner != null) prop.setProperty("fakeSkinOwner", fakeSkinOwner);

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
        } catch (IOException e) {
            LOGGER.error("[NickHider] Failed to load config", e);
        }
    }

    // ─── String Replacement Engine ───────────────────────────────────────────

    private static String detectedChatNick = null;

    public static void setDetectedChatNick(String nick) {
        detectedChatNick = nick;
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
        if (profileName != null && !profileName.isEmpty()) {
            for (String name : getNamesToReplace()) {
                if (profileName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        // 3. Check textures property against realSkinProperty if available
        if (realSkinProperty != null && realSkinProperty.getValue() != null && profile.getProperties() != null) {
            if (profile.getProperties().containsKey("textures")) {
                for (Property prop : profile.getProperties().get("textures")) {
                    if (prop != null && realSkinProperty.getValue().equals(prop.getValue())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static String filterString(String text) {
        if (text == null) return null;
        if (fakeNick == null) return text;

        java.util.List<String> names = getNamesToReplace();
        for (String name : names) {
            if (name == null || name.isEmpty() || name.equalsIgnoreCase(fakeNick)) continue;

            if (text.contains(name)) {
                text = text.replace(name, fakeNick);
            } else {
                Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(name));
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    text = matcher.replaceAll(Matcher.quoteReplacement(fakeNick));
                }
            }
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

    public static IChatComponent filterText(IChatComponent component) {
        return filterComponent(component);
    }

    public static IChatComponent filterComponent(IChatComponent component) {
        if (component == null) return null;
        if (fakeNick == null) return component;

        try {
            return filterComponentRecursive(component);
        } catch (Exception e) {
            return component;
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
