package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayer {

    @Inject(method = "getLocationSkin()Lnet/minecraft/util/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void nickhider_getLocationSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (isSelf()) {
            ResourceLocation fake = NickHiderMod.getFakeSkinLocation();
            if (fake != null) {
                cir.setReturnValue(fake);
            }
        }
    }

    @Inject(method = "getSkinType", at = @At("HEAD"), cancellable = true)
    private void nickhider_getSkinType(CallbackInfoReturnable<String> cir) {
        if (isSelf()) {
            String model = NickHiderMod.getFakeSkinModel();
            if (model != null) {
                cir.setReturnValue(model);
            }
        }
    }

    @Inject(method = "hasSkin", at = @At("HEAD"), cancellable = true)
    private void nickhider_hasSkin(CallbackInfoReturnable<Boolean> cir) {
        if (isSelf()) {
            if (NickHiderMod.getFakeSkinLocation() != null) {
                cir.setReturnValue(true);
            }
        }
    }

    private boolean isSelf() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return false;
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        return self == mc.thePlayer
                || self.getEntityId() == mc.thePlayer.getEntityId()
                || (self.getUniqueID() != null && self.getUniqueID().equals(mc.thePlayer.getUniqueID()));
    }
}
