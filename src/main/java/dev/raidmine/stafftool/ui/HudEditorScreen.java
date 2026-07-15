package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.util.AuthManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class HudEditorScreen extends Screen {
    private DragMode dragMode = DragMode.NONE;
    private double dragOffsetX;
    private double dragOffsetY;
    private int resizeStartX;
    private int resizeAnchorX;
    private int resizeAnchorY;
    private float resizeStartScale;
    private long centeredAt;
    private long openedAt;

    public HudEditorScreen() {
        super(Text.literal("RM Tools HUD editor"));
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, UiTheme.argb(160, 3, 5, 10));
        drawGuides(context);
        HudOverlay.renderEditable(context);
        renderCenterLogo(context, mouseX, mouseY);
        renderBottomHint(context);

        if (System.currentTimeMillis() - centeredAt < 1300L) {
            String centered = "Панель отцентрирована";
            int w = UiTheme.textWidth(centered, 10F, true) + 34;
            int tx = (width - w) / 2;
            UiTheme.glow(context, tx, 42, w, 30, 10, UiTheme.accent());
            UiTheme.roundedRect(context, tx, 42, w, 30, 10, UiTheme.PANEL_2);
            UiTheme.icon(context, UiIcon.CENTER, tx + 9, 50, 14, UiTheme.accent());
            UiTheme.text(context, textRenderer, centered, tx + 28, 51, 10F, UiTheme.TEXT, true);
        }
    }

    private void renderCenterLogo(DrawContext context, int mouseX, int mouseY) {
        Rect logo = centerLogoRect();
        boolean hovered = logo.contains(mouseX, mouseY);
        float appear = UiTheme.easeOutBack(Math.min(1F, (System.currentTimeMillis() - openedAt) / 260F));
        int glow = hovered ? 18 : 12;
        UiTheme.glow(context, logo.x(), logo.y(), logo.w(), logo.h(), glow, UiTheme.accent());
        UiTheme.roundedRect(context, logo.x() - 18, logo.y() - 18, logo.w() + 36, logo.h() + 36,
                28, UiTheme.argb(138, 10, 12, 18));
        UiTheme.logoGlow(context, logo.x(), logo.y(), logo.w(), logo.h());
        renderLogoParticles(context, logo, hovered, appear);

        String title = "Нажмите на логотип, чтобы открыть настройки";
        int titleW = UiTheme.textWidth(title, UiTheme.FONT_MEDIUM, true);
        UiTheme.textMedium(context, textRenderer, title, (width - titleW) / 2, logo.y() + logo.h() + 18,
                hovered ? UiTheme.accent() : UiTheme.TEXT, true);
        String subtitle = "Right Shift — центрирование • стрелки — сдвиг панели";
        int subW = UiTheme.textWidth(subtitle, UiTheme.FONT_SMALL, false);
        UiTheme.text(context, textRenderer, subtitle, (width - subW) / 2, logo.y() + logo.h() + 36,
                UiTheme.FONT_SMALL, UiTheme.MUTED, false);
    }

    private void renderLogoParticles(DrawContext context, Rect logo, boolean hovered, float appear) {
        long now = System.currentTimeMillis();
        int cx = logo.x() + logo.w() / 2;
        int cy = logo.y() + logo.h() / 2;
        int count = hovered ? 12 : 6;
        float radius = hovered ? 92F : 74F;
        for (int i = 0; i < count; i++) {
            float angle = (now / 460F) + i * (6.283185F / count);
            float localRadius = radius + (float) Math.sin(now / 190F + i) * 9F;
            int px = Math.round(cx + (float) Math.cos(angle) * localRadius);
            int py = Math.round(cy + (float) Math.sin(angle) * localRadius * 0.55F);
            int size = hovered ? 4 + (i % 3) : 3;
            int color = UiTheme.blend(UiTheme.accent(), 0xFFFFD24A, i / (float) Math.max(1, count - 1));
            UiTheme.glow(context, px - size, py - size, size * 2, size * 2, 6, color);
            UiTheme.roundedRect(context, px - size / 2, py - size / 2, size, size, size / 2, UiTheme.withAlpha(color, Math.round(190F * appear)));
        }
    }

    private void renderBottomHint(DrawContext context) {
        String title = "Настройка панели RM Tools";
        int titleW = UiTheme.textWidth(title, UiTheme.FONT_TITLE, true);
        int boxW = Math.max(440, titleW + 36);
        int x = (width - boxW) / 2;
        int y = height - 62;
        UiTheme.shadow(context, x, y, boxW, 46, 13);
        UiTheme.roundedRect(context, x, y, boxW, 46, 13, UiTheme.argb(235, 18, 22, 32));
        UiTheme.textTitle(context, textRenderer, title, (width - titleW) / 2, y + 7, UiTheme.TEXT);
        String hint = "Перетаскивание • угол — размер • стрелки — сдвиг • Right Shift — центр • клик по лого — настройки";
        int hintW = UiTheme.textWidth(hint, UiTheme.FONT_SMALL, false);
        UiTheme.text(context, textRenderer, hint, (width - hintW) / 2, y + 27,
                UiTheme.FONT_SMALL, UiTheme.MUTED, false);
    }

    private void drawGuides(DrawContext context) {
        int centerX = width / 2;
        int centerY = height / 2;
        context.fill(centerX, 0, centerX + 1, height, UiTheme.argb(42, 255, 138, 0));
        context.fill(0, centerY, width, centerY + 1, UiTheme.argb(22, 255, 138, 0));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!AuthManager.canUseMod()) return true;
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (centerLogoRect().contains(click.x(), click.y())) {
            MinecraftClient.getInstance().setScreen(new SettingsScreen(this));
            return true;
        }
        HudOverlay.Layout layout = HudOverlay.layout(width, height);
        if (layout.resizeHandle().contains(click.x(), click.y())) {
            dragMode = DragMode.RESIZE;
            resizeStartX = (int) click.x();
            resizeAnchorX = layout.x();
            resizeAnchorY = layout.y();
            resizeStartScale = RaidMineStaffMod.config().hudScale;
            return true;
        }
        if (layout.contains(click.x(), click.y())) {
            dragMode = DragMode.MOVE;
            dragOffsetX = click.x() - layout.x();
            dragOffsetY = click.y() - layout.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragMode == DragMode.MOVE) {
            HudOverlay.setPosition(width, height,
                    (int) Math.round(click.x() - dragOffsetX),
                    (int) Math.round(click.y() - dragOffsetY));
            return true;
        }
        if (dragMode == DragMode.RESIZE) {
            float deltaScale = ((float) click.x() - resizeStartX) / HudOverlay.BASE_WIDTH;
            HudOverlay.setScale(resizeStartScale + deltaScale);
            HudOverlay.setPosition(width, height, resizeAnchorX, resizeAnchorY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragMode != DragMode.NONE) {
            dragMode = DragMode.NONE;
            RaidMineStaffMod.config().save();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        HudOverlay.Layout layout = HudOverlay.layout(width, height);
        if (!layout.contains(mouseX, mouseY)) return false;
        HudOverlay.setScale(RaidMineStaffMod.config().hudScale + (verticalAmount > 0 ? 0.05F : -0.05F));
        RaidMineStaffMod.config().save();
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        switch (input.key()) {
            case GLFW.GLFW_KEY_R -> {
                HudOverlay.reset();
                centeredAt = System.currentTimeMillis();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT_SHIFT, GLFW.GLFW_KEY_C -> {
                HudOverlay.centerTop();
                centeredAt = System.currentTimeMillis();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                HudOverlay.nudge(width, height, 0, -2);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                HudOverlay.nudge(width, height, 0, 2);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                HudOverlay.nudge(width, height, -2, 0);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                HudOverlay.nudge(width, height, 2, 0);
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                close();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        RaidMineStaffMod.config().save();
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private Rect centerLogoRect() {
        int w = 174;
        int h = 140;
        return new Rect((width - w) / 2, (height - h) / 2 - 34, w, h);
    }

    private enum DragMode { NONE, MOVE, RESIZE }

    private record Rect(int x, int y, int w, int h) {
        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}
