package dev.edtrick.nickhider.config;

import dev.edtrick.nickhider.NickHiderMod;
import dev.edtrick.nickhider.command.NickHiderCommand;
import net.fabricmc.loader.api.FabricLoader;

public class OneConfigCompat {

    private static boolean available = false;

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("oneconfig")) {
            return;
        }
        try {
            Holder.initHolder();
            available = true;
            NickHiderMod.LOGGER.info("[NickHider] OneConfig integration initialized successfully.");
        } catch (Throwable t) {
            NickHiderMod.LOGGER.error("[NickHider] Failed to initialize OneConfig integration", t);
        }
    }

    public static void sync() {
        if (available) {
            try {
                Holder.syncHolder();
            } catch (Throwable ignored) {}
        }
    }

    private static class Holder {
        static void initHolder() {
            NickHiderConfig.INSTANCE = new NickHiderConfig();
            NickHiderConfig.INSTANCE.syncFromMod();
            NickHiderConfig.INSTANCE.preload();

            // If OneConfig was loaded and had enabled=true with a saved nick/skin, but mod had none, apply it
            if (NickHiderConfig.enableNickHider && NickHiderMod.getFakeNick() == null) {
                if (NickHiderConfig.fakeNick != null && !NickHiderConfig.fakeNick.trim().isEmpty()) {
                    NickHiderCommand.handleSetNick(NickHiderConfig.fakeNick.trim());
                }
            }
            if (NickHiderConfig.enableSkinHider && NickHiderMod.getFakeSkinOwner() == null) {
                if (NickHiderConfig.fakeSkin != null && !NickHiderConfig.fakeSkin.trim().isEmpty()) {
                    NickHiderCommand.handleSetSkin(NickHiderConfig.fakeSkin.trim());
                }
            }
        }

        static void syncHolder() {
            if (NickHiderConfig.INSTANCE != null) {
                NickHiderConfig.INSTANCE.syncFromMod();
                NickHiderConfig.INSTANCE.save();
            }
            updateOneConfigPlayerName();
        }

        private static void updateOneConfigPlayerName() {
            try {
                Class<?> controllerClass = Class.forName("org.polyfrost.oneconfig.internal.ui.shell.ShellController");
                java.lang.reflect.Method getShared = controllerClass.getMethod("getShared");
                Object controller = getShared.invoke(null);
                if (controller != null) {
                    java.lang.reflect.Method getSession = controllerClass.getMethod("getSession");
                    Object session = getSession.invoke(controller);
                    if (session != null) {
                        java.lang.reflect.Method updatePlayerName = session.getClass().getMethod("updatePlayerName", String.class);
                        String fake = NickHiderMod.getFakeNick();
                        if (fake != null && !fake.trim().isEmpty()) {
                            updatePlayerName.invoke(session, NickHiderMod.stripColorCodes(fake));
                        } else {
                            String real = "Player";
                            try {
                                if (net.minecraft.client.Minecraft.getMinecraft() != null
                                        && net.minecraft.client.Minecraft.getMinecraft().getSession() != null) {
                                    real = net.minecraft.client.Minecraft.getMinecraft().getSession().getUsername();
                                }
                            } catch (Throwable ignored) {}
                            updatePlayerName.invoke(session, real);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
