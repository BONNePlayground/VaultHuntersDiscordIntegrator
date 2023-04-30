//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.parser;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Predicate;

import iskallia.vault.config.EtchingConfig;
import iskallia.vault.config.TrinketConfig;
import iskallia.vault.config.gear.VaultGearTierConfig;
import iskallia.vault.core.data.key.ThemeKey;
import iskallia.vault.core.vault.VaultRegistry;
import iskallia.vault.core.vault.modifier.VaultModifierStack;
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.core.world.generator.layout.ArchitectRoomEntry;
import iskallia.vault.core.world.generator.layout.DIYRoomEntry;
import iskallia.vault.core.world.roll.IntRoll;
import iskallia.vault.dynamodel.DynamicModel;
import iskallia.vault.dynamodel.model.armor.ArmorPieceModel;
import iskallia.vault.dynamodel.model.item.PlainItemModel;
import iskallia.vault.dynamodel.registry.DynamicModelRegistry;
import iskallia.vault.gear.VaultGearState;
import iskallia.vault.gear.attribute.VaultGearAttribute;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.gear.data.AttributeGearData;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.gear.item.VaultGearItem;
import iskallia.vault.gear.tooltip.GearTooltip;
import iskallia.vault.gear.trinket.TrinketEffect;
import iskallia.vault.init.*;
import iskallia.vault.item.*;
import iskallia.vault.item.crystal.CrystalData;
import iskallia.vault.item.crystal.CrystalModifiers;
import iskallia.vault.item.crystal.VaultCrystalItem;
import iskallia.vault.item.crystal.layout.*;
import iskallia.vault.item.crystal.objective.*;
import iskallia.vault.item.crystal.theme.CrystalTheme;
import iskallia.vault.item.crystal.theme.NullCrystalTheme;
import iskallia.vault.item.crystal.theme.PoolCrystalTheme;
import iskallia.vault.item.crystal.theme.ValueCrystalTheme;
import iskallia.vault.item.crystal.time.CrystalTime;
import iskallia.vault.item.crystal.time.NullCrystalTime;
import iskallia.vault.item.crystal.time.PoolCrystalTime;
import iskallia.vault.item.crystal.time.ValueCrystalTime;
import iskallia.vault.item.data.InscriptionData;
import iskallia.vault.item.gear.EtchingItem;
import iskallia.vault.item.gear.TrinketItem;
import iskallia.vault.item.tool.PaxelItem;
import iskallia.vault.util.MiscUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


/**
 * This class allows to parse VaultHunters item tooltips to discord chat.
 */
