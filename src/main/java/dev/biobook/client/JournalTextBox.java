package dev.biobook.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

public final class JournalTextBox extends MultiLineEditBox {
    private final Font font;

    public JournalTextBox(Font font, int x, int y, int width, int height, Component placeholder, Component label) {
        super(font, x, y, width, height, placeholder, label);
        this.font = font;
    }

    @Override
    protected void renderBackground(GuiGraphics g) {
        BioScreen.rounded(g, getX() - 1, getY() - 1, width + 2, height + 2, 4, isFocused() ? 0xFFB69765 : 0xFF394454);
        BioScreen.rounded(g, getX(), getY(), width, height, 3, 0xFF101722);
    }

    @Override
    protected void renderDecorations(GuiGraphics g) {
        if (scrollbarVisible() && getMaxScrollAmount() > 0) {
            int thumb = Math.min(height, Math.max(12, height * height / (getInnerHeight() + 4)));
            int y = getY() + (int) (scrollAmount() * (height - thumb) / getMaxScrollAmount());
            g.fill(getX() + width + 2, getY(), getX() + width + 4, getY() + height, 0xFF303846);
            g.fill(getX() + width + 2, y, getX() + width + 4, y + thumb, 0xFFC5A16C);
        }
        String count = getValue().length() + " / " + dev.biobook.Profile.TEXT_LIMIT;
        g.drawString(font, count, getX() + width - font.width(count), getY() + height + 4, 0xFF77869A, false);
    }
}
