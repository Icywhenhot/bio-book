package dev.biobook;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class BioData extends SavedData {
    private final Map<UUID, Profile> profiles = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> notes = new HashMap<>();

    public static BioData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(BioData::new, BioData::load), "biobook");
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

    public static BioData load(CompoundTag tag, HolderLookup.Provider registries) {
        BioData data = new BioData();
        CompoundTag profiles = tag.getCompound("profiles");
        for (String key : profiles.getAllKeys()) {
            try {
                CompoundTag p = profiles.getCompound(key);
                data.profiles.put(UUID.fromString(key), new Profile(p.getString("name"), p.getString("subtitle"), p.getString("lore")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        CompoundTag notes = tag.getCompound("notes");
        for (String author : notes.getAllKeys()) {
            try {
                UUID owner = UUID.fromString(author);
                Map<UUID, String> entries = new HashMap<>();
                CompoundTag book = notes.getCompound(author);
                for (String target : book.getAllKeys()) {
                    try {
                        entries.put(UUID.fromString(target), Profile.clean(book.getString(target), Profile.TEXT_LIMIT, true));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                data.notes.put(owner, entries);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag people = new CompoundTag();
        profiles.forEach((id, profile) -> {
            CompoundTag p = new CompoundTag();
            p.putString("name", profile.name());
            p.putString("subtitle", profile.subtitle());
            p.putString("lore", profile.lore());
            people.put(id.toString(), p);
        });
        CompoundTag journals = new CompoundTag();
        notes.forEach((author, entries) -> {
            CompoundTag book = new CompoundTag();
            entries.forEach((target, text) -> book.putString(target.toString(), text));
            journals.put(author.toString(), book);
        });
        tag.put("profiles", people);
        tag.put("notes", journals);
        return tag;
    }
}
