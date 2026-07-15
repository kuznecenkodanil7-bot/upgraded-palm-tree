package dev.raidmine.stafftool.mixin;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.ui.ChatHoverRenderer;
import dev.raidmine.stafftool.ui.PunishmentScreen;
import dev.raidmine.stafftool.util.AuthManager;
import dev.raidmine.stafftool.util.NicknameHitConsumer;
import dev.raidmine.stafftool.util.NicknameResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(
            method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rmtools$openFromNickname(Click click, boolean doubled,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!AuthManager.canUseMod()) return;
        if (click.button() != 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        NicknameHitConsumer hitConsumer = findHit(client, (int) click.x(), (int) click.y());
        Optional<String> nickname = hitConsumer.nickname();
        if (nickname.isEmpty()) return;

        Screen parent = (Screen) (Object) this;
        client.setScreen(new PunishmentScreen(parent, nickname.get()));
        cir.setReturnValue(true);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At("TAIL")
    )
    private void rmtools$highlightHoveredNickname(DrawContext context, int mouseX, int mouseY,
                                                   float delta, CallbackInfo ci) {
        if (!AuthManager.canUseMod()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        findHit(client, mouseX, mouseY).hit().ifPresent(hit -> {
            context.setCursor(net.minecraft.client.gui.cursor.StandardCursors.POINTING_HAND);
            ChatHoverRenderer.render(context, hit);
        });
    }

    @Inject(
            method = "sendMessage(Ljava/lang/String;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rmtools$manualOpen(String message, boolean addToHistory, CallbackInfo ci) {
        if (!AuthManager.canUseMod()) return;
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.toLowerCase().startsWith("/rmp ")) {
            String nickname = trimmed.substring(5).trim();
            if (!NicknameResolver.isValid(nickname)) return;

            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = (Screen) (Object) this;
            client.setScreen(new PunishmentScreen(parent, nickname));
            ci.cancel();
            return;
        }
        if (trimmed.startsWith("/")) {
            RaidMineStaffMod.stats().observeManualCommand(trimmed);
        }
    }

    private static NicknameHitConsumer findHit(MinecraftClient client, int mouseX, int mouseY) {
        NicknameHitConsumer hitConsumer = new NicknameHitConsumer(client.textRenderer, mouseX, mouseY);
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().render(
                    hitConsumer,
                    client.getWindow().getScaledHeight(),
                    client.inGameHud.getTicks(),
                    true
            );
        }
        return hitConsumer;
    }
}
