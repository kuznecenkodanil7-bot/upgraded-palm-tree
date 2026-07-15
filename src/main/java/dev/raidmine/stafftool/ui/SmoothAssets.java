package dev.raidmine.stafftool.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.raidmine.stafftool.RaidMineStaffMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates an antialiased font and icon atlas at runtime. No low-resolution
 * bitmap font or third-party font file is bundled with the mod.
 */
public final class SmoothAssets {
    private static final int ATLAS_SIZE = 3072;
    private static final int FONT_SIZE = 64;
    private static final int GLYPH_PADDING = 4;
    private static final int ICON_SOURCE_SIZE = 192;
    private static final Identifier ATLAS_ID = Identifier.of(RaidMineStaffMod.MOD_ID, "runtime/smooth_ui");

    private static final Map<Long, Glyph> GLYPHS = new HashMap<>();
    private static final EnumMap<UiIcon, Region> ICONS = new EnumMap<>(UiIcon.class);

    private static volatile boolean initialized;
    private static volatile boolean failed;
    private static volatile String fontName = "Sans Serif";
    private static Region circleRegion;
    private static int atlasX = 4;
    private static int atlasY = 4;
    private static int rowHeight;

    private SmoothAssets() {
    }

    public static boolean ensureInitialized() {
        if (initialized) {
            return true;
        }
        if (failed) {
            return false;
        }
        synchronized (SmoothAssets.class) {
            if (initialized) {
                return true;
            }
            try {
                buildAtlas();
                initialized = true;
                return true;
            } catch (Throwable throwable) {
                failed = true;
                RaidMineStaffMod.LOGGER.error("Could not create smooth UI atlas", throwable);
                return false;
            }
        }
    }

    private static void buildAtlas() {
        BufferedImage image = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configure(graphics);
        graphics.setComposite(java.awt.AlphaComposite.Src);
        graphics.setColor(new Color(255, 255, 255, 255));

        Font regular = createUiFont(false);
        Font bold = createUiFont(true);
        packFont(graphics, regular, false);
        packFont(graphics, bold, true);

        alignToNextRow(ICON_SOURCE_SIZE + 8);
        for (UiIcon icon : UiIcon.values()) {
            Region region = allocate(ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
            ICONS.put(icon, region);
            Graphics2D iconGraphics = (Graphics2D) graphics.create(region.x(), region.y(), region.w(), region.h());
            configure(iconGraphics);
            iconGraphics.setColor(Color.WHITE);
            drawIcon(iconGraphics, icon);
            iconGraphics.dispose();
        }

        circleRegion = allocate(128, 128);
        Graphics2D circleGraphics = (Graphics2D) graphics.create(circleRegion.x(), circleRegion.y(), 128, 128);
        configure(circleGraphics);
        circleGraphics.setColor(Color.WHITE);
        circleGraphics.fill(new Ellipse2D.Float(0, 0, 128, 128));
        circleGraphics.dispose();
        graphics.dispose();

        NativeImage nativeImage = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, false);
        int[] pixels = image.getRGB(0, 0, ATLAS_SIZE, ATLAS_SIZE, null, 0, ATLAS_SIZE);
        for (int y = 0; y < ATLAS_SIZE; y++) {
            int row = y * ATLAS_SIZE;
            for (int x = 0; x < ATLAS_SIZE; x++) {
                nativeImage.setColorArgb(x, y, pixels[row + x]);
            }
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.getTextureManager().registerTexture(
                ATLAS_ID,
                new LinearTexture(() -> "RM Tools smooth UI atlas", nativeImage)
        );
    }


    private static Font createUiFont(boolean bold) {
        String[] preferred = {
                "Segoe UI Variable", "Inter", "Segoe UI", "Noto Sans",
                "SF Pro Display", "Roboto", "Arial", Font.SANS_SERIF
        };
        String selected = Font.SANS_SERIF;
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        outer:
        for (String candidate : preferred) {
            for (String family : available) {
                if (family.equalsIgnoreCase(candidate)) {
                    selected = family;
                    break outer;
                }
            }
        }
        fontName = selected;
        Font base = new Font(selected, Font.PLAIN, FONT_SIZE);
        Map<TextAttribute, Object> attributes = new HashMap<>(base.getAttributes());
        attributes.put(TextAttribute.WEIGHT, bold ? TextAttribute.WEIGHT_SEMIBOLD : TextAttribute.WEIGHT_REGULAR);
        attributes.put(TextAttribute.TRACKING, -0.018F);
        return base.deriveFont(attributes);
    }

    public static String fontName() {
        ensureInitialized();
        return fontName;
    }

    private static void packFont(Graphics2D graphics, Font font, boolean bold) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);

