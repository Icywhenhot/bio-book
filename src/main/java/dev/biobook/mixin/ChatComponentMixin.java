package dev.biobook.mixin;

import dev.biobook.client.BioClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private int biobook$portrait(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color) {
        int end = graphics.drawString(font, text, x, y, color);
        BioClient.chatPortrait(graphics, text, x, y, color);
        return end;
    }
}
