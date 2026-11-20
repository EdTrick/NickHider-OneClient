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

    private static final Pattern NICK_PAT_1 = Pattern.compile("(?i)nicked as\\s+([A-Za-z0-9_]{1,16})");
    private static final Pattern NICK_PAT_2 = Pattern.compile("(?i)name (?:has been )?changed to\\s+([A-Za-z0-9_]{1,16})");
    private static final Pattern NICK_PAT_3 = Pattern.compile("(?i)known as\\s+([A-Za-z0-9_]{1,16})");
    private static final Pattern UNNICK_PAT = Pattern.compile("(?i)(?:no longer nicked|nick(?:name)? (?:has been )?reset)");

    @ModifyVariable(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IChatComponent nickhider_modifyChatMessage(IChatComponent message) {
        if (message != null) {
            String unformatted = message.getUnformattedText();
            if (UNNICK_PAT.matcher(unformatted).find()) {
                NickHiderMod.setDetectedChatNick(null);
            } else {
                Matcher m = NICK_PAT_1.matcher(unformatted);
                if (m.find()) {
                    NickHiderMod.setDetectedChatNick(m.group(1));
                } else {
                    m = NICK_PAT_2.matcher(unformatted);
                    if (m.find()) {
                        NickHiderMod.setDetectedChatNick(m.group(1));
                    } else {
                        m = NICK_PAT_3.matcher(unformatted);
                        if (m.find()) {
                            NickHiderMod.setDetectedChatNick(m.group(1));
                        }
                    }
                }
            }
        }
        return NickHiderMod.filterText(message);
    }
}
