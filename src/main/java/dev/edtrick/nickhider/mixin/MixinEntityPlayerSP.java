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
        if (!BedwarsLevelManager.isEnabled()) return;
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
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
