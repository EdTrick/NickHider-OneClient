package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import dev.edtrick.nickhider.network.NetworkLevelManager;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(value = ScorePlayerTeam.class, priority = 999999)
public class MixinScorePlayerTeam {

    @Inject(method = "formatPlayerName", at = @At("RETURN"), cancellable = true)
    private static void nickhider_formatPlayerName(Team teamIn, String playersName, CallbackInfoReturnable<String> cir) {
        String ret = cir.getReturnValue();
        if (ret != null) {
            boolean isLocal = NickHiderMod.isLocalPlayerName(playersName);
            ret = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterTeamPlayerName(ret, isLocal);
            if (NetworkLevelManager.isEnabled()) {
                ret = NetworkLevelManager.filterScoreboardLine(ret);
            }
            ret = NickHiderMod.filterNickOnly(ret);
            if (ret != null && !ret.equals(cir.getReturnValue())) {
                cir.setReturnValue(ret);
            }
        }
    }

    @Inject(method = "getColorSuffix", at = @At("RETURN"), cancellable = true)
    private void nickhider_getColorSuffix(CallbackInfoReturnable<String> cir) {
        if (!NetworkLevelManager.isEnabled()) return;
        String suffix = cir.getReturnValue();
        if (suffix == null || suffix.isEmpty()) return;

        ScorePlayerTeam team = (ScorePlayerTeam) (Object) this;
        String prefix = team.getColorPrefix();
        if (prefix != null) {
            String lower = NickHiderMod.stripColorCodes(prefix).toLowerCase();
            if ((lower.contains("hypixel") || lower.contains("network")) && lower.contains("level")) {
                Matcher m = Pattern.compile("(?i)((?:§[0-9a-fk-orA-FK-OR]|\\s)*)[0-9]{1,5}").matcher(suffix);
                if (m.find()) {
                    String replaced = m.replaceFirst(Matcher.quoteReplacement(m.group(1) + NetworkLevelManager.getFakeLevel()));
                    cir.setReturnValue(replaced);
                }
            }
        }
    }

    @Inject(method = "getColorPrefix", at = @At("RETURN"), cancellable = true)
    private void nickhider_getColorPrefix(CallbackInfoReturnable<String> cir) {
        if (!NetworkLevelManager.isEnabled()) return;
        String prefix = cir.getReturnValue();
        if (prefix == null || prefix.isEmpty()) return;

        String lower = NickHiderMod.stripColorCodes(prefix).toLowerCase();
        if ((lower.contains("hypixel") || lower.contains("network")) && lower.contains("level")) {
            String filtered = NetworkLevelManager.filterScoreboardLine(prefix);
            if (filtered != null && !filtered.equals(prefix)) {
                cir.setReturnValue(filtered);
            }
        }
    }
}
