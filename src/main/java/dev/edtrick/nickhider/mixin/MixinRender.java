package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces real IGN occurrences in entity nametags and ArmorStand holograms.
 */
@Mixin(Render.class)
public class MixinRender {

    @ModifyVariable(method = "renderLivingLabel", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String nickhider_modifyLabelText(String text, Entity entityIn) {
        if (text == null) return null;
        return NickHiderMod.filterString(text);
    }
}
