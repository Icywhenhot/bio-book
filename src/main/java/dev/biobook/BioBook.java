package dev.biobook;

import dev.biobook.client.BioClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(BioBook.ID)
public final class BioBook {
    public static final String ID = "biobook";

    public BioBook() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BioNetwork.register();
        if (FMLEnvironment.dist == Dist.CLIENT) BioClient.init(bus);
    }
}
