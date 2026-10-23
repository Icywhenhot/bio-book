package dev.biobook.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.biobook.BioBook;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class JournalButton extends Button {
    private float hover;
    private long frame = System.nanoTime();
    private final ResourceLocation tex, tint;
    private final BooleanSupplier lit;

    public JournalButton(int x, int y, int width, int height, String name, String label, BooleanSupplier lit, Consumer<Button> action) {
        super(x, y, width, height, Component.literal(label), action::accept, DEFAULT_NARRATION);
        tex = new ResourceLocation(BioBook.ID, "textures/gui/" + name + ".png");
        tint = new ResourceLocation(BioBook.ID, "textures/gui/" + name + "_tint.png");
        this.lit = lit;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min((now - frame) / 1_000_000_000f, 0.1f);
        frame = now;
        hover = Mth.lerp(1 - (float) Math.exp(-18 * dt), hover, !active ? 0 : isHoveredOrFocused() ? 0.6f : lit.getAsBoolean() ? 0.3f : 0);
        RenderSystem.enableBlend();
        g.setColor(1, 1, 1, active ? 1 : 0.45f);
        g.blit(tex, getX(), getY(), 0, 0, width, height, width, height);
        if (hover > 0.01f) {
            g.setColor(1, 1, 1, hover);
            g.blit(tint, getX(), getY(), 0, 0, width, height, width, height);
        }
        g.setColor(1, 1, 1, 1);
    }
}
