//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.dcintegration.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import iskallia.vault.util.MiscUtils;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.minecraft.network.chat.Component;


/**
 * This catches message from MiscUtils.broadcast method and resends it to the discord server.
 */
@Mixin(MiscUtils.class)
public class MixinMiscUtils
{
    /**
     * This method sends the VaultHunters MiscUtils.broadcast message to the discord server.
     * That method is called when player unlocks some new gear type, or discovers modifier.
     * @param message Message that was sent to players
     * @param ci Callback info
     */
    @Inject(method = "broadcast(Lnet/minecraft/network/chat/Component;)V", at = @At("RETURN"))
    private static void broadcastMessageToDiscord(Component message, CallbackInfo ci)
    {
        if (Variables.discord_instance != null)
        {
            MessageChannel channel =
                Variables.discord_instance.getChannel(Configuration.instance().advanced.serverChannelID);

            if (channel == null)
            {
                // Channel is not setup. Do nothing.
                return;
            }

            // Send message to the output channel
            Variables.discord_instance.sendMessage(new DiscordMessage(message.getString()), channel);
        }
    }
}
