package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.network.NetworkLevelManager;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Intercepts text rendered on screen via FontRenderer, ensuring scoreboard lines,
 * nametags, and GUI elements displaying Hypixel Network Level are consistently spoofed
 * regardless of which mod or HUD element renders them.
 */
@Mixin(FontRenderer.class)
public class MixinFontRenderer {

    @ModifyVariable(method = "renderString", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String nickhider_modifyRenderString(String text) {
        if (text == null) return null;
        if (NetworkLevelManager.isEnabled()) {
            text = NetworkLevelManager.filterScoreboardLine(text);
        }
        return text;
    }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String nickhider_modifyGetStringWidth(String text) {
        if (text == null) return null;
        if (NetworkLevelManager.isEnabled()) {
            text = NetworkLevelManager.filterScoreboardLine(text);
        }
        return text;
    }
}
