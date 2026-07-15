package dev.raidmine.stafftool.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Style;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NicknameResolver {
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_]{2,16}$");
    private static final Pattern COMMAND_NAME = Pattern.compile(
            "(?:^|\\s)/(?:msg|tell|w|whisper|m|reply|r|party\\s+invite|friend\\s+add)\\s+([A-Za-z0-9_]{2,16})(?:\\s|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RECORD_VALUE = Pattern.compile("(?:value|command|suggestion)=([^,)}]+)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> UI_WORDS = Set.of(
            "raidmine", "global", "local", "server", "chattools", "cmi", "minecraft",
            "hover", "click", "message", "command", "prefix", "staff", "moder", "admin"
    );

    private NicknameResolver() {
    }

    public static Optional<String> onlinePlayerExact(String candidate) {
        String cleaned = clean(candidate);
        if (!isValid(cleaned) || isUiWord(cleaned)) return Optional.empty();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return Optional.empty();
        for (PlayerListEntry entry : client.getNetworkHandler().getListedPlayerListEntries()) {
            String name = entry.getProfile().name();
            if (name.equalsIgnoreCase(cleaned)) return Optional.of(name);
        }
        return Optional.empty();
    }

    public static List<String> onlineNames() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return List.of();
        List<String> names = new ArrayList<>();
        for (PlayerListEntry entry : client.getNetworkHandler().getListedPlayerListEntries()) {
            String name = entry.getProfile().name();
            if (isValid(name) && !isUiWord(name)) names.add(name);
        }
        names.sort(Comparator.comparingInt(String::length).reversed());
        return names;
    }

    public static Optional<String> fromStyle(Style style) {
        if (style == null) return Optional.empty();
        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, style.getInsertion());
        inspectObject(style.getClickEvent(), candidates);
        inspectObject(style.getHoverEvent(), candidates);
        for (String candidate : candidates) {
            Optional<String> online = onlinePlayerExact(candidate);
            if (online.isPresent()) return online;
        }
        return Optional.empty();
    }

    public static Optional<String> fromClickedLine(String line, int charStart, int charEnd) {
        if (line == null || line.isEmpty() || charStart < 0 || charStart >= line.length()) return Optional.empty();
        int start = Math.max(0, charStart);
        int end = Math.min(line.length(), Math.max(charEnd, start + 1));
        while (start > 0 && isNicknameChar(line.charAt(start - 1))) start--;
        while (end < line.length() && isNicknameChar(line.charAt(end))) end++;
        String candidate = line.substring(start, end);

        Optional<String> online = onlinePlayerExact(candidate);
        if (online.isPresent()) return online;

        int separator = firstSeparator(line);
        if (separator >= 0 && start < separator) {
            String prefix = line.substring(0, separator);
            String lastOnline = null;
            Matcher matcher = Pattern.compile("[A-Za-z0-9_]{2,16}").matcher(prefix);
            while (matcher.find()) {
                String token = matcher.group();
                Optional<String> maybeOnline = onlinePlayerExact(token);
                if (maybeOnline.isPresent()) {
                    lastOnline = maybeOnline.get();
                } else if (!isUiWord(token) && isValid(token)) {
                    lastOnline = token;
                }
            }
            if (lastOnline != null) return Optional.of(lastOnline);
        }

        return Optional.empty();
    }

    public static int[] locateResolvedToken(String line, String nickname) {
        if (line == null || nickname == null || nickname.isBlank()) return new int[]{-1, -1};
        Matcher matcher = Pattern.compile("[A-Za-z0-9_]{2,16}").matcher(line);
        int[] lastBeforeSeparator = {-1, -1};
        int separator = firstSeparator(line);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.equalsIgnoreCase(nickname)) {
                if (separator >= 0 && matcher.end() <= separator) {
                    lastBeforeSeparator[0] = matcher.start();
                    lastBeforeSeparator[1] = matcher.end();
                }
            }
        }
        if (lastBeforeSeparator[0] >= 0) return lastBeforeSeparator;
        matcher.reset();
        while (matcher.find()) {
            String token = matcher.group();
            if (token.equalsIgnoreCase(nickname)) {
                return new int[]{matcher.start(), matcher.end()};
            }
        }
        return new int[]{-1, -1};
    }

    private static int firstSeparator(String line) {
        int best = -1;
        for (char separator : new char[]{':', '»', '›'}) {
            int index = line.indexOf(separator);
            if (index >= 0 && (best < 0 || index < best)) best = index;
        }
        return best;
    }

    public static boolean isValid(String value) {
        return value != null && VALID.matcher(value).matches();
    }

    public static boolean isNicknameChar(char character) {
        return character == '_'
                || character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9';
    }

    private static void inspectObject(Object object, Set<String> candidates) {
        if (object == null) return;
        for (String methodName : new String[]{"value", "getValue", "command", "suggestion"}) {
            try {
                Method method = object.getClass().getMethod(methodName);
                Object value = method.invoke(object);
                if (value != null) addFromString(candidates, value.toString());
            } catch (ReflectiveOperationException ignored) {
            }
        }
        addFromString(candidates, object.toString());
    }

    private static void addFromString(Set<String> candidates, String value) {
        if (value == null || value.isBlank()) return;
        addCandidate(candidates, value);
        Matcher commandMatcher = COMMAND_NAME.matcher(value);
        while (commandMatcher.find()) addCandidate(candidates, commandMatcher.group(1));
        Matcher recordMatcher = RECORD_VALUE.matcher(value);
        while (recordMatcher.find()) addCandidate(candidates, recordMatcher.group(1));
    }

    private static void addCandidate(Set<String> candidates, String value) {
        if (value != null && !value.isBlank()) candidates.add(value);
    }

    private static String clean(String value) {
        String result = value == null ? "" : value.trim();
        result = result.replace("\\\"", "").replace("\"", "");
        if (result.startsWith("/")) {
            Matcher matcher = COMMAND_NAME.matcher(result);
            if (matcher.find()) return matcher.group(1);
        }
        String[] tokens = result.split("[^A-Za-z0-9_]+");
        for (String token : tokens) {
            if (isValid(token) && !isUiWord(token)) return token;
        }
        return result.replaceAll("[^A-Za-z0-9_]", "");
    }

    private static boolean isUiWord(String value) {
        return UI_WORDS.contains(value.toLowerCase(Locale.ROOT));
    }
}
