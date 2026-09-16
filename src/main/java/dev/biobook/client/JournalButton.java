package dev.biobook.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class JournalButton extends Button {
    private float hover;
    private long frame = System.nanoTime();
    private final boolean accent;

    public JournalButton(int x, int y, int width, String label, boolean accent, Consumer<Button> action) {
        super(x, y, width, 22, Component.literal(label), action::accept, DEFAULT_NARRATION);
        this.accent = accent;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min((now - frame) / 1_000_000_000f, 0.1f);
        frame = now;
        hover = Mth.lerp(1 - (float) Math.exp(-18 * dt), hover, isHoveredOrFocused() && active ? 1 : 0);
        int background = accent ? blend(0xFFAA8350, 0xFFE3BE7C, hover) : blend(0xFF252C39, 0xFF3B4656, hover);
        BioScreen.rounded(g, getX(), getY(), width, height, 4, active ? background : 0xFF202530);
        g.fill(getX() + 7, getY() + height - 2, getX() + 7 + (int) ((width - 14) * hover), getY() + height - 1, 0xFFDABD87);
        var font = Minecraft.getInstance().font;
        g.text(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + 7,
                !active ? 0xFF697383 : accent ? 0xFF161B24 : 0xFFECE5D8, false);
    }

    private static int blend(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
