package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import dev.edtrick.nickhider.bedwars.BedwarsLevelManager;
import dev.edtrick.nickhider.network.NetworkLevelManager;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces real IGN occurrences and Hypixel Network Level in item and GUI tooltips,
 * strictly scoping Network Level spoofing to the local player's own profile/items.
 */
@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    @ModifyVariable(method = "drawHoveringText", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<String> nickhider_modifyHoveringText(List<String> textLines) {
        if (textLines == null || textLines.isEmpty()) return textLines;

        boolean isLocalTooltip = NetworkLevelManager.isLocalPlayerTooltip(textLines);

        List<String> result = new ArrayList<>(textLines.size());
        for (String line : textLines) {
            String processed = line;
            if (isLocalTooltip && NetworkLevelManager.isEnabled()) {
                processed = NetworkLevelManager.filterGeneralString(processed);
            }
            if (BedwarsLevelManager.isEnabled()) {
                processed = BedwarsLevelManager.filterGeneralString(processed);
            }
            processed = NickHiderMod.filterNickOnly(processed);
            result.add(processed);
        }
        return result;
    }
}
