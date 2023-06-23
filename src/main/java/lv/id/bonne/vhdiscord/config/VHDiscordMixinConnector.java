//
// Created by BONNe
// Copyright - 2023
//

package lv.id.bonne.vhdiscord.config;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;


/**
 * Connector to the mixin json files.
 */
public class VHDiscordMixinConnector implements IMixinConnector
{
    /**
     * Connect to Mixin files
     */
    @Override
    public void connect() {
        Mixins.addConfiguration("mixins.vhdiscord.dcintegration.json");
        Mixins.addConfiguration("mixins.vhdiscord.mc2discord.json");
        Mixins.addConfiguration("mixins.vhdiscord.sdlink.json");
    }
}