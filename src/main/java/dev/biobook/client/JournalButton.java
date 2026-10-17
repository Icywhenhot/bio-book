package dev.biobook.client;

import dev.biobook.BioBook;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class JournalButton extends Button {
    private float hover;
    private long frame = System.nanoTime();
    private final Identifier tex, tint;
    private final BooleanSupplier lit;

    public JournalButton(int x, int y, int width, int height, String name, String label, BooleanSupplier lit, Consumer<Button> action) {
        super(x, y, width, height, Component.literal(label), action::accept, DEFAULT_NARRATION);
        tex = BioBook.id("textures/gui/" + name + ".png");
        tint = BioBook.id("textures/gui/" + name + "_tint.png");
        this.lit = lit;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min((now - frame) / 1_000_000_000f, 0.1f);
        frame = now;
        hover = Mth.lerp(1 - (float) Math.exp(-18 * dt), hover, !active ? 0 : isHoveredOrFocused() ? 0.6f : lit.getAsBoolean() ? 0.3f : 0);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, getX(), getY(), 0, 0, width, height, width, height, ARGB.white(active ? 1 : 0.45f));
        if (hover > 0.01f) g.blit(RenderPipelines.GUI_TEXTURED, tint, getX(), getY(), 0, 0, width, height, width, height, ARGB.white(hover));
    }
}
