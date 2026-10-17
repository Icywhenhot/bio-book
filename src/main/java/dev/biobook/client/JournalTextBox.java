package dev.biobook.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

public final class JournalTextBox extends MultiLineEditBox {
    public static final int INK = 0xFF4A2F29, FADED = 0xFF835E56, LINE = 0xFFA3847D;

    public JournalTextBox(Font font, int x, int y, int width, int height, Component placeholder, Component label) {
        super(font, x, y, width, height, placeholder, label);
    }

    @Override
    protected void renderBackground(GuiGraphics g) {}

    @Override
    protected void renderDecorations(GuiGraphics g) {
        int max = getMaxScrollAmount();
        if (!scrollbarVisible() || max <= 0) return;
        int x = getX() + width + 1, y = getY() + 2, h = height - 4;
        int thumb = Math.min(h, Math.max(10, h * h / (getInnerHeight() + 4)));
        int sy = y + (int) (scrollAmount() * (h - thumb) / max);
        g.fill(x, y, x + 2, y + h, 0x40835E56);
        g.fill(x, sy, x + 2, sy + thumb, FADED);
    }
}
