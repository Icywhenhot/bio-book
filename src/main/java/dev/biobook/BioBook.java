package dev.biobook;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

public final class BioBook implements ModInitializer {
    public static final String ID = "biobook";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    @Override
    public void onInitialize() {
        BioNetwork.register();
    }
}
