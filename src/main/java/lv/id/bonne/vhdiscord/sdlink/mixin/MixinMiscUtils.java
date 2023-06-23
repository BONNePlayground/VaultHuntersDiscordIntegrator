//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.sdlink.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import iskallia.vault.util.MiscUtils;
import me.hypherionmc.sdlink.server.ServerEvents;
import me.hypherionmc.sdlinklib.config.ConfigController;
import me.hypherionmc.sdlinklib.discord.DiscordMessage;
import me.hypherionmc.sdlinklib.discord.messages.MessageAuthor;
import me.hypherionmc.sdlinklib.discord.messages.MessageType;
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
        if (ServerEvents.getInstance().getBotEngine() == null)
        {
            // Bot engine is not setup. Do nothing.
            return;
        }

        if (!ConfigController.modConfig.generalConfig.enabled)
        {
            // Config is disabled. Do nothing.
            return;
        }

        DiscordMessage discordMessage = new DiscordMessage.Builder(ServerEvents.getInstance().getBotEngine(), MessageType.JOIN_LEAVE).
            withMessage(message.getString()).
            withAuthor(MessageAuthor.SERVER).
            build();
        discordMessage.sendMessage();
    }
}
