package dev.biobook.mixin;

import dev.biobook.client.BioScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphics.class)
public abstract class JournalTextShadowMixin {
    @ModifyVariable(method = {
            "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"
    }, at = @At("HEAD"), argsOnly = true)
    private boolean biobook$cleanText(boolean shadow) {
        return shadow && !(Minecraft.getInstance().screen instanceof BioScreen);
    }
}
