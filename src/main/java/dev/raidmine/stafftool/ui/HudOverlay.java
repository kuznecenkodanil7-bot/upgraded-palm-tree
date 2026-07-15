package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.chat.UiNotificationCenter;
import dev.raidmine.stafftool.rules.PunishmentType;
import dev.raidmine.stafftool.stats.SessionStats;
import dev.raidmine.stafftool.util.AuthManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class HudOverlay {
    public static final int BASE_WIDTH = 292;
    public static final int BASE_HEIGHT = 26;

    private HudOverlay() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HudEditorScreen) return;
        renderInternal(context, false);
    }

    public static void renderEditable(DrawContext context) {
        renderInternal(context, true);
    }

    private static void renderInternal(DrawContext context, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null || !RaidMineStaffMod.config().hudEnabled || !AuthManager.canUseMod()) {
            return;
        }

        SessionStats stats = RaidMineStaffMod.stats();
        stats.tick(client);
        Layout l = layout(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        UiNotificationCenter.Notice notice = UiNotificationCenter.top();
        if (notice != null && !editing) {
            renderNoticeBar(context, l, notice);
        } else {
            renderStatsBar(context, l, stats, editing);
        }
    }

    private static void renderStatsBar(DrawContext context, Layout l, SessionStats stats, boolean editing) {
        UiTheme.shadow(context, l.x(), l.y(), l.width(), l.height(), Math.max(7, scale(10, l.scale())));
        UiTheme.roundedRect(context, l.x(), l.y(), l.width(), l.height(), Math.max(7, scale(10, l.scale())),
                UiTheme.argb(235, 7, 8, 10));
        UiTheme.roundedRect(context, l.x() + 1, l.y() + 1, l.width() - 2, l.height() - 2,
                Math.max(6, scale(9, l.scale())), UiTheme.argb(244, 14, 15, 18));
        context.fillGradient(l.x() + scale(12, l.scale()), l.y() + 1,
                l.x() + l.width() - scale(12, l.scale()), l.y() + Math.max(3, scale(3, l.scale())),
                UiTheme.accent(), UiTheme.accent2());

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int logoW = scale(25, l.scale());
        int logoH = scale(21, l.scale());
        int cursor = l.x() + scale(6, l.scale());
        int logoY = l.y() + (l.height() - logoH) / 2;
        UiTheme.logoGlow(context, cursor, logoY, logoW, logoH);
        cursor += logoW + scale(4, l.scale());
        UiTheme.text(context, tr, "RM Tools", cursor, l.y() + scale(6, l.scale()),
                Math.max(8F, 9.8F * l.scale()), UiTheme.TEXT, true);
        cursor += scale(50, l.scale());
        context.fill(cursor, l.y() + scale(6, l.scale()), cursor + 1,
                l.y() + l.height() - scale(6, l.scale()), UiTheme.BORDER);
        cursor += scale(6, l.scale());

        cursor = compactStat(context, tr, cursor, l, UiIcon.BAN, stats.bans(), UiTheme.DANGER,
                Math.max(stats.pulse(PunishmentType.BAN), stats.pulse(PunishmentType.PERMANENT_BAN)));
        cursor = compactStat(context, tr, cursor, l, UiIcon.MUTE, stats.mutes(), UiTheme.WARNING,
                stats.pulse(PunishmentType.MUTE));
        cursor = compactStat(context, tr, cursor, l, UiIcon.WARN, stats.warns(), UiTheme.accent(),
                stats.pulse(PunishmentType.WARN));

        int clockSize = scale(11, l.scale());
        UiTheme.icon(context, UiIcon.CLOCK, cursor, l.y() + (l.height() - clockSize) / 2,
                clockSize, UiTheme.MUTED);
        cursor += clockSize + scale(3, l.scale());
        UiTheme.text(context, tr, formatTime(stats.elapsedSeconds()), cursor,
                l.y() + scale(6, l.scale()), Math.max(8F, 8.9F * l.scale()), UiTheme.TEXT, true);

        String mode = stats.currentMode();
        int modeX = l.x() + l.width() - scale(66, l.scale());
        UiTheme.text(context, tr, mode, modeX, l.y() + scale(6, l.scale()), Math.max(7F, 8.0F * l.scale()), UiTheme.MUTED, false);

        if (editing) {
            UiTheme.glow(context, l.x() - 2, l.y() - 2, l.width() + 4, l.height() + 4,
                    Math.max(8, scale(11, l.scale())), UiTheme.accent());
            Rect handle = l.resizeHandle();
            UiTheme.roundedRect(context, handle.x(), handle.y(), handle.w(), handle.h(), 6, UiTheme.accent());
            UiTheme.icon(context, UiIcon.RESIZE, handle.x() + 2, handle.y() + 2,
                    Math.max(8, handle.w() - 4), UiTheme.TEXT);
        }
    }

    private static void renderNoticeBar(DrawContext context, Layout l, UiNotificationCenter.Notice notice) {
        float p = UiNotificationCenter.progress(notice);
        int width = Math.max(scale(196, l.scale()), Math.round(l.width() * p));
        int x = l.x() + (l.width() - width) / 2;
        int y = l.y() - Math.round((1F - p) * scale(8, l.scale()));
        int alpha = Math.round(245F * p);
        int accent = switch (notice.kind()) {
            case VIOLATION -> UiTheme.accent();
            case MENTION -> 0xFFFFD24A;
            case INFO -> UiTheme.SUCCESS;
        };

        UiTheme.glow(context, x, y, width, l.height(), Math.max(7, scale(10, l.scale())), accent);
        UiTheme.roundedRect(context, x, y, width, l.height(), Math.max(7, scale(10, l.scale())),
                UiTheme.withAlpha(UiTheme.PANEL_2, alpha));
        context.fillGradient(x + scale(8, l.scale()), y + 1,
                x + width - scale(8, l.scale()), y + Math.max(3, scale(3, l.scale())),
                UiTheme.withAlpha(UiTheme.accent(), alpha), UiTheme.withAlpha(UiTheme.accent2(), alpha));

        int iconSize = scale(14, l.scale());
        int iconX = x + scale(8, l.scale());
        UiTheme.icon(context,
                notice.kind() == UiNotificationCenter.Kind.INFO ? UiIcon.BELL : UiIcon.WARN,
                iconX, y + (l.height() - iconSize) / 2, iconSize, UiTheme.withAlpha(accent, alpha));
        int textX = iconX + iconSize + scale(6, l.scale());
        UiTheme.text(context, MinecraftClient.getInstance().textRenderer,
                UiTheme.ellipsize(MinecraftClient.getInstance().textRenderer, notice.title(), width - (textX - x) - 10),
                textX, y + scale(4, l.scale()), Math.max(8F, 9.2F * l.scale()),
                UiTheme.withAlpha(UiTheme.TEXT, alpha), true);
        UiTheme.text(context, MinecraftClient.getInstance().textRenderer,
                UiTheme.ellipsize(MinecraftClient.getInstance().textRenderer, notice.message(), width - (textX - x) - 10),
                textX, y + scale(14, l.scale()), Math.max(7F, 7.8F * l.scale()),
                UiTheme.withAlpha(UiTheme.MUTED, alpha), false);
    }

    private static int compactStat(DrawContext context, TextRenderer tr, int x, Layout l,
                                   UiIcon icon, int value, int accent, float pulse) {
        int blockW = scale(34, l.scale());
        int blockH = l.height() - scale(7, l.scale());
        int y = l.y() + scale(3.5F, l.scale());
        int alpha = 24 + Math.round(72F * pulse);
        UiTheme.roundedRect(context, x, y, blockW, blockH, Math.max(4, scale(6, l.scale())),
                pulse > 0F ? UiTheme.withAlpha(accent, alpha) : UiTheme.argb(105, 34, 40, 54));
        int iconSize = scale(10, l.scale());
        UiTheme.icon(context, icon, x + scale(4, l.scale()), y + (blockH - iconSize) / 2, iconSize, accent);
        UiTheme.text(context, tr, Integer.toString(value), x + scale(18, l.scale()),
                y + scale(4, l.scale()), Math.max(8F, 8.9F * l.scale()), UiTheme.TEXT, true);
        return x + blockW + scale(3, l.scale());
    }

    public static Layout layout(int screenWidth, int screenHeight) {
        float scale = RaidMineStaffMod.config().hudScale;
        int width = Math.max(150, Math.round(BASE_WIDTH * scale));
        int height = Math.max(18, Math.round(BASE_HEIGHT * scale));
        int availableX = Math.max(0, screenWidth - width);
        int availableY = Math.max(0, screenHeight - height);
        int x = Math.round(availableX * RaidMineStaffMod.config().hudX);
        int y = Math.round(availableY * RaidMineStaffMod.config().hudY);
        x = Math.max(0, Math.min(availableX, x));
        y = Math.max(0, Math.min(availableY, y));
        return new Layout(x, y, width, height, scale);
    }

    public static void setPosition(int screenWidth, int screenHeight, int x, int y) {
        Layout current = layout(screenWidth, screenHeight);
        int maxX = Math.max(1, screenWidth - current.width());
        int maxY = Math.max(1, screenHeight - current.height());
        int clampedX = Math.max(0, Math.min(maxX, x));
        int clampedY = Math.max(0, Math.min(maxY, y));
        RaidMineStaffMod.config().hudX = clampedX / (float) maxX;
        RaidMineStaffMod.config().hudY = clampedY / (float) maxY;
    }

    public static void nudge(int screenWidth, int screenHeight, int dx, int dy) {
        Layout current = layout(screenWidth, screenHeight);
        setPosition(screenWidth, screenHeight, current.x() + dx, current.y() + dy);
        RaidMineStaffMod.config().save();
    }

    public static void setScale(float scale) {
        RaidMineStaffMod.config().hudScale = Math.max(0.60F, Math.min(1.45F, scale));
    }

    public static void centerTop() {
        RaidMineStaffMod.config().hudX = 0.5F;
        RaidMineStaffMod.config().hudY = 0.015F;
        RaidMineStaffMod.config().save();
    }

    public static void reset() {
        RaidMineStaffMod.config().hudX = 0.5F;
        RaidMineStaffMod.config().hudY = 0.015F;
        RaidMineStaffMod.config().hudScale = 0.82F;
        RaidMineStaffMod.config().save();
    }

    private static int scale(int value, float scale) {
        return Math.max(1, Math.round(value * scale));
    }

    private static int scale(float value, float scale) {
        return Math.max(1, Math.round(value * scale));
    }

    private static String formatTime(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, secs)
                : String.format("%02d:%02d", minutes, secs);
    }

    public record Layout(int x, int y, int width, int height, float scale) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        public Rect resizeHandle() {
            int size = Math.max(12, Math.round(16 * scale));
            return new Rect(x + width - size / 2, y + height - size / 2, size, size);
        }
    }

    public record Rect(int x, int y, int w, int h) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }
}
