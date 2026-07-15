package dev.raidmine.stafftool.util;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.chat.UiNotificationCenter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotService {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private ScreenshotService() {
    }

    public static void capture(String player, String reason) {
        if (!RaidMineStaffMod.config().autoScreenshot) return;
        MinecraftClient client = MinecraftClient.getInstance();
        String safePlayer = sanitize(player, "player");
        String safeReason = sanitize(reason, "rule");
        String fileName = safePlayer + "_" + safeReason + "_" + FORMAT.format(LocalDateTime.now()) + ".png";
        try {
            ScreenshotRecorder.saveScreenshot(
                    client.runDirectory,
                    fileName,
                    client.getFramebuffer(),
                    1,
                    text -> client.execute(() -> UiNotificationCenter.info("Скриншот сохранён", fileName))
            );
        } catch (Exception exception) {
            RaidMineStaffMod.LOGGER.error("Could not capture moderation screenshot", exception);
            UiNotificationCenter.info("Ошибка скриншота", exception.getMessage() == null ? "Не удалось сохранить" : exception.getMessage());
        }
    }

    private static String sanitize(String value, String fallback) {
        String safe = value == null ? "" : value.trim().replaceAll("[^A-Za-zА-Яа-я0-9_.-]+", "_");
        safe = safe.replaceAll("_+", "_");
        if (safe.length() > 48) safe = safe.substring(0, 48);
        return safe.isBlank() ? fallback : safe;
    }
}
