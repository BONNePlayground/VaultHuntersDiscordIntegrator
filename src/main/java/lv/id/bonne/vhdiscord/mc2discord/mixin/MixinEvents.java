//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.mc2discord.mixin;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import javax.annotation.Nullable;
import lv.id.bonne.vhdiscord.parser.VaultItemsHandler;
import ml.denisd3d.mc2discord.core.DiscordLogging;
import ml.denisd3d.mc2discord.core.M2DUtils;
import ml.denisd3d.mc2discord.core.Mc2Discord;
import ml.denisd3d.mc2discord.core.entities.Entity;
import ml.denisd3d.mc2discord.core.entities.Message;
import ml.denisd3d.mc2discord.core.entities.Player;
import ml.denisd3d.mc2discord.core.events.MinecraftEvents;
import ml.denisd3d.mc2discord.forge.Events;
import ml.denisd3d.mc2discord.repack.discord4j.common.util.Snowflake;
import ml.denisd3d.mc2discord.repack.discord4j.core.object.entity.channel.MessageChannel;
import ml.denisd3d.mc2discord.repack.discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import ml.denisd3d.mc2discord.repack.discord4j.core.spec.*;
import ml.denisd3d.mc2discord.repack.discord4j.rest.util.AllowedMentions;
import ml.denisd3d.mc2discord.repack.reactor.core.publisher.Mono;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;


/**
 * This class takes over ForgeMessageUtils class public static method genItemStackEmbedIfAvailable and
 * rewrite it, so it could handle VaultHunters items.
 */
@Mixin(Events.class)
public class MixinEvents
{
    /**
     * The Forge Item registry.
     */
    private static final IForgeRegistry<Item> ITEM_REGISTRY = ForgeRegistries.ITEMS;


    /**
     * @author BONNe
     * @reason Implements custom VH Item parsing.
     */
    @SubscribeEvent
    @Overwrite(remap = false)
    public static void onMinecraftChatMessageEvent(ServerChatEvent event)
    {
        boolean messageParsed;

        final net.minecraft.network.chat.Component msg = event.getComponent();
        final JsonObject json = JsonParser.parseString(Component.Serializer.toJson(msg)).getAsJsonObject();

        Player player = new Player(event.getPlayer().getGameProfile().getName(),
            event.getPlayer().getDisplayName().getString(),
            event.getPlayer().getGameProfile().getId());

        if (json.has("with") && json.get("with").isJsonArray())
        {
            messageParsed = MixinEvents.searchAndParseArray(player, json.getAsJsonArray("with"));
        }
        else if (json.has("extra") && json.get("extra").isJsonArray())
        {
            messageParsed = MixinEvents.searchAndParseArray(player, json.getAsJsonArray("extra"));
        }
        else
        {
            messageParsed = false;
        }

        if (!messageParsed)
        {
            MinecraftEvents.onMinecraftChatMessageEvent(event.getMessage(), player);
        }
    }


    /**
     * This method search for "show_item" hoverEvent and crafts MessageEmbed for VaultHunters items
     * from it.
     * @param player Player who sends the chat message.
     * @param array of json objects.
     * @return MessageEmbed text for item, or null.
     */
    private static boolean searchAndParseArray(Player player, JsonArray array)
    {
        for (JsonElement object : array)
        {
            if (object instanceof JsonObject singleElement)
            {
                if (singleElement.has("hoverEvent"))
                {
                    final JsonObject hoverEvent = singleElement.getAsJsonObject("hoverEvent");

                    if (hoverEvent.has("action") &&
                        hoverEvent.get("action").getAsString().equals("show_item") &&
                        hoverEvent.has("contents"))
                    {
                        if (hoverEvent.getAsJsonObject("contents").has("tag"))
                        {
                            return MixinEvents.parseAndSendMessage(player,
                                hoverEvent.getAsJsonObject("contents").getAsJsonObject());
                        }
                    }
                }
            }
        }

        return false;
    }


