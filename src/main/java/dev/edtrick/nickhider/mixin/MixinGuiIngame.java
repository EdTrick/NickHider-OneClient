package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import dev.edtrick.nickhider.bedwars.BedwarsLevelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces real IGN occurrences in titles, subtitles, actionbar messages, and experience bar.
 */
@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method = "renderExpBar", at = @At("HEAD"))
    private void nickhider_renderExpBar(ScaledResolution scaledRes, int x, CallbackInfo ci) {
        if (BedwarsLevelManager.isEnabled() && BedwarsLevelManager.isBedwarsMatchActive()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.experienceLevel = BedwarsLevelManager.getFakeStars();
            }
        }
    }

    @ModifyVariable(method = "displayTitle", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String nickhider_modifyTitle(String title) {
        title = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterActionBarOrTitle(title);
        return NickHiderMod.filterString(title);
    }

    @ModifyVariable(method = "displayTitle", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private String nickhider_modifySubtitle(String subTitle) {
        subTitle = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterActionBarOrTitle(subTitle);
        return NickHiderMod.filterString(subTitle);
    }

    @ModifyVariable(method = "setRecordPlaying(Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String nickhider_modifyRecordPlayingString(String message) {
        message = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterActionBarOrTitle(message);
        return NickHiderMod.filterString(message);
    }

    @ModifyVariable(method = "setRecordPlaying(Lnet/minecraft/util/IChatComponent;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IChatComponent nickhider_modifyRecordPlayingComponent(IChatComponent message) {
        if (message != null) {
            message = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterChat(message);
        }
        return NickHiderMod.filterText(message);
    }
}