public class VaultItemsHandler
{
    /**
     * This method generates string with tooltip of given item stack
     * @param itemJson Item Json object.
     * @param itemStack Item Stack object.
     * @param itemTag Item Compound Tag
     * @return String with a tooltip
     */
    public static String generateVaultHuntersItemTooltips(JsonObject itemJson,
        ItemStack itemStack,
        CompoundTag itemTag)
    {
        try
        {
            StringBuilder builder = new StringBuilder();

            if (itemStack.getItem() instanceof BottleItem)
            {
                VaultItemsHandler.handleBottleTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultGearItem)
            {
                VaultItemsHandler.handleGearTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultDollItem)
            {
                VaultItemsHandler.handleDollTooltip(builder, itemTag);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof PaxelItem paxelItem)
            {
                VaultItemsHandler.handlePaxelTooltip(builder, itemStack, paxelItem);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof EtchingItem)
            {
                VaultItemsHandler.handleEtchingTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultRuneItem)
            {
                VaultItemsHandler.handleRuneTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof InscriptionItem)
            {
                VaultItemsHandler.handleInscriptionTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultCrystalItem)
            {
                VaultItemsHandler.handleVaultCrystalTooltip(builder, CrystalData.read(itemStack));
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof TrinketItem)
            {
                VaultItemsHandler.handleTrinketTooltip(builder, itemStack, itemTag);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof RelicFragmentItem relic)
            {
                VaultItemsHandler.handleRelicFragmentTooltip(builder, itemStack, relic);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultCatalystInfusedItem)
            {
                VaultItemsHandler.handleCatalystTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof AugmentItem)
            {
                VaultItemsHandler.handleAugmentTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (ModBlocks.VAULT_ARTIFACT.getRegistryName().equals(itemStack.getItem().getRegistryName()))
            {
                //VaultItemsHandler.handleVaultArtifactTooltip(builder, itemTag);
                return builder.toString();
            }
        }
        catch (Exception e)
        {
            // If I fail, then return nothing.
            return null;
        }

        return null;
    }


    /**
     * This method parses gear tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Gear ItemStack.
     */
    public static void handleGearTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultGearData data = VaultGearData.read(itemStack);
        VaultGearState state = data.getState();

        // Add gear Level
        builder.append("**Level:** ").append(data.getItemLevel()).append("\n");

        // Add crafter name
        data.getFirstValue(ModGearAttributes.CRAFTED_BY).ifPresent(crafter ->
            builder.append("**Crafted by:** ").append(crafter).append("\n"));

        // Add Rarity
        switch (state)
        {
            case UNIDENTIFIED ->
            {
                Objects.requireNonNull(ModConfigs.VAULT_GEAR_TYPE_CONFIG);
                data.getFirstValue(ModGearAttributes.GEAR_ROLL_TYPE).
                    flatMap(ModConfigs.VAULT_GEAR_TYPE_CONFIG::getRollPool).
                    ifPresent(pool -> builder.append("**Roll:** ").append(pool.getName()).append("\n"));
            }
            case IDENTIFIED ->
            {
                builder.append("**Roll:** ").append(data.getRarity().getDisplayName().getString()).append("\n");
            }
        }

        if (state == VaultGearState.IDENTIFIED)
        {
            // Add Model
            data.getFirstValue(ModGearAttributes.GEAR_MODEL).
                flatMap(modelId -> ModDynamicModels.REGISTRIES.getModel(itemStack.getItem(), modelId)).
                ifPresent(gearModel -> {
                    Item pattern = itemStack.getItem();
                    if (pattern instanceof VaultGearItem)
                    {
                        String name = gearModel.getDisplayName();

                        if (gearModel instanceof ArmorPieceModel modelPiece)
                        {
                            name = modelPiece.getArmorModel().getDisplayName();
                        }

                        builder.append("**Model:** ").append(name).append("\n");
                    }
                });

            // Add Etchings
            data.getFirstValue(ModGearAttributes.ETCHING).
                ifPresent(etchingSet -> {
                    EtchingConfig.Etching etchingConfig = ModConfigs.ETCHING.getEtchingConfig(etchingSet);
                    if (etchingConfig != null)
                    {
                        builder.append("**Etching:** ").append(etchingConfig.getName()).append("\n");
                    }
                });

            // Add Repair text.
            int usedRepairs = data.getUsedRepairSlots();
            int totalRepairs = data.getRepairSlots();

            builder.append(VaultItemsHandler.createRepairText(usedRepairs, totalRepairs)).append("\n");

            // Add Implicits

            List<VaultGearModifier<?>> implicits = data.getModifiers(VaultGearModifier.AffixType.IMPLICIT);

            if (!implicits.isEmpty())
            {
                VaultItemsHandler.addAffixList(builder, data, VaultGearModifier.AffixType.IMPLICIT, itemStack);
                builder.append("\n");
            }

            int maxPrefixes = data.getFirstValue(ModGearAttributes.PREFIXES).orElse(0);
            List<VaultGearModifier<?>> prefixes = data.getModifiers(VaultGearModifier.AffixType.PREFIX);

            if (maxPrefixes > 0 || !prefixes.isEmpty())
            {
                VaultItemsHandler.addAffixList(builder, data, VaultGearModifier.AffixType.PREFIX, itemStack);
                builder.append("\n");
            }

            int maxSuffixes = data.getFirstValue(ModGearAttributes.SUFFIXES).orElse(0);
            List<VaultGearModifier<?>> suffixes = data.getModifiers(VaultGearModifier.AffixType.SUFFIX);

            if (maxSuffixes > 0 || !suffixes.isEmpty())
            {
                VaultItemsHandler.addAffixList(builder, data, VaultGearModifier.AffixType.SUFFIX, itemStack);
            }
        }
    }


