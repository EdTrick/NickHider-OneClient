package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScorePlayerTeam.class)
public class MixinScorePlayerTeam {

    @Inject(method = "formatPlayerName", at = @At("RETURN"), cancellable = true)
    private static void nickhider_formatPlayerName(Team teamIn, String playersName, CallbackInfoReturnable<String> cir) {
        String ret = cir.getReturnValue();
        if (ret != null) {
            String filtered = NickHiderMod.filterString(ret);
            if (!ret.equals(filtered)) {
                cir.setReturnValue(filtered);
            }
        }
    }
}
