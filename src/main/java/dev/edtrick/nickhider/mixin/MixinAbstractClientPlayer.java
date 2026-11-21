package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayer {

    @Inject(method = "getLocationSkin", at = @At("HEAD"), cancellable = true)
    private void nickhider_getLocationSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (isSelfOrClone()) {
            ResourceLocation fake = NickHiderMod.getFakeSkinLocation();
            if (fake != null) {
                cir.setReturnValue(fake);
            }
        }
    }

    @Inject(method = "getSkinType", at = @At("HEAD"), cancellable = true)
    private void nickhider_getSkinType(CallbackInfoReturnable<String> cir) {
        if (isSelfOrClone()) {
            String model = NickHiderMod.getFakeSkinModel();
            if (model != null) {
                cir.setReturnValue(model);
            }
        }
    }

    @Inject(method = "hasSkin", at = @At("HEAD"), cancellable = true)
    private void nickhider_hasSkin(CallbackInfoReturnable<Boolean> cir) {
        if (isSelfOrClone()) {
            if (NickHiderMod.getFakeSkinLocation() != null) {
                cir.setReturnValue(true);
            }
        }
    }

    private boolean isSelfOrClone() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return false;
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;

        // 1. Direct entity check (thePlayer, UUID, or matching names/profiles)
        if (NickHiderMod.isLocalPlayerEntity(self)) return true;

        // 2. Profile check (real IGN, current name, server-assigned nick e.g. in Hypixel replays)
        if (self.getGameProfile() != null && NickHiderMod.isLocalPlayerProfile(self.getGameProfile())) return true;

        // 3. Name check directly on entity name
        String name = self.getName();
        if (name != null && !name.isEmpty() && NickHiderMod.isLocalPlayerName(name)) return true;

        // 4. Skin texture property / URL match (catches lobby profile NPCs cloned with real skin)
        if (NickHiderMod.matchesRealSkin(self)) return true;

        // 5. Standing directly under a "Your ... Profile" ArmorStand hologram in lobbies
        if (isUnderProfileHologram(self)) return true;

        return false;
    }

    private boolean isUnderProfileHologram(AbstractClientPlayer player) {
        try {
            if (player.worldObj == null) return false;
            AxisAlignedBB bb = player.getEntityBoundingBox().expand(1.5, 3.5, 1.5);
            List<EntityArmorStand> stands = player.worldObj.getEntitiesWithinAABB(EntityArmorStand.class, bb);
            if (stands != null && !stands.isEmpty()) {
                for (EntityArmorStand stand : stands) {
                    if (stand != null && stand.hasCustomName()) {
                        String name = NickHiderMod.stripColorCodes(stand.getCustomNameTag()).toLowerCase();
                        if (name.contains("your") && (name.contains("profile") || name.contains("level:"))) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
