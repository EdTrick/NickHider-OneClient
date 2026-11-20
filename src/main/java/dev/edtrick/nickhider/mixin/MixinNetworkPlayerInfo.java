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

@Mixin(NetworkPlayerInfo.class)
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

    private boolean isLocalPlayer() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || gameProfile == null) return false;

        if (mc.thePlayer != null) {
            if (mc.thePlayer.getUniqueID() != null && mc.thePlayer.getUniqueID().equals(gameProfile.getId())) return true;
            if (mc.thePlayer.getName() != null && mc.thePlayer.getName().equalsIgnoreCase(gameProfile.getName())) return true;
        }

        if (mc.getSession() != null) {
            if (mc.getSession().getProfile() != null && mc.getSession().getProfile().getId() != null) {
                if (mc.getSession().getProfile().getId().equals(gameProfile.getId())) return true;
            }
            String realName = mc.getSession().getUsername();
            if (realName != null && realName.equalsIgnoreCase(gameProfile.getName())) return true;
        }

        if (gameProfile.getName() != null && NickHiderMod.getNamesToReplace().contains(gameProfile.getName())) return true;

        return false;
    }
}
