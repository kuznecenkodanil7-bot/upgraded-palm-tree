package dev.raidmine.stafftool.util;

import dev.raidmine.stafftool.RaidMineStaffMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ServerGuard {
    private ServerGuard() {
    }

    public static boolean isAllowed(MinecraftClient client) {
        if (!RaidMineStaffMod.config().restrictToRaidMine) {
            return true;
        }
        String context = collectContext(client).toLowerCase(Locale.ROOT);
        return RaidMineStaffMod.config().allowedAddressFragments.stream()
                .filter(fragment -> fragment != null && !fragment.isBlank())
                .map(fragment -> fragment.toLowerCase(Locale.ROOT))
                .anyMatch(context::contains);
    }

    public static String currentAddress(MinecraftClient client) {
        ServerInfo server = client.getCurrentServerEntry();
        return server == null || server.address == null ? "одиночная игра" : server.address;
    }

    public static boolean isActivityCounted(MinecraftClient client) {
        if (client == null || client.player == null || client.getNetworkHandler() == null) return false;
        if (!isAllowed(client)) return false;
        String context = collectContext(client).toLowerCase(Locale.ROOT);
        for (String keyword : RaidMineStaffMod.config().pausedServerKeywords) {
            if (!keyword.isBlank() && context.contains(keyword)) return false;
        }
        for (String keyword : RaidMineStaffMod.config().activeServerKeywords) {
            if (!keyword.isBlank() && context.contains(keyword)) return true;
        }
        return !context.contains("hub") && !context.contains("lobby") && !context.contains("limbo");
    }

    public static String detectMode(MinecraftClient client) {
        if (client == null || client.player == null || client.getNetworkHandler() == null) return "—";
        String context = collectContext(client).toLowerCase(Locale.ROOT);
        for (String keyword : RaidMineStaffMod.config().pausedServerKeywords) {
            if (!keyword.isBlank() && context.contains(keyword)) return keyword.toUpperCase(Locale.ROOT);
        }
        for (String keyword : RaidMineStaffMod.config().activeServerKeywords) {
            if (!keyword.isBlank() && context.contains(keyword)) return keyword.toUpperCase(Locale.ROOT);
        }
        return "RAIDMINE";
    }

    private static String collectContext(MinecraftClient client) {
        List<String> parts = new ArrayList<>();
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null) {
            add(parts, server.name);
            add(parts, server.address);
        }
        add(parts, invokeToString(client.getNetworkHandler(), "getBrand"));
        add(parts, invokeToString(client.getNetworkHandler(), "getServerBrand"));
        add(parts, invokeToString(client.getNetworkHandler(), "getPlayerListHeader"));
        add(parts, invokeToString(client.getNetworkHandler(), "getPlayerListFooter"));
        add(parts, invokeToString(client.player, "getDisplayName"));
        add(parts, invokeToString(client.player, "getName"));
        add(parts, invokeScoreboardSidebar(client));
        return String.join(" | ", parts);
    }

    private static String invokeScoreboardSidebar(MinecraftClient client) {
        try {
            // In 1.21.11 ClientPlayerEntity no longer exposes getScoreboard() in
            // the selected Yarn mappings. Resolve it reflectively from ClientWorld
            // first, with ClientPlayerEntity only as a compatibility fallback.
            Object scoreboard = invokeNoArg(client.world, "getScoreboard");
            if (scoreboard == null) {
                scoreboard = invokeNoArg(client.player, "getScoreboard");
            }
            if (scoreboard == null) {
                return "";
            }

            StringBuilder context = new StringBuilder(scoreboard.toString());
            for (Method method : scoreboard.getClass().getMethods()) {
                String name = method.getName().toLowerCase(Locale.ROOT);
                if (method.getParameterCount() != 0) continue;
                if (!name.contains("sidebar") && !name.contains("display") && !name.contains("objective")) {
                    continue;
                }
                try {
                    Object value = method.invoke(scoreboard);
                    if (value != null) {
                        context.append(" | ").append(value);
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return context.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String invokeToString(Object target, String methodName) {
        if (target == null) return "";
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static void add(List<String> parts, String value) {
        if (value != null && !value.isBlank()) parts.add(value);
    }
}
