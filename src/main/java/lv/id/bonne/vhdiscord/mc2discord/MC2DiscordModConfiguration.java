//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.mc2discord;

import lv.id.bonne.vhdiscord.config.MixinConfigPlugin;
import net.minecraftforge.fml.loading.LoadingModList;


/**
 * Configuration for MC2Discord mod.
 * {@linkplain <a href="https://www.curseforge.com/minecraft/mc-mods/mc2discord">MC2Discord</a>}
 */
public class MC2DiscordModConfiguration extends MixinConfigPlugin
{
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        return LoadingModList.get().getModFileById("mc2discord") != null && !LoadingModList.get().getModFileById("mc2discord").versionString().startsWith("3");
    }
}
