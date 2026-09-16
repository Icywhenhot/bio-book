package dev.biobook.mixin;

import dev.biobook.client.BioClient;
import dev.biobook.client.BioScreen;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void biobook$openJournal(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) return;
        UUID target = BioClient.portraitAt(event.x(), event.y());
        if (target == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.gui.hud.getChat().preserveCurrentChatScreen();
        mc.setScreenAndShow(new BioScreen(target));
        cir.setReturnValue(true);
    }
}
