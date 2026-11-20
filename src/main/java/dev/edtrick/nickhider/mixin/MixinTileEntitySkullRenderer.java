package dev.edtrick.nickhider.mixin;

import com.mojang.authlib.GameProfile;
import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the texture of skulls belonging to the local player with the fake skin.
 * This applies to skulls in inventory/hotbar, placed in the world, held, or worn as helmets.
 */
@Mixin(TileEntitySkullRenderer.class)
public class MixinTileEntitySkullRenderer {

    private final ThreadLocal<GameProfile> nickhider_currentProfile = new ThreadLocal<GameProfile>();

    @Inject(method = "renderSkull", at = @At("HEAD"))
    private void nickhider_onRenderSkullHead(float x, float y, float z, EnumFacing facing, float rotation,
                                             int skullType, GameProfile profile, int destroyStage, CallbackInfo ci) {
        if (skullType == 3) {
            nickhider_currentProfile.set(profile);
        } else {
            nickhider_currentProfile.remove();
        }
    }

    @ModifyArg(
        method = "renderSkull",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntitySkullRenderer;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 4),
        index = 0
    )
    private ResourceLocation nickhider_modifySkullTexture(ResourceLocation original) {
        GameProfile profile = nickhider_currentProfile.get();
        if (NickHiderMod.isLocalPlayerProfile(profile)) {
            ResourceLocation fakeSkin = NickHiderMod.getFakeSkinLocation();
            if (fakeSkin != null) {
                return fakeSkin;
            }
        }
        return original;
    }

    @Inject(method = "renderSkull", at = @At("RETURN"))
    private void nickhider_onRenderSkullReturn(float x, float y, float z, EnumFacing facing, float rotation,
                                               int skullType, GameProfile profile, int destroyStage, CallbackInfo ci) {
        nickhider_currentProfile.remove();
    }
}
