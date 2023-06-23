//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.sdlink.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;


import me.hypherionmc.sdlink.server.ServerEvents;
import me.hypherionmc.sdlinklib.config.ConfigController;
import me.hypherionmc.sdlinklib.discord.DiscordMessage;
import me.hypherionmc.sdlinklib.discord.messages.MessageAuthor;
import me.hypherionmc.sdlinklib.discord.messages.MessageType;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.server.ServerLifecycleHooks;


/**
 * Adds missing messages when players enters/exits vault
 */
@Mixin(PlayerList.class)
public class MixinPlayerVaultInteraction
{
    @Inject(method = "broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V", at = @At("HEAD"))
    private void broadcastMessageToDiscord(Component p_11265_, ChatType p_11266_, UUID p_11267_, CallbackInfo ci)
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

        if (p_11266_ != ChatType.CHAT)
        {
            // VaultHunters posts messages with ChatType.CHAT
            return;
        }

        String text = p_11265_.getString();

        final ServerPlayer p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(p_11267_);

        if (p == null || !text.startsWith(p.getName().getString()))
        {
            // If player is not known or message does not start with player name, do nothing.
            return;
        }

        // These are currently known messages when player enters/exits vault.

        if (text.endsWith("opened a Vault!") ||
            text.endsWith("opened the Final Vault!") ||
            (text.contains(" entered a ") && text.endsWith("Vault!")) ||
            (text.contains(" entered an ") && text.endsWith("Vault!")) ||
            (text.contains(" defeated ") && text.endsWith("!")) ||
            text.endsWith(" was defeated.") ||
            text.endsWith(" survived.") ||
            text.endsWith(" completed the Vault!"))
        {
            DiscordMessage discordMessage = new DiscordMessage.Builder(ServerEvents.getInstance().getBotEngine(), MessageType.JOIN_LEAVE).
                withMessage(text.replaceFirst(p.getName().getString(), "**" + p.getName().getString() + "**")).
                withAuthor(MessageAuthor.SERVER).
                build();
            discordMessage.sendMessage();
        }
    }
}
