//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.dcintegration.mixin;


import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import iskallia.vault.util.MiscUtils;
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
    @Inject(method = "broadcast(Lnet/minecraft/network/chat/Component;)V", at = @At("RETURN"), remap = false)
    private static void broadcastMessageToDiscord(Component message, CallbackInfo ci)
    {
        if (DiscordIntegration.INSTANCE != null)
        {
            // Send message to the output channel
            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(message.getString()));
        }
    }
}
