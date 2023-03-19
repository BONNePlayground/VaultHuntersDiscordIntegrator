//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.dcintegration;


import lv.id.bonne.vhdiscord.config.MixinConfigPlugin;
import net.minecraftforge.fml.loading.LoadingModList;


/**
 * Configuration for Discord Integration for Forge mod.
 * {@linkplain <a href="https://www.curseforge.com/minecraft/mc-mods/dcintegration">DCIntegration</a>}
 */
public class DiscordIntegrationModConfiguration extends MixinConfigPlugin
{
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        return LoadingModList.get().getModFileById("dcintegration") != null;
    }
}
