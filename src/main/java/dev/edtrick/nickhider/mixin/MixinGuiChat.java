package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.command.NickHiderCommand;
import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides client-side Tab autocompletion for /nickhider and subcommands.
 */
@Mixin(GuiChat.class)
public abstract class MixinGuiChat {

    @Shadow
    public abstract void onAutocompleteResponse(String[] matches);

    @Inject(method = "sendAutocompleteRequest", at = @At("HEAD"), cancellable = true)
    private void nickhider_onSendAutocomplete(String leftOfCursor, String fullText, CallbackInfo ci) {
        if (leftOfCursor == null) return;
        String[] completions = NickHiderCommand.getTabCompletions(leftOfCursor);
        if (completions != null && completions.length > 0) {
            this.onAutocompleteResponse(completions);
            ci.cancel();
        }
    }
}
