package dev.biobook.mixin;

import com.mojang.authlib.GameProfile;
import dev.biobook.client.BioClient;
import java.time.Instant;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
    @Redirect(method = "showMessageToPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"))
    private void biobook$mark(ChatComponent chat, Component message, MessageSignature signature, GuiMessageTag tag,
                              ChatType.Bound bound, PlayerChatMessage chatMessage, Component decorated, GameProfile profile,
                              boolean overlay, Instant time) {
        chat.addPlayerMessage(BioClient.withPortrait(message, profile.id()), signature, tag);
    }
}