    /**
     * This method parses bottle item into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Bottle Item.
     */
    public static void handleBottleTooltip(StringBuilder builder, ItemStack itemStack)
    {
        BottleItem.getType(itemStack).ifPresent(type -> {
            builder.append("Heals ").
                append(type.getHealing()).
                append(" hitpoints\n");

            BottleItem.getRecharge(itemStack).ifPresent(recharge -> {
                switch (recharge)
                {
                    case TIME ->
                        builder.append("Recharges every ").
                            append(type.getTimeRecharge() / 1200).
                            append(" minutes").
                            append("\n");
                    case MOBS ->
                        builder.append("Recharges every ").
                            append(type.getMobRecharge()).
                            append(" mob kills").
                            append("\n");
                }
            });
        });

        VaultGearData data = VaultGearData.read(itemStack);

        List<VaultGearModifier<?>> implicits = data.getModifiers(VaultGearModifier.AffixType.IMPLICIT);

        if (!implicits.isEmpty())
        {
            VaultItemsHandler.addAffixList(builder, data, VaultGearModifier.AffixType.IMPLICIT, itemStack);
            builder.append("\n");
        }

        int maxPrefixes = data.getFirstValue(ModGearAttributes.PREFIXES).orElse(0);
        List<VaultGearModifier<?>> prefixes = data.getModifiers(VaultGearModifier.AffixType.PREFIX);

        if (maxPrefixes > 0 || !prefixes.isEmpty())
        {
            VaultItemsHandler.addAffixList(builder, data, VaultGearModifier.AffixType.PREFIX, itemStack);
            builder.append("\n");
        }

        int maxSuffixes = data.getFirstValue(ModGearAttributes.SUFFIXES).orElse(0);
        List<VaultGearModifier<?>> suffixes = data.getModifiers(VaultGearModifier.AffixType.SUFFIX);

        if (maxSuffixes > 0 || !suffixes.isEmpty())
        {
            VaultItemsHandler.addAffixList(builder, data, VaultGearModifier.AffixType.SUFFIX, itemStack);
        }
    }


    /**
     * This method parses doll tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemTag Vault Doll Item Tag.
     */
    public static void handleDollTooltip(StringBuilder builder, CompoundTag itemTag)
    {
        String owner = itemTag.getCompound("playerProfile").getString("Name");

        builder.append("**Owner:**").
            append(" ").
            append(owner).
            append("\n");

        int lootPercent = (int) (itemTag.getFloat("lootPercent") * 100.0F);

        builder.append("**Loot Efficiency:**").
            append(" ").
            append(String.format("%d", lootPercent)).
            append("%").
            append("\n");

        int xpPercent = (int) (itemTag.getFloat("xpPercent") * 100.0F);

        builder.append("**Experience Efficiency:**").
            append(" ").
            append(String.format("%d", xpPercent)).
            append("%").
            append("\n");

        if (itemTag.contains("vaultUUID"))
        {
            builder.append("**Ready to be released!**");
        }
        else
        {
            builder.append("**Ready for a vault!**");
        }
    }


    /**
     * This method parses paxel item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Paxel Item Stack.
     * @param paxelItem Vault Paxel Item.
     */
    public static void handlePaxelTooltip(StringBuilder builder, ItemStack itemStack, PaxelItem paxelItem)
    {
        int durability = paxelItem.getMaxDamage(itemStack);
        float miningSpeed = PaxelItem.getUsableStat(itemStack, PaxelItem.Stat.MINING_SPEED);
        float reach = PaxelItem.getUsableStat(itemStack, PaxelItem.Stat.REACH);
        float copiously = PaxelItem.getUsableStat(itemStack, PaxelItem.Stat.COPIOUSLY);

        // Generic information.

        builder.append("**D:** ").append(FORMAT.format(durability));

        if (reach > 0)
        {
            builder.append(" **R:** ").append(FORMAT.format(reach));
        }

        builder.append(" **S:** ").append(FORMAT.format(miningSpeed));

        if (copiously > 0)
        {
            builder.append(" **C:** ").append(FORMAT.format(copiously)).append("%");
        }

        builder.append("\n");

        // Perks

        List<PaxelItem.Perk> perks = PaxelItem.getPerks(itemStack);

        if (perks.size() > 0)
        {
            builder.append("**Perks:**");
            perks.forEach(perk -> {
                builder.append("\n");
                builder.append("  ").append(perk.getSerializedName());
            });

            builder.append("\n");
        }

        // Main information

        int level = PaxelItem.getPaxelLevel(itemStack);
        builder.append("**Level:** ").append(level).append("\n");

        builder.append(VaultItemsHandler.createRepairText(
            PaxelItem.getUsedRepairSlots(itemStack),
            PaxelItem.getMaxRepairSlots(itemStack))).
            append("\n");

        int sockets = PaxelItem.getSockets(itemStack);

        if (sockets != 0)
        {
            builder.append("**Sockets:** ").
                append(VaultItemsHandler.createDots(sockets, EMPTY_CIRCLE)).
                append("\n");
        }

        builder.append("\n");

        PaxelItem.Stat[] stats = PaxelItem.Stat.values();

        for (int index = 0; index < stats.length; ++index)
        {
            PaxelItem.Stat stat = stats[index];

            float value = PaxelItem.getStatUpgrade(itemStack, stat);

            if (value != 0.0F)
            {
                builder.append("**").append(stat.getReadableName()).append("**");
                builder.append(value > 0.0F ? " +" : " ");
                builder.append(ModConfigs.PAXEL_CONFIGS.getUpgrade(stat).formatValue(value));
                builder.append("\n");
            }
        }
    }


