package dev.edtrick.nickhider.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetworkPlayerInfo.class, priority = 999999)
public class MixinNetworkPlayerInfo {

    @Shadow
    @Final
    private GameProfile gameProfile;

    @Inject(method = "getLocationSkin", at = @At("HEAD"), cancellable = true)
    private void nickhider_tabLocationSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (isLocalPlayer()) {
            ResourceLocation fake = NickHiderMod.getFakeSkinLocation();
            if (fake != null) {
                cir.setReturnValue(fake);
            }
        }
    }

    @Inject(method = "getSkinType", at = @At("HEAD"), cancellable = true)
    private void nickhider_tabSkinType(CallbackInfoReturnable<String> cir) {
        if (isLocalPlayer()) {
            String model = NickHiderMod.getFakeSkinModel();
            if (model != null) {
                cir.setReturnValue(model);
            }
        }
    }

    @Inject(method = "hasLocationSkin", at = @At("HEAD"), cancellable = true)
    private void nickhider_tabHasLocationSkin(CallbackInfoReturnable<Boolean> cir) {
        if (isLocalPlayer()) {
            if (NickHiderMod.getFakeSkinLocation() != null) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "loadPlayerTextures", at = @At("HEAD"))
    private void nickhider_loadPlayerTextures(CallbackInfo ci) {
        if (!isLocalPlayer()) return;

        // Capture real account skin property if not yet saved
        if (!NickHiderMod.isRealSkinCaptured()) {
            PropertyMap props = gameProfile.getProperties();
            if (props.containsKey("textures")) {
                for (Property p : props.get("textures")) {
                    NickHiderMod.setRealSkinProperty(p);
                    break;
                }
            }
        }

        // Inject fake skin if active
        String fakeSkinValue = NickHiderMod.getFakeSkinValue();
        if (fakeSkinValue != null) {
            gameProfile.getProperties().removeAll("textures");
            String sig = NickHiderMod.getFakeSkinSignature();
            gameProfile.getProperties().put(
                    "textures",
                    new Property("textures", fakeSkinValue, sig != null ? sig : "")
            );
        }
    }

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void nickhider_getDisplayName(CallbackInfoReturnable<net.minecraft.util.IChatComponent> cir) {
        if (isLocalPlayer()) {
            net.minecraft.util.IChatComponent comp = cir.getReturnValue();
            if (comp != null) {
                if (dev.edtrick.nickhider.bedwars.BedwarsLevelManager.isEnabled() && dev.edtrick.nickhider.bedwars.BedwarsLevelManager.isBedwarsActive()) {
                    comp = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterChat(comp);
                }
                comp = NickHiderMod.filterText(comp);
                cir.setReturnValue(comp);
            }
        }
    }

    private boolean isLocalPlayer() {
        if (gameProfile == null) return false;
        return NickHiderMod.isLocalPlayerProfile(gameProfile);
    }
}
