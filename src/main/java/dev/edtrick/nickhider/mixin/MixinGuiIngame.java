package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces real IGN occurrences in titles, subtitles and actionbar messages.
 */
@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @ModifyVariable(method = "displayTitle", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String nickhider_modifyTitle(String title) {
        return NickHiderMod.filterString(title);
    }

    @ModifyVariable(method = "displayTitle", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private String nickhider_modifySubtitle(String subTitle) {
        return NickHiderMod.filterString(subTitle);
    }

    @ModifyVariable(method = "setRecordPlaying(Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String nickhider_modifyRecordPlayingString(String message) {
        return NickHiderMod.filterString(message);
    }

    @ModifyVariable(method = "setRecordPlaying(Lnet/minecraft/util/IChatComponent;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IChatComponent nickhider_modifyRecordPlayingComponent(IChatComponent message) {
        return NickHiderMod.filterText(message);
    }
}
