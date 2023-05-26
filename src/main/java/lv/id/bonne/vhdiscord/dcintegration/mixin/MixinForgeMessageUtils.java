//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.dcintegration.mixin;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Arrays;
import java.util.Objects;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.forge.util.ForgeMessageUtils;
import lv.id.bonne.vhdiscord.parser.VaultItemsHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;


/**
 * This class takes over ForgeMessageUtils class public static method genItemStackEmbedIfAvailable and
 * rewrite it, so it could handle VaultHunters items.
 */
@Mixin(ForgeMessageUtils.class)
public class MixinForgeMessageUtils
{
    /**
     * @author BONNe
     * @reason Implements custom VH Item parsing.
     */
    @Overwrite(remap = false)
    public static MessageEmbed genItemStackEmbedIfAvailable(final Component component)
    {
        if (!Configuration.instance().forgeSpecific.sendItemInfo)
        {
            return null;
        }

        final JsonObject json = JsonParser.parseString(Component.Serializer.toJson(component)).getAsJsonObject();

        if (json.has("with") && json.get("with").isJsonArray())
        {
            // This is how quarks shares items in chat.
            return MixinForgeMessageUtils.searchAndParseArray(json.getAsJsonArray("with"));
        }
        else if (json.has("extra") && json.get("extra").isJsonArray())
        {
            // This is how FTBRank shares items in chat.
            return MixinForgeMessageUtils.searchAndParseArray(json.getAsJsonArray("extra"));
        }
        else
        {
            return null;
        }
    }


    /**
     * This method search for "show_item" hoverEvent and crafts MessageEmbed for VaultHunters items
     * from it.
     * @param array of json objects.
     * @return MessageEmbed text for item, or null.
     */
    private static MessageEmbed searchAndParseArray(JsonArray array)
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
                            return MixinForgeMessageUtils.parseJsonArgs(
                                hoverEvent.getAsJsonObject("contents").getAsJsonObject());
                        }
                    }
                }
            }
        }

        return null;
    }


    /**
     * This method parses given item json data.
     * @param itemJson item json data.
     * @return MessageEmbed text for item.
     */
    private static MessageEmbed parseJsonArgs(JsonObject itemJson)
    {
        try
        {
            ItemStack itemStack = new ItemStack(itemreg.getValue(new ResourceLocation(itemJson.get("id").getAsString())));

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
                return MixinForgeMessageUtils.craftVaultHuntersItemMessage(itemJson, itemStack, itemTag);
            }

            EmbedBuilder embedBuilder = new EmbedBuilder();

            // Here we continue to process vanilla items.

            String title = itemStack.hasCustomHoverName() ?
                itemStack.getDisplayName().getContents() :
                new TranslatableComponent(itemStack.getItem().getDescriptionId()).getContents();

            ResourceLocation registryName = Objects.requireNonNull(itemStack.getItem().getRegistryName());

            if (title.isEmpty())
            {
                title = registryName.toString();
            }
            else
            {
                embedBuilder.setFooter(registryName.toString());
            }

            embedBuilder.setTitle(title);
            StringBuilder tooltip = new StringBuilder();

            // Enchantments, Modifiers, Unbreakable, CanDestroy, CanPlace, Other
            boolean[] flags = new boolean[6];
            // Set everything visible
            Arrays.fill(flags, false);

            if (itemTag.contains("HideFlags"))
            {
                final int input = (itemTag.getInt("HideFlags"));
                for (int i = 0; i < flags.length; i++)
                {
                    flags[i] = (input & (1 << i)) != 0;
                }
            }

            //Add Enchantments
            if (!flags[0])
            {
                //Implementing this code myself because the original is broken
                for (int i = 0; i < itemStack.getEnchantmentTags().size(); ++i)
                {
                    final CompoundTag compoundTag = itemStack.getEnchantmentTags().getCompound(i);
                    Registry.ENCHANTMENT.getOptional(ResourceLocation.tryParse(compoundTag.getString("id"))).
                        ifPresent((enchantment) ->
                        {
                            if (compoundTag.get("lvl") != null)
                            {
                                final int level;
                                if (compoundTag.get("lvl") instanceof StringTag)
                                {
                                    level = Integer.parseInt(compoundTag.getString("lvl").
                                        replace("s", ""));
                                }
                                else
                                    level = compoundTag.getInt("lvl") == 0 ?
                                        compoundTag.getShort("lvl") : compoundTag.getInt("lvl");
                                tooltip.append(ChatFormatting.stripFormatting(enchantment.getFullname(level).getString())).
                                    append("\n");
                            }
                        });
                }
            }

            //Add Lores
            final ListTag list = itemTag.getCompound("display").getList("Lore", 8);
            list.forEach((nbt) ->
            {
                try
                {
                    if (nbt instanceof StringTag)
                    {
                        final TextComponent comp =
                            (TextComponent) ComponentArgument.textComponent()
                                .parse(new StringReader(nbt.getAsString()));
                        tooltip.append("_").append(comp.getContents()).append("_\n");
                    }
                }
                catch (CommandSyntaxException e)
                {
                    e.printStackTrace();
                }
            });

            //Add 'Unbreakable' Tag
            if (!flags[2] && itemTag.contains("Unbreakable") &&
                itemTag.getBoolean("Unbreakable"))
                tooltip.append("Unbreakable\n");

            embedBuilder.setDescription(tooltip.toString());

            return embedBuilder.build();
        }
        catch (CommandSyntaxException ignored)
        {
            //Just go on and ignore it
        }

        return null;
    }


    /**
     * This message crafts item descriptions for Vault Hunters items.
     * @param itemJson Item JSON data.
     * @param itemStack ItemStack item.
     * @param itemTag Item Tag.
     * @return MessageEmbed for Vault item.
     */
    private static MessageEmbed craftVaultHuntersItemMessage(JsonObject itemJson,
        ItemStack itemStack,
        CompoundTag itemTag)
    {
        try
        {
            EmbedBuilder messageBuilder = new EmbedBuilder();
            messageBuilder.setTitle(itemStack.getHoverName().getString());

            String description = VaultItemsHandler.generateVaultHuntersItemTooltips(itemJson, itemStack, itemTag);

            if (description != null && !description.isBlank())
            {
                messageBuilder.appendDescription(description);
                return messageBuilder.build();
            }

            return null;
        }
        catch (Exception e)
        {
            // If I fail, then return nothing.
            return null;
        }
    }


    private static final IForgeRegistry<Item> itemreg = ForgeRegistries.ITEMS;
}
