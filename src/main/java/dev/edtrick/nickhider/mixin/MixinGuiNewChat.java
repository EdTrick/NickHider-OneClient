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
            "(?i)(?:no longer nicked|nick(?:name)? (?:has been )?reset|removed your nick)"
    );
    private static final Pattern REPLAY_TP_PAT = Pattern.compile(
            "(?i)Teleporting you to\\s+([A-Za-z0-9_]{3,16})"
    );

    private static long lastReplayLoadTime = 0;

    @ModifyVariable(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IChatComponent nickhider_modifyChatMessage(IChatComponent message) {
        if (message != null) {
            String unformatted = message.getUnformattedText();

            // Track replay loading messages
            if (unformatted.contains("Attempting to load replay") || unformatted.contains("Sending you to mini")) {
                lastReplayLoadTime = System.currentTimeMillis();
            }

            // 1. Check unnick notification
            if (UNNICK_PAT.matcher(unformatted).find()) {
                NickHiderMod.setDetectedChatNick(null);
            } else {
                // 2. Check nick notification
                Matcher m = NICK_PAT.matcher(unformatted);
                if (m.find()) {
                    NickHiderMod.setDetectedChatNick(m.group(1));
                } else {
                    // Check Hypixel replay auto-teleport message ("Teleporting you to <NAME>")
                    Matcher tp = REPLAY_TP_PAT.matcher(unformatted);
                    if (tp.find()) {
                        String target = tp.group(1);
                        String fake = NickHiderMod.getFakeNick();
                        String real = dev.edtrick.nickhider.command.NickHiderCommand.getRealName();
                        if (target != null && (fake == null || !target.equalsIgnoreCase(NickHiderMod.stripColorCodes(fake)))
                                && (real == null || !target.equalsIgnoreCase(real))) {
                            // Target is a valid player name in the replay!
                            NickHiderMod.setDetectedChatNick(target);
                        }
                    }
                }
            }

            // 3. Correlate with recently sent chat message
            String lastSent = NickHiderMod.getLastSentChatMessage();
            if (lastSent != null && (System.currentTimeMillis() - NickHiderMod.getLastSentChatTime() < 4000)) {
                int colonIdx = unformatted.lastIndexOf(": " + lastSent);
                if (colonIdx != -1) {
                    String beforeColon = unformatted.substring(0, colonIdx).trim();
                    int lastSpace = beforeColon.lastIndexOf(' ');
                    String possibleNick = (lastSpace != -1) ? beforeColon.substring(lastSpace + 1) : beforeColon;
                    possibleNick = possibleNick.replaceAll("[^A-Za-z0-9_]", "");
                    if (possibleNick.length() >= 3 && possibleNick.length() <= 16) {
                        NickHiderMod.setDetectedChatNick(possibleNick);
                    }
                }
            }

            message = dev.edtrick.nickhider.bedwars.BedwarsLevelManager.filterChat(message);
        }
        return NickHiderMod.filterText(message);
    }
}
