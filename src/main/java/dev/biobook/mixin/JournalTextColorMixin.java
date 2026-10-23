package dev.biobook.mixin;

import dev.biobook.client.JournalTextBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MultiLineEditBox.class)
public abstract class JournalTextColorMixin {
    @ModifyConstant(method = "renderContents", constant = {@Constant(intValue = -2039584), @Constant(intValue = -3092272)})
    private int biobook$ink(int color) {
        return (Object) this instanceof JournalTextBox ? JournalTextBox.INK : color;
    }
}
