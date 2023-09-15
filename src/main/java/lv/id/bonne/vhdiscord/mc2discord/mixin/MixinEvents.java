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
import fr.denisd3d.mc2discord.core.M2DUtils;
import fr.denisd3d.mc2discord.core.Mc2Discord;
import fr.denisd3d.mc2discord.core.MessageManager;
import fr.denisd3d.mc2discord.core.entities.Entity;
import fr.denisd3d.mc2discord.core.entities.MessageEntity;
import fr.denisd3d.mc2discord.core.entities.PlayerEntity;
import fr.denisd3d.mc2discord.forge.ForgeEvents;
import fr.denisd3d.mc2discord.shadow.discord4j.core.spec.EmbedCreateSpec;
import fr.denisd3d.mc2discord.shadow.discord4j.discordjson.possible.Possible;
import fr.denisd3d.mc2discord.shadow.reactor.core.publisher.Flux;
import lv.id.bonne.vhdiscord.parser.VaultItemsHandler;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * This class takes over ForgeMessageUtils class public static method genItemStackEmbedIfAvailable and
 * rewrite it, so it could handle VaultHunters items.
 */
@Mixin(ForgeEvents.class)
public class MixinEvents {
    /**
     * The Forge Item registry.
     */
    @Unique
    private static final IForgeRegistry<Item> vhdiscord_ITEM_REGISTRY = ForgeRegistries.ITEMS;


    /**
     * @author BONNe
     * @reason Implements custom VH Item parsing.
     */
    @SubscribeEvent
    @Inject(method = "onServerChat(Lnet/minecraftforge/event/ServerChatEvent;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private static void onServerChat(ServerChatEvent event, CallbackInfo ci) {
        if (event.isCanceled()) return;

        boolean messageParsed = false;

        final Component msg = event.getComponent();
        final JsonObject json = JsonParser.parseString(Component.Serializer.toJson(msg)).getAsJsonObject();

        PlayerEntity player = new PlayerEntity(event.getPlayer().getGameProfile().getName(), event.getPlayer().getDisplayName().getString(), event.getPlayer().getGameProfile().getId());

        if (json.has("with") && json.get("with").isJsonArray()) {
            messageParsed = MixinEvents.vaultHuntersDiscordIntegrator$searchAndParseArray(player, json.getAsJsonArray("with"));
        } else if (json.has("extra") && json.get("extra").isJsonArray()) {
            messageParsed = MixinEvents.vaultHuntersDiscordIntegrator$searchAndParseArray(player, json.getAsJsonArray("extra"));
        }

        if (messageParsed) ci.cancel(); // Prevent Mc2Discord from sending message again.
    }


    /**
     * This method search for "show_item" hoverEvent and crafts MessageEmbed for VaultHunters items
     * from it.
     *
     * @param player Player who sends the chat message.
     * @param array  of json objects.
     * @return MessageEmbed text for item, or null.
     */
    @Unique
    private static boolean vaultHuntersDiscordIntegrator$searchAndParseArray(PlayerEntity player, JsonArray array) {
        for (JsonElement object : array) {
            if (!(object instanceof JsonObject singleElement)) continue;

            if (!singleElement.has("hoverEvent")) continue;

            final JsonObject hoverEvent = singleElement.getAsJsonObject("hoverEvent");

            if (!hoverEvent.has("action") || !hoverEvent.get("action").getAsString().equals("show_item") || !hoverEvent.has("contents"))
                continue;

            if (!hoverEvent.getAsJsonObject("contents").has("tag")) continue;

            return MixinEvents.vaultHuntersDiscordIntegrator$parseAndSendMessage(player, hoverEvent.getAsJsonObject("contents").getAsJsonObject());
        }

        return false;
    }


    /**
     * This method parses given item json data and sends chat message if parsing was successful.
     *
     * @param itemJson item json data.
     * @param player   Player who sends the chat message.
     * @return {@code true} if parsing was successful, {@code false} otherwise.
     */
    @Unique
    private static boolean vaultHuntersDiscordIntegrator$parseAndSendMessage(PlayerEntity player, JsonObject itemJson) {
        try {
            ItemStack itemStack = new ItemStack(vhdiscord_ITEM_REGISTRY.getValue(new ResourceLocation(itemJson.get("id").getAsString())));

            if (itemJson.has("tag")) {
                CompoundTag tag = (CompoundTag) NbtTagArgument.nbtTag().parse(new StringReader(itemJson.get("tag").getAsString()));
                itemStack.setTag(tag);
            }

            CompoundTag itemTag = itemStack.getOrCreateTag();

            // Here we hook into Vault Hunters items.

            if (itemJson.get("id").getAsString().startsWith("the_vault")) {
                String description = VaultItemsHandler.generateVaultHuntersItemTooltips(itemJson, itemStack, itemTag);

                if (description != null && !description.isBlank()) {
                    MixinEvents.vaultHuntersDiscordIntegrator$craftVaultHuntersItemMessage(player, itemStack, description);
                    return true;
                }
            }
        } catch (CommandSyntaxException ignored) {
        }

        return false;
    }


    /**
     * This message sends given item description in the chat.
     *
     * @param player      Player who sends chat message.
     * @param itemStack   ItemStack item.
     * @param description the description of message.
     */
    @Unique
    private static void vaultHuntersDiscordIntegrator$craftVaultHuntersItemMessage(PlayerEntity player, ItemStack itemStack, final String description) {
        if (M2DUtils.isNotConfigured() || description == null || description.isBlank()) {
            return;
        }

        String itemName = itemStack.hasCustomHoverName() ? itemStack.getDisplayName().getContents() : new TranslatableComponent(itemStack.getItem().getDescriptionId()).getContents();

        String title = itemName.isBlank() ? itemStack.getHoverName().getString() : itemName;

        String footer = Objects.requireNonNull(itemStack.getItem().getRegistryName()).toString();

        List<String> types = List.of("chat", "vault_hunter_discord_integration");

        Flux.fromIterable(MessageManager.getMatchingChannels(types))
                .flatMap(channel -> switch (channel.mode) {
                    case WEBHOOK -> {
                        EmbedCreateSpec embedSpec = EmbedCreateSpec.builder().title(title).description(description).footer(footer, null).build();
                        yield MessageManager.createWebhookMessage(channel.channel_id,
                                "",
                                Possible.of(player.displayName),
                                Possible.of(Entity.replace(Mc2Discord.INSTANCE.config.style.webhook_avatar_api, List.of(player))),
                                false,
                                Collections.singleton(embedSpec),
                                null);
                    }
                    case PLAIN_TEXT -> {
                        String embedDescription = "\n**" + title + "**" + "\n" + description;

                        yield MessageManager.createPlainTextMessage(channel.channel_id,
                                Entity.replace(Mc2Discord.INSTANCE.config.style.discord_chat_format, Arrays.asList(player, new MessageEntity(embedDescription))),
                                Possible.of(player.displayName),
                                false);
                    }
                    case EMBED -> {
                        String embedDescription = "\n**" + title + "**" + "\n" + description;

                        yield MessageManager.createEmbedMessage(channel.channel_id,
                                embedDescription,
                                Possible.of(player.displayName),
                                Possible.of(Entity.replace(Mc2Discord.INSTANCE.config.style.webhook_avatar_api, List.of(player))),
                                types);
                    }
                }).subscribe();
    }
}
