package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces real IGN occurrences in skull item display names (e.g. "blutness's Head" -> "EdTrick's Head").
 */
@Mixin(ItemSkull.class)
public class MixinItemSkull {

    @Inject(method = "getItemStackDisplayName", at = @At("RETURN"), cancellable = true)
    private void nickhider_skullDisplayName(ItemStack stack, CallbackInfoReturnable<String> cir) {
        if (cir.getReturnValue() != null) {
            cir.setReturnValue(NickHiderMod.filterString(cir.getReturnValue()));
        }
    }
}
