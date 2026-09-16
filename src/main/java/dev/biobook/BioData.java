package dev.biobook;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class BioData extends SavedData {
    private static final Codec<Profile> PROFILE = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(Profile::name),
            Codec.STRING.optionalFieldOf("subtitle", "").forGetter(Profile::subtitle),
            Codec.STRING.optionalFieldOf("lore", "").forGetter(Profile::lore)
    ).apply(i, Profile::new));

    public static final Codec<BioData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PROFILE).optionalFieldOf("profiles", Map.of()).forGetter(data -> data.profiles),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING))
                    .optionalFieldOf("notes", Map.of()).forGetter(data -> data.notes)
    ).apply(i, BioData::new));

    public static final SavedDataType<BioData> TYPE =
            new SavedDataType<>(BioBook.id("journals"), BioData::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private final Map<UUID, Profile> profiles = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> notes = new HashMap<>();

    public BioData() {
    }

    private BioData(Map<UUID, Profile> profiles, Map<UUID, Map<UUID, String>> notes) {
        this.profiles.putAll(profiles);
        notes.forEach((author, entries) -> {
            Map<UUID, String> book = new HashMap<>();
            entries.forEach((target, text) -> book.put(target, Profile.clean(text, Profile.TEXT_LIMIT, true)));
            this.notes.put(author, book);
        });
    }

    public static BioData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Profile profile(UUID id) {
        return profiles.getOrDefault(id, Profile.EMPTY);
    }

    public String note(UUID author, UUID target) {
        return notes.getOrDefault(author, Map.of()).getOrDefault(target, "");
    }

    public void putProfile(UUID id, Profile profile) {
        profiles.put(id, profile);
        setDirty();
    }

    public void putNote(UUID author, UUID target, String text) {
        Map<UUID, String> entries = notes.computeIfAbsent(author, key -> new HashMap<>());
        if (text.isBlank()) entries.remove(target);
        else entries.put(target, Profile.clean(text, Profile.TEXT_LIMIT, true));
        setDirty();
    }
}
