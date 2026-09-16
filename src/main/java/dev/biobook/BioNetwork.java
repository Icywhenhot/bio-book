package dev.biobook;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BioNetwork {
    public static Consumer<Reply> receiver = reply -> {};
    private static final java.util.Map<ServerPlayer, long[]> RATE = new java.util.WeakHashMap<>();

    public record Request(UUID target, int action, String name, String subtitle, String text) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BioBook.ID, "request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeUUID(p.target); buf.writeVarInt(p.action);
            buf.writeUtf(p.name, Profile.NAME_LIMIT); buf.writeUtf(p.subtitle, Profile.SUBTITLE_LIMIT); buf.writeUtf(p.text, Profile.TEXT_LIMIT);
        }, buf -> new Request(buf.readUUID(), buf.readVarInt(), buf.readUtf(Profile.NAME_LIMIT), buf.readUtf(Profile.SUBTITLE_LIMIT), buf.readUtf(Profile.TEXT_LIMIT)));

        @Override
        public Type<Request> type() { return TYPE; }
    }

    public record Reply(UUID target, Profile profile, String note, int action) implements CustomPacketPayload {
        public static final Type<Reply> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BioBook.ID, "reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Reply> CODEC = StreamCodec.of((buf, p) -> {
            buf.writeUUID(p.target); buf.writeUtf(p.profile.name(), Profile.NAME_LIMIT);
            buf.writeUtf(p.profile.subtitle(), Profile.SUBTITLE_LIMIT); buf.writeUtf(p.profile.lore(), Profile.TEXT_LIMIT);
            buf.writeUtf(p.note, Profile.TEXT_LIMIT); buf.writeVarInt(p.action);
        }, buf -> new Reply(buf.readUUID(), new Profile(buf.readUtf(Profile.NAME_LIMIT), buf.readUtf(Profile.SUBTITLE_LIMIT), buf.readUtf(Profile.TEXT_LIMIT)), buf.readUtf(Profile.TEXT_LIMIT), buf.readVarInt()));

        @Override
        public Type<Reply> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Reply.TYPE, Reply.CODEC, (reply, context) -> receiver.accept(reply));
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (request.action < 0 || request.action > 2) return;
            long now = System.nanoTime();
            long[] rate = RATE.computeIfAbsent(player, key -> new long[] { now, 0 });
            if (now - rate[0] > 1_000_000_000L) { rate[0] = now; rate[1] = 0; }
            if (++rate[1] > 30) return;
            BioData data = BioData.get(player.server);
            if (request.action == 1) {
                if (!request.target.equals(player.getUUID())) return;
                data.putProfile(player.getUUID(), new Profile(request.name, request.subtitle, request.text));
            } else if (request.action == 2) {
                data.putNote(player.getUUID(), request.target, request.text);
            }
            context.reply(new Reply(request.target, data.profile(request.target), data.note(player.getUUID(), request.target), request.action));
        });
    }
}