    /**
     * This method parses given item json data and sends chat message if parsing was successful.
     * @param itemJson item json data.
     * @param player Player who sends the chat message.
     * @return {@code true} if parsing was successful, {@code false} otherwise.
     */
    private static boolean parseAndSendMessage(Player player, JsonObject itemJson)
    {
        try
        {
            ItemStack itemStack = new ItemStack(ITEM_REGISTRY.getValue(
                new ResourceLocation(itemJson.get("id").getAsString())));

            if (itemJson.has("tag"))
            {
                CompoundTag tag = (CompoundTag) NbtTagArgument.nbtTag().parse(
                    new StringReader(itemJson.get("tag").getAsString()));
                itemStack.setTag(tag);
            }

            CompoundTag itemTag = itemStack.getOrCreateTag();

            // Here we hook into Vault Hunters items.

            if (itemJson.get("id").getAsString().startsWith("the_vault"))
            {
                String description = VaultItemsHandler.generateVaultHuntersItemTooltips(itemJson,
                    itemStack,
                    itemTag);

                if (description != null && !description.isBlank())
                {
                    MixinEvents.craftVaultHuntersItemMessage(player, itemStack, description);
                    return true;
                }
            }
        }
        catch (CommandSyntaxException ignored)
        {
        }

        return false;
    }


    /**
     * This message sends given item description in the chat.
     * @param player Player who sends chat message.
     * @param itemStack ItemStack item.
     * @param description the description of message.
     */
    private static void craftVaultHuntersItemMessage(Player player,
        ItemStack itemStack,
        final String description)
    {
        if (Mc2Discord.INSTANCE == null ||
            Mc2Discord.INSTANCE.messageManager == null ||
            description == null ||
            description.isBlank())
        {
            return;
        }

        String itemName = itemStack.hasCustomHoverName() ?
            itemStack.getDisplayName().getContents() :
            new TranslatableComponent(itemStack.getItem().getDescriptionId()).getContents();

        String title = itemName.isBlank() ?
            itemStack.getHoverName().getString() :
            itemName;

        String footer = Objects.requireNonNull(itemStack.getItem().getRegistryName()).toString();

        Mc2Discord.INSTANCE.config.channels.channels.forEach(channel -> {

            if (channel.subscriptions.contains("chat"))
            {
                switch (channel.mode)
                {
                    case WEBHOOK ->
                    {
                        EmbedCreateSpec embedSpec = EmbedCreateSpec.builder().
                            title(title).
                            description(description).
                            footer(footer, null).
                            build();

                        MixinEvents.sendWebhookMessage(channel.channel_id,
                            player.displayName,
                            Entity.replace(Mc2Discord.INSTANCE.config.style.avatar_api, Collections.singletonList(player)),
                            embedSpec);
                    }
                    case PLAIN_TEXT ->
                    {
                        String embedDescription = "\n**" + title + "**" + "\n" + description;

                        MixinEvents.sendChannelMessage(channel.channel_id,
                            Entity.replace(Mc2Discord.INSTANCE.config.style.discord_chat_format,
                                Arrays.asList(player, new Message(embedDescription))));
                    }
                    case EMBED ->
                    {
                        String embedDescription =  "\n**" + title + "**" + "\n" + description;

                        MixinEvents.sendEmbedMessage(channel.channel_id,
                            embedDescription,
                            player.displayName,
                            Entity.replace(Mc2Discord.INSTANCE.config.style.avatar_api,
                                Collections.singletonList(player)));
                    }
                }
            }
        });
    }


