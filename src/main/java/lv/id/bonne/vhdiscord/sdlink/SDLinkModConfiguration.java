//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.sdlink;

import lv.id.bonne.vhdiscord.config.MixinConfigPlugin;
import net.minecraftforge.fml.loading.LoadingModList;


/**
 * Configuration for Simple Discord Link Bot mod.
 * {@linkplain <a href="https://www.curseforge.com/minecraft/mc-mods/simple-discord-link-bot-forge-fabric-spigot/">SDLink</a>}
 */
public class SDLinkModConfiguration extends MixinConfigPlugin
{
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        return LoadingModList.get().getModFileById("sdlink") != null;
    }
}
