package dev.biobook.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.biobook.BioBook;
import dev.biobook.BioNetwork;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;

public final class BioClient implements ClientModInitializer {
    private static final String MARKER = "biobook:";
    private static KeyMapping open;

    @Override
    public void onInitializeClient() {
        open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.biobook.open", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_B, KeyMapping.Category.register(BioBook.id("journal"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.setScreenAndShow(new BioScreen(client.player.getUUID()));
                }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
                dispatcher.register(ClientCommands.literal("bio").executes(context -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) mc.execute(() -> mc.setScreenAndShow(new BioScreen(mc.player.getUUID())));
                    return 1;
                }).then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(context.getSource().getOnlinePlayerNames(), builder))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "player");
                            Minecraft mc = context.getSource().getClient();
                            var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfoIgnoreCase(name);
                            if (info == null) {
                                context.getSource().sendError(Component.literal("No player named " + name + " online"));
                                return 0;
                            }
                            UUID id = info.getProfile().id();
                            mc.execute(() -> mc.setScreenAndShow(new BioScreen(id)));
                            return 1;
                        }))));

        ClientPlayNetworking.registerGlobalReceiver(BioNetwork.Reply.TYPE, (reply, context) ->
                context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof BioScreen screen) screen.receive(reply);
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BioScreen.clearDrafts());
    }

    public static Component withPortrait(Component message, UUID sender) {
        Style style = Style.EMPTY.withInsertion(MARKER + sender)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Open character journal")));
        return Component.empty().append(Component.literal("   ").setStyle(style)).append(message);
    }

    public static UUID target(Style style) {
        if (style == null || style.getInsertion() == null || !style.getInsertion().startsWith(MARKER)) return null;
        try { return UUID.fromString(style.getInsertion().substring(MARKER.length())); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public static void portrait(GuiGraphicsExtractor g, UUID id, int x, int y, int size, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
        var skin = player == null ? DefaultPlayerSkin.get(id) : player.getSkin();
        PlayerFaceExtractor.extractRenderState(g, skin, x, y, size, ARGB.white(alpha));
    }

    public static UUID target(FormattedCharSequence text) {
        UUID[] id = new UUID[1];
        text.accept((index, style, codepoint) -> {
            id[0] = target(style);
            return false;
        });
        return id[0];
    }

    public static void chatPortrait(GuiGraphicsExtractor g, FormattedCharSequence text, int y, float alpha) {
        UUID id = target(text);
        if (id != null) portrait(g, id, 0, y, 8, alpha);
    }
}
