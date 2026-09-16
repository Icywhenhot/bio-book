package dev.biobook;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(BioBook.ID)
public final class BioBook {
    public static final String ID = "biobook";

    public BioBook(IEventBus bus) {
        bus.addListener(BioNetwork::register);
    }
}
