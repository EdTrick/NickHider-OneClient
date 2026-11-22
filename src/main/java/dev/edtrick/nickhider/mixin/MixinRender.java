package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import dev.edtrick.nickhider.bedwars.BedwarsLevelManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces real IGN occurrences in entity nametags and ArmorStand holograms,
 * and replaces Bedwars star levels in nametags rendered over the local player.
 */
@Mixin(Render.class)
public class MixinRender {

    @ModifyVariable(method = "renderLivingLabel", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String nickhider_modifyLabelText(String text, Entity entityIn) {
        if (text == null) return null;

        boolean isLocal = NickHiderMod.isLocalPlayerEntity(entityIn);
        if (!isLocal) {
            isLocal = NickHiderMod.containsLocalPlayerName(text);
        }
        if (!isLocal && text != null) {
            String stripped = NickHiderMod.stripColorCodes(text).toLowerCase();
            if (stripped.contains("your level:") || (stripped.contains("your") && stripped.contains("profile"))) {
                isLocal = true;
            }
        }
        if (!isLocal && NickHiderMod.getFakeNick() != null && !NickHiderMod.getFakeNick().isEmpty()) {
            isLocal = NickHiderMod.stripColorCodes(text).toLowerCase().contains(NickHiderMod.getFakeNick().toLowerCase());
        }

        if (isLocal) {
            if (dev.edtrick.nickhider.network.NetworkLevelManager.isEnabled()) {
                text = dev.edtrick.nickhider.network.NetworkLevelManager.replaceLevelInNametag(text);
            }

            if (BedwarsLevelManager.isEnabled() && BedwarsLevelManager.isBedwarsActive()) {
                text = BedwarsLevelManager.replaceTagInString(text);
            }
        }

        return NickHiderMod.filterNickOnly(text);
    }
}