    /**
     * This method parses Etching item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Etching Item Stack.
     */
    public static void handleEtchingTooltip(StringBuilder builder, ItemStack itemStack)
    {
        AttributeGearData data = AttributeGearData.read(itemStack);

        if (data.getFirstValue(ModGearAttributes.STATE).orElse(VaultGearState.UNIDENTIFIED) == VaultGearState.IDENTIFIED)
        {
            data.getFirstValue(ModGearAttributes.ETCHING).ifPresent((etchingSet) ->
            {
                EtchingConfig.Etching config = ModConfigs.ETCHING.getEtchingConfig(etchingSet);

                if (config != null)
                {
                    builder.append("**Etching:** ").append(config.getName());

                    for (TextComponent cmp : MiscUtils.splitDescriptionText(config.getEffectText()))
                    {
                        builder.append("\n");
                        builder.append(cmp.getString());
                    }
                }
            });
        }
    }


    /**
     * This method parses VaultRune item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Rune Item Stack.
     */
    public static void handleRuneTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultRuneItem.getEntries(itemStack).forEach(diyRoomEntry -> {
            int count = diyRoomEntry.get(DIYRoomEntry.COUNT);

            builder.append("- Has ").
                append(count).
                append(" ").
                append(diyRoomEntry.getName().getString()).
                append(" ").
                append(count > 1 ? "Rooms" : "Room");
            builder.append("\n");
        });
    }


    /**
     * This method parses Vault Inscription item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Rune Item Stack.
     */
    public static void handleInscriptionTooltip(StringBuilder builder, ItemStack itemStack)
    {
        InscriptionData data = InscriptionData.from(itemStack);

        CompoundTag compoundTag = data.serializeNBT();

        builder.append("**Completion:** ").
            append(Math.round(compoundTag.getFloat("completion") * 100.0F)).
            append("%").
            append("\n");
        builder.append("**Time:** ").
            append(VaultItemsHandler.formatTimeString(compoundTag.getInt("time"))).
            append("\n");
        builder.append("**Instability:** ").
            append(Math.round(compoundTag.getInt("instability") * 100.0F)).
            append("%").
            append("\n");

        for (InscriptionData.Entry entry : data.getEntries())
        {
            String roomStr = entry.count > 1 ? "Rooms" : "Room";

            builder.append(" ").
                append(VaultItemsHandler.DOT).
                append(" ").
                append(entry.count).
                append(" ").
                append(entry.toRoomEntry().has(ArchitectRoomEntry.TYPE) ?
                    entry.toRoomEntry().get(ArchitectRoomEntry.TYPE).getName() : "Unknown").
                append(" ").
                append(roomStr).
                append("\n");
        }
    }


    /**
     * This method parses Vault Crystal item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param crystalData Vault Crystal Data.
     */
    public static void handleVaultCrystalTooltip(StringBuilder builder, CrystalData crystalData)
    {
        builder.append("**Level:** ").append(crystalData.getLevel()).append("\n");

        // Objective
        builder.append("**Objective:** ").
            append(parseObjectiveName(crystalData.getObjective()));
        builder.append("\n");

        // Vault Theme
        builder.append("**Theme:** ").
            append(parseThemeName(crystalData.getTheme()));
        builder.append("\n");

        // Vault Layout
        builder.append("**Layout:** ").
            append(parseLayoutName(crystalData.getLayout()));
        builder.append("\n");

        // Vault Time
        String time = parseTime(crystalData.getTime());

        if (!time.isBlank())
        {
            builder.append(time);
            builder.append("\n");
        }

        // Instability
        float instability = crystalData.getInstability();

        if (instability > 0.0F)
        {
            builder.append("**Instability:** ").
                append(Math.round(crystalData.getInstability() * 100.0F)).
                append("%").
                append("\n");
        }

        // Unmodifiable
        if (crystalData.isUnmodifiable())
        {
            builder.append("**Unmodifiable**").append("\n");
        }

        // Modifiers
        VaultItemsHandler.parseModifiers(builder, crystalData.getModifiers());
    }


    /**
     * This method parses VaultTrinket item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Trinket Item Stack.
     * @param itemTag The item tag data.
     */
    public static void handleTrinketTooltip(StringBuilder builder, ItemStack itemStack, CompoundTag itemTag)
    {
        // I do not want to include Botania in dependencies. This is workaround.

        AttributeGearData data = AttributeGearData.read(itemStack);

        if (data.getFirstValue(ModGearAttributes.STATE).orElse(VaultGearState.UNIDENTIFIED) == VaultGearState.IDENTIFIED)
        {
            int totalUses = itemTag.getInt("vaultUses");
            int used = itemTag.getList("usedVaults", 10).size();
            int remaining = Math.max(totalUses - used, 0);

            builder.append("**Uses:** ").append(String.valueOf(remaining)).append("\n");

            data.getFirstValue(ModGearAttributes.CRAFTED_BY).ifPresent(crafter ->
                builder.append("**Crafted by:** ").append(crafter).append("\n"));

            data.getFirstValue(ModGearAttributes.TRINKET_EFFECT).ifPresent(effect -> {
                TrinketConfig.Trinket trinket = effect.getTrinketConfig();
                builder.append(trinket.getEffectText()).append("\n");
            });

            data.getFirstValue(ModGearAttributes.TRINKET_EFFECT).
                map(TrinketEffect::getConfig).
                filter(TrinketEffect.Config::hasCuriosSlot).
                map(TrinketEffect.Config::getCuriosSlot).
                ifPresent(slot -> {
                    MutableComponent slotTranslation = new TranslatableComponent("curios.slot").append(": ");
                    MutableComponent slotType = new TranslatableComponent("curios.identifier." + slot);

                    builder.append("\n").
                        append(slotTranslation.getString()).
                        append(slotType.getString());
                });
        }
    }


    /**
     * This method parses Vault Relic Fragment item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Trinket Item Stack.
     * @param relic The item.
     */
    public static void handleRelicFragmentTooltip(StringBuilder builder, ItemStack itemStack, RelicFragmentItem relic)
    {
        Optional<ResourceLocation> resourceLocation = relic.getDynamicModelId(itemStack);
        DynamicModelRegistry<PlainItemModel> fragmentRegistry = ModDynamicModels.Relics.FRAGMENT_REGISTRY;

        resourceLocation = resourceLocation.
            flatMap(fragmentRegistry::get).
            map(DynamicModel::getId).
            flatMap(ModRelics::getRelicOfFragment).
            map(ModRelics.RelicRecipe::getResultingRelic);

        fragmentRegistry = ModDynamicModels.Relics.RELIC_REGISTRY;

        resourceLocation.flatMap(fragmentRegistry::get).ifPresent(relicModel ->
            builder.append("**Assembles:** ").append(relicModel.getDisplayName()));
    }


    /**
     * This method parses Vault Catalyst item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Catalyst Item Stack.
     */
    public static void handleCatalystTooltip(StringBuilder builder, ItemStack itemStack)
    {
        List<ResourceLocation> modifierIdList = VaultCatalystInfusedItem.getModifiers(itemStack);

        if (!modifierIdList.isEmpty())
        {
            builder.append("\n");
            builder.append(new TranslatableComponent(modifierIdList.size() <= 1 ?
                "tooltip.the_vault.vault_catalyst.modifier.singular" :
                "tooltip.the_vault.vault_catalyst.modifier.plural").getString());
            builder.append("\n");

            modifierIdList.forEach(modifierId ->
                VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                    builder.append(vaultModifier.getDisplayName()).append("\n")));
        }
    }


    /**
     * This method adds Vault Artifact image to the discord chat.
     * @param builder Embed Builder.
     * @param itemTag Vault artifact tag.
     */
    public static void handleVaultArtifactTooltip(StringBuilder builder, CompoundTag itemTag)
    {
         int customModelData = Math.max(itemTag.getInt("CustomModelData"), 1);
         String name = "vault_artifact_" + customModelData + ".png";
         builder.append("https://bonne.id.lv/assets/img/").append(name);
    }


    /**
     * This method adds Vault Augment Item description the discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault augment item.
     */
    public static void handleAugmentTooltip(StringBuilder builder, ItemStack itemStack)
    {
        builder.append("**Theme:** ");
        AugmentItem.getTheme(itemStack).ifPresentOrElse(
            (key) -> builder.append(key.getName()),
            () -> builder.append("???"));
    }


