package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public final class SettingsScreen extends Screen {
    private static final int[] PRESETS = {
            0xFFFFA31A, 0xFFFF6B00,
            0xFFFF8A45, 0xFFFF3D6E,
            0xFF8B7CFF, 0xFF5D5FEF,
            0xFF36D1A5, 0xFF0EA87A,
            0xFF45B7FF, 0xFF3978FF
    };

    private final Screen parent;
    private TextFieldWidget wordField;
    private TextFieldWidget accentField;
    private TextFieldWidget reasonField;
    private int wordOffset;
    private String status;
    private long statusAt;

    public SettingsScreen(Screen parent) {
        super(Text.literal("RM Tools — настройки"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig config = RaidMineStaffMod.config();
        wordField = field("Добавить слово", 64, "");
        accentField = field("Цвет интерфейса", 7, String.format(Locale.ROOT, "#%06X", config.accentColor & 0xFFFFFF));
        reasonField = field("Формат причины", 64, config.punishmentReasonTemplate);
    }

    private TextFieldWidget field(String label, int maxLength, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, 10, 22, Text.literal(label));
        field.setMaxLength(maxLength);
        field.setText(value);
        field.setDrawsBackground(false);
        field.setEditableColor(UiTheme.TEXT);
        field.setUneditableColor(UiTheme.FAINT);
        return field;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, UiTheme.argb(235, 5, 7, 12), UiTheme.argb(245, 12, 14, 21));
        Layout l = layout();
        UiTheme.shadow(context, l.x(), l.y(), l.w(), l.h(), 18);
        UiTheme.roundedRect(context, l.x(), l.y(), l.w(), l.h(), 18, UiTheme.BG);
        context.fillGradient(l.x() + 1, l.y() + 1, l.x() + l.w() - 1, l.y() + 4,
                UiTheme.accent(), UiTheme.accent2());

        renderHeader(context, l, mouseX, mouseY);
        renderAppearance(context, l, mouseX, mouseY);
        renderModeration(context, l, mouseX, mouseY);
        renderWords(context, l, mouseX, mouseY);
        renderStatus(context, l);
    }

    private void renderHeader(DrawContext context, Layout l, int mouseX, int mouseY) {
        UiTheme.logoGlow(context, l.x() + 20, l.y() + 13, 42, 35);
        UiTheme.textTitle(context, textRenderer, "RM Tools", l.x() + 73, l.y() + 17, UiTheme.TEXT);
        UiTheme.text(context, textRenderer, "Настройки интерфейса и контроля чата",
                l.x() + 73, l.y() + 37, UiTheme.MUTED);
        Rect close = closeRect(l);
        UiTheme.roundedRect(context, close.x(), close.y(), close.w(), close.h(), 9,
                close.contains(mouseX, mouseY) ? UiTheme.CARD_HOVER : UiTheme.CARD);
        UiTheme.icon(context, UiIcon.CLOSE, close.x() + 8, close.y() + 8, 14, UiTheme.MUTED);
    }

    private void renderAppearance(DrawContext context, Layout l, int mouseX, int mouseY) {
        int x = l.x() + 20;
        int y = l.y() + 72;
        int w = (l.w() - 52) / 2;
        int h = 210;
        card(context, x, y, w, h, "Интерфейс", UiIcon.PALETTE);
        UiTheme.text(context, textRenderer, "Основной цвет", x + 18, y + 48, UiTheme.MUTED);

        for (int i = 0; i < PRESETS.length / 2; i++) {
            Rect r = presetRect(x, y, i);
            int primary = PRESETS[i * 2];
            int secondary = PRESETS[i * 2 + 1];
            UiTheme.glow(context, r.x(), r.y(), r.w(), r.h(), 8, primary);
            UiTheme.roundedRect(context, r.x(), r.y(), r.w(), r.h(), 8, primary);
            context.fillGradient(r.x() + r.w() / 2, r.y(), r.x() + r.w(), r.y() + r.h(), primary, secondary);
            if ((RaidMineStaffMod.config().accentColor & 0xFFFFFF) == (primary & 0xFFFFFF)) {
                UiTheme.icon(context, UiIcon.CHECK, r.x() + 9, r.y() + 7, 14, UiTheme.TEXT);
            }
        }

        int fieldY = y + 114;
        accentField.setX(x + 18);
        accentField.setY(fieldY);
        accentField.setWidth(w - 90);
        input(context, accentField, "HEX");
        Rect apply = new Rect(x + w - 62, fieldY, 44, 26);
        button(context, apply, "OK", apply.contains(mouseX, mouseY), true);

        UiTheme.text(context, textRenderer, "Шрифт", x + 18, y + 154, UiTheme.MUTED);
        UiTheme.textMedium(context, textRenderer, SmoothAssets.fontName(), x + 18, y + 171, UiTheme.TEXT, true);
        UiTheme.textSmall(context, textRenderer, "Сглаживание и линейная фильтрация включены", x + 18, y + 190, UiTheme.FAINT);
    }

    private void renderModeration(DrawContext context, Layout l, int mouseX, int mouseY) {
        int w = (l.w() - 52) / 2;
        int x = l.x() + 32 + w;
        int y = l.y() + 72;
        int h = 210;
        card(context, x, y, w, h, "Наказания", UiIcon.SHIELD);

        int rowY = y + 47;
        toggle(context, new Rect(x + 18, rowY, w - 36, 30), "Автоматический скриншот",
                RaidMineStaffMod.config().autoScreenshot, mouseX, mouseY, UiIcon.SCREENSHOT);
        rowY += 36;
        toggle(context, new Rect(x + 18, rowY, w - 36, 30), "Уведомление об упоминании",
                RaidMineStaffMod.config().mentionNotifications, mouseX, mouseY, UiIcon.BELL);
        rowY += 36;
        toggle(context, new Rect(x + 18, rowY, w - 36, 30), "Контроль запрещённых слов",
                RaidMineStaffMod.config().forbiddenWordAlerts, mouseX, mouseY, UiIcon.WARN);

        UiTheme.text(context, textRenderer, "Формат причины команды", x + 18, y + 161, UiTheme.MUTED);
        reasonField.setX(x + 18);
        reasonField.setY(y + 178);
        reasonField.setWidth(w - 36);
        input(context, reasonField, "{rule} — только пункт правил");
    }

    private void renderWords(DrawContext context, Layout l, int mouseX, int mouseY) {
        int x = l.x() + 20;
        int y = l.y() + 296;
        int w = l.w() - 40;
        int h = l.h() - 316;
        card(context, x, y, w, h, "Запрещённые слова", UiIcon.WARN);
        UiTheme.text(context, textRenderer,
                "Совпадения подсвечиваются оранжевым, а верхняя панель временно показывает нарушение.",
                x + 18, y + 43, UiTheme.MUTED);

        wordField.setX(x + 18);
        wordField.setY(y + 62);
        wordField.setWidth(Math.min(340, w - 110));
        input(context, wordField, "Введите слово или фразу");
        Rect add = new Rect(wordField.getX() + wordField.getWidth() + 10, wordField.getY(), 72, 26);
        button(context, add, "ДОБАВИТЬ", add.contains(mouseX, mouseY), true);

        List<String> words = RaidMineStaffMod.config().forbiddenWords;
        int visibleRows = Math.max(1, (h - 112) / 31);
        wordOffset = Math.max(0, Math.min(Math.max(0, words.size() - visibleRows), wordOffset));
        int listY = y + 99;
        if (words.isEmpty()) {
            UiTheme.text(context, textRenderer, "Список пуст. Добавьте слова вручную.", x + 18, listY + 9, UiTheme.FAINT);
            return;
        }
        for (int i = 0; i < visibleRows && wordOffset + i < words.size(); i++) {
            int index = wordOffset + i;
            Rect row = new Rect(x + 18, listY + i * 31, w - 36, 25);
            UiTheme.roundedRect(context, row.x(), row.y(), row.w(), row.h(), 8,
                    row.contains(mouseX, mouseY) ? UiTheme.CARD_HOVER : UiTheme.CARD);
            UiTheme.roundedRect(context, row.x() + 8, row.y() + 8, 7, 7, 4, UiTheme.accent());
            UiTheme.textMedium(context, textRenderer,
                    UiTheme.ellipsize(textRenderer, words.get(index), row.w() - 65),
                    row.x() + 23, row.y() + 6, UiTheme.TEXT, true);
            Rect remove = removeRect(row);
            UiTheme.icon(context, UiIcon.TRASH, remove.x() + 5, remove.y() + 5, 13,
                    remove.contains(mouseX, mouseY) ? UiTheme.DANGER : UiTheme.FAINT);
        }
    }

    private void renderStatus(DrawContext context, Layout l) {
        if (status == null || System.currentTimeMillis() - statusAt > 2600L) return;
        int w = UiTheme.textWidth(status, 9.5F, true) + 30;
        int x = l.x() + (l.w() - w) / 2;
        int y = l.y() + l.h() - 42;
        UiTheme.glow(context, x, y, w, 28, 9, UiTheme.accent());
        UiTheme.roundedRect(context, x, y, w, 28, 9, UiTheme.PANEL_2);
        UiTheme.text(context, textRenderer, status, x + 15, y + 9, 9.5F, UiTheme.TEXT, true);
    }

    private void card(DrawContext context, int x, int y, int w, int h, String title, UiIcon icon) {
        UiTheme.roundedRect(context, x, y, w, h, 14, UiTheme.PANEL);
        UiTheme.roundedRect(context, x + 14, y + 13, 28, 28, 9, UiTheme.withAlpha(UiTheme.accent(), 62));
        UiTheme.icon(context, icon, x + 21, y + 20, 14, UiTheme.accent());
        UiTheme.textMedium(context, textRenderer, title, x + 52, y + 21, UiTheme.TEXT, true);
    }

    private void input(DrawContext context, TextFieldWidget field, String placeholder) {
        UiTheme.roundedRect(context, field.getX(), field.getY(), field.getWidth(), 26, 8,
                field.isFocused() ? UiTheme.withAlpha(UiTheme.accent(), 42) : UiTheme.CARD);
        String value = field.getText();
        UiTheme.text(context, textRenderer, value.isBlank() ? placeholder : value,
                field.getX() + 10, field.getY() + 8, 9.4F,
                value.isBlank() ? UiTheme.FAINT : UiTheme.TEXT, false);
        if (field.isFocused() && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caret = field.getX() + 10 + UiTheme.textWidth(value, 9.4F, false);
            context.fill(caret, field.getY() + 6, caret + 1, field.getY() + 20, UiTheme.accent());
        }
    }

    private void toggle(DrawContext context, Rect rect, String label, boolean value,
                        int mouseX, int mouseY, UiIcon icon) {
        UiTheme.roundedRect(context, rect.x(), rect.y(), rect.w(), rect.h(), 9,
                rect.contains(mouseX, mouseY) ? UiTheme.CARD_HOVER : UiTheme.CARD);
        UiTheme.icon(context, icon, rect.x() + 9, rect.y() + 8, 14, value ? UiTheme.accent() : UiTheme.FAINT);
        UiTheme.text(context, textRenderer, label, rect.x() + 31, rect.y() + 10, UiTheme.TEXT);
        Rect switchRect = new Rect(rect.x() + rect.w() - 43, rect.y() + 7, 34, 16);
        UiTheme.roundedRect(context, switchRect.x(), switchRect.y(), switchRect.w(), switchRect.h(), 8,
                value ? UiTheme.accent() : UiTheme.argb(255, 57, 64, 76));
        int knobX = value ? switchRect.x() + 20 : switchRect.x() + 3;
        UiTheme.roundedRect(context, knobX, switchRect.y() + 3, 10, 10, 5, UiTheme.TEXT);
    }

    private void button(DrawContext context, Rect rect, String text, boolean hovered, boolean accent) {
        int bg = accent ? (hovered ? UiTheme.blend(UiTheme.accent(), UiTheme.accent2(), 0.55F) : UiTheme.accent())
                : (hovered ? UiTheme.CARD_HOVER : UiTheme.CARD);
        UiTheme.roundedRect(context, rect.x(), rect.y(), rect.w(), rect.h(), 8, bg);
        int tw = UiTheme.textWidth(text, 8.6F, true);
        UiTheme.text(context, textRenderer, text, rect.x() + (rect.w() - tw) / 2, rect.y() + 9,
                8.6F, UiTheme.TEXT, true);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, doubled);
        Layout l = layout();
        double mx = click.x();
        double my = click.y();
        if (closeRect(l).contains(mx, my)) { close(); return true; }

        int appearanceX = l.x() + 20;
        int appearanceY = l.y() + 72;
        for (int i = 0; i < PRESETS.length / 2; i++) {
            if (presetRect(appearanceX, appearanceY, i).contains(mx, my)) {
                RaidMineStaffMod.config().setAccent(PRESETS[i * 2], PRESETS[i * 2 + 1]);
                accentField.setText(String.format(Locale.ROOT, "#%06X", PRESETS[i * 2] & 0xFFFFFF));
                showStatus("Цвет интерфейса изменён");
                return true;
            }
        }

        Rect apply = new Rect(appearanceX + (l.w() - 52) / 2 - 62, appearanceY + 114, 44, 26);
        if (apply.contains(mx, my)) { applyHex(); return true; }

        int moderationW = (l.w() - 52) / 2;
        int moderationX = l.x() + 32 + moderationW;
        int moderationY = l.y() + 72;
        Rect auto = new Rect(moderationX + 18, moderationY + 47, moderationW - 36, 30);
        Rect mention = new Rect(moderationX + 18, moderationY + 83, moderationW - 36, 30);
        Rect wordsToggle = new Rect(moderationX + 18, moderationY + 119, moderationW - 36, 30);
        if (auto.contains(mx, my)) { RaidMineStaffMod.config().autoScreenshot ^= true; save("Настройки сохранены"); return true; }
        if (mention.contains(mx, my)) { RaidMineStaffMod.config().mentionNotifications ^= true; save("Настройки сохранены"); return true; }
        if (wordsToggle.contains(mx, my)) { RaidMineStaffMod.config().forbiddenWordAlerts ^= true; save("Настройки сохранены"); return true; }

        if (wordField.mouseClicked(click, doubled)) { focusOnly(wordField); return true; }
        if (accentField.mouseClicked(click, doubled)) { focusOnly(accentField); return true; }
        if (reasonField.mouseClicked(click, doubled)) { focusOnly(reasonField); return true; }

        int wordsX = l.x() + 20;
        int wordsY = l.y() + 296;
        int wordsW = l.w() - 40;
        Rect add = new Rect(wordField.getX() + wordField.getWidth() + 10, wordField.getY(), 72, 26);
        if (add.contains(mx, my)) { addWord(); return true; }

        List<String> words = RaidMineStaffMod.config().forbiddenWords;
        int visibleRows = Math.max(1, (l.h() - 316 - 112) / 31);
        int listY = wordsY + 99;
        for (int i = 0; i < visibleRows && wordOffset + i < words.size(); i++) {
            Rect row = new Rect(wordsX + 18, listY + i * 31, wordsW - 36, 25);
            if (removeRect(row).contains(mx, my)) {
                words.remove(wordOffset + i);
                RaidMineStaffMod.config().save();
                showStatus("Слово удалено");
                return true;
            }
        }
        focusOnly(null);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout l = layout();
        int wordsY = l.y() + 296;
        if (mouseY < wordsY || mouseY > l.y() + l.h()) return false;
        int visibleRows = Math.max(1, (l.h() - 316 - 112) / 31);
        int max = Math.max(0, RaidMineStaffMod.config().forbiddenWords.size() - visibleRows);
        wordOffset = Math.max(0, Math.min(max, wordOffset + (verticalAmount < 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (input.isEnter()) {
            if (wordField.isFocused()) { addWord(); return true; }
            if (accentField.isFocused()) { applyHex(); return true; }
            if (reasonField.isFocused()) { saveReason(); return true; }
        }
        if (wordField.isFocused() && wordField.keyPressed(input)) return true;
        if (accentField.isFocused() && accentField.keyPressed(input)) return true;
        if (reasonField.isFocused() && reasonField.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (wordField.isFocused() && wordField.charTyped(input)) return true;
        if (accentField.isFocused() && accentField.charTyped(input)) return true;
        if (reasonField.isFocused() && reasonField.charTyped(input)) return true;
        return super.charTyped(input);
    }

    private void addWord() {
        String value = wordField.getText().trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) { showStatus("Введите слово"); return; }
        if (!RaidMineStaffMod.config().forbiddenWords.contains(value)) {
            RaidMineStaffMod.config().forbiddenWords.add(value);
            RaidMineStaffMod.config().save();
        }
        wordField.setText("");
        showStatus("Слово добавлено");
    }

    private void applyHex() {
        String raw = accentField.getText().trim().replace("#", "");
        try {
            int primary = Integer.parseInt(raw, 16) & 0xFFFFFF;
            int secondary = darken(primary, 0.72F);
            RaidMineStaffMod.config().setAccent(0xFF000000 | primary, 0xFF000000 | secondary);
            accentField.setText(String.format(Locale.ROOT, "#%06X", primary));
            showStatus("Цвет интерфейса изменён");
        } catch (NumberFormatException exception) {
            showStatus("HEX должен быть вида #FFA31A");
        }
    }

    private void saveReason() {
        String template = reasonField.getText().trim();
        RaidMineStaffMod.config().punishmentReasonTemplate = template.isEmpty() ? "{rule}" : template;
        RaidMineStaffMod.config().save();
        showStatus("Формат причины сохранён");
    }

    private static int darken(int rgb, float factor) {
        int r = Math.round(((rgb >> 16) & 255) * factor);
        int g = Math.round(((rgb >> 8) & 255) * factor);
        int b = Math.round((rgb & 255) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private void focusOnly(TextFieldWidget focused) {
        wordField.setFocused(wordField == focused);
        accentField.setFocused(accentField == focused);
        reasonField.setFocused(reasonField == focused);
    }

    private void save(String message) {
        saveReasonSilently();
        RaidMineStaffMod.config().save();
        showStatus(message);
    }

    private void saveReasonSilently() {
        if (reasonField != null) {
            String template = reasonField.getText().trim();
            RaidMineStaffMod.config().punishmentReasonTemplate = template.isEmpty() ? "{rule}" : template;
        }
    }

    private void showStatus(String message) {
        status = message;
        statusAt = System.currentTimeMillis();
    }

    @Override
    public void close() {
        saveReasonSilently();
        RaidMineStaffMod.config().save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private Layout layout() {
        int w = Math.min(900, width - 32);
        int h = Math.min(650, height - 28);
        return new Layout((width - w) / 2, (height - h) / 2, w, h);
    }

    private Rect closeRect(Layout l) { return new Rect(l.x() + l.w() - 48, l.y() + 16, 30, 30); }
    private Rect presetRect(int x, int y, int index) { return new Rect(x + 18 + index * 43, y + 68, 34, 28); }
    private Rect removeRect(Rect row) { return new Rect(row.x() + row.w() - 28, row.y() + 1, 24, 23); }

    private record Layout(int x, int y, int w, int h) {}
    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
}
