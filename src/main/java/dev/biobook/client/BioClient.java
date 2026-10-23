package dev.biobook.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.biobook.BioNetwork;
import dev.biobook.mixin.ChatScreenAccessor;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

public final class BioClient implements ClientModInitializer {
    private static final String MARKER = "biobook:";
    private static KeyMapping open;

    @Override
    public void onInitializeClient() {
        open = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.biobook.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.biobook"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (open.consumeClick()) {
                if (mc.player != null && mc.screen == null) mc.setScreen(new BioScreen(null, mc.player.getUUID()));
            }
        });

        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (screen instanceof ChatScreen chat) ScreenMouseEvents.allowMouseClick(screen).register((s, x, y, button) -> !click(chat, x, y, button));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
                dispatcher.register(ClientCommandManager.literal("bio").executes(context -> {
                    Minecraft mc = context.getSource().getClient();
                    if (mc.player != null) mc.tell(() -> mc.setScreen(new BioScreen(null, mc.player.getUUID())));
                    return 1;
                }).then(ClientCommandManager.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(context.getSource().getOnlinePlayerNames(), builder))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "player");
                            Minecraft mc = context.getSource().getClient();
                            var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(name);
                            if (info == null) {
                                context.getSource().sendError(Component.literal("No player named " + name + " online"));
                                return 0;
                            }
                            UUID id = info.getProfile().getId();
                            mc.tell(() -> mc.setScreen(new BioScreen(null, id)));
                            return 1;
                        }))));

        ClientPlayNetworking.registerGlobalReceiver(BioNetwork.Reply.TYPE, (reply, context) -> {
            if (context.client().screen instanceof BioScreen screen) screen.receive(reply);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> BioScreen.clearDrafts());
    }

    private static boolean click(ChatScreen chat, double x, double y, int button) {
        if (button != 0) return false;
        Minecraft mc = Minecraft.getInstance();
        UUID target = target(mc.gui.getChat().getClickedComponentStyleAt(x, y));
        if (target == null) return false;
        mc.setScreen(new BioScreen(new ChatScreen(((ChatScreenAccessor) chat).biobook$getInput().getValue()), target));
        return true;
    }

    public static Component withPortrait(Component message, UUID sender) {
        Style style = Style.EMPTY.withInsertion(MARKER + sender)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open character journal")));
        return Component.empty().append(Component.literal("   ").setStyle(style)).append(message);
    }

    public static UUID target(Style style) {
        if (style == null || style.getInsertion() == null || !style.getInsertion().startsWith(MARKER)) return null;
        try { return UUID.fromString(style.getInsertion().substring(MARKER.length())); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public static void portrait(GuiGraphics graphics, UUID id, int x, int y, int size, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
        var skin = player == null ? DefaultPlayerSkin.get(id) : player.getSkin();
        RenderSystem.enableBlend();
        graphics.setColor(1, 1, 1, alpha);
        PlayerFaceRenderer.draw(graphics, skin.texture(), x, y, size, true, false);
        graphics.setColor(1, 1, 1, 1);
    }

    public static void chatPortrait(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color) {
        UUID[] id = new UUID[1];
        text.accept((index, style, codepoint) -> {
            id[0] = target(style);
            return false;
        });
        if (id[0] != null) portrait(graphics, id[0], x, y, 8, (color >>> 24) / 255f);
    }
}