// ---------------------------------------------------------------------
// Section: Private processing methods
// ---------------------------------------------------------------------


    /**
     * This method adds affixes to the embed builder.
     * @param builder Embed builder that need to be populated.
     * @param data Vault Gear Data.
     * @param type Affix type.
     * @param itemStack Item Stack that is displayed.
     */
    private static void addAffixList(StringBuilder builder,
        VaultGearData data,
        VaultGearModifier.AffixType type,
        ItemStack itemStack)
    {
        List<VaultGearModifier<?>> affixes = data.getModifiers(type);

        VaultGearAttribute<Integer> affixAttribute = (type == VaultGearModifier.AffixType.PREFIX) ?
            ModGearAttributes.PREFIXES : ModGearAttributes.SUFFIXES;

        int emptyAffixes = data.getFirstValue(affixAttribute).orElse(0);

        builder.append("**").
            append(affixes.size() != 1 ? type.getPlural() : type.getSingular()).
            append(":** ");
        builder.append("\n");
        affixes.forEach(modifier -> VaultItemsHandler.addAffix(builder, modifier, data, type, itemStack));

        if (type != VaultGearModifier.AffixType.IMPLICIT)
        {
            for (int i = 0; i < emptyAffixes - affixes.size(); i++)
            {
                builder.append(VaultItemsHandler.createEmptyAffix(type));
                builder.append("\n");
            }
        }
    }


    /**
     * This method adds affix text to given builder.
     * @param builder Embed builder.
     * @param modifier Vault Gear Modifier
     * @param data Vault Gear data
     * @param type Affix Type.
     * @param stack ItemStack of item.
     */
    @SuppressWarnings("rawtypes unchecked")
    private static void addAffix(StringBuilder builder,
        VaultGearModifier modifier,
        VaultGearData data,
        VaultGearModifier.AffixType type,
        ItemStack stack)
    {
        Optional.ofNullable(modifier.getAttribute().getReader().getDisplay(modifier, data, type, stack)).
            map(text -> {
                if (modifier.getCategory() != VaultGearModifier.AffixCategory.LEGENDARY)
                {
                    return text.getString();
                }
                else
                {
                    return VaultItemsHandler.STAR + " " + text.getString();
                }
            }).
            ifPresent(text ->
            {
                MutableComponent tierDisplay = VaultGearTierConfig.getConfig(stack.getItem()).map((tierConfig) ->
                {
                    Object config = tierConfig.getTierConfig(modifier);

                    if (config != null)
                    {
                        return modifier.getAttribute().getGenerator().getConfigDisplay(
                            modifier.getAttribute().getReader(),
                            config);
                    }
                    else
                    {
                        return null;
                    }
                }).orElse(null);

                builder.append(text);

                if (tierDisplay != null)
                {
                    String legendaryInfo = modifier.getCategory() == VaultGearModifier.AffixCategory.LEGENDARY ? "**Legendary** " : "";

                    if (tierDisplay.getString().isEmpty())
                    {
                        builder.append(" (%sT%s)".formatted(legendaryInfo, modifier.getRolledTier() + 1));
                    }
                    else
                    {
                        builder.append(" (%sT%s: ".formatted(legendaryInfo, modifier.getRolledTier() + 1));
                        builder.append(tierDisplay.getString());
                        builder.append(")");
                    }
                }

                builder.append("\n");
            });
    }


    /**
     * This method creates empty affix of given type.
     * @param type Affix type.
     * @return Empty affix text.
     */
    private static String createEmptyAffix(VaultGearModifier.AffixType type)
    {
        return (SQUARE + " empty %s").formatted(type.name().toLowerCase(Locale.ROOT));
    }


    /**
     * This method creates repair text based on used repairs and total repairs values.
     * @param usedRepairs Number of used repairs.
     * @param totalRepairs Number of total repairs.
     * @return Text for repairs.
     */
    private static String createRepairText(int usedRepairs, int totalRepairs)
    {
        int remaining = totalRepairs - usedRepairs;

        return "**Repairs:** " +
            VaultItemsHandler.createDots(usedRepairs, FULL_CIRCLE) +
            VaultItemsHandler.createDots(remaining, EMPTY_CIRCLE);
    }


    /**
     * This method generates a repair dots for gear.
     * @param amount Amount of dots.
     * @param symbol Dot symbol.
     * @return String that contains number of repairs for gear.
     */
    private static String createDots(int amount, String symbol)
    {
        return (symbol + " ").repeat(Math.max(0, amount));
    }


    /**
     * This method adds crystal modifier data to the discord embed based on given filter.
     * @param builder Builder that need to be populated.
     * @param data Crystal Data object.
     * @param header Header of elements.
     * @param filter Filter for modifiers.
     */
    private static void populateCatalystInformation(StringBuilder builder,
        List<VaultModifierStack> data,
        String header,
        Predicate<VaultModifierStack> filter)
    {
        List<VaultModifierStack> modifierList = data.stream().filter(filter).toList();

        if (!modifierList.isEmpty())
        {
            builder.append(header).append("\n");

            for (VaultModifierStack modifierStack : modifierList)
            {
                VaultModifier<?> vaultModifier = modifierStack.getModifier();
                String formattedName = vaultModifier.getDisplayNameFormatted(modifierStack.getSize());

                builder.append("  ").
                    append("%dx".formatted(modifierStack.getSize())).
                    append(formattedName).
                    append("\n");
            }
        }
    }


    /**
     * Returns Crystal Objective name from instance of CrystalObjective.
     * @param objective class.
     * @return Name of the objective.
     */
    private static String parseObjectiveName(CrystalObjective objective)
    {
        if (objective instanceof BossCrystalObjective)
        {
            return "Hunt the Guardians";
        }
        else if (objective instanceof CakeCrystalObjective)
        {
            return "Cake Hunt";
        }
        else if (objective instanceof ElixirCrystalObjective)
        {
            return "Elixir Rush";
        }
        else if (objective instanceof EmptyCrystalObjective)
        {
            return "None";
        }
        else if (objective instanceof MonolithCrystalObjective)
        {
            return "Light the Monoliths";
        }
        else if (objective instanceof NullCrystalObjective)
        {
            return "???";
        }
        else if (objective instanceof ScavengerCrystalObjective)
        {
            return "Scavenger Hunt";
        }
        else if (objective instanceof SpeedrunCrystalObjective)
        {
            return "Speedrun";
        }
        else
        {
            return "???";
        }
    }


    /**
     * Returns Crystal Theme name from instance of CrystalTheme.
     * @param theme class.
     * @return Name of the theme.
     */
    private static String parseThemeName(CrystalTheme theme)
    {
        if (theme instanceof PoolCrystalTheme)
        {
            return "???";
        }
        else if (theme instanceof ValueCrystalTheme)
        {
            ThemeKey themeKey = VaultRegistry.THEME.getKey(theme.serializeNBT().getString("id"));

            if (themeKey == null)
            {
                return "Unknown";
            }
            else
            {
                return themeKey.getName();
            }
        }
        else if (theme instanceof NullCrystalTheme)
        {
            return "???";
        }
        else
        {
            return "???";
        }
    }


    /**
     * Returns Crystal Layout name from instance of CrystalLayout.
     * @param layout class.
     * @return Name of the layout.
     */
    private static String parseLayoutName(CrystalLayout layout)
    {
        if (layout instanceof ArchitectCrystalLayout)
        {
            StringBuilder builder = new StringBuilder();
            builder.append("Architect");

            Optional<JsonObject> jsonObject = layout.writeJson();

            jsonObject.ifPresent(json ->
            {
                if (json.has("completion"))
                {
                    JsonPrimitive completionObject = json.getAsJsonPrimitive("completion");

                    if (completionObject.isNumber())
                    {
                        float completion = json.getAsJsonPrimitive("completion").getAsFloat();
                        builder.append(" | ").
                            append(Math.min(100.0F, Math.round(completion * 100.0F))).
                            append("%");
                    }
                }

                builder.append("\n");

                if (json.has("entries") && json.get("entries").isJsonArray())
                {
                    JsonArray entries = json.getAsJsonArray("entries");

                    entries.forEach(entry ->
                    {
                        ArchitectRoomEntry architectRoomEntry = ArchitectRoomEntry.fromJson((JsonObject) entry);
                        Component roomName = architectRoomEntry.getName();

                        if (roomName != null)
                        {
                            int count = architectRoomEntry.get(ArchitectRoomEntry.COUNT);

                            builder.append("-Has ").
                                append(count).
                                append(" *").
                                append(roomName.getString()).
                                append("* ").
                                append(count > 1 ? "Rooms" : "Room").
                                append("\n");
                        }
                    });
                }
            });

            return builder.toString();
        }
        else if (layout instanceof ClassicCircleCrystalLayout)
        {
            return "Circle";
        }
        else if (layout instanceof ClassicPolygonCrystalLayout)
        {
            return "Polygon";
        }
        else if (layout instanceof ClassicSpiralCrystalLayout)
        {
            return "Spiral";
        }
        else if (layout instanceof ClassicInfiniteCrystalLayout)
        {
            return "Infinite";
        }
        else if (layout instanceof NullCrystalLayout)
        {
            return "???";
        }
        else
        {
            return "???";
        }
    }


    /**
     * Returns Crystal Time name from instance of CrystalTime.
     * @param time class.
     * @return Name of the time.
     */
    private static String parseTime(CrystalTime time)
    {
        if (time instanceof PoolCrystalTime)
        {
            return "";
        }
        else if (time instanceof ValueCrystalTime vaultTime)
        {
            int min = IntRoll.getMin(vaultTime.getRoll());
            int max = IntRoll.getMax(vaultTime.getRoll());
            String text = formatTimeString(min);
            if (min != max) {
                text = text + " - " + formatTimeString(max);
            }

            return "**Time:** " + text;
        }
        else if (time instanceof NullCrystalTime)
        {
            return "";
        }
        else
        {
            return "";
        }
    }


    /**
     * This method parses crystal modifiers.
     * @param builder Builder that need to be populated.
     * @param modifiers The object that contains all crystal modifiers.
     */
    private static void parseModifiers(StringBuilder builder, CrystalModifiers modifiers)
    {
        if (modifiers.hasClarity()) {
            builder.append("*Clarity*\n");
        }

        List<VaultModifierStack> modifierList = new ArrayList<>();

        for (VaultModifierStack modifier : modifiers)
        {
            modifierList.add(modifier);
        }

        int curseCount = modifiers.getCurseCount();

        if (curseCount > 0)
        {
            if (modifiers.hasClarity())
            {
                VaultItemsHandler.populateCatalystInformation(builder,
                    modifierList,
                    "**Cursed:**",
                    catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isCurse(catalyst.getModifierId()));
            }
            else
            {
                builder.append("**Cursed** ").
                    append(CURSE.repeat(curseCount)).
                    append("\n");
            }
        }

        VaultItemsHandler.populateCatalystInformation(builder,
            modifierList,
            "**Positive Modifiers:**",
            catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isGood(catalyst.getModifierId()));

        VaultItemsHandler.populateCatalystInformation(builder,
            modifierList,
            "**Negative Modifiers:**",
            catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isBad(catalyst.getModifierId()));

        VaultItemsHandler.populateCatalystInformation(builder,
            modifierList,
            "**Other Modifiers:**",
            catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isUnlisted(catalyst.getModifierId()));
    }


    /**
     * Time parser
     * @param remainingTicks how many ticks remaining
     * @return remaining ticks parsed as string.
     */
    private static String formatTimeString(int remainingTicks)
    {
        long seconds = remainingTicks / 20 % 60;
        long minutes = remainingTicks / 20 / 60 % 60;
        long hours = remainingTicks / 20 / 60 / 60;
        return hours > 0L ? String.format("%02d:%02d:%02d", hours, minutes, seconds) :
            String.format("%02d:%02d", minutes, seconds);
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * symbol for text fields.
     */
    private static final String EMPTY_CIRCLE = "\u25CB";

    /**
     * symbol for text fields.
     */
    private static final String FULL_CIRCLE = "\u25CF";

    /**
     * symbol for text fields.
     */
    private static final String STAR = "\u2726";

    /**
     * symbol for text fields.
     */
    private static final String SQUARE = "\u25A0";

    /**
     * Symbol for text fields.
     */
    private static final String CURSE = "\u2620";

    /**
     * Symbol for text fields.
     */
    private static final String DOT = "\u2022";

    /**
     * Variable format for numbers.
     */
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");
}
