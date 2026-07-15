package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.util.AuthManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class LoginScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget passwordField;
    private String status = "Введите пароль персонала";
    private boolean error;
    private long openedAt;

    public LoginScreen(Screen parent) {
        super(Text.literal("RM Tools — вход"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        passwordField = new TextFieldWidget(textRenderer, 0, 0, 10, 22, Text.literal("Пароль"));
        passwordField.setMaxLength(32);
        passwordField.setDrawsBackground(false);
        openedAt = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, UiTheme.argb(245, 3, 3, 4), UiTheme.argb(250, 8, 8, 11));

        float appear = UiTheme.easeOutBack(Math.min(1F, (System.currentTimeMillis() - openedAt) / 260F));
        int panelW = Math.min(540, width - 40);
        int panelH = 320;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2 + Math.round((1F - appear) * 18F);

        UiTheme.shadow(context, x, y, panelW, panelH, 22);
        UiTheme.roundedRect(context, x, y, panelW, panelH, 22, UiTheme.BG);
        context.fillGradient(x + 1, y + 1, x + panelW - 1, y + 5, UiTheme.accent(), UiTheme.accent2());

        UiTheme.logoGlow(context, x + (panelW - 126) / 2, y + 22, 126, 102);
        UiTheme.textTitle(context, textRenderer, "RM Tools", x + (panelW - UiTheme.textWidth("RM Tools", UiTheme.FONT_TITLE, true)) / 2, y + 136, UiTheme.TEXT);
        UiTheme.text(context, textRenderer, "Логин определяется автоматически", x + 0 + (panelW - UiTheme.textWidth("Логин определяется автоматически", UiTheme.FONT_SIZE, false)) / 2, y + 157, UiTheme.MUTED);

        String username = AuthManager.currentSessionName(MinecraftClient.getInstance());
        boolean allowed = AuthManager.isAllowedUsername(username);
        UiTheme.roundedRect(context, x + 42, y + 188, panelW - 84, 42, 12, UiTheme.CARD);
        UiTheme.text(context, textRenderer, "Логин", x + 58, y + 200, UiTheme.FAINT);
        UiTheme.textMedium(context, textRenderer, username.isBlank() ? "Не найден" : username, x + 134, y + 198, allowed ? UiTheme.TEXT : UiTheme.DANGER, true);

        if (allowed) {
            int fieldX = x + 42;
            int fieldY = y + 242;
            int fieldW = panelW - 84;
            UiTheme.roundedRect(context, fieldX, fieldY, fieldW, 42, 12, passwordField.isFocused() ? UiTheme.CARD_HOVER : UiTheme.CARD);
            passwordField.setDimensionsAndPosition(fieldX, fieldY, fieldW, 42);
            renderPasswordField(context);

            Rect login = new Rect(x + panelW - 150, y + panelH - 54, 108, 34);
            boolean hovered = login.contains(mouseX, mouseY);
            UiTheme.glow(context, login.x(), login.y(), login.w(), login.h(), 10, UiTheme.accent());
            UiTheme.roundedRect(context, login.x(), login.y(), login.w(), login.h(), 11, hovered ? UiTheme.blend(UiTheme.accent(), UiTheme.accent2(), 0.32F) : UiTheme.accent());
            UiTheme.textBold(context, textRenderer, "ВОЙТИ", login.x() + 24, login.y() + 11, UiTheme.TEXT);
        } else {
            UiTheme.roundedRect(context, x + 42, y + 242, panelW - 84, 42, 12, UiTheme.argb(96, 95, 28, 30));
            UiTheme.text(context, textRenderer, "Этот ник не входит в список персонала. Мод отключён.", x + 56, y + 256, UiTheme.DANGER);
        }

        UiTheme.text(context, textRenderer, status,
                x + 42, y + panelH - 42,
                error ? UiTheme.DANGER : UiTheme.MUTED);
    }

    private void renderPasswordField(DrawContext context) {
        String raw = passwordField.getText();
        String masked = "•".repeat(Math.max(0, raw.length()));
        int x = passwordField.getX() + 16;
        int y = passwordField.getY() + 13;
        if (raw.isBlank()) {
            UiTheme.text(context, textRenderer, "Введите пароль", x, y, UiTheme.FAINT);
        } else {
            UiTheme.textMedium(context, textRenderer, masked, x, y - 1, UiTheme.TEXT, true);
        }
        if (passwordField.isFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caretX = x + UiTheme.textWidth(masked, UiTheme.FONT_MEDIUM, true) + 1;
            context.fill(caretX, y - 1, caretX + 1, y + 12, UiTheme.accent());
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        String username = AuthManager.currentSessionName(MinecraftClient.getInstance());
        boolean allowed = AuthManager.isAllowedUsername(username);
        if (allowed && passwordField.mouseClicked(click, doubled)) {
            return true;
        }
        int panelW = Math.min(540, width - 40);
        int panelH = 320;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        Rect login = new Rect(x + panelW - 150, y + panelH - 54, 108, 34);
        if (allowed && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && login.contains(click.x(), click.y())) {
            attemptLogin();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            attemptLogin();
            return true;
        }
        return passwordField != null && passwordField.keyPressed(input) || super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return passwordField != null && passwordField.charTyped(input) || super.charTyped(input);
    }

    private void attemptLogin() {
        String username = AuthManager.currentSessionName(MinecraftClient.getInstance());
        if (!AuthManager.isAllowedUsername(username)) {
            status = "Доступ запрещён для этого аккаунта";
            error = true;
            return;
        }
        if (AuthManager.login(passwordField.getText())) {
            error = false;
            status = "Вход выполнен";
            MinecraftClient.getInstance().setScreen(parent);
        } else {
            error = true;
            status = "Неверный пароль";
            passwordField.setText("");
        }
    }

    @Override
    public void close() {
        if (AuthManager.canUseMod()) {
            MinecraftClient.getInstance().setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Rect(int x, int y, int w, int h) {
        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}
