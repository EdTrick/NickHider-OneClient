package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import dev.edtrick.nickhider.bedwars.BedwarsLevelManager;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiPlayerTabOverlay.class, priority = 999999)
public class MixinGuiPlayerTabOverlay {

    @Redirect(
        method = "renderPlayerlist",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;getPlayerName(Lnet/minecraft/client/network/NetworkPlayerInfo;)Ljava/lang/String;")
    )
    private String nickhider_redirectGetPlayerName(GuiPlayerTabOverlay instance, NetworkPlayerInfo info) {
        String ret = instance.getPlayerName(info);
        if (ret != null) {
            boolean isLocal = false;
            if (info != null && info.getGameProfile() != null) {
                isLocal = NickHiderMod.isLocalPlayerProfile(info.getGameProfile());
            }
            if (!isLocal) {
                isLocal = NickHiderMod.containsLocalPlayerName(ret);
            }
            if (!isLocal && NickHiderMod.getFakeNick() != null && !NickHiderMod.getFakeNick().isEmpty()) {
                isLocal = NickHiderMod.stripColorCodes(ret).toLowerCase().contains(NickHiderMod.getFakeNick().toLowerCase());
            }

            if (isLocal && BedwarsLevelManager.isEnabled() && BedwarsLevelManager.isBedwarsActive()) {
                ret = BedwarsLevelManager.replaceTagInString(ret);
            }
            ret = NickHiderMod.filterString(ret);
        }
        return ret;
    }

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void nickhider_getPlayerName(NetworkPlayerInfo networkPlayerInfoIn, CallbackInfoReturnable<String> cir) {
        String ret = cir.getReturnValue();
        if (ret != null) {
            boolean isLocal = false;
            if (networkPlayerInfoIn != null && networkPlayerInfoIn.getGameProfile() != null) {
                isLocal = NickHiderMod.isLocalPlayerProfile(networkPlayerInfoIn.getGameProfile());
            }
            if (!isLocal) {
                isLocal = NickHiderMod.containsLocalPlayerName(ret);
            }
            if (!isLocal && NickHiderMod.getFakeNick() != null && !NickHiderMod.getFakeNick().isEmpty()) {
                isLocal = NickHiderMod.stripColorCodes(ret).toLowerCase().contains(NickHiderMod.getFakeNick().toLowerCase());
            }

            if (isLocal && BedwarsLevelManager.isEnabled() && BedwarsLevelManager.isBedwarsActive()) {
                ret = BedwarsLevelManager.replaceTagInString(ret);
            }

            ret = NickHiderMod.filterString(ret);
            if (!ret.equals(cir.getReturnValue())) {
                cir.setReturnValue(ret);
            }
        }
    }
}
