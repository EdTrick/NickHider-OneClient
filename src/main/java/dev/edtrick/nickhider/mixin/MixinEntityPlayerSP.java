package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.command.NickHiderCommand;
import dev.edtrick.nickhider.bedwars.BedwarsLevelManager;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void nickhider_sendChatMessage(String message, CallbackInfo ci) {
        if (message == null) return;
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("/nickhider") || lower.equals("nickhider")
                || lower.startsWith("nickhider ")) {
            if (NickHiderCommand.handle(trimmed)) {
                ci.cancel();
            }
            return;
        }

        // Track chat message or /nick command
        if (!trimmed.startsWith("/")) {
            dev.edtrick.nickhider.NickHiderMod.recordSentChatMessage(trimmed);
        } else {
            if (lower.startsWith("/nick ")) {
                String arg = trimmed.substring(6).trim();
                if (!arg.equalsIgnoreCase("reset") && !arg.equalsIgnoreCase("help") && !arg.equalsIgnoreCase("random")) {
                    String clean = arg.replaceAll("[^A-Za-z0-9_]", "");
                    if (clean.length() >= 3 && clean.length() <= 16) {
                        dev.edtrick.nickhider.NickHiderMod.setDetectedChatNick(clean);
                    }
                } else if (arg.equalsIgnoreCase("reset")) {
                    dev.edtrick.nickhider.NickHiderMod.setDetectedChatNick(null);
                }
            } else if (lower.equals("/unnick")) {
                dev.edtrick.nickhider.NickHiderMod.setDetectedChatNick(null);
            }
        }
    }

    @Inject(method = "setXPStats", at = @At("RETURN"))
    private void nickhider_setXPStats(float currentXP, int maxXP, int level, CallbackInfo ci) {
        if (BedwarsLevelManager.isEnabled() && BedwarsLevelManager.isBedwarsMatchActive()) {
            BedwarsLevelManager.setDetectedRealStars(level);
            ((EntityPlayerSP) (Object) this).experienceLevel = BedwarsLevelManager.getFakeStars();
        } else {
            BedwarsLevelManager.setRealNonMatchXpLevel(level);
        }
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void nickhider_onUpdate(CallbackInfo ci) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;

        // Auto-detect server-assigned nick from tablist entry
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null && mc.getNetHandler() != null && player.getUniqueID() != null) {
                net.minecraft.client.network.NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
                if (info != null && info.getGameProfile() != null) {
                    String currentTabName = info.getGameProfile().getName();
                    String realName = (mc.getSession() != null) ? mc.getSession().getUsername() : null;
                    if (currentTabName != null && !currentTabName.isEmpty()) {
                        if (realName != null && !currentTabName.equalsIgnoreCase(realName)) {
                            String fake = dev.edtrick.nickhider.NickHiderMod.getFakeNick();
                            if (fake == null || !currentTabName.equalsIgnoreCase(dev.edtrick.nickhider.NickHiderMod.stripColorCodes(fake))) {
                                String currentDetected = dev.edtrick.nickhider.NickHiderMod.getDetectedChatNick();
                                if (currentDetected == null || !currentDetected.equalsIgnoreCase(currentTabName)) {
                                    dev.edtrick.nickhider.NickHiderMod.setDetectedChatNick(currentTabName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (!BedwarsLevelManager.isEnabled()) return;
        if (BedwarsLevelManager.isBedwarsMatchActive()) {
            if (player.experienceLevel != BedwarsLevelManager.getFakeStars()) {
                BedwarsLevelManager.setDetectedRealStars(player.experienceLevel);
                player.experienceLevel = BedwarsLevelManager.getFakeStars();
            }
        } else {
            int realXp = BedwarsLevelManager.getRealNonMatchXpLevel();
            if (realXp >= 0 && player.experienceLevel == BedwarsLevelManager.getFakeStars()
                    && BedwarsLevelManager.getFakeStars() != realXp) {
                player.experienceLevel = realXp;
            }
        }
    }
}