        for (int codePoint = 32; codePoint <= 126; codePoint++) {
            packGlyph(graphics, font, metrics, codePoint, bold);
        }
        for (int codePoint = 0x0400; codePoint <= 0x052F; codePoint++) {
            if (font.canDisplay(codePoint)) {
                packGlyph(graphics, font, metrics, codePoint, bold);
            }
        }
        for (int codePoint : new int[]{0x2026, 0x2116, 0x2039, 0x203A, 0x2014, 0x2713, 0x00D7}) {
            if (font.canDisplay(codePoint)) {
                packGlyph(graphics, font, metrics, codePoint, bold);
            }
        }
    }

    private static void packGlyph(Graphics2D graphics, Font font, FontMetrics metrics, int codePoint, boolean bold) {
        String text = new String(Character.toChars(codePoint));
        int advance = Math.max(1, metrics.stringWidth(text));
        int sourceWidth = advance + GLYPH_PADDING * 2;
        int sourceHeight = metrics.getHeight() + GLYPH_PADDING * 2;
        Region region = allocate(sourceWidth, sourceHeight);

        graphics.setFont(font);
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, region.x() + GLYPH_PADDING, region.y() + GLYPH_PADDING + metrics.getAscent());
        GLYPHS.put(glyphKey(codePoint, bold), new Glyph(
                region.x(), region.y(), region.w(), region.h(), advance, metrics.getHeight(), GLYPH_PADDING
        ));
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static Region allocate(int width, int height) {
        if (atlasX + width + 4 > ATLAS_SIZE) {
            atlasX = 4;
            atlasY += rowHeight + 4;
            rowHeight = 0;
        }
        if (atlasY + height + 4 > ATLAS_SIZE) {
            throw new IllegalStateException("Smooth UI atlas overflow");
        }
        Region region = new Region(atlasX, atlasY, width, height);
        atlasX += width + 4;
        rowHeight = Math.max(rowHeight, height);
        return region;
    }

    private static void alignToNextRow(int minimumHeight) {
        atlasX = 4;
        atlasY += rowHeight + 8;
        rowHeight = minimumHeight;
    }

    public static int drawText(DrawContext context, String text, float x, float y,
                               float size, int color, boolean bold) {
        if (!ensureInitialized() || text == null || text.isEmpty()) {
            return 0;
        }
        float scale = size / FONT_SIZE;
        float cursor = x;
        int[] points = text.codePoints().toArray();
        for (int point : points) {
            Glyph glyph = GLYPHS.get(glyphKey(point, bold));
            if (glyph == null) {
                glyph = GLYPHS.get(glyphKey('?', bold));
            }
            if (glyph == null) {
                continue;
            }
            int pad = Math.max(1, Math.round(glyph.padding() * scale));
            int destWidth = Math.max(1, Math.round(glyph.w() * scale));
            int destHeight = Math.max(1, Math.round(glyph.h() * scale));
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    ATLAS_ID,
                    Math.round(cursor) - pad,
                    Math.round(y) - pad,
                    glyph.x(), glyph.y(),
                    destWidth, destHeight,
                    glyph.w(), glyph.h(),
                    ATLAS_SIZE, ATLAS_SIZE,
                    color
            );
            cursor += glyph.advance() * scale;
        }
        return Math.round(cursor - x);
    }

    public static int textWidth(String text, float size, boolean bold) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (!ensureInitialized()) {
            return 0;
        }
        float scale = size / FONT_SIZE;
        float width = 0F;
        for (int point : text.codePoints().toArray()) {
            Glyph glyph = GLYPHS.get(glyphKey(point, bold));
            if (glyph == null) {
                glyph = GLYPHS.get(glyphKey('?', bold));
            }
            if (glyph != null) {
                width += glyph.advance() * scale;
            }
        }
        return Math.round(width);
    }

    public static void drawIcon(DrawContext context, UiIcon icon, int x, int y, int size, int color) {
        if (!ensureInitialized()) {
            return;
        }
        Region region = ICONS.get(icon);
        if (region == null) {
            return;
        }
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                ATLAS_ID,
                x, y,
                region.x(), region.y(),
                size, size,
                region.w(), region.h(),
                ATLAS_SIZE, ATLAS_SIZE,
                color
        );
    }

    public static void roundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (!ensureInitialized() || circleRegion == null) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        if (width <= 0 || height <= 0) {
            return;
        }
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (radius == 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        int half = circleRegion.w() / 2;
        drawRegion(context, circleRegion.x(), circleRegion.y(), half, half,
                x, y, radius, radius, color);
        drawRegion(context, circleRegion.x() + half, circleRegion.y(), half, half,
                x + width - radius, y, radius, radius, color);
        drawRegion(context, circleRegion.x(), circleRegion.y() + half, half, half,
                x, y + height - radius, radius, radius, color);
        drawRegion(context, circleRegion.x() + half, circleRegion.y() + half, half, half,
                x + width - radius, y + height - radius, radius, radius, color);
    }

    private static void drawRegion(DrawContext context, int sourceX, int sourceY, int sourceW, int sourceH,
                                   int x, int y, int width, int height, int color) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                ATLAS_ID,
                x, y,
                sourceX, sourceY,
                width, height,
                sourceW, sourceH,
                ATLAS_SIZE, ATLAS_SIZE,
                color
        );
    }

    private static long glyphKey(int codePoint, boolean bold) {
        return ((long) codePoint << 1) | (bold ? 1L : 0L);
    }

    private static void drawIcon(Graphics2D g, UiIcon icon) {
        float scale = ICON_SOURCE_SIZE / 96F;
        g.scale(scale, scale);
        g.setStroke(new BasicStroke(6.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (icon) {
            case SHIELD -> {
                Path2D p = new Path2D.Float();
                p.moveTo(48, 8); p.lineTo(79, 20); p.lineTo(75, 57);
                p.curveTo(72, 73, 61, 84, 48, 89);
                p.curveTo(35, 84, 24, 73, 21, 57); p.lineTo(17, 20); p.closePath();
                g.draw(p); g.draw(new Line2D.Float(33, 48, 43, 58)); g.draw(new Line2D.Float(43, 58, 65, 35));
            }
            case CHAT -> {
                g.drawRoundRect(12, 15, 72, 54, 20, 20);
                Path2D p = new Path2D.Float(); p.moveTo(34, 68); p.lineTo(24, 84); p.lineTo(49, 69); g.draw(p);
                g.fill(new Ellipse2D.Float(27, 39, 7, 7)); g.fill(new Ellipse2D.Float(45, 39, 7, 7)); g.fill(new Ellipse2D.Float(63, 39, 7, 7));
            }
            case GAMEPLAY -> {
                g.drawRoundRect(12, 28, 72, 44, 20, 20);
                g.draw(new Line2D.Float(30, 41, 30, 59)); g.draw(new Line2D.Float(21, 50, 39, 50));
                g.fill(new Ellipse2D.Float(59, 41, 8, 8)); g.fill(new Ellipse2D.Float(69, 52, 8, 8));
            }
            case DONATER -> {
                Path2D p = new Path2D.Float(); p.moveTo(48, 9); p.lineTo(84, 38); p.lineTo(48, 87); p.lineTo(12, 38); p.closePath();
                g.draw(p); g.draw(new Line2D.Float(12, 38, 84, 38)); g.draw(new Line2D.Float(30, 16, 48, 87)); g.draw(new Line2D.Float(66, 16, 48, 87));
            }
            case QUICK -> {
                Path2D p = new Path2D.Float(); p.moveTo(55, 7); p.lineTo(20, 54); p.lineTo(43, 54); p.lineTo(37, 90); p.lineTo(78, 41); p.lineTo(53, 41); p.closePath(); g.fill(p);
            }
            case USER -> { g.draw(new Ellipse2D.Float(31, 11, 34, 34)); g.drawArc(16, 48, 64, 44, 190, 160); }
            case WARN -> {
                Path2D p = new Path2D.Float(); p.moveTo(48, 8); p.lineTo(88, 83); p.lineTo(8, 83); p.closePath(); g.draw(p);
                g.draw(new Line2D.Float(48, 32, 48, 59)); g.fill(new Ellipse2D.Float(44, 69, 8, 8));
            }
            case MUTE -> {
                Path2D p = new Path2D.Float(); p.moveTo(12, 38); p.lineTo(30, 38); p.lineTo(51, 21); p.lineTo(51, 75); p.lineTo(30, 58); p.lineTo(12, 58); p.closePath(); g.draw(p);
                g.draw(new Line2D.Float(64, 34, 85, 62)); g.draw(new Line2D.Float(85, 34, 64, 62));
            }
            case BAN -> { g.draw(new Ellipse2D.Float(10, 10, 76, 76)); g.draw(new Line2D.Float(23, 73, 73, 23)); }
            case KICK -> {
                g.drawRoundRect(12, 17, 48, 62, 8, 8); g.draw(new Line2D.Float(40, 48, 85, 48));
                g.draw(new Line2D.Float(71, 35, 85, 48)); g.draw(new Line2D.Float(71, 61, 85, 48));
            }
            case CLOCK -> { g.draw(new Ellipse2D.Float(11, 11, 74, 74)); g.draw(new Line2D.Float(48, 27, 48, 50)); g.draw(new Line2D.Float(48, 50, 65, 60)); }
            case CHEVRON_LEFT -> { g.draw(new Line2D.Float(61, 21, 34, 48)); g.draw(new Line2D.Float(34, 48, 61, 75)); }
            case CHEVRON_RIGHT -> { g.draw(new Line2D.Float(35, 21, 62, 48)); g.draw(new Line2D.Float(62, 48, 35, 75)); }
            case CHECK -> { g.draw(new Line2D.Float(17, 50, 39, 71)); g.draw(new Line2D.Float(39, 71, 80, 26)); }
            case CLOSE -> { g.draw(new Line2D.Float(22, 22, 74, 74)); g.draw(new Line2D.Float(74, 22, 22, 74)); }
            case MOVE -> {
                g.draw(new Line2D.Float(48, 9, 48, 87)); g.draw(new Line2D.Float(9, 48, 87, 48));
                g.draw(new Line2D.Float(48, 9, 36, 22)); g.draw(new Line2D.Float(48, 9, 60, 22));
                g.draw(new Line2D.Float(48, 87, 36, 74)); g.draw(new Line2D.Float(48, 87, 60, 74));
                g.draw(new Line2D.Float(9, 48, 22, 36)); g.draw(new Line2D.Float(9, 48, 22, 60));
                g.draw(new Line2D.Float(87, 48, 74, 36)); g.draw(new Line2D.Float(87, 48, 74, 60));
            }
            case RESIZE -> {
                g.draw(new Line2D.Float(21, 75, 75, 21)); g.draw(new Line2D.Float(50, 75, 75, 75)); g.draw(new Line2D.Float(75, 50, 75, 75));
                g.draw(new Line2D.Float(21, 46, 21, 21)); g.draw(new Line2D.Float(21, 21, 46, 21));
            }
            case SETTINGS -> {
                g.draw(new Ellipse2D.Float(35, 35, 26, 26));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4.0; float x1=(float)(48+25*Math.cos(a)); float y1=(float)(48+25*Math.sin(a));
                    float x2=(float)(48+38*Math.cos(a)); float y2=(float)(48+38*Math.sin(a)); g.draw(new Line2D.Float(x1,y1,x2,y2));
                }
                g.draw(new Ellipse2D.Float(17, 17, 62, 62));
            }
            case PLUS -> { g.draw(new Line2D.Float(48, 18, 48, 78)); g.draw(new Line2D.Float(18, 48, 78, 48)); }
            case TRASH -> {
                g.drawRoundRect(25, 28, 46, 56, 8, 8); g.draw(new Line2D.Float(20, 25, 76, 25)); g.draw(new Line2D.Float(37, 16, 59, 16));
                g.draw(new Line2D.Float(39, 42, 39, 70)); g.draw(new Line2D.Float(57, 42, 57, 70));
            }
            case BELL -> {
                Path2D p = new Path2D.Float(); p.moveTo(23, 67); p.curveTo(31,58,29,45,31,35); p.curveTo(34,20,62,20,65,35); p.curveTo(67,45,65,58,73,67); p.closePath(); g.draw(p);
                g.draw(new Line2D.Float(19, 68, 77, 68)); g.drawArc(39, 69, 18, 14, 180, 180);
            }
            case CENTER -> {
                g.draw(new Line2D.Float(48, 12, 48, 34)); g.draw(new Line2D.Float(48, 62, 48, 84));
                g.draw(new Line2D.Float(12, 48, 34, 48)); g.draw(new Line2D.Float(62, 48, 84, 48));
                g.drawRoundRect(35, 35, 26, 26, 8, 8);
            }
            case SCREENSHOT -> {
                g.drawRoundRect(12, 27, 72, 50, 12, 12); g.draw(new Ellipse2D.Float(36, 37, 24, 24));
                g.draw(new Line2D.Float(27, 27, 34, 17)); g.draw(new Line2D.Float(34, 17, 57, 17)); g.draw(new Line2D.Float(57, 17, 64, 27));
            }
            case PALETTE -> {
                g.draw(new Ellipse2D.Float(12, 14, 72, 68)); g.fill(new Ellipse2D.Float(30, 27, 7, 7)); g.fill(new Ellipse2D.Float(47, 22, 7, 7));
                g.fill(new Ellipse2D.Float(63, 31, 7, 7)); g.fill(new Ellipse2D.Float(25, 47, 7, 7));
                g.draw(new Ellipse2D.Float(52, 50, 25, 21));
            }
        }
    }

    private record Glyph(int x, int y, int w, int h, int advance, int sourceLineHeight, int padding) {
    }

    private record Region(int x, int y, int w, int h) {
    }

    private static final class LinearTexture extends NativeImageBackedTexture {
        private LinearTexture(java.util.function.Supplier<String> nameSupplier, NativeImage image) {
            super(nameSupplier, image);
            this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        }
    }
}
