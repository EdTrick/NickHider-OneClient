package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ScorePlayerTeam.class, priority = 999999)
public class MixinScorePlayerTeam {

    @Inject(method = "formatPlayerName", at = @At("RETURN"), cancellable = true)
    private static void nickhider_formatPlayerName(Team teamIn, String playersName, CallbackInfoReturnable<String> cir) {
        String ret = cir.getReturnValue();
        if (ret != null) {
            boolean isLocal = NickHiderMod.isLocalPlayerName(playersName);
            ret = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterTeamPlayerName(ret, isLocal);
            ret = NickHiderMod.filterString(ret);
            if (ret != null && !ret.equals(cir.getReturnValue())) {
                cir.setReturnValue(ret);
            }
        }
    }
}
