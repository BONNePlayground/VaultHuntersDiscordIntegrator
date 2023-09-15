//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.dcintegration.mixin;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;

import de.erdbeerbaerlp.dcintegration.forge.util.ForgeMessageUtils;
import lv.id.bonne.vhdiscord.parser.VaultItemsHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;


/**
 * This class takes over ForgeMessageUtils class public static method genItemStackEmbedIfAvailable and
 * rewrite it, so it could handle VaultHunters items.
 */
@Mixin(ForgeMessageUtils.class)
public class MixinForgeMessageUtils {

    @Inject(method = "genItemStackEmbedIfAvailable",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/item/ItemStack;getOrCreateTag()Lnet/minecraft/nbt/CompoundTag;"),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void genItemStackEmbedIfAvailable(Component component, CallbackInfoReturnable<MessageEmbed> cir,
        JsonObject json, JsonArray args, Iterator var3,
        JsonElement el, JsonObject arg1, JsonObject hoverEvent,
        JsonObject item, ItemStack is, CompoundTag itemTag) {
        if (item.get("id").getAsString().startsWith("the_vault")) {
            cir.setReturnValue(MixinForgeMessageUtils.vaultHuntersDiscordIntegrator$craftVaultHuntersItemMessage(item, is, itemTag));
        }
    }

    /**
     * This message crafts item descriptions for Vault Hunters items.
     * @param itemJson Item JSON data.
     * @param itemStack ItemStack item.
     * @param itemTag Item Tag.
     * @return MessageEmbed for Vault item.
     */
    @Unique
    private static MessageEmbed vaultHuntersDiscordIntegrator$craftVaultHuntersItemMessage(JsonObject itemJson,
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
}
