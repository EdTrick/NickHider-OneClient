package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void nickhider_getDisplayName(CallbackInfoReturnable<IChatComponent> cir) {
        IChatComponent comp = cir.getReturnValue();
        if (comp != null) {
            EntityPlayer self = (EntityPlayer) (Object) this;
            if (NickHiderMod.isLocalPlayerEntity(self) || NickHiderMod.containsLocalPlayerName(comp.getFormattedText())) {
                cir.setReturnValue(NickHiderMod.filterText(comp));
            }
        }
    }
}
