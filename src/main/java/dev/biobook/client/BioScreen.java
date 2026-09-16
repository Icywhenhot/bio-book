package dev.biobook.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.biobook.BioNetwork;
import dev.biobook.Profile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class BioScreen extends Screen {
    private record Draft(String name, String subtitle, String lore, String note) {}
    private static final Map<UUID, Draft> DRAFTS = new HashMap<>();
    private final UUID target;
    private final long opened = System.nanoTime();
    private long closing;
    private long frame = opened;
    private long requested;
    private boolean fetched;
    private boolean loaded;
    private int pending = -1;
    private int tab;
    private int left, top, panelWidth, panelHeight, bodyTop, bodyHeight;
    private Profile profile = Profile.EMPTY;
    private String note = "";
    private String draftName = "", draftSubtitle = "", draftLore = "", draftNote = "";
    private String status = "Opening journal...";
    private EditBox nameBox, subtitleBox;
    private MultiLineEditBox textBox;
    private JournalButton saveButton;
    private List<FormattedCharSequence> lines = List.of();
    private float scroll, scrollTarget;
    private long tabChanged = opened;

    public BioScreen(UUID target) {
        super(Component.literal("Bio Book"));
        this.target = target;
    }

    public static void clearDrafts() { DRAFTS.clear(); }

    private boolean own() { return minecraft.player != null && minecraft.player.getUUID().equals(target); }

    @Override
    protected void init() {
        capture();
        panelWidth = Math.min(520, width - 24);
        panelHeight = Math.min(342, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        bodyTop = top + 104;
        bodyHeight = Math.max(24, panelHeight - 148);
        rebuild();
        if (!fetched) {
            fetched = true;
            request(0);
        }
    }

    private void rebuild() {
        clearWidgets();
        nameBox = null;
        subtitleBox = null;
        textBox = null;
        int x = left + 16;
        addRenderableWidget(new JournalButton(x, top + 72, 72, "Lore", tab == 0, b -> changeTab(0)));
        addRenderableWidget(new JournalButton(x + 78, top + 72, 88, "My notes", tab == 1, b -> changeTab(1)));
        if (own()) addRenderableWidget(new JournalButton(x + 172, top + 72, 80, "Edit bio", tab == 2, b -> changeTab(2)));
        addRenderableWidget(new JournalButton(left + panelWidth - 38, top + 12, 24, "x", false, b -> onClose()));
        if (tab > 0 && loaded) {
            int editorTop = bodyTop + 15;
            if (tab == 2) {
                int half = (panelWidth - 42) / 2;
                nameBox = addRenderableWidget(new EditBox(font, x + 1, editorTop, half, 18, Component.literal("Character name")));
                nameBox.setMaxLength(Profile.NAME_LIMIT);
                nameBox.setValue(draftName);
                nameBox.setHint(Component.literal("Character name"));
                nameBox.setTextShadow(false);
                subtitleBox = addRenderableWidget(new EditBox(font, x + half + 10, editorTop, half, 18, Component.literal("Title or tagline")));
                subtitleBox.setMaxLength(Profile.SUBTITLE_LIMIT);
                subtitleBox.setValue(draftSubtitle);
                subtitleBox.setHint(Component.literal("Title or tagline"));
                subtitleBox.setTextShadow(false);
                editorTop += 25;
            }
            textBox = addRenderableWidget(MultiLineEditBox.builder()
                    .setX(x)
                    .setY(editorTop)
                    .setShowBackground(false)
                    .setShowDecorations(false)
                    .setTextShadow(false)
                    .setPlaceholder(Component.literal(tab == 1 ? "What will you remember about them?" : "Every character has a story. Tell yours..."))
                    .build(font, panelWidth - 32, Math.max(20, bodyTop + bodyHeight - editorTop),
                            Component.literal(tab == 1 ? "Private character notes" : "Public character lore")));
            textBox.setCharacterLimit(Profile.TEXT_LIMIT);
            textBox.setValue(tab == 1 ? draftNote : draftLore);
        }
        saveButton = addRenderableWidget(new JournalButton(left + panelWidth - 116, top + panelHeight - 31, 100,
                tab == 0 ? "Back to chat" : "Save changes", true, b -> {
                    if (tab == 0) onClose(); else save();
                }));
        lines = font.split(Component.literal(profile.lore().isBlank() ? "An unwritten chapter.\n\nThis character has not shared their story yet." : profile.lore()), panelWidth - 58);
        updateButton();
    }

    private void changeTab(int next) {
        if (tab == next || closing != 0 || pending > 0) return;
        capture();
        tab = next;
        scroll = scrollTarget = 0;
        tabChanged = System.nanoTime();
        rebuild();
    }

    private void capture() {
        if (nameBox != null) draftName = nameBox.getValue();
        if (subtitleBox != null) draftSubtitle = subtitleBox.getValue();
        if (textBox != null) {
            if (tab == 1) draftNote = textBox.getValue();
            if (tab == 2) draftLore = textBox.getValue();
        }
    }

    private void request(int action) {
        if (minecraft.getConnection() == null) return;
        pending = action;
        requested = System.nanoTime();
        ClientPlayNetworking.send(new BioNetwork.Request(target, action, action == 1 ? draftName : "",
                action == 1 ? draftSubtitle : "", action == 1 ? draftLore : action == 2 ? draftNote : ""));
    }

    private void save() {
        if (!loaded || pending != -1) return;
        capture();
        status = "Saving...";
        request(tab == 1 ? 2 : 1);
        if (textBox != null) textBox.active = false;
        if (nameBox != null) nameBox.active = false;
        if (subtitleBox != null) subtitleBox.active = false;
        updateButton();
    }

    public void receive(BioNetwork.Reply reply) {
        if (!reply.target().equals(target) || reply.action() != pending) return;
        capture();
        profile = reply.profile();
        note = reply.note();
        if (!loaded) {
            Draft draft = DRAFTS.get(target);
            draftName = draft == null ? profile.name() : draft.name;
            draftSubtitle = draft == null ? profile.subtitle() : draft.subtitle;
            draftLore = draft == null ? profile.lore() : draft.lore;
            draftNote = draft == null ? note : draft.note;
            loaded = true;
        } else if (reply.action() == 1) {
            draftName = profile.name(); draftSubtitle = profile.subtitle(); draftLore = profile.lore();
        } else if (reply.action() == 2) {
            draftNote = note;
        }
        status = reply.action() == 0 ? "" : "Saved to your journal";
        pending = -1;
        nameBox = null;
        subtitleBox = null;
        textBox = null;
        rememberDraft();
        rebuild();
    }

    private boolean dirty() {
        capture();
        return tab == 1 ? !draftNote.equals(note) : !draftName.equals(profile.name()) || !draftSubtitle.equals(profile.subtitle()) || !draftLore.equals(profile.lore());
    }

    private void updateButton() {
        if (saveButton != null) saveButton.active = tab == 0 || loaded && pending == -1 && dirty();
    }

    private void rememberDraft() {
        if (!loaded) return;
        capture();
        if (draftName.equals(profile.name()) && draftSubtitle.equals(profile.subtitle()) && draftLore.equals(profile.lore()) && draftNote.equals(note)) DRAFTS.remove(target);
        else DRAFTS.put(target, new Draft(draftName, draftSubtitle, draftLore, draftNote));
    }

    private void leave() {
        minecraft.setScreenAndShow(minecraft.gui.hud.getChat().restoreChatScreen());
    }

    @Override
    public void tick() {
        if (minecraft.player == null) { minecraft.setScreenAndShow(null); return; }
        updateButton();
        if (pending != -1 && System.nanoTime() - requested > 8_000_000_000L) {
            pending = -1;
            status = "No response. Reopen to retry.";
            rebuild();
        }
        if (closing != 0 && System.nanoTime() - closing > 150_000_000L) leave();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min((now - frame) / 1_000_000_000f, 0.1f);
        frame = now;
        float progress = Mth.clamp((now - opened) / 240_000_000f, 0, 1);
        float ease = 1 - (float) Math.pow(1 - progress, 3);
        if (closing != 0) ease *= 1 - Mth.clamp((now - closing) / 150_000_000f, 0, 1);
        g.fillGradient(0, 0, width, height, ((int) (175 * ease) << 24) | 0x080D17, ((int) (220 * ease) << 24) | 0x080D17);
        int lift = (int) ((1 - ease) * 18);
        g.pose().pushMatrix();
        g.pose().translate(0, lift);
        rounded(g, left - 1, top - 1, panelWidth + 2, panelHeight + 2, 8, 0xFF514734);
        rounded(g, left, top, panelWidth, panelHeight, 8, 0xFF151C28);
        g.fillGradient(left + 8, top + 1, left + panelWidth - 8, top + 65, 0xFF263141, 0xFF151C28);
        g.fill(left + 16, top + 64, left + panelWidth - 16, top + 65, 0xFF343A43);
        rounded(g, left + 15, top + 13, 42, 42, 5, 0xFFAA8350);
        BioClient.portrait(g, target, left + 20, top + 18, 32, 1);
        var player = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(target);
        String username = player == null ? "Unknown traveler" : player.getProfile().name();
        String name = profile.name().isBlank() ? username : profile.name();
        g.text(font, "B I O  B O O K", left + 68, top + 13, 0xFFD0AA70, false);
        g.text(font, font.plainSubstrByWidth(name, panelWidth - 118), left + 68, top + 29, 0xFFF3EBDD, false);
        String subtitle = profile.subtitle().isBlank() ? "@" + username + "  /  Character journal" : profile.subtitle();
        g.text(font, font.plainSubstrByWidth(subtitle, panelWidth - 90), left + 68, top + 44, 0xFF9AA8BB, false);
        if (tab == 0) renderLore(g, now, dt);
        else {
            g.text(font, tab == 1 ? "PRIVATE  /  Only you can read these notes" : "PUBLIC  /  Your name, title and story", left + 16, bodyTop, 0xFFB6A78E, false);
            editorFrame(g);
        }
        String footer = tab > 0 && dirty() && pending == -1 ? "Unsaved draft kept until disconnect" : status;
        g.text(font, font.plainSubstrByWidth(footer, panelWidth - 145), left + 16, top + panelHeight - 23, 0xFF9AA8BB, false);
        super.extractRenderState(g, mouseX, mouseY - lift, partialTick);
        g.pose().popMatrix();
    }

    private void editorFrame(GuiGraphicsExtractor g) {
        if (nameBox != null) boxFrame(g, nameBox.getX(), nameBox.getY(), nameBox.getWidth(), nameBox.getHeight(), nameBox.isFocused());
        if (subtitleBox != null) boxFrame(g, subtitleBox.getX(), subtitleBox.getY(), subtitleBox.getWidth(), subtitleBox.getHeight(), subtitleBox.isFocused());
        if (textBox == null) return;
        int x = textBox.getX(), y = textBox.getY(), w = textBox.getWidth(), h = textBox.getHeight();
        boxFrame(g, x, y, w, h, textBox.isFocused());
        int max = textBox.maxScrollAmount();
        if (max > 0) {
            int thumb = Math.min(h, Math.max(12, h * h / (textBox.getInnerHeight() + 4)));
            int sy = y + (int) (textBox.scrollAmount() * (h - thumb) / max);
            g.fill(x + w + 2, y, x + w + 4, y + h, 0xFF303846);
            g.fill(x + w + 2, sy, x + w + 4, sy + thumb, 0xFFC5A16C);
        }
        String count = textBox.getValue().length() + " / " + Profile.TEXT_LIMIT;
        g.text(font, count, x + w - font.width(count), y + h + 4, 0xFF77869A, false);
    }

    private static void boxFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean focused) {
        rounded(g, x - 1, y - 1, w + 2, h + 2, 4, focused ? 0xFFB69765 : 0xFF394454);
        rounded(g, x, y, w, h, 3, 0xFF101722);
    }

    private void renderLore(GuiGraphicsExtractor g, long now, float dt) {
        g.text(font, "THE STORY SO FAR", left + 24, bodyTop, 0xFFD0AA70, false);
        int textTop = bodyTop + 20;
        int available = bodyHeight - 20;
        float max = Math.max(0, lines.size() * 13 - available);
        scrollTarget = Mth.clamp(scrollTarget, 0, max);
        scroll = Mth.lerp(1 - (float) Math.exp(-16 * dt), scroll, scrollTarget);
        g.enableScissor(left + 20, textTop, left + panelWidth - 20, bodyTop + bodyHeight);
        float transition = 1 - Mth.clamp((now - tabChanged) / 180_000_000f, 0, 1);
        int y = textTop - (int) scroll + (int) (transition * transition * 10);
        if (!loaded) g.text(font, "Turning to their chapter...", left + 24, y, 0xFF9AA8BB, false);
        else for (FormattedCharSequence line : lines) {
            if (y + 9 >= textTop && y < bodyTop + bodyHeight) g.text(font, line, left + 24, y, 0xFFD5DCE5, false);
            y += 13;
        }
        g.disableScissor();
        if (max > 0) {
            int track = Math.max(12, available);
            int thumb = Math.max(12, (int) (track * available / (float) (lines.size() * 13)));
            int sy = textTop + (int) ((track - thumb) * scroll / max);
            g.fill(left + panelWidth - 19, textTop, left + panelWidth - 17, textTop + track, 0xFF303846);
            g.fill(left + panelWidth - 19, sy, left + panelWidth - 17, sy + thumb, 0xFFC5A16C);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (tab == 0) { scrollTarget -= (float) vertical * 26; return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pending > 0 && !event.isEscape()) return true;
        if (event.key() == InputConstants.KEY_S && event.hasControlDown() && tab > 0) { save(); return true; }
        if (tab == 0 && (event.key() == InputConstants.KEY_PAGEDOWN || event.key() == InputConstants.KEY_PAGEUP)) {
            scrollTarget += (event.key() == InputConstants.KEY_PAGEDOWN ? 1 : -1) * bodyHeight;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing != 0 || System.nanoTime() - opened < 200_000_000L) return true;
        if (pending > 0 && event.y() >= bodyTop && event.y() < bodyTop + bodyHeight) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return pending > 0 || super.charTyped(event);
    }

    @Override
    public void onClose() {
        rememberDraft();
        if (closing == 0) closing = System.nanoTime();
    }

    @Override
    public void removed() { rememberDraft(); }

    @Override
    public boolean isPauseScreen() { return false; }

    public static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        g.fill(x + radius, y, x + w - radius, y + h, color);
        g.fill(x, y + radius, x + w, y + h - radius, color);
        for (int row = 0; row < radius; row++) {
            int inset = radius - (int) Math.sqrt(radius * radius - (radius - row - 1) * (radius - row - 1));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }
}
