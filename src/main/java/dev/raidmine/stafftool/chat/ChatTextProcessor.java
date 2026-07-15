package dev.raidmine.stafftool.chat;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.ui.UiTheme;
import dev.raidmine.stafftool.util.NicknameResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ChatTextProcessor {
    private ChatTextProcessor() {
    }

    public static Text process(Text original) {
        if (original == null) return Text.empty();
        MinecraftClient client = MinecraftClient.getInstance();
        String plain = original.getString();
        if (plain.isEmpty()) return original;

        List<Range> forbiddenRanges = findForbiddenRanges(plain);

        String author = findAuthor(plain);
        if (RaidMineStaffMod.config().mentionNotifications && client.player != null) {
            String ownName = client.player.getGameProfile().name();
            boolean ownMessage = !author.isBlank() && author.equalsIgnoreCase(ownName);
            if (!ownMessage && containsExactToken(plain, ownName)) {
                UiNotificationCenter.mention(author);
            }
        }
        if (RaidMineStaffMod.config().forbiddenWordAlerts && !forbiddenRanges.isEmpty()) {
            UiNotificationCenter.violation(author, forbiddenRanges.getFirst().value());
        }

        if (forbiddenRanges.isEmpty()) {
            return original;
        }

        List<StyledPoint> points = new ArrayList<>();
        StringBuilder rebuiltPlain = new StringBuilder();
        original.asOrderedText().accept((index, style, codePoint) -> {
            int start = rebuiltPlain.length();
            rebuiltPlain.appendCodePoint(codePoint);
            points.add(new StyledPoint(start, rebuiltPlain.length(), codePoint, style));
            return true;
        });

        MutableText result = Text.empty();
        for (StyledPoint point : points) {
            Range forbidden = containing(forbiddenRanges, point.start());
            Style style = point.style();
            if (forbidden != null) {
                float t = ratio(point.start(), forbidden.start(), forbidden.end());
                style = style.withColor(UiTheme.blend(UiTheme.accent(), 0xFFFFD24A, t)).withBold(true);
            }
            result.append(Text.literal(new String(Character.toChars(point.codePoint()))).setStyle(style));
        }
        return result;
    }

    private static List<Range> findForbiddenRanges(String text) {
        List<Range> ranges = new ArrayList<>();
        if (!RaidMineStaffMod.config().forbiddenWordAlerts) return ranges;
        for (String configured : RaidMineStaffMod.config().forbiddenWords) {
            String word = configured == null ? "" : configured.trim();
            if (word.isEmpty()) continue;
            int from = 0;
            while (from < text.length()) {
                int index = indexOfIgnoreCase(text, word, from);
                if (index < 0) break;
                int end = index + word.length();
                ranges.add(new Range(index, end, text.substring(index, end)));
                from = Math.max(end, index + 1);
            }
        }
        ranges.sort(Comparator.comparingInt(Range::start));
        return ranges;
    }

    private static String findAuthor(String line) {
        int separator = firstSeparator(line);
        String prefix = separator < 0 ? line : line.substring(0, separator);
        String found = "";
        for (String token : prefix.split("[^A-Za-z0-9_]+")) {
            String online = NicknameResolver.onlinePlayerExact(token).orElse("");
            if (!online.isEmpty()) found = online;
        }
        return found;
    }

    private static int firstSeparator(String value) {
        int best = -1;
        for (char separator : new char[]{':', '»', '›'}) {
            int index = value.indexOf(separator);
            if (index >= 0 && (best < 0 || index < best)) best = index;
        }
        return best;
    }

    private static boolean containsExactToken(String text, String token) {
        if (token == null || token.isBlank()) return false;
        int index = indexOfIgnoreCase(text, token, 0);
        while (index >= 0) {
            int end = index + token.length();
            if (tokenBoundary(text, index, end)) return true;
            index = indexOfIgnoreCase(text, token, index + 1);
        }
        return false;
    }

    private static boolean tokenBoundary(String text, int start, int end) {
        return (start == 0 || !nicknameChar(text.charAt(start - 1)))
                && (end >= text.length() || !nicknameChar(text.charAt(end)));
    }

    private static boolean nicknameChar(char c) {
        return c == '_' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
    }

    private static int indexOfIgnoreCase(String text, String search, int from) {
        return text.toLowerCase(Locale.ROOT).indexOf(search.toLowerCase(Locale.ROOT), from);
    }

    private static Range containing(List<Range> ranges, int position) {
        for (Range range : ranges) {
            if (position >= range.start() && position < range.end()) return range;
        }
        return null;
    }

    private static float ratio(int position, int start, int end) {
        return end <= start + 1 ? 0F : Math.max(0F, Math.min(1F, (position - start) / (float) (end - start - 1)));
    }

    private record StyledPoint(int start, int end, int codePoint, Style style) {
    }

    private record Range(int start, int end, String value) {
    }
}
