package dev.raidmine.stafftool.stats;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.rules.PunishmentType;
import dev.raidmine.stafftool.util.ServerGuard;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;

public final class SessionStats {
    private long accumulatedActiveMillis;
    private long lastTickMillis;
    private boolean active;
    private String currentMode = "—";
    private int bans;
    private int mutes;
    private int warns;
    private long lastActionMillis;
    private PunishmentType lastType;
    private String lastRecordedCommand = "";
    private long lastRecordedCommandAt;

    public void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (lastTickMillis == 0L) {
            lastTickMillis = now;
            currentMode = ServerGuard.detectMode(client);
            active = ServerGuard.isActivityCounted(client);
            return;
        }
        if (active) {
            accumulatedActiveMillis += Math.max(0L, now - lastTickMillis);
        }
        currentMode = ServerGuard.detectMode(client);
        active = ServerGuard.isActivityCounted(client);
        lastTickMillis = now;
    }

    public void record(PunishmentType type) {
        switch (type) {
            case BAN, PERMANENT_BAN -> bans++;
            case MUTE -> mutes++;
            case WARN -> warns++;
            case KICK -> {
                // kicks are intentionally not shown in the HUD anymore
            }
        }
        lastType = type;
        lastActionMillis = System.currentTimeMillis();
    }

    public void observeManualCommand(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;
        String normalized = rawMessage.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("/")) return;
        long now = System.currentTimeMillis();
        if (normalized.equals(lastRecordedCommand) && now - lastRecordedCommandAt < 1500L) return;

        String warnToken = firstToken(RaidMineStaffMod.config().warnCommand);
        String muteToken = firstToken(RaidMineStaffMod.config().muteCommand);
        String banToken = firstToken(RaidMineStaffMod.config().banCommand);
        String permanentBanToken = firstToken(RaidMineStaffMod.config().permanentBanCommand);

        PunishmentType type = null;
        if (startsWithCommand(normalized, warnToken)) type = PunishmentType.WARN;
        else if (startsWithCommand(normalized, muteToken)) type = PunishmentType.MUTE;
        else if (startsWithCommand(normalized, permanentBanToken)) type = PunishmentType.PERMANENT_BAN;
        else if (startsWithCommand(normalized, banToken)) type = PunishmentType.BAN;

        if (type != null) {
            record(type);
            lastRecordedCommand = normalized;
            lastRecordedCommandAt = now;
        }
    }

    private boolean startsWithCommand(String raw, String token) {
        if (token.isBlank()) return false;
        String normalizedToken = token.startsWith("/") ? token : "/" + token;
        return raw.equals(normalizedToken) || raw.startsWith(normalizedToken + " ");
    }

    private String firstToken(String template) {
        String cleaned = template == null ? "" : template.trim().toLowerCase(Locale.ROOT);
        if (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        int space = cleaned.indexOf(' ');
        return space < 0 ? cleaned : cleaned.substring(0, space);
    }

    public void reset() {
        accumulatedActiveMillis = 0L;
        lastTickMillis = 0L;
        active = false;
        currentMode = "—";
        bans = 0;
        mutes = 0;
        warns = 0;
        lastActionMillis = 0L;
        lastType = null;
    }

    public int bans() {
        return bans;
    }

    public int mutes() {
        return mutes;
    }

    public int warns() {
        return warns;
    }

    public String currentMode() {
        return currentMode;
    }

    public long elapsedSeconds() {
        long live = accumulatedActiveMillis;
        if (active && lastTickMillis > 0L) {
            live += Math.max(0L, System.currentTimeMillis() - lastTickMillis);
        }
        return Math.max(0L, live / 1000L);
    }

    public float pulse(PunishmentType type) {
        if (type != lastType || lastActionMillis == 0L) {
            return 0F;
        }
        long age = System.currentTimeMillis() - lastActionMillis;
        if (age >= 700L) {
            return 0F;
        }
        float t = age / 700F;
        return (1F - t) * (1F - t);
    }
}
