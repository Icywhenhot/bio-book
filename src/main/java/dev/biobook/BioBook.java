package dev.biobook;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public final class BioBook implements ModInitializer {
    public static final String ID = "biobook";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    @Override
    public void onInitialize() {
        BioNetwork.register();
    }
}
