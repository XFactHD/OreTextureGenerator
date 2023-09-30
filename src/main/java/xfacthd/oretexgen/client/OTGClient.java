package xfacthd.oretexgen.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xfacthd.oretexgen.OreTextureGenerator;
import xfacthd.oretexgen.client.loader.OreTextureSource;

@Mod.EventBusSubscriber(modid = OreTextureGenerator.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OTGClient
{
    @SubscribeEvent
    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event)
    {
        OreTextureSource.register();
    }



    private OTGClient() { }
}
