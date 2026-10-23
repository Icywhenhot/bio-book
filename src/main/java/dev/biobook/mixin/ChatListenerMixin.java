package dev.biobook.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import dev.biobook.client.BioClient;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
    @ModifyArg(method = "showMessageToPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"), index = 0)
    private Component biobook$mark(Component message, @Local(argsOnly = true) GameProfile profile) {
        return BioClient.withPortrait(message, profile.getId());
    }
}
