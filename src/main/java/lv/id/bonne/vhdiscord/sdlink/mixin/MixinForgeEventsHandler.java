//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.sdlink.mixin;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import lv.id.bonne.vhdiscord.parser.VaultItemsHandler;
import me.hypherionmc.sdlink.ForgeEventHandler;
import me.hypherionmc.sdlink.server.ServerEvents;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
@Mixin(ForgeEventHandler.class)
public class MixinForgeEventsHandler
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
    public void serverChatEvent(ServerChatEvent event)
    {
        boolean messageParsed;

        final Component msg = event.getComponent();
        final JsonObject json = JsonParser.parseString(Component.Serializer.toJson(msg)).getAsJsonObject();

        if (json.has("with") && json.get("with").isJsonArray())
        {
            messageParsed = MixinForgeEventsHandler.searchAndParseArray(event.getPlayer(), json.getAsJsonArray("with"));
        }
        else if (json.has("extra") && json.get("extra").isJsonArray())
        {
            messageParsed = MixinForgeEventsHandler.searchAndParseArray(event.getPlayer(), json.getAsJsonArray("extra"));
        }
        else
        {
            messageParsed = false;
        }

        if (!messageParsed)
        {
            ServerEvents.getInstance().onServerChatEvent(msg,
                event.getPlayer().getName(),
                event.getPlayer().getUUID().toString());
        }
    }


    /**
     * This method search for "show_item" hoverEvent and crafts MessageEmbed for VaultHunters items
     * from it.
     * @param player Player who sends the chat message.
     * @param array of json objects.
     * @return MessageEmbed text for item, or null.
     */
    private static boolean searchAndParseArray(ServerPlayer player, JsonArray array)
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
                            return MixinForgeEventsHandler.parseAndSendMessage(player,
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
    private static boolean parseAndSendMessage(ServerPlayer player, JsonObject itemJson)
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
                    MixinForgeEventsHandler.craftVaultHuntersItemMessage(player, itemStack, description);
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
    private static void craftVaultHuntersItemMessage(ServerPlayer player,
        ItemStack itemStack,
        final String description)
    {
        if (ServerEvents.getInstance() == null ||
            !ServerEvents.getInstance().getBotEngine().isBotReady() ||
            description == null ||
            description.isBlank())
        {
            return;
        }

        MutableComponent chatComponent = new TextComponent("\n" + description);
        ServerEvents.getInstance().onServerChatEvent(chatComponent, player.getName(), player.getUUID().toString());
    }
}
