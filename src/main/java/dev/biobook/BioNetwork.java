package dev.biobook;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BioNetwork {
    public static Consumer<Reply> receiver = reply -> {};
    private static final java.util.Map<ServerPlayer, long[]> RATE = new java.util.WeakHashMap<>();
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(BioBook.ID, "main"), () -> "1", "1"::equals, "1"::equals);

    public record Request(UUID target, int action, String name, String subtitle, String text) {
        void write(FriendlyByteBuf buf) {
            buf.writeUUID(target); buf.writeVarInt(action);
            buf.writeUtf(name, Profile.NAME_LIMIT); buf.writeUtf(subtitle, Profile.SUBTITLE_LIMIT); buf.writeUtf(text, Profile.TEXT_LIMIT);
        }

        static Request read(FriendlyByteBuf buf) {
            return new Request(buf.readUUID(), buf.readVarInt(), buf.readUtf(Profile.NAME_LIMIT), buf.readUtf(Profile.SUBTITLE_LIMIT), buf.readUtf(Profile.TEXT_LIMIT));
        }
    }

    public record Reply(UUID target, Profile profile, String note, int action) {
        void write(FriendlyByteBuf buf) {
            buf.writeUUID(target); buf.writeUtf(profile.name(), Profile.NAME_LIMIT);
            buf.writeUtf(profile.subtitle(), Profile.SUBTITLE_LIMIT); buf.writeUtf(profile.lore(), Profile.TEXT_LIMIT);
            buf.writeUtf(note, Profile.TEXT_LIMIT); buf.writeVarInt(action);
        }

        static Reply read(FriendlyByteBuf buf) {
            return new Reply(buf.readUUID(), new Profile(buf.readUtf(Profile.NAME_LIMIT), buf.readUtf(Profile.SUBTITLE_LIMIT), buf.readUtf(Profile.TEXT_LIMIT)), buf.readUtf(Profile.TEXT_LIMIT), buf.readVarInt());
        }
    }

    public static void register() {
        CHANNEL.messageBuilder(Reply.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Reply::write).decoder(Reply::read)
                .consumerMainThread((reply, ctx) -> receiver.accept(reply)).add();
        CHANNEL.messageBuilder(Request.class, 1, NetworkDirection.PLAY_TO_SERVER).encoder(Request::write).decoder(Request::read)
                .consumerMainThread((request, ctx) -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player == null) return;
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
                    CHANNEL.reply(new Reply(request.target, data.profile(request.target), data.note(player.getUUID(), request.target), request.action), ctx.get());
                }).add();
    }
}
