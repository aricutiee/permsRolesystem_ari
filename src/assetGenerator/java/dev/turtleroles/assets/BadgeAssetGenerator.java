package dev.turtleroles.assets;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BadgeAssetGenerator {
    private static final List<RoleArt> ROLES = List.of(
        new RoleArt("owner", "OWNER", 0xE001, "owner.png", new int[]{0x5BCEFA, 0xF5A9B8, 0xFFFFFF, 0xF5A9B8, 0x5BCEFA}, false),
        new RoleArt("co_owner", "CO-OWNER", 0xE002, "co_owner.png", new int[]{0x164B35, 0x3E9161, 0xA8CA7B}, true),
        new RoleArt("sr_admin", "SR ADMIN", 0xE003, "sr_admin.png", new int[]{0x080A0D, 0x242830, 0x8E98A6}, false),
        new RoleArt("admin", "ADMIN", 0xE004, "admin.png", new int[]{0x650E24, 0x8B1234, 0xBD2142}, false),
        new RoleArt("moderator", "MODERATOR", 0xE005, "moderator.png", new int[]{0x14532D, 0x15803D, 0x41CE70}, false),
        new RoleArt("helper", "HELPER", 0xE006, "helper.png", new int[]{0xA96D09, 0xD49A12, 0xFFE477}, false),
        new RoleArt("member", "MEMBER", 0xE007, "member.png", new int[]{0x454B55, 0x626975, 0xAEB5C0}, false)
    );

    private static final Map<Character, String[]> FONT = font();

    public static void main(String[] args) throws Exception {
        if (args.length != 9) {
            throw new IllegalArgumentException("Expected packDir badgeDir previewDir zip sha1 basePack logo shocksDir wordmark");
        }
        Path packDir = Path.of(args[0]);
        Path badgeDir = Path.of(args[1]);
        Path previewDir = Path.of(args[2]);
        Path zip = Path.of(args[3]);
        Path sha1 = Path.of(args[4]);
        Files.createDirectories(packDir);
        mergeBasePack(Path.of(args[5]), packDir);
        Files.createDirectories(badgeDir);
        Files.createDirectories(previewDir);
        Path textureDir = packDir.resolve("assets/turtleroles/textures/font");
        Path fontDir = packDir.resolve("assets/turtleroles/font");
        Files.createDirectories(textureDir);
        Files.createDirectories(fontDir);

        StringBuilder providers = new StringBuilder();
        providers.append("{\n  \"providers\": [\n");
        for (int i = 0; i < ROLES.size(); i++) {
            RoleArt role = ROLES.get(i);
            // Native-resolution artwork matches the font metrics exactly.
            BufferedImage image = drawBadge(role);
            ImageIO.write(image, "png", badgeDir.resolve(role.texture).toFile());
            ImageIO.write(image, "png", textureDir.resolve(role.texture).toFile());
            ImageIO.write(scale(image, 4), "png", previewDir.resolve(role.id + "-8x.png").toFile());
            providers.append("    {\"type\":\"bitmap\",\"file\":\"turtleroles:font/")
                .append(role.texture)
                .append("\",\"height\":8,\"ascent\":7,\"chars\":[\"\\u")
                .append(String.format("%04X", role.codepoint))
                .append("\"]}");
            if (i + 1 < ROLES.size()) {
                providers.append(",");
            }
            providers.append("\n");
        }
        providers.append("  ]\n}\n");
        Files.writeString(fontDir.resolve("roles.json"), providers.toString(), StandardCharsets.UTF_8);
        BufferedImage logo = prepareTabLogo(ImageIO.read(Path.of(args[6]).toFile()));
        ImageIO.write(logo, "png", textureDir.resolve("shock_smp_logo.png").toFile());
        StringBuilder headerFont = new StringBuilder("{\"providers\":[\n");
        headerFont.append("{\"type\":\"bitmap\",\"file\":\"turtleroles:font/shock_smp_logo.png\",\"height\":28,\"ascent\":27,\"chars\":[\"\\uE100\"]}");
        String[] shocks = {"ice", "time", "emerald", "speed", "earth", "healing"};
        double[] rotations = {-8, -8, -48, 42, 32, -38};
        for (int i = 0; i < shocks.length; i++) {
            int height = i < 2 ? 28 : 20;
            int ascent = i < 2 ? 27 : (i < 4 ? 19 : 14);
            BufferedImage shock = rotatedShock(ImageIO.read(Path.of(args[7]).resolve(shocks[i] + ".png").toFile()), height, rotations[i]);
            String name = "tab_" + shocks[i] + ".png";
            ImageIO.write(shock, "png", textureDir.resolve(name).toFile());
            ImageIO.write(scale(shock, 6), "png", previewDir.resolve("tab_" + shocks[i] + "-6x.png").toFile());
            headerFont.append(",\n{\"type\":\"bitmap\",\"file\":\"turtleroles:font/").append(name)
                .append("\",\"height\":").append(height).append(",\"ascent\":").append(ascent)
                .append(",\"chars\":[\"\\u").append(String.format("%04X", 0xE101 + i)).append("\"]}");
        }
        // Overhanging edge pairs draw eight pixels beyond a 180px measured line.
        // Explicit negative spacing prevents the artwork widening the panel.
        for (int row = 0; row < 2; row++) {
            BufferedImage pair = new BufferedImage(196, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = pair.createGraphics();
            for (int side = 0; side < 2; side++) {
                int index = 2 + row * 2 + side;
                BufferedImage small = rotatedShock(ImageIO.read(Path.of(args[7]).resolve(shocks[index] + ".png").toFile()), 16, rotations[index]);
                g.drawImage(small, side * 180, 0, null);
            }
            g.dispose();
            String name = "tab_edges_" + row + ".png";
            ImageIO.write(pair, "png", textureDir.resolve(name).toFile());
            ImageIO.write(scale(pair, 4), "png", previewDir.resolve(name).toFile());
            int rightmost = 0;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 196; x++) {
                if ((pair.getRGB(x, y) >>> 24) != 0) rightmost = Math.max(rightmost, x);
            }
            int advance = rightmost + 2;
            headerFont.append(",\n{\"type\":\"bitmap\",\"file\":\"turtleroles:font/").append(name)
                .append("\",\"height\":16,\"ascent\":").append(row == 0 ? 9 : 0)
                .append(",\"chars\":[\"\\u").append(String.format("%04X", 0xE110 + row)).append("\"]}");
            headerFont.append(",\n{\"type\":\"space\",\"advances\":{\"\\u")
                .append(String.format("%04X", 0xE121 + row)).append("\":").append(188 - advance).append("}}");
        }
        BufferedImage mainRow = new BufferedImage(156, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mainGraphics = mainRow.createGraphics();
        Path[] mainSources = {Path.of(args[7]).resolve("ice.png"), Path.of(args[6]), Path.of(args[7]).resolve("time.png")};
        double[] mainAngles = {-60, -22, 15};
        for (int i = 0; i < 3; i++) {
            mainGraphics.drawImage(rotatedShock(ImageIO.read(mainSources[i].toFile()), 48, mainAngles[i]), 2 + i * 52, 0, null);
        }
        mainGraphics.dispose();
        ImageIO.write(mainRow, "png", textureDir.resolve("tab_main_row.png").toFile());
        ImageIO.write(scale(mainRow, 6), "png", previewDir.resolve("tab_main_row-6x.png").toFile());
        int lastPixel = 0;
        for (int y = 0; y < 48; y++) for (int x = 0; x < 156; x++) {
            if ((mainRow.getRGB(x, y) >>> 24) != 0) lastPixel = Math.max(lastPixel, x);
        }
        headerFont.append(",\n{\"type\":\"bitmap\",\"file\":\"turtleroles:font/tab_main_row.png\",\"height\":48,\"ascent\":47,\"chars\":[\"\\uE130\"]}");
        BufferedImage wordmark = trimVisible(ImageIO.read(Path.of(args[8]).toFile()), 24);
        // Each bitmap glyph must fit in the client's 256-pixel font atlas.
        int wordmarkHeight = Math.min(192, (int) Math.floor(240.0 * wordmark.getHeight() / wordmark.getWidth()));
        int wordmarkWidth = (int) Math.round(wordmark.getWidth() * (double) wordmarkHeight / wordmark.getHeight());
        BufferedImage titleTexture = new BufferedImage(wordmarkWidth, wordmarkHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D titleGraphics = titleTexture.createGraphics();
        titleGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        titleGraphics.drawImage(wordmark, 0, 0, wordmarkWidth, wordmarkHeight, null);
        titleGraphics.dispose();
        ImageIO.write(titleTexture, "png", textureDir.resolve("shock_smp_wordmark.png").toFile());
        headerFont.append(",\n{\"type\":\"bitmap\",\"file\":\"turtleroles:font/shock_smp_wordmark.png\",\"height\":48,\"ascent\":7,\"chars\":[\"\\uE140\"]}");
        headerFont.append(",\n{\"type\":\"space\",\"advances\":{\"\\uE120\":-8,\"\\uE131\":")
            .append(156 - (lastPixel + 2)).append("}}\n]}\n");
        Files.writeString(fontDir.resolve("header.json"), headerFont.toString(), StandardCharsets.UTF_8);
        Files.writeString(packDir.resolve("pack.mcmeta"), """
            {
              "pack": {
                "description": "TurtleRoles badges + Shock SMP custom item textures",
                "min_format": [75, 0],
                "max_format": [75, 0]
              }
            }
            """, StandardCharsets.UTF_8);

        Files.createDirectories(zip.getParent());
        zipDirectory(packDir, zip);
        Files.writeString(sha1, hex(sha1(Files.readAllBytes(zip))) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static void mergeBasePack(Path source, Path destination) throws IOException {
        Path root = destination.toAbsolutePath().normalize();
        try (java.util.zip.ZipFile archive = new java.util.zip.ZipFile(source.toFile())) {
            var entries = archive.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || name.equals("pack.mcmeta")) continue;
                Path target = root.resolve(name).normalize();
                if (!target.startsWith(root) || name.contains("\\") || name.startsWith("/")) {
                    throw new IOException("Unsafe pack path: " + name);
                }
                if (name.startsWith("assets/turtleroles/")) {
                    throw new IOException("Base pack conflicts with role assets: " + name);
                }
                Files.createDirectories(target.getParent());
                try (var input = archive.getInputStream(entry)) {
                    Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static BufferedImage prepareTabLogo(BufferedImage source) {
        BufferedImage trimmed = trimVisible(source, 24);
        // TabListService explicitly reserves three text lines above this glyph.
        // Bitmap metrics alone do not increase Minecraft's header line height.
        int targetHeight = 28;
        int targetWidth = Math.max(1, (int) Math.round((double) trimmed.getWidth() * targetHeight / trimmed.getHeight()));
        BufferedImage out = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = out.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.drawImage(trimmed, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return out;
    }

    private static BufferedImage rotatedShock(BufferedImage source, int size, double degrees) {
        BufferedImage cropped = trimVisible(source, 24);
        double angle = Math.toRadians(degrees);
        double c = Math.abs(Math.cos(angle)), s = Math.abs(Math.sin(angle));
        double width = cropped.getWidth() * c + cropped.getHeight() * s;
        double height = cropped.getWidth() * s + cropped.getHeight() * c;
        double scale = (size - 2.0) / Math.max(width, height);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.translate(size / 2.0, size / 2.0);
        g.rotate(angle);
        g.scale(scale, scale);
        g.translate(-cropped.getWidth() / 2.0, -cropped.getHeight() / 2.0);
        g.drawImage(cropped, 0, 0, null);
        g.dispose();
        return out;
    }

    private static BufferedImage trimVisible(BufferedImage image, int alphaThreshold) {
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) >= alphaThreshold) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("Logo contains no visible pixels.");
        }
        BufferedImage out = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = out.createGraphics();
        graphics.drawImage(image, 0, 0, out.getWidth(), out.getHeight(), minX, minY, maxX + 1, maxY + 1, null);
        graphics.dispose();
        return out;
    }

    private static BufferedImage drawBadge(RoleArt role) {
        // Draw on the actual eight-pixel GUI grid: no fractional downsampling.
        int iconWidth = role.turtle ? 8 : 0;
        int width = textWidth(role.label) + iconWidth + 4;
        BufferedImage image = new BufferedImage(width, 8, BufferedImage.TYPE_INT_ARGB);
        int background = switch (role.id) {
            case "co_owner" -> 0x159E72;
            case "sr_admin" -> 0x15171C;
            case "admin" -> 0xD32149;
            case "moderator" -> 0x168D42;
            case "helper" -> 0xFFE151;
            case "member" -> 0x697586;
            default -> 0x5BCEFA;
        };
        for (int y = 0; y < 8; y++) {
            for (int x = 1; x < width - 1; x++) {
                int color = role.id.equals("owner")
                    ? gradient(role.colors, (double) (x - 1) / (width - 3))
                    : background;
                image.setRGB(x, y, argb(color));
            }
        }
        if (role.turtle) {
            int light = argb(0xBAFFD7);
            rect(image, 3, 2, 4, 4, light);
            rect(image, 4, 3, 2, 2, argb(0x08734E));
            rect(image, 7, 3, 1, 2, light);
            rect(image, 3, 1, 1, 1, light);
            rect(image, 6, 1, 1, 1, light);
            rect(image, 3, 6, 1, 1, light);
            rect(image, 6, 6, 1, 1, light);
        }
        // Dark ink keeps the pastel flag and yellow helper badge readable.
        int ink = role.id.equals("owner") || role.id.equals("helper") ? 0x192337 : 0xFFFFFF;
        drawText(image, role.label, 2 + iconWidth, 1, argb(ink));
        return image;
    }
    private static boolean insidePlaque(int x, int y, int width) {
        // Square corners keep the badge aligned with Minecraft's rectangular
        // player-list rows.
        return true;
    }

    private static void drawBorder(BufferedImage image, int width, int[] colors) {
        int rim = shade(colors[Math.min(colors.length - 1, 1)], 1.45, 255);
        int dark = shade(colors[0], 0.36, 255);
        for (int x = 0; x < width; x++) {
            setIfInside(image, x, 0, rim);
            setIfInside(image, x, 15, dark);
        }
        for (int y = 0; y < 16; y++) {
            setIfInside(image, 0, y, rim);
            setIfInside(image, width - 1, y, dark);
        }
        for (int x = 1; x < width - 1; x++) {
            setIfInside(image, x, 1, shade(rim & 0xFFFFFF, 0.92, 255));
            setIfInside(image, x, 14, shade(dark & 0xFFFFFF, 0.92, 255));
        }
    }

    private static void setIfInside(BufferedImage image, int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight() && ((image.getRGB(x, y) >>> 24) != 0)) {
            image.setRGB(x, y, argb);
        }
    }

    private static void drawTurtle(BufferedImage image, int x, int y) {
        int shellDark = argb(0x164B35);
        int shell = argb(0x3E9161);
        int shellLight = argb(0xA8CA7B);
        int accent = argb(0xFFE477);
        rect(image, x + 3, y + 2, 6, 7, shellDark);
        rect(image, x + 4, y + 1, 4, 1, shellLight);
        rect(image, x + 4, y + 3, 4, 5, shell);
        rect(image, x + 5, y + 4, 2, 3, shellDark);
        rect(image, x + 4, y + 5, 4, 1, shellLight);
        rect(image, x + 5, y, 2, 1, accent);
        rect(image, x + 0, y + 4, 2, 3, accent);
        rect(image, x + 10, y + 4, 2, 3, accent);
        rect(image, x + 2, y + 9, 2, 2, accent);
        rect(image, x + 8, y + 9, 2, 2, accent);
    }

    private static void drawOutlinedText(BufferedImage image, String text, int x, int y) {
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                if (Math.abs(ox) + Math.abs(oy) <= 1) {
                    drawText(image, text, x + ox, y + oy, argb(0x050507));
                }
            }
        }
        drawText(image, text, x, y, argb(0xFFFFFF));
        drawText(image, text, x, y + 1, argb(0xECEFF4));
    }

    private static void drawText(BufferedImage image, String text, int x, int y, int color) {
        int cursor = x;
        for (char ch : text.toCharArray()) {
            String[] glyph = FONT.get(ch);
            if (glyph == null) {
                cursor += 3;
                continue;
            }
            for (int row = 0; row < glyph.length; row++) {
                for (int col = 0; col < glyph[row].length(); col++) {
                    if (glyph[row].charAt(col) == '1') {
                        int px = cursor + col;
                        int py = y + row;
                        if (px >= 0 && py >= 0 && px < image.getWidth() && py < image.getHeight()) {
                            image.setRGB(px, py, color);
                        }
                    }
                }
            }
            cursor += glyph[0].length() + 1;
        }
    }

    private static int textWidth(String text) {
        int width = 0;
        for (char ch : text.toCharArray()) {
            String[] glyph = FONT.get(ch);
            width += (glyph == null ? 2 : glyph[0].length()) + 1;
        }
        return Math.max(0, width - 1);
    }

    private static int gradient(int[] colors, double t) {
        if (colors.length == 1) {
            return colors[0];
        }
        double scaled = t * (colors.length - 1);
        int left = Math.min(colors.length - 2, (int) Math.floor(scaled));
        double local = scaled - left;
        return lerp(colors[left], colors[left + 1], local);
    }

    private static int lerp(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return ((int) Math.round(ar + (br - ar) * t) << 16)
            | ((int) Math.round(ag + (bg - ag) * t) << 8)
            | (int) Math.round(ab + (bb - ab) * t);
    }

    private static int shade(int rgb, double shade, int alpha) {
        int r = Math.max(0, Math.min(255, (int) (((rgb >> 16) & 0xFF) * shade)));
        int g = Math.max(0, Math.min(255, (int) (((rgb >> 8) & 0xFF) * shade)));
        int b = Math.max(0, Math.min(255, (int) ((rgb & 0xFF) * shade)));
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private static int argb(int rgb) {
        return 0xFF000000 | rgb;
    }

    private static void rect(BufferedImage image, int x, int y, int width, int height, int color) {
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                if (xx >= 0 && yy >= 0 && xx < image.getWidth() && yy < image.getHeight()) {
                    image.setRGB(xx, yy, color);
                }
            }
        }
    }

    private static BufferedImage trimTransparent(BufferedImage image) {
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        BufferedImage out = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(image, 0, 0, out.getWidth(), out.getHeight(), minX, minY, maxX + 1, maxY + 1, null);
        g.dispose();
        return out;
    }

    private static BufferedImage scale(BufferedImage image, int factor) {
        BufferedImage scaled = new BufferedImage(image.getWidth() * factor, image.getHeight() * factor, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage transparentMargin(BufferedImage image, int margin) {
        BufferedImage out = new BufferedImage(
            image.getWidth() + margin * 2,
            image.getHeight() + margin * 2,
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = out.createGraphics();
        g.drawImage(image, margin, margin, null);
        g.dispose();
        return out;
    }

    private static void zipDirectory(Path source, Path zip) throws IOException {
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            try (var files = Files.walk(source)) {
                for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                    String name = source.relativize(file).toString().replace('\\', '/');
                    ZipEntry entry = new ZipEntry(name);
                    entry.setTime(0L);
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private static byte[] sha1(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(String.format("%02x", b));
        }
        return out.toString();
    }

    private static Map<Character, String[]> font() {
        Map<Character, String[]> map = new LinkedHashMap<>();
        map.put('A', glyph("01110", "10001", "10001", "11111", "10001", "10001", "10001"));
        map.put('B', glyph("11110", "10001", "10001", "11110", "10001", "10001", "11110"));
        map.put('C', glyph("01111", "10000", "10000", "10000", "10000", "10000", "01111"));
        map.put('D', glyph("11110", "10001", "10001", "10001", "10001", "10001", "11110"));
        map.put('E', glyph("11111", "10000", "10000", "11110", "10000", "10000", "11111"));
        map.put('F', glyph("11111", "10000", "10000", "11110", "10000", "10000", "10000"));
        map.put('G', glyph("01111", "10000", "10000", "10111", "10001", "10001", "01111"));
        map.put('H', glyph("10001", "10001", "10001", "11111", "10001", "10001", "10001"));
        map.put('I', glyph("11111", "00100", "00100", "00100", "00100", "00100", "11111"));
        map.put('L', glyph("10000", "10000", "10000", "10000", "10000", "10000", "11111"));
        map.put('M', glyph("10001", "11011", "10101", "10101", "10001", "10001", "10001"));
        map.put('N', glyph("10001", "11001", "10101", "10011", "10001", "10001", "10001"));
        map.put('O', glyph("01110", "10001", "10001", "10001", "10001", "10001", "01110"));
        map.put('P', glyph("11110", "10001", "10001", "11110", "10000", "10000", "10000"));
        map.put('R', glyph("11110", "10001", "10001", "11110", "10100", "10010", "10001"));
        map.put('S', glyph("01111", "10000", "10000", "01110", "00001", "00001", "11110"));
        map.put('T', glyph("11111", "00100", "00100", "00100", "00100", "00100", "00100"));
        map.put('V', glyph("10001", "10001", "10001", "10001", "01010", "01010", "00100"));
        map.put('W', glyph("10001", "10001", "10001", "10101", "10101", "11011", "10001"));
        map.put('-', glyph("000", "000", "000", "111", "000", "000", "000"));
        map.put(' ', glyph("00", "00", "00", "00", "00", "00", "00"));
        map.put('A', glyph("01110","10001","11111","10001","10001"));
        map.put('B', glyph("11110","10001","11110","10001","11110"));
        map.put('C', glyph("01111","10000","10000","10000","01111"));
        map.put('D', glyph("11110","10001","10001","10001","11110"));
        map.put('E', glyph("11111","10000","11110","10000","11111"));
        map.put('H', glyph("10001","10001","11111","10001","10001"));
        map.put('I', glyph("111","010","010","010","111"));
        map.put('L', glyph("10000","10000","10000","10000","11111"));
        map.put('M', glyph("10001","11011","10101","10001","10001"));
        map.put('N', glyph("10001","11001","10101","10011","10001"));
        map.put('O', glyph("01110","10001","10001","10001","01110"));
        map.put('P', glyph("11110","10001","11110","10000","10000"));
        map.put('R', glyph("11110","10001","11110","10010","10001"));
        map.put('S', glyph("01111","10000","01110","00001","11110"));
        map.put('T', glyph("11111","00100","00100","00100","00100"));
        map.put('W', glyph("10001","10001","10101","11011","10001"));
        map.put('-', glyph("000","000","111","000","000"));
        map.put(' ', glyph("00","00","00","00","00"));        return map;
    }

    private static String[] glyph(String... rows) {
        return rows;
    }

    private record RoleArt(String id, String label, int codepoint, String texture, int[] colors, boolean turtle) {
    }
}

