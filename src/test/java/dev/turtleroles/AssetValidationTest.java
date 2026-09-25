package dev.turtleroles;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetValidationTest {
    private static final String[] TEXTURES = {"owner.png", "co_owner.png", "sr_admin.png", "admin.png", "moderator.png", "helper.png", "member.png"};

    @Test
    void generatedPngsAreTransparentReadableAndTightlyTrimmed() throws Exception {
        Path badgeDir = Path.of(System.getProperty("turtleroles.generatedBadgeDir"));
        for (String texture : TEXTURES) {
            BufferedImage image = ImageIO.read(badgeDir.resolve(texture).toFile());
            assertNotNull(image, texture);
            assertEquals(8, image.getHeight(), texture);
            assertTrue(image.getWidth() >= 25 && image.getWidth() <= 80, texture + " width");
            int transparent = 0;
            int opaque = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) == 0) {
                        transparent++;
                    } else {
                        opaque++;
                    }
                }
            }
            assertTrue(transparent > 0, texture + " has a transparent texture margin");
            assertTrue(opaque > 150, texture + " has visible pixels");
            assertTrue(!edgeHasOpaque(image, 0) && !edgeHasOpaque(image, image.getWidth() - 1), texture + " has transparent outer edges");
        }
    }

    @Test
    void packZipHasCorrectRootAndFontReferences() throws Exception {
        Path zipPath = Path.of(System.getProperty("turtleroles.generatedZip"));
        assertTrue(Files.size(zipPath) > 0);
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            assertNotNull(zip.getEntry("pack.mcmeta"));
            assertNotNull(zip.getEntry("assets/turtleroles/font/roles.json"));
            assertNotNull(zip.getEntry("assets/turtleroles/font/header.json"));
            assertNotNull(zip.getEntry("assets/turtleroles/textures/font/shock_smp_logo.png"));
            for (String texture : TEXTURES) {
                assertNotNull(zip.getEntry("assets/turtleroles/textures/font/" + texture), texture);
            }
        }
        String fontJson = Files.readString(Path.of(System.getProperty("turtleroles.generatedPackDir", "build/generated/turtleroles/resource-pack")).resolve("assets/turtleroles/font/roles.json"));
        for (String codepoint : Set.of("\\uE001", "\\uE002", "\\uE003", "\\uE004", "\\uE005", "\\uE006", "\\uE007")) {
            assertTrue(fontJson.contains(codepoint));
        }
    }

    @Test
    void tabLogoIsTransparentCrispAndMapped() throws Exception {
        Path pack = Path.of(System.getProperty("turtleroles.generatedPackDir"));
        BufferedImage logo = ImageIO.read(pack.resolve("assets/turtleroles/textures/font/shock_smp_logo.png").toFile());
        assertNotNull(logo);
        assertEquals(28, logo.getHeight());
        assertTrue(logo.getWidth() >= 10 && logo.getWidth() <= 20);
        int transparent = 0;
        int visible = 0;
        for (int y = 0; y < logo.getHeight(); y++) {
            for (int x = 0; x < logo.getWidth(); x++) {
                if ((logo.getRGB(x, y) >>> 24) == 0) transparent++; else visible++;
            }
        }
        assertTrue(transparent > visible);
        String mapping = Files.readString(pack.resolve("assets/turtleroles/font/header.json"));
        assertTrue(mapping.contains("\\uE100"));
        assertTrue(mapping.contains("shock_smp_logo.png"));
        assertTrue(mapping.contains("\"height\":28"));
        assertTrue(mapping.contains("\"ascent\":27"));
    }

    @Test
    void mergedPackPreservesEveryShockAssetExactly() throws Exception {
        try (ZipFile source = new ZipFile("design/packs/shockSMPpack-fixed.zip");
             ZipFile merged = new ZipFile(System.getProperty("turtleroles.generatedZip"))) {
            var entries = source.entries();
            int checked = 0;
            while (entries.hasMoreElements()) {
                var original = entries.nextElement();
                if (original.isDirectory() || original.getName().equals("pack.mcmeta")) continue;
                var result = merged.getEntry(original.getName());
                assertNotNull(result, original.getName());
                try (var before = source.getInputStream(original); var after = merged.getInputStream(result)) {
                    org.junit.jupiter.api.Assertions.assertArrayEquals(before.readAllBytes(), after.readAllBytes(), original.getName());
                }
                checked++;
            }
            assertTrue(checked >= 20);
            assertEquals(merged.size(), merged.stream().map(java.util.zip.ZipEntry::getName).distinct().count());
        }
    }

    @Test
    void headerTexturesFitClientGlyphAtlas() throws Exception {
        Path pack = Path.of(System.getProperty("turtleroles.generatedPackDir"));
        var yaml = new org.yaml.snakeyaml.Yaml();
        java.util.Map<String, Object> font = yaml.load(Files.readString(pack.resolve("assets/turtleroles/font/header.json")));
        var providers = (java.util.List<java.util.Map<String, Object>>) font.get("providers");
        boolean foundWordmark = false;
        for (var provider : providers) {
            if (!"bitmap".equals(provider.get("type"))) continue;
            String file = (String) provider.get("file");
            String[] resource = file.split(":", 2);
            BufferedImage texture = ImageIO.read(pack.resolve("assets/" + resource[0] + "/textures/" + resource[1]).toFile());
            assertNotNull(texture, file);
            var rows = (java.util.List<String>) provider.get("chars");
            int columns = rows.getFirst().codePointCount(0, rows.getFirst().length());
            assertTrue(texture.getWidth() / columns <= 256, file + " exceeds glyph atlas width");
            assertTrue(texture.getHeight() / rows.size() <= 256, file + " exceeds glyph atlas height");
            if (file.endsWith("shock_smp_wordmark.png")) {
                foundWordmark = true;
                assertEquals(48, provider.get("height"));
                assertEquals("\uE140", rows.getFirst());
                assertTrue(texture.getColorModel().hasAlpha());
            }
        }
        assertTrue(foundWordmark);
    }

    private boolean edgeHasOpaque(BufferedImage image, int x) {
        for (int y = 0; y < image.getHeight(); y++) {
            if ((image.getRGB(x, y) >>> 24) != 0) {
                return true;
            }
        }
        return false;
    }
}

