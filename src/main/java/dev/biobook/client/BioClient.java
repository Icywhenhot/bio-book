package dev.biobook.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.biobook.BioBook;
import dev.biobook.BioNetwork;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import dev.biobook.mixin.ChatScreenAccessor;
import org.lwjgl.glfw.GLFW;

@Mod(value = BioBook.ID, dist = Dist.CLIENT)
public final class BioClient {
    private static final String MARKER = "biobook:";
    private static final KeyMapping OPEN = new KeyMapping("key.biobook.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.biobook");

    public BioClient(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent event) -> event.register(OPEN));
        NeoForge.EVENT_BUS.addListener(BioClient::chat);
        NeoForge.EVENT_BUS.addListener(BioClient::click);
        NeoForge.EVENT_BUS.addListener(BioClient::tick);
        NeoForge.EVENT_BUS.addListener(BioClient::commands);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> BioScreen.clearDrafts());
        BioNetwork.receiver = reply -> {
            if (Minecraft.getInstance().screen instanceof BioScreen screen) screen.receive(reply);
        };
    }

    private static void chat(ClientChatReceivedEvent.Player event) {
        Style style = Style.EMPTY.withInsertion(MARKER + event.getSender())
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open character journal")));
        event.setMessage(Component.empty().append(Component.literal("   ").setStyle(style)).append(event.getMessage()));
    }

    private static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen chat) || event.getButton() != 0) return;
        Minecraft mc = Minecraft.getInstance();
        UUID target = target(mc.gui.getChat().getClickedComponentStyleAt(event.getMouseX(), event.getMouseY()));
        if (target == null) return;
        event.setCanceled(true);
        mc.setScreen(new BioScreen(new ChatScreen(((ChatScreenAccessor) chat).biobook$getInput().getValue()), target));
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new BioScreen(null, mc.player.getUUID()));
        }
    }

    private static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bio").executes(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.tell(() -> mc.setScreen(new BioScreen(null, mc.player.getUUID())));
            return 1;
        }).then(Commands.argument("player", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(context.getSource().getOnlinePlayerNames(), builder))
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "player");
                    Minecraft mc = Minecraft.getInstance();
                    var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(name);
                    if (info == null) {
                        context.getSource().sendFailure(Component.literal("No player named " + name + " online"));
                        return 0;
                    }
                    UUID id = info.getProfile().getId();
                    mc.tell(() -> mc.setScreen(new BioScreen(null, id)));
                    return 1;
                })));
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
