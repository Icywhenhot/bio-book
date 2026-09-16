package dev.biobook.mixin;

import dev.biobook.client.BioClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess")
public abstract class ChatFocusedPortraitMixin {
    @Shadow @Final private GuiGraphicsExtractor graphics;

    @Inject(method = "handleMessage", at = @At("HEAD"))
    private void biobook$portrait(int y, float alpha, FormattedCharSequence text, CallbackInfoReturnable<Boolean> cir) {
        BioClient.chatPortrait(graphics, text, y, alpha);
    }
}
