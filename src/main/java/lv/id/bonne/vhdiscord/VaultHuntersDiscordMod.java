package lv.id.bonne.vhdiscord;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartingEvent;


/**
 * Mod initializing system.
 */
@Mod("vhdiscord")
public class VaultHuntersDiscordMod
{
    public VaultHuntersDiscordMod()
    {
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
    }
}
