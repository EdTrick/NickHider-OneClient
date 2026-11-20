package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.polyfrost.oneconfig.internal.ui.shell.ShellSession", remap = false)
public class MixinShellSession {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void nickhider_fakePlayerName(CallbackInfoReturnable<String> cir) {
        String fake = NickHiderMod.getFakeNick();
        if (fake != null && !fake.trim().isEmpty()) {
            cir.setReturnValue(NickHiderMod.stripColorCodes(fake));
        }
    }

    @ModifyVariable(method = "updatePlayerName", at = @At("HEAD"), argsOnly = true, remap = false, require = 0)
    private String nickhider_modifyUpdatePlayerName(String name) {
        String fake = NickHiderMod.getFakeNick();
        if (fake != null && !fake.trim().isEmpty()) {
            return NickHiderMod.stripColorCodes(fake);
        }
        return name;
    }
}