    /**
     * This is a clone of {@see MessageManager} method send webhook message.
     * @param channelId Channel ID that should receive the message.
     * @param username Username who send chat message.
     * @param avatarUrl User avatar url.
     * @param embed Embed of the message.
     */
    private static void sendWebhookMessage(long channelId,
        String username,
        String avatarUrl,
        @Nullable EmbedCreateSpec embed)
    {
        Mono<TopLevelGuildMessageChannel> channelMono =
            Mc2Discord.INSTANCE.client.getChannelById(Snowflake.of(channelId)).
                ofType(TopLevelGuildMessageChannel.class);

        channelMono.flatMapMany(TopLevelGuildMessageChannel::getWebhooks).
            filter((webhook) -> webhook.getName().filter(
                (s) -> s.equals("Mc2Dis Webhook - " + Mc2Discord.INSTANCE.botName + "#" + Mc2Discord.INSTANCE.botDiscriminator)).isPresent()).
            switchIfEmpty(Mono.defer(() ->
                channelMono.flatMap((textChannel) ->
                    textChannel.createWebhook(WebhookCreateSpec.builder().
                        name("Mc2Dis Webhook - " + Mc2Discord.INSTANCE.botName + "#" + Mc2Discord.INSTANCE.botDiscriminator).build())))).
            next().subscribe((webhook) ->
            {
                M2DUtils.breakStringToLines("", 2000, true).forEach((s) ->
                {
                    WebhookExecuteSpec.Builder builder =
                        WebhookExecuteSpec.builder().username(username).avatarUrl(avatarUrl).allowedMentions(
                            AllowedMentions.builder().parseType(Mc2Discord.INSTANCE.config.misc.allowed_mention.stream().
                                    map(AllowedMentions.Type::valueOf).
                                    toArray(AllowedMentions.Type[]::new)).
                                build());

                    if (!s.equals(""))
                    {
                        builder.content(s);
                    }

                    if (embed != null)
                    {
                        builder.addEmbed(embed);
                    }

                    Mono<Void> webHook = webhook.execute(builder.build());
                    Logger logger = Mc2Discord.logger;

                    webHook.doOnError(logger::error).subscribe(
                        (unused) -> {},
                        (throwable) -> DiscordLogging.logs = "Cannot send webhook message.",
                        null);
                });
            });
    }


    /**
     * This is a clone of {@see MessageManager} method send plain-text message.
     * @param channelId Channel ID that should receive the message.
     * @param content Content of chat message.
     */
    private static void sendChannelMessage(long channelId, String content)
    {
        Mc2Discord.INSTANCE.client.getChannelById(Snowflake.of(channelId)).ofType(MessageChannel.class)
            .subscribe((textChannel) ->
            {
                M2DUtils.breakStringToLines(content, 2000, false).forEach((s) ->
                {
                    MessageCreateSpec.Builder builder =
                        MessageCreateSpec.builder().allowedMentions(AllowedMentions.builder().
                            parseType(Mc2Discord.INSTANCE.config.misc.allowed_mention.stream().
                                map(AllowedMentions.Type::valueOf).
                                toArray(AllowedMentions.Type[]::new)).
                            build());

                    if (!s.equals(""))
                    {
                        builder.content(s);
                    }

                    Mono mono = textChannel.createMessage(builder.build());
                    Logger logger = Mc2Discord.logger;

                    mono.doOnError(logger::error).subscribe(
                        (unused) -> {},
                        (throwable) -> DiscordLogging.logs = "Cannot send chat message.",
                        null);
                });
            });
    }


    /**
     * This is a clone of {@see MessageManager} method send embed message.
     * @param channelId Channel ID that should receive the message.
     * @param username Username who send chat message.
     * @param avatarUrl User avatar url.
     * @param message Content of the message.
     */
    private static void sendEmbedMessage(long channelId,
        String message,
        String username,
        String avatarUrl)
    {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();

        builder.color(M2DUtils.getColorFromString(Mc2Discord.INSTANCE.config.style.embed_color_chat));

        if (!username.equals(Mc2Discord.INSTANCE.botDisplayName) || Mc2Discord.INSTANCE.config.style.embed_show_server_avatar) {
            builder.author(username, null, avatarUrl);
        }

        Mc2Discord.INSTANCE.client.getChannelById(Snowflake.of(channelId)).ofType(MessageChannel.class).subscribe((textChannel) -> {
            M2DUtils.breakStringToLines(message, 2000, false).forEach((s) -> {
                MessageCreateSpec.Builder builder1 = MessageCreateSpec.builder();
                if (!s.equals("")) {
                    builder1.addEmbed(builder.description(s).build());
                }

                Mono mono = textChannel.createMessage(builder1.build());
                Logger logger = Mc2Discord.logger;

                mono.doOnError(logger::error).subscribe(
                    (unused) -> {},
                    (throwable) -> DiscordLogging.logs = "Cannot send embed message.",
                    null);
            });
        });
    }
}
