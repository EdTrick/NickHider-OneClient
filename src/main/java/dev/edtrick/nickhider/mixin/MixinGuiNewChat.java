package dev.edtrick.nickhider.mixin;

import dev.edtrick.nickhider.NickHiderMod;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {

    private static final Pattern NICK_PAT = Pattern.compile(
            "(?i)(?:nicked as|nickname is(?: now)?|nick(?:name)?\\s*:\\s*|known as|name (?:has been )?changed to)\\s*[:\"'\\s]*(?:\\[[^\\]]+\\]\\s*)?([A-Za-z0-9_]{3,16})"
    );
    private static final Pattern UNNICK_PAT = Pattern.compile(
            "(?i)(?:no longer nicked|nick(?:name)? (?:has been )?(?:reset|removed|cleared)|removed your nick)"
    );

    @ModifyVariable(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IChatComponent nickhider_modifyChatMessage(IChatComponent message) {
        if (message != null) {
            String unformatted = message.getUnformattedText();

            // 1. Check unnick notification
            if (UNNICK_PAT.matcher(unformatted).find()) {
                NickHiderMod.setDetectedChatNick(null);
            } else {
                // 2. Check official Hypixel nick notification
                Matcher m = NICK_PAT.matcher(unformatted);
                if (m.find()) {
                    NickHiderMod.setDetectedChatNick(m.group(1));
                }
            }

            message = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterChat(message);
            message = dev.edtrick.nickhider.network.NetworkLevelManager.filterChat(message);
        }
        return NickHiderMod.filterText(message);
    }
}
