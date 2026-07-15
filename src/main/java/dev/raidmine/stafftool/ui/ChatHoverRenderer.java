package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.util.NicknameHitConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ChatHoverRenderer {
    private ChatHoverRenderer() {
    }

    public static void render(DrawContext context, NicknameHitConsumer.Hit hit) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (hit == null || hit.visibleToken().isEmpty()) return;

        int glow = Math.max(2, hit.height() / 3);
        for (int i = glow + 3; i >= 2; i--) {
            int alpha = Math.max(3, 25 - i * 2);
            UiTheme.roundedRect(context,
                    hit.left() - i,
                    hit.top() - Math.max(1, i / 2),
                    hit.width() + i * 2,
                    hit.height() + i,
                    Math.max(3, hit.height() / 2),
                    UiTheme.withAlpha(UiTheme.accent(), alpha));
        }
        UiTheme.roundedRect(context, hit.left() - 2, hit.top() - 1,
                hit.width() + 4, hit.height() + 2,
                Math.max(3, hit.height() / 2), UiTheme.withAlpha(UiTheme.accent2(), 52));

        int vanillaWidth = Math.max(1, client.textRenderer.getWidth(hit.visibleToken()));
        float scaleX = hit.width() / (float) vanillaWidth;
        float scaleY = hit.height() / 9F;
        float scale = Math.max(0.65F, Math.min(2.0F, Math.min(scaleX, scaleY)));

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(hit.left(), hit.top());
        context.getMatrices().scale(scale, scale);
        int cursor = 0;
        int[] codePoints = hit.visibleToken().codePoints().toArray();
        for (int i = 0; i < codePoints.length; i++) {
            String character = new String(Character.toChars(codePoints[i]));
            float t = codePoints.length <= 1 ? 0F : i / (float) (codePoints.length - 1);
            int color = UiTheme.blend(0xFFFFD86A, UiTheme.accent2(), t);
            context.drawText(client.textRenderer, Text.literal(character), cursor, 0, color, false);
            cursor += client.textRenderer.getWidth(character);
        }
        context.getMatrices().popMatrix();
    }
}
