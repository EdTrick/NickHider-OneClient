package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

    @Inject(method = "getEntityTexture", at = @At("HEAD"), cancellable = true)
    private void nickhider_getEntityTexture(AbstractClientPlayer entity, CallbackInfoReturnable<ResourceLocation> cir) {
        if (entity != null && NickHiderMod.isLocalPlayerEntity(entity)) {
            ResourceLocation fake = NickHiderMod.getFakeSkinLocation();
            if (fake != null) {
                cir.setReturnValue(fake);
            }
        }
    }
}
