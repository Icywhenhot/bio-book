package dev.biobook.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.biobook.BioBook;
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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class BioScreen extends Screen {
    private record Draft(String name, String subtitle, String lore, String note) {}
    private static final Map<UUID, Draft> DRAFTS = new HashMap<>();
    private static final Identifier BOOK = BioBook.id("textures/gui/book_background.png");
    private static final Identifier HEAD = BioBook.id("textures/gui/player_head_icon.png");
    private static final int W = 456, H = 245;
    private static final int INK = 0xFF4A2F29, FADED = 0xFF835E56, LINE = 0xFFA3847D;
    private final UUID target;
    private final long opened = System.nanoTime();
    private long closing;
    private long frame = opened;
    private long requested;
    private boolean fetched;
    private boolean loaded;
    private boolean editing;
    private int pending = -1;
    private int left, top;
    private Profile profile = Profile.EMPTY;
    private String note = "";
    private String draftName = "", draftSubtitle = "", draftLore = "", draftNote = "";
    private String status = "Loading...";
    private EditBox nameBox, subtitleBox;
    private MultiLineEditBox loreBox, noteBox;
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
        left = (width - W) / 2;
        top = (height - H) / 2;
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
        loreBox = null;
        noteBox = null;
        addRenderableWidget(new JournalButton(left + 25, top + 72, 41, 15, "lore_button", "Lore", () -> !editing, b -> edit(false)));
        if (own()) addRenderableWidget(new JournalButton(left + 153, top + 72, 67, 15, "edit_bio_button", "Edit bio", () -> editing, b -> edit(true)));
        addRenderableWidget(new JournalButton(left + 236, top + 48, 53, 15, "notes_button", "Notes",
                () -> noteBox != null && noteBox.isFocused(), b -> { if (noteBox != null) setFocused(noteBox); }));
        if (loaded) {
            boolean idle = pending == -1;
            if (editing) {
                nameBox = field(top + 24, Profile.NAME_LIMIT, draftName, "Character name");
                subtitleBox = field(top + 40, Profile.SUBTITLE_LIMIT, draftSubtitle, "Title or tagline");
                loreBox = area(left + 26, top + 91, 85, draftLore, "Write your backstory...", "Public character lore");
                nameBox.active = subtitleBox.active = loreBox.active = idle;
            }
            noteBox = area(left + 236, top + 72, 137, draftNote, "Notes about this player...", "Private character notes");
            noteBox.active = idle;
        }
        saveButton = addRenderableWidget(new JournalButton(left + 175, top + 180, 45, 13, "save_button", "Save", () -> false, b -> save()));
        lines = font.split(Component.literal(profile.lore().isBlank() ? "Nothing here yet." : profile.lore()), 182);
        updateButton();
    }

    private EditBox field(int y, int limit, String value, String hint) {
        EditBox box = addRenderableWidget(new EditBox(font, left + 72, y, 148, 10, Component.literal(hint)));
        box.setBordered(false);
        box.setMaxLength(limit);
        box.setValue(value);
        box.setHint(Component.literal(hint).withColor(LINE));
        box.setTextColor(INK);
        box.setTextShadow(false);
        return box;
    }

    private MultiLineEditBox area(int x, int y, int h, String value, String placeholder, String label) {
        MultiLineEditBox box = addRenderableWidget(MultiLineEditBox.builder()
                .setX(x)
                .setY(y)
                .setShowBackground(false)
                .setShowDecorations(false)
                .setTextShadow(false)
                .setTextColor(INK)
                .setCursorColor(INK)
                .setPlaceholder(Component.literal(placeholder).withColor(LINE))
                .build(font, 186, h, Component.literal(label)));
        box.setCharacterLimit(Profile.TEXT_LIMIT);
        box.setValue(value);
        return box;
    }

    private void edit(boolean on) {
        if (editing == on || closing != 0 || pending > 0) return;
        capture();
        editing = on;
        scroll = scrollTarget = 0;
        tabChanged = System.nanoTime();
        rebuild();
    }

    private void capture() {
        if (nameBox != null) draftName = nameBox.getValue();
        if (subtitleBox != null) draftSubtitle = subtitleBox.getValue();
        if (loreBox != null) draftLore = loreBox.getValue();
        if (noteBox != null) draftNote = noteBox.getValue();
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
        if (bioDirty()) request(1);
        else if (noteDirty()) request(2);
        else return;
        status = "Saving...";
        rebuild();
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
        loreBox = null;
        noteBox = null;
        rememberDraft();
        if (reply.action() == 1 && !draftNote.equals(note)) {
            status = "Saving...";
            request(2);
        }
        rebuild();
    }

    private boolean bioDirty() {
        capture();
        return !draftName.equals(profile.name()) || !draftSubtitle.equals(profile.subtitle()) || !draftLore.equals(profile.lore());
    }

    private boolean noteDirty() {
        capture();
        return !draftNote.equals(note);
    }

    private void updateButton() {
        if (saveButton != null) saveButton.active = loaded && pending == -1 && (bioDirty() || noteDirty());
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
        g.blit(RenderPipelines.GUI_TEXTURED, BOOK, left, top, 0, 0, W, H, W, H);
        g.blit(RenderPipelines.GUI_TEXTURED, HEAD, left + 20, top + 17, 0, 0, 45, 47, 45, 47);
        BioClient.portrait(g, target, left + 26, top + 28, 32, 1);
        if (editing && nameBox != null) {
            underline(g, nameBox);
            underline(g, subtitleBox);
        } else {
            var player = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(target);
            String username = player == null ? "Unknown" : player.getProfile().name();
            String name = profile.name().isBlank() ? username : profile.name();
            String subtitle = profile.subtitle().isBlank() ? "@" + username : profile.subtitle();
            g.text(font, font.plainSubstrByWidth(name, 148), left + 72, top + 26, INK, false);
            g.text(font, font.plainSubstrByWidth(subtitle, 148), left + 72, top + 40, FADED, false);
        }
        if (editing) scrollbar(g, loreBox);
        else renderLore(g, now, dt);
        scrollbar(g, noteBox);
        String footer = pending == -1 && loaded && (bioDirty() || noteDirty()) ? "Unsaved draft" : status;
        g.text(font, font.plainSubstrByWidth(footer, 145), left + 27, top + 183, FADED, false);
        super.extractRenderState(g, mouseX, mouseY - lift, partialTick);
        g.pose().popMatrix();
    }

    private static void underline(GuiGraphicsExtractor g, EditBox box) {
        g.fill(box.getX(), box.getY() + 10, box.getX() + box.getWidth(), box.getY() + 11, box.isFocused() ? FADED : LINE);
    }

    private static void scrollbar(GuiGraphicsExtractor g, MultiLineEditBox box) {
        if (box == null) return;
        int max = box.maxScrollAmount();
        if (max <= 0) return;
        int x = box.getX() + box.getWidth() + 1, y = box.getY() + 2, h = box.getHeight() - 4;
        int thumb = Math.min(h, Math.max(10, h * h / (box.getInnerHeight() + 4)));
        int sy = y + (int) (box.scrollAmount() * (h - thumb) / max);
        g.fill(x, y, x + 2, y + h, 0x40835E56);
        g.fill(x, sy, x + 2, sy + thumb, FADED);
    }

    private void renderLore(GuiGraphicsExtractor g, long now, float dt) {
        int textTop = top + 94, bottom = top + 175;
        int available = bottom - textTop;
        float max = Math.max(0, lines.size() * 10 - available);
        scrollTarget = Mth.clamp(scrollTarget, 0, max);
        scroll = Mth.lerp(1 - (float) Math.exp(-16 * dt), scroll, scrollTarget);
        g.enableScissor(left + 26, textTop, left + 214, bottom);
        float transition = 1 - Mth.clamp((now - tabChanged) / 180_000_000f, 0, 1);
        int y = textTop - (int) scroll + (int) (transition * transition * 10);
        if (!loaded) g.text(font, "Loading...", left + 30, y, FADED, false);
        else for (FormattedCharSequence line : lines) {
            if (y + 9 >= textTop && y < bottom) g.text(font, line, left + 30, y, INK, false);
            y += 10;
        }
        g.disableScissor();
        if (max > 0) {
            int thumb = Math.max(10, (int) (available * available / (float) (lines.size() * 10)));
            int sy = textTop + (int) ((available - thumb) * scroll / max);
            g.fill(left + 215, textTop, left + 217, bottom, 0x40835E56);
            g.fill(left + 215, sy, left + 217, sy + thumb, FADED);
        }
    }

    private boolean overLore(double x, double y) {
        return x >= left + 25 && x < left + 221 && y >= top + 90 && y < top + 178;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!editing && overLore(x, y)) { scrollTarget -= (float) vertical * 20; return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pending > 0 && !event.isEscape()) return true;
        if (event.key() == InputConstants.KEY_S && event.hasControlDown()) { save(); return true; }
        if (!editing && getFocused() != noteBox && (event.key() == InputConstants.KEY_PAGEDOWN || event.key() == InputConstants.KEY_PAGEUP)) {
            scrollTarget += (event.key() == InputConstants.KEY_PAGEDOWN ? 1 : -1) * 80;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing != 0 || pending > 0 || System.nanoTime() - opened < 200_000_000L) return true;
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
}
