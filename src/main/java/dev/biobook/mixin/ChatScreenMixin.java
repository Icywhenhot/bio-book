package dev.biobook.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.biobook.client.BioClient;
import dev.biobook.client.BioScreen;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow private ChatComponent.DisplayMode displayMode;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void biobook$openJournal(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return;
        Minecraft mc = Minecraft.getInstance();
        UUID[] hit = new UUID[1];
        mc.gui.hud.getChat().captureClickableText(new ActiveTextCollector() {
            private Parameters params = new Parameters(new Matrix3x2f());

            @Override
            public Parameters defaultParameters() { return params; }

            @Override
            public void defaultParameters(Parameters p) { params = p; }

            @Override
            public void accept(TextAlignment align, int x, int y, Parameters p, FormattedCharSequence text) {
                if (hit[0] != null) return;
                Vector2f pos = new Matrix3x2f(p.pose()).invert().transformPosition(new Vector2f((float) event.x(), (float) event.y()));
                if (pos.x < x || pos.x >= x + 9 || pos.y < y || pos.y >= y + 9) return;
                hit[0] = BioClient.target(text);
            }

            @Override
            public void acceptScrolling(Component text, int center, int left, int right, int top, int bottom, Parameters p) {}
        }, mc.getWindow().getGuiScaledHeight(), mc.gui.hud.getGuiTicks(), displayMode);
        if (hit[0] == null) return;
        mc.gui.hud.getChat().preserveCurrentChatScreen();
        mc.setScreenAndShow(new BioScreen(hit[0]));
        cir.setReturnValue(true);
    }
}
