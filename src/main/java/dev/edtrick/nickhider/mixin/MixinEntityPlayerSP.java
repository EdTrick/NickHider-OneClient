package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.command.NickHiderCommand;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void nickhider_sendChatMessage(String message, CallbackInfo ci) {
        if (message == null) return;
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("/nickhider") || lower.equals("nickhider")
                || lower.startsWith("nickhider ")) {
            if (NickHiderCommand.handle(trimmed)) {
                ci.cancel();
            }
        }
    }
}
