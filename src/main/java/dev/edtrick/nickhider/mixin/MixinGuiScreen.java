package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces real IGN occurrences in item and GUI tooltips.
 */
@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    @ModifyVariable(method = "drawHoveringText", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<String> nickhider_modifyHoveringText(List<String> textLines) {
        if (textLines == null || textLines.isEmpty()) return textLines;
        List<String> result = new ArrayList<>(textLines.size());
        for (String line : textLines) {
            result.add(NickHiderMod.filterString(line));
        }
        return result;
    }
}
