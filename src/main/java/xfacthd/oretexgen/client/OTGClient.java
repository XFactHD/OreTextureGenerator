package xfacthd.oretexgen.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterTextureAtlasSpriteLoadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xfacthd.oretexgen.OreTextureGenerator;
import xfacthd.oretexgen.client.loader.OreTextureLoader;

@Mod.EventBusSubscriber(modid = OreTextureGenerator.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OTGClient
{
    @SubscribeEvent
    public static void onRegisterTextureLoader(final RegisterTextureAtlasSpriteLoadersEvent event)
    {
        event.register("loader", new OreTextureLoader());
    }
}
